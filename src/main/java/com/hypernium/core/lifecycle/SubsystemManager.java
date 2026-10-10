package com.hypernium.core.lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Starts subsystems with the fallback policy from plan section 62: a failing subsystem is logged
 * and disabled, and the game keeps running on the vanilla (or other mod's) path.
 */
public final class SubsystemManager {

  private final List<Subsystem> running = new ArrayList<>();

  public synchronized void register(Subsystem subsystem) {
    try {
      subsystem.start();
      running.add(subsystem);
      HyperniumLog.info("Subsystem '{}' started", subsystem.id());
    } catch (Throwable t) {
      HyperniumLog.error("Subsystem '{}' failed to start and is disabled", subsystem.id(), t);
      stopQuietly(subsystem);
    }
  }

  /** Disables one subsystem at runtime, e.g. after it failed inside a hook. */
  public synchronized void disable(Subsystem subsystem, Throwable cause) {
    if (running.remove(subsystem)) {
      HyperniumLog.error("Subsystem '{}' disabled; falling back", subsystem.id(), cause);
      stopQuietly(subsystem);
    }
  }

  public synchronized void shutdown() {
    for (int i = running.size() - 1; i >= 0; i--) {
      stopQuietly(running.get(i));
    }
    running.clear();
  }

  public synchronized int runningCount() {
    return running.size();
  }

  private static void stopQuietly(Subsystem subsystem) {
    try {
      subsystem.stop();
    } catch (Throwable t) {
      HyperniumLog.warn("Subsystem '{}' did not stop cleanly", subsystem.id(), t);
    }
  }
}
