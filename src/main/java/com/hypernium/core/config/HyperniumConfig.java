package com.hypernium.core.config;

import com.hypernium.core.lifecycle.HyperniumLog;
import java.util.Locale;

/**
 * Plain data model for {@code config/hypernium.json} (plan section 45).
 *
 * <p>Enums are stored as lowercase strings so the file stays human-friendly; use the typed
 * accessors instead of reading the strings directly. Defaults are deliberately conservative:
 * everything experimental is off.
 */
public final class HyperniumConfig {

  public Profiler profiler = new Profiler();
  public Adaptive adaptive = new Adaptive();
  public Rendering rendering = new Rendering();
  public Compatibility compatibility = new Compatibility();
  public Chunk chunk = new Chunk();
  public Entity entity = new Entity();
  public Experimental experimental = new Experimental();

  public static final class Profiler {
    public boolean enabled = true;
    public boolean hud = false;
  }

  public static final class Adaptive {
    public boolean enabled = true;
    public String mode = "balanced";
    /** Degrade quality when FPS stays below this for {@link #hysteresisSeconds}. */
    public int lowFpsThreshold = 75;
    /** Restore quality when FPS stays above this for {@link #hysteresisSeconds}. */
    public int highFpsThreshold = 120;
    public int hysteresisSeconds = 5;
    /** Minimum time between two quality changes. */
    public int cooldownSeconds = 15;
  }

  /** Which vanilla video options adaptive rendering may lower while performance is poor. */
  public static final class Rendering {
    public boolean enabled = true;
    /** Off by default: changing render distance makes the game reload chunks, which can hitch. */
    public boolean adaptRenderDistance = false;
    public boolean adaptEntityDistance = true;
    public boolean adaptParticles = true;
  }

  public static final class Compatibility {
    public String mode = "auto";
  }

  public static final class Chunk {
    public boolean predictiveLoading = true;
  }

  public static final class Entity {
    // Plan section 54: the entity tick scheduler is OFF by default.
    public boolean adaptiveTicking = false;
  }

  public static final class Experimental {
    public boolean customRenderer = false;
    public boolean greedyMeshing = false;
    public boolean occlusionCulling = false;
  }

  public PerformanceMode performanceMode() {
    return parse(PerformanceMode.class, adaptive.mode, PerformanceMode.BALANCED, "adaptive.mode");
  }

  public CompatibilityMode compatibilityMode() {
    return parse(
        CompatibilityMode.class,
        compatibility.mode,
        CompatibilityMode.AUTO,
        "compatibility.mode");
  }

  /** Replaces missing sections/invalid values with defaults so callers never see nulls. */
  void normalize() {
    if (profiler == null) profiler = new Profiler();
    if (adaptive == null) adaptive = new Adaptive();
    if (rendering == null) rendering = new Rendering();
    if (compatibility == null) compatibility = new Compatibility();
    if (chunk == null) chunk = new Chunk();
    if (entity == null) entity = new Entity();
    if (experimental == null) experimental = new Experimental();
    adaptive.mode = performanceMode().name().toLowerCase(Locale.ROOT);
    adaptive.lowFpsThreshold = Math.max(10, adaptive.lowFpsThreshold);
    adaptive.highFpsThreshold = Math.max(adaptive.lowFpsThreshold + 10, adaptive.highFpsThreshold);
    adaptive.hysteresisSeconds = Math.max(1, adaptive.hysteresisSeconds);
    adaptive.cooldownSeconds = Math.max(0, adaptive.cooldownSeconds);
    compatibility.mode = compatibilityMode().name().toLowerCase(Locale.ROOT);
  }

  private static <E extends Enum<E>> E parse(Class<E> type, String raw, E fallback, String key) {
    if (raw == null) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      HyperniumLog.warn("Invalid value '{}' for {}; using {}", raw, key, fallback);
      return fallback;
    }
  }
}
