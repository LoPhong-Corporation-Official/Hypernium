package com.hypernium.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypernium.adaptive.controller.Action;
import com.hypernium.adaptive.controller.ControllerSettings;
import com.hypernium.adaptive.controller.Decision;
import com.hypernium.adaptive.controller.PerformanceController;
import com.hypernium.core.config.PerformanceMode;
import org.junit.jupiter.api.Test;

class PerformanceControllerTest {

  private static final long SEC = 1_000_000_000L;

  private static ControllerSettings settings(PerformanceMode mode) {
    // low 75, high 120, 5 s window, 15 s cooldown, like the plan's example.
    return new ControllerSettings(true, mode, 75, 120, 5 * SEC, 15 * SEC, false);
  }

  private static PerformanceController controller(PerformanceMode mode) {
    return new PerformanceController(settings(mode));
  }

  /** Feeds one snapshot per second with constant FPS; returns the last decision. */
  private static Decision feed(PerformanceController c, int from, int toInclusive, double fps) {
    Decision last = null;
    for (int t = from; t <= toInclusive; t++) {
      last = c.evaluate(Snapshots.at(t, fps), true, 0);
    }
    return last;
  }

  @Test
  void fpsBetweenThresholdsNeverChangesAnything() {
    var c = controller(PerformanceMode.BALANCED);
    feed(c, 1, 120, 90);
    assertTrue(c.policy().isNeutral());
  }

  @Test
  void degradesOnlyAfterTheFullWindowBelowTheThreshold() {
    var c = controller(PerformanceMode.BALANCED);
    assertEquals(Action.HOLD, feed(c, 1, 5, 40).action()); // 4 s below threshold so far
    assertTrue(c.policy().isNeutral());
    assertEquals(Action.DEGRADE_RENDER, c.evaluate(Snapshots.at(6, 40), true, 0).action());
    assertEquals(1, c.policy().renderLevel());
    assertEquals(0.9, c.policy().renderDistanceScale(), 1e-9);
  }

  @Test
  void flickeringAroundTheThresholdNeverTriggers() {
    var c = controller(PerformanceMode.BALANCED);
    for (int t = 1; t <= 200; t++) {
      c.evaluate(Snapshots.at(t, t % 2 == 0 ? 70 : 80), true, 0);
    }
    assertTrue(c.policy().isNeutral());
  }

  @Test
  void cooldownSpacesOutChanges() {
    var c = controller(PerformanceMode.BALANCED);
    feed(c, 1, 6, 40); // degrades at t=6
    assertEquals(1, c.policy().renderLevel());
    feed(c, 7, 20, 40); // low again, but still inside the 15 s cooldown
    assertEquals(1, c.policy().renderLevel());
    assertEquals(Action.DEGRADE_RENDER, c.evaluate(Snapshots.at(21, 40), true, 0).action());
    assertEquals(2, c.policy().renderLevel());
  }

  @Test
  void restoresAfterSustainedHeadroom() {
    var c = controller(PerformanceMode.BALANCED);
    feed(c, 1, 6, 40);
    assertEquals(1, c.policy().renderLevel());
    feed(c, 7, 30, 150); // headroom, cooldown over by t=21
    assertTrue(c.policy().isNeutral());
  }

  @Test
  void cpuBoundDegradesSimulationNotRendering() {
    var c = controller(PerformanceMode.BALANCED);
    Decision last = null;
    for (int t = 1; t <= 6; t++) {
      last = c.evaluate(Snapshots.build(t, 40, 2000, 4000, 0, 5, 55), true, 0);
    }
    assertEquals(Action.DEGRADE_SIMULATION, last.action());
    assertEquals(0, c.policy().renderLevel());
    assertEquals(1, c.policy().simLevel());
    assertEquals(0.8, c.policy().chunkSchedulingScale(), 1e-9);
  }

  @Test
  void safeModeDoesNotTouchSimulation() {
    var c = controller(PerformanceMode.SAFE);
    Decision last = null;
    for (int t = 1; t <= 30; t++) {
      last = c.evaluate(Snapshots.build(t, 40, 2000, 4000, 0, 5, 55), true, 0);
    }
    assertTrue(c.policy().isNeutral());
    assertTrue(last.reason().contains("not allowed"));
  }

  @Test
  void gcBoundHoldsBecauseLoweringQualityWouldNotHelp() {
    var c = controller(PerformanceMode.AGGRESSIVE);
    Decision last = null;
    for (int t = 1; t <= 120; t++) {
      last = c.evaluate(Snapshots.build(t, 40, 2000, 4000, 80, 0, 0), true, 0);
    }
    assertTrue(c.policy().isNeutral());
    assertTrue(last.reason().contains("gc-bound"));
  }

  @Test
  void renderLevelStopsAtTheModeLimit() {
    var c = controller(PerformanceMode.BALANCED);
    feed(c, 1, 400, 20);
    assertEquals(3, c.policy().renderLevel()); // BALANCED allows up to 3
  }

  @Test
  void leavingTheWorldResetsTheWindow() {
    var c = controller(PerformanceMode.BALANCED);
    feed(c, 1, 5, 40);
    c.evaluate(Snapshots.at(6, 40), false, 0); // back to the menu
    c.evaluate(Snapshots.at(7, 40), true, 0); // timer restarts here
    assertEquals(Action.HOLD, feed(c, 8, 11, 40).action());
    assertEquals(Action.DEGRADE_RENDER, c.evaluate(Snapshots.at(12, 40), true, 0).action());
  }

  @Test
  void aGapInSnapshotsRestartsTheWindow() {
    var c = controller(PerformanceMode.BALANCED);
    feed(c, 1, 5, 40);
    // Game paused / loading for ten seconds, then low FPS continues.
    assertEquals(Action.HOLD, c.evaluate(Snapshots.at(15, 40), true, 0).action());
    assertTrue(c.policy().isNeutral());
  }

  @Test
  void aKnownFpsCapIsNotTreatedAsPoorPerformance() {
    var c = controller(PerformanceMode.BALANCED);
    for (int t = 1; t <= 120; t++) {
      c.evaluate(Snapshots.at(t, 57), true, 60); // vsync at 60 Hz
    }
    assertTrue(c.policy().isNeutral());
  }

  @Test
  void cappedGameStillDegradesWhenItFallsWellBelowTheCap() {
    var c = controller(PerformanceMode.BALANCED);
    for (int t = 1; t <= 6; t++) {
      c.evaluate(Snapshots.at(t, 30), true, 60); // 30 FPS on a 60 FPS cap is a real problem
    }
    assertEquals(1, c.policy().renderLevel());
  }

  @Test
  void switchingToSafeModeClampsExistingLevels() {
    var c = controller(PerformanceMode.AGGRESSIVE);
    feed(c, 1, 400, 20);
    assertEquals(4, c.policy().renderLevel());
    c.configure(settings(PerformanceMode.SAFE));
    assertEquals(2, c.policy().renderLevel());
  }

  @Test
  void disabledControllerIsIdle() {
    var c =
        new PerformanceController(
            new ControllerSettings(false, PerformanceMode.BALANCED, 75, 120, 5 * SEC, 15 * SEC, false));
    assertEquals(Action.IDLE, feed(c, 1, 30, 20).action());
    assertTrue(c.policy().isNeutral());
  }
}
