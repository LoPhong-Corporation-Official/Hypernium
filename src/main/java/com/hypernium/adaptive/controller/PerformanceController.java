package com.hypernium.adaptive.controller;

import com.hypernium.adaptive.classifier.Bottleneck;
import com.hypernium.adaptive.classifier.BottleneckClassifier;
import com.hypernium.adaptive.classifier.BottleneckResult;
import com.hypernium.adaptive.policy.AdaptivePolicy;
import com.hypernium.adaptive.policy.PolicyEngine;
import com.hypernium.profiler.ProfilerSnapshot;
import java.util.Locale;

/**
 * Metrics -> classifier -> policy (plan section 51), with hysteresis and cooldown (section 21).
 *
 * <p>Rules:
 *
 * <ul>
 *   <li>FPS below the low threshold for the whole window: degrade ONE step in the domain that the
 *       bottleneck points to. Render-bound or GPU-bound degrades rendering only, CPU-bound
 *       degrades simulation only. GC/memory/unknown trouble holds: lowering visual quality would
 *       not fix it.
 *   <li>FPS above the high threshold for the whole window: restore one step (rendering first).
 *   <li>FPS between the thresholds, or any change within the cooldown: hold.
 *   <li>Not in a world, no data, or a gap in snapshots (pause, loading): timers reset.
 * </ul>
 *
 * <p>The thresholds are lowered to fit a known FPS cap, so a 60 FPS vsync limit does not look like
 * permanent stress. Not thread-safe: call from one thread (the client thread).
 */
public final class PerformanceController {

  /** Snapshots arrive about once per second; a longer gap means a pause or loading screen. */
  private static final long MAX_SNAPSHOT_GAP_NANOS = 3_000_000_000L;

  private static final double CAP_LOW_FRACTION = 0.90;
  private static final double CAP_HIGH_FRACTION = 0.97;

  private final BottleneckClassifier classifier = new BottleneckClassifier();
  private ControllerSettings settings;

  private int renderLevel;
  private int simLevel;
  private AdaptivePolicy policy;

  private long lowSince = -1;
  private long highSince = -1;
  private boolean hasChanged;
  private long lastChangeNanos;
  private long lastSnapshotNanos;
  private Decision lastDecision;

  public PerformanceController(ControllerSettings settings) {
    this.settings = settings;
    this.policy = PolicyEngine.build(0, 0, settings.entitySimulationAllowed());
    this.lastDecision =
        new Decision(Action.IDLE, BottleneckResult.UNKNOWN, policy, "waiting for data");
  }

  /** Applies new settings (e.g. after a config reload) and clamps levels to the new mode. */
  public void configure(ControllerSettings newSettings) {
    this.settings = newSettings;
    this.renderLevel = Math.min(renderLevel, PolicyEngine.maxRenderLevel(newSettings.mode()));
    this.simLevel = Math.min(simLevel, PolicyEngine.maxSimLevel(newSettings.mode()));
    rebuildPolicy();
    resetTimers();
  }

  /** Back to full quality, clearing timers and cooldown. */
  public void reset() {
    renderLevel = 0;
    simLevel = 0;
    hasChanged = false;
    rebuildPolicy();
    resetTimers();
  }

  public AdaptivePolicy policy() {
    return policy;
  }

  public ControllerSettings settings() {
    return settings;
  }

  public Decision lastDecision() {
    return lastDecision;
  }

  /**
   * @param fpsCap known frame cap (limiter or vsync refresh rate) or 0 if unknown/unlimited
   */
  public Decision evaluate(ProfilerSnapshot snapshot, boolean inWorld, int fpsCap) {
    Decision decision = decide(snapshot, inWorld, fpsCap);
    lastDecision = decision;
    return decision;
  }

