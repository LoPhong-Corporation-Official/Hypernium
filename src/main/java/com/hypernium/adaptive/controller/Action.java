package com.hypernium.adaptive.controller;

/** What the controller did on one evaluation. */
public enum Action {
  /** Controller is off, or there is nothing to evaluate (not in a world, no data). */
  IDLE,
  /** Watching, no change needed or allowed yet. */
  HOLD,
  DEGRADE_RENDER,
  DEGRADE_SIMULATION,
  RESTORE_RENDER,
  RESTORE_SIMULATION
}
