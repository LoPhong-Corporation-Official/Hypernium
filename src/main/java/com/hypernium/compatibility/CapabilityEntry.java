package com.hypernium.compatibility;

/**
 * Resolved state of one capability.
 *
 * @param provider who is responsible, e.g. "Sodium 0.6.13" or "Hypernium"
 * @param reason short explanation, empty when the state needs none
 */
public record CapabilityEntry(
    Capability capability, CapabilityState state, String provider, String reason) {

  public String describe() {
    StringBuilder sb = new StringBuilder(capability.label()).append(": ").append(state);
    if (!provider.isEmpty()) {
      sb.append(" (").append(provider).append(')');
    }
    if (!reason.isEmpty()) {
      sb.append(" - ").append(reason);
    }
    return sb.toString();
  }
}
