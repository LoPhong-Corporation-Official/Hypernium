package com.hypernium.profiler.memory;

/**
 * Memory and GC state at the last sample.
 *
 * @param allocMbPerSec allocation rate of the sampling (render) thread only; NaN if the JVM does
 *     not support per-thread allocation counters or no interval has elapsed yet
 * @param gcMsPerSec time spent in garbage collection per second during the last interval. For
 *     concurrent collectors this includes concurrent phases, so it is an upper bound on pauses.
 */
public record MemorySnapshot(
    double usedHeapMb,
    double maxHeapMb,
    double allocMbPerSec,
    double gcMsPerSec,
    long gcCountTotal,
    long gcTimeMsTotal) {

  public static final MemorySnapshot EMPTY =
      new MemorySnapshot(0, 0, Double.NaN, 0, 0, 0);

  public boolean hasAllocationRate() {
    return !Double.isNaN(allocMbPerSec);
  }
}