  private Decision decide(ProfilerSnapshot s, boolean inWorld, int fpsCap) {
    if (!settings.enabled()) {
      resetTimers();
      return idle("adaptive controller is off in config");
    }
    if (!inWorld || s.frame().isEmpty() || s.recentFps() <= 0) {
      resetTimers();
      return idle(inWorld ? "waiting for frame data" : "not in a world");
    }
    long now = s.capturedAtNanos();
    if (now == lastSnapshotNanos) {
      return lastDecision; // same snapshot as last time, nothing new to learn
    }
    if (lastSnapshotNanos != 0 && now - lastSnapshotNanos > MAX_SNAPSHOT_GAP_NANOS) {
      resetTimers();
    }
    lastSnapshotNanos = now;

    double effLow = settings.lowFps();
    double effHigh = settings.highFps();
    if (fpsCap > 0) {
      effLow = Math.min(effLow, CAP_LOW_FRACTION * fpsCap);
      effHigh = Math.min(effHigh, CAP_HIGH_FRACTION * fpsCap);
    }
    double fps = s.recentFps();
    boolean stressed = fps < effLow;
    boolean relaxed = fps > effHigh;
    BottleneckResult bottleneck = classifier.classify(s, true, stressed, Double.NaN);

    if (stressed) {
      highSince = -1;
      if (lowSince < 0) {
        lowSince = now;
      }
    } else if (relaxed) {
      lowSince = -1;
      if (highSince < 0) {
        highSince = now;
      }
    } else {
      lowSince = -1;
      highSince = -1;
    }

    boolean cooling = hasChanged && now - lastChangeNanos < settings.cooldownNanos();

    if (lowSince >= 0 && now - lowSince >= settings.windowNanos()) {
      if (cooling) {
        return hold(bottleneck, "low FPS, waiting for cooldown");
      }
      return degrade(bottleneck, now, fps, effLow);
    }
    if (highSince >= 0 && now - highSince >= settings.windowNanos()) {
      if (cooling) {
        return hold(bottleneck, "FPS has headroom, waiting for cooldown");
      }
      return restore(bottleneck, now);
    }
    return hold(bottleneck, stressed ? "FPS low, confirming it lasts" : relaxed ? "FPS high, confirming it lasts" : "FPS within target");
  }

  private Decision degrade(BottleneckResult b, long now, double fps, double effLow) {
    String why = String.format(Locale.ROOT, "FPS %.0f below %.0f", fps, effLow);
    Bottleneck kind = b.bottleneck();
    switch (kind) {
      case RENDER_BOUND, GPU_BOUND -> {
        if (renderLevel >= PolicyEngine.maxRenderLevel(settings.mode())) {
          return hold(b, why + "; render policy already at the " + settings.mode() + " limit");
        }
        renderLevel++;
        return changed(Action.DEGRADE_RENDER, b, now, why);
      }
      case CPU_BOUND -> {
        if (simLevel >= PolicyEngine.maxSimLevel(settings.mode())) {
          return hold(
              b,
              why
                  + "; "
                  + (PolicyEngine.maxSimLevel(settings.mode()) == 0
                      ? "simulation policies are not allowed in " + settings.mode() + " mode"
                      : "simulation policy already at the " + settings.mode() + " limit"));
        }
        simLevel++;
        return changed(Action.DEGRADE_SIMULATION, b, now, why);
      }
      default -> {
        return hold(b, why + "; lowering quality would not fix a " + kind.label() + " problem");
      }
    }
  }

  private Decision restore(BottleneckResult b, long now) {
    if (renderLevel > 0) {
      renderLevel--;
      return changed(Action.RESTORE_RENDER, b, now, "FPS has headroom");
    }
    if (simLevel > 0) {
      simLevel--;
      return changed(Action.RESTORE_SIMULATION, b, now, "FPS has headroom");
    }
    highSince = -1;
    return hold(b, "FPS has headroom, quality already full");
  }

  private Decision changed(Action action, BottleneckResult b, long now, String why) {
    hasChanged = true;
    lastChangeNanos = now;
    lowSince = -1;
    highSince = -1;
    rebuildPolicy();
    return new Decision(action, b, policy, why);
  }

  private Decision hold(BottleneckResult b, String why) {
    return new Decision(Action.HOLD, b, policy, why);
  }

  private Decision idle(String why) {
    return new Decision(Action.IDLE, BottleneckResult.UNKNOWN, policy, why);
  }

  private void rebuildPolicy() {
    policy = PolicyEngine.build(renderLevel, simLevel, settings.entitySimulationAllowed());
  }

  private void resetTimers() {
    lowSince = -1;
    highSince = -1;
    lastSnapshotNanos = 0;
  }
}
