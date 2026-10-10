package com.hypernium.profiler;

import com.hypernium.profiler.frame.FrameSnapshot;
import com.hypernium.profiler.memory.MemorySnapshot;
import com.hypernium.profiler.tick.TickSnapshot;

/**
 * Everything the profiler knows at one moment, refreshed once per second on the render thread.
 *
 * @param mainThreadMs average main-thread time of a frame, from the start to the end of the
 *     render-loop iteration. It includes waiting for buffer swap/vsync, so it is NOT a pure CPU
 *     cost and cannot be compared with GPU time. It is NaN when the render-loop hook is absent.
 * @param recentFps FPS over the last refresh interval only (about one second), so it reacts
 *     quickly, unlike the averages in {@code frame} which cover thousands of frames
 * @param capturedAtNanos {@link System#nanoTime()} when this snapshot was taken
 * @param entities client entities currently loaded
 * @param chunks client chunks currently loaded
 */
public record ProfilerSnapshot(
    FrameSnapshot frame,
    double recentFps,
    long capturedAtNanos,
    double mainThreadMs,
    TickSnapshot clientTick,
    TickSnapshot serverTick,
    int entities,
    int chunks,
    MemorySnapshot memory,
    FrameSource frameSource) {

  public static final ProfilerSnapshot EMPTY =
      new ProfilerSnapshot(
          FrameSnapshot.EMPTY,
          0,
          0,
          Double.NaN,
          TickSnapshot.EMPTY,
          TickSnapshot.EMPTY,
          0,
          0,
          MemorySnapshot.EMPTY,
          FrameSource.NONE);
}
