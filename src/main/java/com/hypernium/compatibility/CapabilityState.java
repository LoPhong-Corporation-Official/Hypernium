package com.hypernium.compatibility;

/** Who is responsible for a capability right now (plan section 15). */
public enum CapabilityState {
  /** Hypernium's own implementation is running. */
  ACTIVE,
  /** Another mod already provides it; Hypernium does not duplicate it. */
  PROVIDED_EXTERNALLY,
  /** Hypernium could provide it but it is turned off (config or compatibility mode). */
  DISABLED,
  /** Nobody provides it: Hypernium has no implementation (yet) and no other mod was found. */
  UNAVAILABLE,
  /** Hypernium's own implementation is running and marked experimental. */
  EXPERIMENTAL
}
