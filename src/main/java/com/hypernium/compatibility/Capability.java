package com.hypernium.compatibility;

import java.util.List;

/**
 * A performance feature that either Hypernium or another mod can provide.
 *
 * <p>{@link #defaultImplementation()} says what Hypernium itself ships today. Update it as each
 * phase lands; the registry and report follow automatically.
 */
public enum Capability {
  CHUNK_RENDERING("Chunk rendering", Implementation.PLANNED, 9, true, OptimizationMod.SODIUM),
  CHUNK_MESHING("Chunk meshing", Implementation.PLANNED, 9, true, OptimizationMod.SODIUM),
  ENTITY_CULLING("Entity culling", Implementation.PLANNED, 6, true, OptimizationMod.ENTITYCULLING),
  AI_OPTIMIZATION("AI optimization", Implementation.PLANNED, 6, true, OptimizationMod.LITHIUM),
  MEMORY_OPTIMIZATION(
      "Memory optimization",
      Implementation.PLANNED,
      7,
      true,
      OptimizationMod.FERRITECORE,
      OptimizationMod.MODERNFIX),
  PROFILER("Profiler", Implementation.READY, 1, false),
  ADAPTIVE_CONTROL("Adaptive controller", Implementation.READY, 3, false),
  ADAPTIVE_RENDERING("Adaptive rendering", Implementation.READY, 4, false),
  PREDICTIVE_LOADING("Predictive chunk loading", Implementation.PLANNED, 5, false),
  BENCHMARK("Benchmark", Implementation.PLANNED, 11, false);

  /** How far along Hypernium's own implementation of a capability is. */
  public enum Implementation {
    READY,
    EXPERIMENTAL,
    PLANNED
  }

  private final String label;
  private final Implementation defaultImplementation;
  private final int plannedPhase;
  private final boolean conflictRisk;
  private final List<OptimizationMod> providers;

  Capability(
      String label,
      Implementation defaultImplementation,
      int plannedPhase,
      boolean conflictRisk,
      OptimizationMod... providers) {
    this.label = label;
    this.defaultImplementation = defaultImplementation;
    this.plannedPhase = plannedPhase;
    this.conflictRisk = conflictRisk;
    this.providers = List.of(providers);
  }

  public String label() {
    return label;
  }

  public Implementation defaultImplementation() {
    return defaultImplementation;
  }

  public int plannedPhase() {
    return plannedPhase;
  }

  /**
   * True if running Hypernium's version next to another mod's version of the same feature could
   * conflict. SAFE mode switches these off; observing-only features (profiler, controller,
   * benchmark) are never affected.
   */
  public boolean conflictRisk() {
    return conflictRisk;
  }

  /** Other mods that can provide this capability, in order of preference. */
  public List<OptimizationMod> providers() {
    return providers;
  }
}
