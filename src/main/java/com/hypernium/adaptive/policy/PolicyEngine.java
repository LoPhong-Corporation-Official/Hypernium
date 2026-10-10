package com.hypernium.adaptive.policy;

import com.hypernium.core.config.PerformanceMode;

/**
 * Turns quality levels into concrete scales (plan section 51: "Policy Engine") and says how
 * deep each domain may go in each performance mode (plan section 46).
 *
 * <p>Two independent domains keep the controller from flip-flopping when the classification
 * changes: the render domain (render distance, entity distance, particles, LOD) and the
 * simulation domain (chunk scheduling, AI/entity ticking). Each has its own level.
 */
public final class PolicyEngine {

  private static final double[] RENDER_DISTANCE = {1.0, 0.9, 0.8, 0.7, 0.6};
  private static final double[] ENTITY_DISTANCE = {1.0, 0.85, 0.7, 0.55, 0.4};
  private static final double[] PARTICLES = {1.0, 0.8, 0.6, 0.4, 0.2};
  private static final double[] CHUNK_SCHEDULING = {1.0, 0.8, 0.6, 0.5};
  private static final double[] SIMULATION = {1.0, 1.0, 0.8, 0.6};

  private PolicyEngine() {}

  /** Deepest render level allowed. Safe still adapts rendering (plan: "Adaptive rendering"). */
  public static int maxRenderLevel(PerformanceMode mode) {
    return switch (mode) {
      case SAFE -> 2;
      case BALANCED -> 3;
      case AGGRESSIVE, EXPERIMENTAL -> 4;
    };
  }

  /** Deepest simulation level allowed. Safe never touches simulation. */
  public static int maxSimLevel(PerformanceMode mode) {
    return switch (mode) {
      case SAFE -> 0;
      case BALANCED -> 2;
      case AGGRESSIVE, EXPERIMENTAL -> 3;
    };
  }

  /**
   * @param entitySimulationAllowed true only when the user enabled entity.adaptiveTicking and the
   *     mode is Aggressive or higher; otherwise AI/entity tick work is never scaled down
   */
  public static AdaptivePolicy build(int renderLevel, int simLevel, boolean entitySimulationAllowed) {
    int r = clamp(renderLevel, RENDER_DISTANCE.length - 1);
    int s = clamp(simLevel, CHUNK_SCHEDULING.length - 1);
    return new AdaptivePolicy(
        r,
        s,
        RENDER_DISTANCE[r],
        ENTITY_DISTANCE[r],
        PARTICLES[r],
        r,
        CHUNK_SCHEDULING[s],
        entitySimulationAllowed ? SIMULATION[s] : 1.0);
  }

  private static int clamp(int level, int max) {
    return Math.max(0, Math.min(level, max));
  }
}
