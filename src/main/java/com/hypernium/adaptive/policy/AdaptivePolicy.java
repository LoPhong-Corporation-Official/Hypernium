package com.hypernium.adaptive.policy;

/**
 * The controller's output (plan section 18): how far each quality knob should be scaled.
 * 1.0 means "unchanged". Phase 3 only computes this; applying it to the game is Phase 4+.
 *
 * @param renderLevel 0 = full quality ... higher = more reduction (render domain)
 * @param simLevel 0 = no change ... higher = more reduction (simulation domain)
 * @param lodBias extra LOD steps, 0 = none
 * @param chunkSchedulingScale share of normal chunk-load work per tick
 * @param simulationScale share of normal AI/entity tick work; 1.0 unless explicitly allowed
 */
public record AdaptivePolicy(
    int renderLevel,
    int simLevel,
    double renderDistanceScale,
    double entityDistanceScale,
    double particleScale,
    int lodBias,
    double chunkSchedulingScale,
    double simulationScale) {

  public boolean isNeutral() {
    return renderLevel == 0 && simLevel == 0;
  }
}
