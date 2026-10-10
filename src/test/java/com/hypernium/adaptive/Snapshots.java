package com.hypernium.adaptive;

import com.hypernium.profiler.FrameSource;
import com.hypernium.profiler.ProfilerSnapshot;
import com.hypernium.profiler.frame.FrameSnapshot;
import com.hypernium.profiler.memory.MemorySnapshot;
import com.hypernium.profiler.tick.TickSnapshot;

/** Builds synthetic profiler snapshots for controller tests. */
final class Snapshots {

  private Snapshots() {}

  static ProfilerSnapshot at(long second, double fps) {
    return build(second, fps, 2000, 4000, 0, 0, 0);
  }

  static ProfilerSnapshot build(
      long second,
      double fps,
      double usedMb,
      double maxMb,
      double gcMsPerSec,
      double clientTickMs,
      double serverTickMs) {
    double ms = 1000.0 / fps;
    FrameSnapshot frame = new FrameSnapshot(100, ms, ms, ms, fps, fps, fps, fps, 0, 20);
    return new ProfilerSnapshot(
        frame,
        fps,
        second * 1_000_000_000L,
        Double.NaN,
        clientTickMs > 0 ? new TickSnapshot(clientTickMs, clientTickMs, 20) : TickSnapshot.EMPTY,
        serverTickMs > 0 ? new TickSnapshot(serverTickMs, serverTickMs, 20) : TickSnapshot.EMPTY,
        0,
        0,
        new MemorySnapshot(usedMb, maxMb, Double.NaN, gcMsPerSec, 0, 0),
        FrameSource.RENDER_LOOP);
  }
}
