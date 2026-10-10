package com.hypernium.adaptive.classifier;

public record BottleneckResult(Bottleneck bottleneck, Confidence confidence, String reason) {

  public static final BottleneckResult UNKNOWN =
      new BottleneckResult(Bottleneck.UNKNOWN, Confidence.LOW, "no data yet");

  public String describe() {
    return bottleneck.label() + " (" + confidence.name().toLowerCase(java.util.Locale.ROOT)
        + " confidence): " + reason;
  }
}
