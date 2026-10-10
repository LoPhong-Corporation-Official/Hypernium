package com.hypernium.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hypernium.adaptive.classifier.Bottleneck;
import com.hypernium.adaptive.classifier.BottleneckClassifier;
import com.hypernium.adaptive.classifier.Confidence;
import com.hypernium.profiler.ProfilerSnapshot;
import org.junit.jupiter.api.Test;

class BottleneckClassifierTest {

  private final BottleneckClassifier classifier = new BottleneckClassifier();

  private Bottleneck kind(ProfilerSnapshot s, boolean stressed) {
    return classifier.classify(s, true, stressed, Double.NaN).bottleneck();
  }

  @Test
  void healthyFpsIsNotABottleneck() {
    assertEquals(Bottleneck.NONE, kind(Snapshots.at(1, 100), false));
  }

  @Test
  void notInAWorldIsUnknown() {
    var r = classifier.classify(Snapshots.at(1, 30), false, true, Double.NaN);
    assertEquals(Bottleneck.UNKNOWN, r.bottleneck());
  }

  @Test
  void fullHeapIsMemoryBound() {
    assertEquals(
        Bottleneck.MEMORY_BOUND, kind(Snapshots.build(1, 30, 3800, 4000, 0, 0, 0), true));
  }

  @Test
  void heavyGcIsGcBoundWithConfidenceFromSize() {
    var high = classifier.classify(Snapshots.build(1, 30, 2000, 4000, 80, 0, 0), true, true, Double.NaN);
    var medium = classifier.classify(Snapshots.build(1, 30, 2000, 4000, 30, 0, 0), true, true, Double.NaN);
    assertEquals(Bottleneck.GC_BOUND, high.bottleneck());
    assertEquals(Confidence.HIGH, high.confidence());
    assertEquals(Confidence.MEDIUM, medium.confidence());
  }

  @Test
  void slowServerTickIsCpuBound() {
    var r = classifier.classify(Snapshots.build(1, 30, 2000, 4000, 0, 5, 55), true, true, Double.NaN);
    assertEquals(Bottleneck.CPU_BOUND, r.bottleneck());
    assertEquals(Confidence.HIGH, r.confidence());
  }

  @Test
  void slowFramesWithNoOtherEvidenceAreRenderBoundWithLowConfidence() {
    var r = classifier.classify(Snapshots.at(1, 30), true, true, Double.NaN);
    assertEquals(Bottleneck.RENDER_BOUND, r.bottleneck());
    assertEquals(Confidence.LOW, r.confidence());
  }

  @Test
  void gpuBoundOnlyWhenAGpuTimeIsProvided() {
    // 30 FPS = 33.3 ms per frame; 30 ms of GPU time is >= 85% of it.
    var r = classifier.classify(Snapshots.at(1, 30), true, true, 30.0);
    assertEquals(Bottleneck.GPU_BOUND, r.bottleneck());
    assertEquals(Bottleneck.RENDER_BOUND, kind(Snapshots.at(1, 30), true));
  }
}
