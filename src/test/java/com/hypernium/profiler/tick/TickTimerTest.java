package com.hypernium.profiler.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TickTimerTest {

  @Test
  void unavailableUntilFirstTick() {
    assertFalse(new TickTimer().snapshot().isAvailable());
  }

  @Test
  void averagesAndTracksMaximum() {
    TickTimer timer = new TickTimer(4);
    timer.record(10_000_000L);
    timer.record(30_000_000L);
    TickSnapshot s = timer.snapshot();
    assertTrue(s.isAvailable());
    assertEquals(20.0, s.avgMs(), 0.001);
    assertEquals(30.0, s.maxMs(), 0.001);
  }
}
