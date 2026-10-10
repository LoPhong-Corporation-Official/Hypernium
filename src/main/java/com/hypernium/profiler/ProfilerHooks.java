package com.hypernium.profiler;

import com.hypernium.Hypernium;

/**
 * Static entry points called from mixins and event callbacks. Each one is exception-safe: if the
 * profiler throws, it is disabled and the game carries on (plan section 62).
 */
public final class ProfilerHooks {

  private static volatile HyperniumProfiler active;

  private ProfilerHooks() {}

  public static void activate(HyperniumProfiler profiler) {
    active = profiler;
  }

  public static HyperniumProfiler profiler() {
    return active;
  }

  // The methods below run every frame or tick: no allocation, no logging.

  public static void frameStart() {
    HyperniumProfiler p = active;
    if (p == null) {
      return;
    }
    try {
      p.frameStart();
    } catch (Throwable t) {
      fail(p, t);
    }
  }

  public static void frameEnd() {
    HyperniumProfiler p = active;
    if (p == null) {
      return;
    }
    try {
      p.frameEnd();
    } catch (Throwable t) {
      fail(p, t);
    }
  }

  public static void hudFrame() {
    HyperniumProfiler p = active;
    if (p == null) {
      return;
    }
    try {
      p.hudFrame();
    } catch (Throwable t) {
      fail(p, t);
    }
  }

  private static void fail(HyperniumProfiler p, Throwable t) {
    active = null;
    Hypernium.subsystems().disable(p, t);
  }
}
