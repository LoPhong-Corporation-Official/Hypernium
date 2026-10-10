package com.hypernium.core.config;

/** How Hypernium behaves around other optimization mods (plan section 47). */
public enum CompatibilityMode {
  /** Detect other mods and avoid duplicating what they already provide. */
  AUTO,
  /** Ignore other mods; Hypernium provides its own fallbacks. */
  STANDALONE,
  /** Assume other optimization mods are present and only monitor/control. */
  INTEGRATION,
  /** Disable every feature that could conflict with other mods. */
  SAFE
}
