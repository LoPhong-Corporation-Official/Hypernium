package com.hypernium.adaptive.classifier;

import com.hypernium.profiler.ProfilerSnapshot;
import java.util.Locale;

/**
 * Names the most likely bottleneck from what the profiler actually measures.
 *
 * <p>Honest limits: without a GPU timer, a slow frame with no other evidence is reported as
 * {@link Bottleneck#RENDER_BOUND} at LOW confidence. It is never labelled GPU- or CPU-bound by
 * guesswork. Only measured tick time, GC time and heap fill produce HIGH/MEDIUM results.
 */
public final class BottleneckClassifier {

  static final double HEAP_FULL_FRACTION = 0.92;
  static final double GC_HIGH_MS_PER_SEC = 50;
  static final double GC_MEDIUM_MS_PER_SEC = 25;
  static final double TICK_BUDGET_MS = 50;
  static final double TICK_HIGH_MS = 50;
  static final double TICK_MEDIUM_MS = 40;
  static final double GPU_BOUND_FRACTION = 0.85;

  /**
   * @param stressed true when FPS is below the controller's low threshold
   * @param gpuFrameMs GPU time per frame, or NaN when unavailable (always NaN for now)
   */
  public BottleneckResult classify(ProfilerSnapshot s, boolean inWorld, boolean stressed, double gpuFrameMs) {
    if (!inWorld || s.frame().isEmpty() || s.recentFps() <= 0) {
      return BottleneckResult.UNKNOWN;
    }
    if (!stressed) {
      return new BottleneckResult(Bottleneck.NONE, Confidence.HIGH, "FPS is within target");
    }

    var mem = s.memory();
    if (mem.maxHeapMb() > 0 && mem.usedHeapMb() / mem.maxHeapMb() >= HEAP_FULL_FRACTION) {
      return new BottleneckResult(
          Bottleneck.MEMORY_BOUND,
          Confidence.HIGH,
          String.format(
              Locale.ROOT, "heap %.0f of %.0f MB used", mem.usedHeapMb(), mem.maxHeapMb()));
    }
    if (mem.gcMsPerSec() >= GC_MEDIUM_MS_PER_SEC) {
      return new BottleneckResult(
          Bottleneck.GC_BOUND,
          mem.gcMsPerSec() >= GC_HIGH_MS_PER_SEC ? Confidence.HIGH : Confidence.MEDIUM,
          String.format(Locale.ROOT, "GC took %.0f ms of the last second", mem.gcMsPerSec()));
    }

    double clientTick = s.clientTick().isAvailable() ? s.clientTick().avgMs() : 0;
    double serverTick = s.serverTick().isAvailable() ? s.serverTick().avgMs() : 0;
    double worstTick = Math.max(clientTick, serverTick);
    if (worstTick >= TICK_MEDIUM_MS) {
      String which = serverTick >= clientTick ? "server" : "client";
      return new BottleneckResult(
          Bottleneck.CPU_BOUND,
          worstTick >= TICK_HIGH_MS ? Confidence.HIGH : Confidence.MEDIUM,
          String.format(
              Locale.ROOT, "%s tick averages %.0f ms of a %.0f ms budget", which, worstTick, TICK_BUDGET_MS));
    }

    if (!Double.isNaN(gpuFrameMs) && gpuFrameMs >= GPU_BOUND_FRACTION * s.frame().avgMs()) {
      return new BottleneckResult(
          Bottleneck.GPU_BOUND,
          Confidence.HIGH,
          String.format(Locale.ROOT, "GPU needs %.1f ms of a %.1f ms frame", gpuFrameMs, s.frame().avgMs()));
    }

    return new BottleneckResult(
        Bottleneck.RENDER_BOUND,
        Confidence.LOW,
        "slow frames, no tick/GC/memory pressure found; CPU vs GPU split needs a GPU timer");
  }
}
