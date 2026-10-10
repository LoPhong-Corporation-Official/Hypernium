package com.hypernium.adaptive.classifier;

/** How directly the evidence supports a classification. */
public enum Confidence {
  /** A direct measurement crossed a clear threshold. */
  HIGH,
  /** A direct measurement is near its threshold. */
  MEDIUM,
  /** Inferred by elimination, not measured. */
  LOW
}
