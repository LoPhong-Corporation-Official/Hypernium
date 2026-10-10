package com.hypernium.profiler.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * Samples heap, GC and allocation counters. Call {@link #sample(long)} from one thread, at a low
 * rate (about once per second). Allocation is measured for the calling thread, which is the
 * render thread in practice, since that is where frame-time hitches from allocation show up.
 */
public final class MemoryProfiler {

  private static final double BYTES_PER_MB = 1024.0 * 1024.0;
  private static final double NANOS_PER_SECOND = 1_000_000_000.0;

  private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
  private final List<GarbageCollectorMXBean> collectors =
      ManagementFactory.getGarbageCollectorMXBeans();
  private final com.sun.management.ThreadMXBean allocationBean = findAllocationBean();

  private long lastNanos;
  private long lastAllocBytes;
  private long lastGcMs;

  private static com.sun.management.ThreadMXBean findAllocationBean() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    if (bean instanceof com.sun.management.ThreadMXBean sun
        && sun.isThreadAllocatedMemorySupported()) {
      if (!sun.isThreadAllocatedMemoryEnabled()) {
        sun.setThreadAllocatedMemoryEnabled(true);
      }
      return sun;
    }
    return null;
  }

  public MemorySnapshot sample(long nowNanos) {
    long gcCount = 0;
    long gcMs = 0;
    for (GarbageCollectorMXBean collector : collectors) {
      long count = collector.getCollectionCount();
      long time = collector.getCollectionTime();
      if (count > 0) {
        gcCount += count;
      }
      if (time > 0) {
        gcMs += time;
      }
    }

    long allocBytes = allocationBean != null ? allocationBean.getCurrentThreadAllocatedBytes() : -1;

    double allocRate = Double.NaN;
    double gcMsPerSec = 0;
    if (lastNanos != 0) {
      double seconds = (nowNanos - lastNanos) / NANOS_PER_SECOND;
      if (seconds > 0) {
        if (allocBytes >= 0 && lastAllocBytes >= 0) {
          allocRate = (allocBytes - lastAllocBytes) / BYTES_PER_MB / seconds;
        }
        gcMsPerSec = (gcMs - lastGcMs) / seconds;
      }
    }
    lastNanos = nowNanos;
    lastAllocBytes = allocBytes;
    lastGcMs = gcMs;

    long used = memory.getHeapMemoryUsage().getUsed();
    long max = memory.getHeapMemoryUsage().getMax();
    return new MemorySnapshot(
        used / BYTES_PER_MB,
        max > 0 ? max / BYTES_PER_MB : 0,
        allocRate,
        Math.max(0, gcMsPerSec),
        gcCount,
        gcMs);
  }
}
