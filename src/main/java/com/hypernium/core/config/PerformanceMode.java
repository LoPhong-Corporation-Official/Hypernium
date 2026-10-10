package com.hypernium.core.config;

/** Feature tiers from the plan (section 46). Higher tiers include the risk of lower ones. */
public enum PerformanceMode {
  SAFE,
  BALANCED,
  AGGRESSIVE,
  EXPERIMENTAL
}
