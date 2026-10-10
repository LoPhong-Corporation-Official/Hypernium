package com.hypernium.adaptive.render;

import com.hypernium.adaptive.policy.AdaptivePolicy;
import com.hypernium.core.lifecycle.HyperniumLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Applies the render half of an {@link AdaptivePolicy} to the player's video settings
 * (plan section 52): entity distance, particle density and, if allowed, render distance.
 *
 * <p>Rules that keep it from fighting the player:
 *
 * <ul>
 *   <li>It remembers the player's own value (the baseline) before lowering anything, and never
 *       raises a setting above that baseline.
 *   <li>If a setting no longer holds the value Hypernium last wrote, the player (or another mod)
 *       changed it: the new value becomes the baseline and is respected.
 *   <li>When the policy is neutral again everything goes back to the baseline and Hypernium lets
 *       go of the settings completely.
 *   <li>Baselines are mirrored to disk while lowered so a crash cannot leave them reduced.
 * </ul>
 *
 * <p>LOD: vanilla has no LOD to control, so {@code lodBias} is reported but not applied until a
 * backend that supports it exists. Client thread only. Never throws.
 */
public final class AdaptiveRenderer implements Consumer<AdaptivePolicy> {

  static final int MIN_RENDER_DISTANCE = 5;
  static final double MIN_ENTITY_DISTANCE = 0.5;
  private static final double ENTITY_TOLERANCE = 0.005;
  private static final double PARTICLES_ALL_FROM = 0.7;
  private static final double PARTICLES_DECREASED_FROM = 0.3;

  private final RenderSettings settings;
  private final RenderKnobs knobs;
  private final BaselineStore store;

  private Integer renderDistanceBase;
  private Integer renderDistanceApplied;
  private Double entityDistanceBase;
  private Double entityDistanceApplied;
  private Integer particleBase;
  private Integer particleApplied;
  private int lodBias;

  public AdaptiveRenderer(RenderSettings settings, RenderKnobs knobs, BaselineStore store) {
    this.settings = settings;
    this.knobs = knobs;
    this.store = store;
  }

  /** Call once the client has loaded its options: undoes a previous session that crashed. */
  public void recoverFromCrash() {
    Optional<Baseline> saved = store.load();
    if (saved.isEmpty()) {
      return;
    }
    try {
      Baseline b = saved.get();
      if (b.renderDistance() != null) {
        settings.setRenderDistance(b.renderDistance());
      }
      if (b.entityDistance() != null) {
        settings.setEntityDistance(b.entityDistance());
      }
      if (b.particleLevel() != null) {
        settings.setParticleLevel(b.particleLevel());
      }
      HyperniumLog.info("Restored video settings left lowered by a previous session");
    } catch (RuntimeException e) {
      HyperniumLog.warn("Could not restore saved video settings: {}", e.toString());
    }
    store.delete();
  }

  @Override
  public void accept(AdaptivePolicy policy) {
    try {
      apply(policy);
    } catch (RuntimeException e) {
      HyperniumLog.warn("Adaptive rendering could not apply a policy: {}", e.toString());
    }
  }

  private void apply(AdaptivePolicy policy) {
    boolean neutral = policy.isNeutral();
    if (neutral && !holding()) {
      lodBias = 0;
      return;
    }
    lodBias = policy.lodBias();
    if (knobs.renderDistance()) {
      applyRenderDistance(policy.renderDistanceScale());
    }
    if (knobs.entityDistance()) {
      applyEntityDistance(policy.entityDistanceScale());
    }
    if (knobs.particles()) {
      applyParticles(policy.particleScale());
    }
    if (neutral) {
      release();
    }
    persist();
  }

