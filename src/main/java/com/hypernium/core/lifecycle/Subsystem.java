package com.hypernium.core.lifecycle;

/** A self-contained Hypernium feature that can be switched off without affecting the game. */
public interface Subsystem {

  /** Stable identifier used in logs and (later) in config/commands. */
  String id();

  /** Called once at startup. Throwing disables only this subsystem. */
  void start();

  /** Called on shutdown or when the subsystem is disabled after a failure. */
  default void stop() {}
}
