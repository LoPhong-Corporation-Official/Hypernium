package com.hypernium.compatibility;

/** Optimization mods Hypernium knows how to coexist with (plan section 50). */
public enum OptimizationMod {
  SODIUM("Sodium", "sodium"),
  LITHIUM("Lithium", "lithium"),
  FERRITECORE("FerriteCore", "ferritecore"),
  MODERNFIX("ModernFix", "modernfix"),
  ENTITYCULLING("EntityCulling", "entityculling");

  private final String displayName;
  private final String modId;

  OptimizationMod(String displayName, String modId) {
    this.displayName = displayName;
    this.modId = modId;
  }

  public String displayName() {
    return displayName;
  }

  /** Fabric mod id. */
  public String modId() {
    return modId;
  }
}