  private void applyRenderDistance(double scale) {
    int current = settings.renderDistance();
    if (renderDistanceApplied != null && current != renderDistanceApplied) {
      renderDistanceBase = current; // the player changed it
    } else if (renderDistanceBase == null) {
      renderDistanceBase = current;
    }
    int base = renderDistanceBase;
    int target =
        scale >= 1.0
            ? base
            : Math.min(base, Math.max(MIN_RENDER_DISTANCE, (int) Math.round(base * scale)));
    if (target != current) {
      settings.setRenderDistance(target);
    }
    renderDistanceApplied = target;
  }

  private void applyEntityDistance(double scale) {
    double current = settings.entityDistance();
    if (entityDistanceApplied != null && Math.abs(current - entityDistanceApplied) > ENTITY_TOLERANCE) {
      entityDistanceBase = current;
    } else if (entityDistanceBase == null) {
      entityDistanceBase = current;
    }
    double base = entityDistanceBase;
    double target =
        scale >= 1.0
            ? base
            : Math.min(base, Math.max(MIN_ENTITY_DISTANCE, Math.round(base * scale * 100) / 100.0));
    if (Math.abs(target - current) > ENTITY_TOLERANCE) {
      settings.setEntityDistance(target);
    }
    entityDistanceApplied = target;
  }

  private void applyParticles(double scale) {
    int current = settings.particleLevel();
    if (particleApplied != null && current != particleApplied) {
      particleBase = current;
    } else if (particleBase == null) {
      particleBase = current;
    }
    int base = particleBase;
    int target = scale >= 1.0 ? base : Math.max(base, particleLevelFor(scale));
    if (target != current) {
      settings.setParticleLevel(target);
    }
    particleApplied = target;
  }

  /** Particle density scale to vanilla level: All, Decreased or Minimal. */
  static int particleLevelFor(double scale) {
    if (scale >= PARTICLES_ALL_FROM) {
      return 0;
    }
    return scale >= PARTICLES_DECREASED_FROM ? 1 : 2;
  }

  private boolean holding() {
    return renderDistanceBase != null || entityDistanceBase != null || particleBase != null;
  }

  private void release() {
    renderDistanceBase = null;
    renderDistanceApplied = null;
    entityDistanceBase = null;
    entityDistanceApplied = null;
    particleBase = null;
    particleApplied = null;
  }

  private void persist() {
    if (holding()) {
      store.save(new Baseline(renderDistanceBase, entityDistanceBase, particleBase));
    } else {
      store.delete();
    }
  }

  /** Lines for {@code /hypernium adaptive}. */
  public List<String> status() {
    List<String> out = new ArrayList<>();
    out.add("Adaptive rendering backend: vanilla video options");
    out.add(
        "Render distance: "
            + knobStatus(
                knobs.renderDistance(),
                renderDistanceBase,
                renderDistanceApplied,
                "off in config (reloads chunks when changed)",
                " chunks"));
    out.add(
        "Entity distance: "
            + knobStatus(
                knobs.entityDistance(),
                entityDistanceBase == null ? null : Math.round(entityDistanceBase * 100),
                entityDistanceApplied == null ? null : Math.round(entityDistanceApplied * 100),
                "off in config",
                "%"));
    out.add(
        "Particles: "
            + knobStatus(
                knobs.particles(),
                particleBase == null ? null : particleName(particleBase),
                particleApplied == null ? null : particleName(particleApplied),
                "off in config",
                ""));
    out.add(
        lodBias > 0
            ? String.format(
                Locale.ROOT, "LOD: +%d wanted, not applied (no LOD backend is installed)", lodBias)
            : "LOD: no backend (vanilla has none); nothing to apply");
    return out;
  }

  private static String knobStatus(
      boolean enabled, Object base, Object applied, String offReason, String unit) {
    if (!enabled) {
      return offReason;
    }
    if (applied == null) {
      return "unchanged";
    }
    return base.equals(applied) ? "at your setting (" + base + unit + ")" : applied + unit + " (your setting: " + base + unit + ")";
  }

  private static String particleName(int level) {
    return switch (level) {
      case 0 -> "All";
      case 1 -> "Decreased";
      default -> "Minimal";
    };
  }
}
