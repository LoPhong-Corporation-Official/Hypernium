package com.hypernium.adaptive.controller;

import com.hypernium.core.config.HyperniumConfig;
import com.hypernium.core.config.PerformanceMode;

/** Immutable controller tuning, derived from the config (plan sections 21, 45, 46). */
public record ControllerSettings(
    boolean enabled,
    PerformanceMode mode,
    int lowFps,
    int highFps,
    long windowNanos,
    long cooldownNanos,
    boolean entitySimulationAllowed) {

  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  public static ControllerSettings from(HyperniumConfig config) {
    PerformanceMode mode = config.performanceMode();
    boolean aggressive = mode == PerformanceMode.AGGRESSIVE || mode == PerformanceMode.EXPERIMENTAL;
    return new ControllerSettings(
        config.adaptive.enabled,
        mode,
        config.adaptive.lowFpsThreshold,
        config.adaptive.highFpsThreshold,
        config.adaptive.hysteresisSeconds * NANOS_PER_SECOND,
        config.adaptive.cooldownSeconds * NANOS_PER_SECOND,
        aggressive && config.entity.adaptiveTicking);
  }
}
