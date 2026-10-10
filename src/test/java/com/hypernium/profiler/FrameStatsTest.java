package com.hypernium.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypernium.profiler.frame.FrameSnapshot;
import com.hypernium.profiler.frame.FrameStats;
import org.junit.jupiter.api.Test;

class FrameStatsTest {

  private static final long MS = 1_000_000L;

  @Test
  void emptyWindowGivesEmptySnapshot() {
    assertTrue(new FrameStats(16).snapshot().isEmpty());
  }

  @Test
  void steadyFramesHaveEqualAverageAndLows() {
    FrameStats stats = new FrameStats(256);
    for (int i = 0; i < 200; i++) {
      stats.record(10 * MS); // 100 FPS
    }
    FrameSnapshot s = stats.snapshot();
    assertEquals(100.0, s.avgFps(), 0.001);
    assertEquals(100.0, s.low1Fps(), 0.001);
    assertEquals(100.0, s.low01Fps(), 0.001);
    assertEquals(0, s.spikes());
  }

  @Test
  void lowsReflectTheSlowestFrames() {
    FrameStats stats = new FrameStats(1000);
    for (int i = 0; i < 990; i++) {
      stats.record(10 * MS);
    }
    for (int i = 0; i < 10; i++) {
      stats.record(50 * MS); // worst 1%
    }
    FrameSnapshot s = stats.snapshot();
    assertEquals(20.0, s.low1Fps(), 0.001); // mean of the slowest 1% = 50 ms
    assertEquals(20.0, s.low01Fps(), 0.001); // the slowest frame is also 50 ms
    assertEquals(10, s.spikes());
    assertEquals(50.0, s.maxMs(), 0.001);
  }

  @Test
  void oldestFramesFallOutOfTheWindow() {
    FrameStats stats = new FrameStats(10);
    for (int i = 0; i < 10; i++) {
      stats.record(100 * MS);
    }
    for (int i = 0; i < 10; i++) {
      stats.record(5 * MS);
    }
    FrameSnapshot s = stats.snapshot();
    assertEquals(10, s.frames());
    assertEquals(5.0, s.maxMs(), 0.001);
  }
}
