package com.hypernium.adaptive.classifier;

/** What is currently limiting performance (plan section 19). */
public enum Bottleneck {
  /** FPS is within target; nothing to fix. */
  NONE("none"),
  /** Not enough data to say (no frames yet, or not in a world). */
  UNKNOWN("unknown"),
  /** Slow frames with no sign of simulation, GC or memory trouble. CPU vs GPU is undetermined. */
  RENDER_BOUND("render-bound"),
  /** Game tick (simulation) is taking most of its 50 ms budget. */
  CPU_BOUND("cpu-bound"),
  /** Needs a GPU timer, which the profiler does not have yet; never reported until then. */
  GPU_BOUND("gpu-bound"),
  /** Heap is nearly full. */
  MEMORY_BOUND("memory-bound"),
  /** Garbage collection is eating a visible share of each second. */
  GC_BOUND("gc-bound"),
  /** Reserved: needs chunk build/upload timings (later phase). Never reported yet. */
  CHUNK_BOUND("chunk-bound"),
  /** Reserved: needs IO timings. Never reported yet. */
  IO_BOUND("io-bound");

  private final String label;

  Bottleneck(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
