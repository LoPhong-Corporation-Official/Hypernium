package com.hypernium.profiler;

import com.hypernium.profiler.frame.FrameSnapshot;
import com.hypernium.profiler.memory.MemorySnapshot;
import com.hypernium.profiler.tick.TickSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats profiler data as text for the report command and the HUD. Anything that is not
 * measured is printed as "unavailable"; values are never estimated or faked (plan section 13).
 */
public final class PerformanceReport {

  private PerformanceReport() {}

  /**
   * @param optimizationMods one entry per known mod, e.g. "Sodium 0.6.13" or "Lithium (not found)"
   * @param capabilities one line per capability from the compatibility registry
   * @param warnings compatibility warnings, empty when none
   * @param adaptiveSummary short controller state for the HUD, e.g. "R1 S0 render-bound"
   * @param adaptiveDetails full controller state for the report
   */
  public record Environment(
      String minecraftVersion,
      String hyperniumVersion,
      String performanceMode,
      String compatibilityMode,
      List<String> optimizationMods,
      List<String> capabilities,
      List<String> warnings,
      String adaptiveSummary,
      List<String> adaptiveDetails) {}

  public static List<String> full(ProfilerSnapshot s, Environment env) {
    List<String> out = new ArrayList<>();
    out.add("Hypernium Performance Report");
    out.add("Minecraft " + env.minecraftVersion() + " | Hypernium " + env.hyperniumVersion());
    out.add("Mode " + env.performanceMode() + " | Compatibility " + env.compatibilityMode());
    out.add("Other mods: " + String.join(", ", env.optimizationMods()));
    out.add("Capabilities:");
    for (String line : env.capabilities()) {
      out.add("  " + line);
    }
    for (String warning : env.warnings()) {
      out.add("Warning: " + warning);
    }

    FrameSnapshot f = s.frame();
    if (f.isEmpty()) {
      out.add("Frames: no data yet (wait a second, or the frame source is not active)");
    } else {
      out.add(
          fmt(
              "FPS %.0f avg | %.0f median | 1%% low %.0f | 0.1%% low %.0f",
              f.avgFps(), f.medianFps(), f.low1Fps(), f.low01Fps()));
      out.add(
          fmt(
              "Frame time %.1f ms avg | %.1f ms median | %.1f ms worst (%d frames)",
              f.avgMs(), f.medianMs(), f.maxMs(), f.frames()));
      out.add(
          fmt(
              "Spikes %d frame(s) over %.0f ms in the window",
              f.spikes(), f.spikeThresholdMs()));
    }
    out.add("Frame source: " + s.frameSource().label());
    out.add(
        Double.isNaN(s.mainThreadMs())
            ? "Main thread: unavailable (needs the render-loop hook)"
            : fmt("Main thread %.1f ms/frame (includes vsync/present wait)", s.mainThreadMs()));
    out.add("GPU frame time: unavailable (no GPU timer queries yet)");
    out.add("Draw calls / vertices: unavailable (renderer metrics arrive with later phases)");

    out.add("Client tick " + tick(s.clientTick()));
    out.add("Server tick " + tick(s.serverTick()) + (s.serverTick().isAvailable() ? "" : " (no integrated server?)"));
    out.add("Entities loaded " + s.entities() + " | Chunks loaded " + s.chunks());
    out.add("Chunk build/upload times: unavailable (arrive with the Phase 3 hooks)");

    MemorySnapshot m = s.memory();
    out.add(fmt("Heap %.0f / %.0f MB", m.usedHeapMb(), m.maxHeapMb()));
    out.add(
        m.hasAllocationRate()
            ? fmt("Allocation %.1f MB/s (render thread)", m.allocMbPerSec())
            : "Allocation: unavailable");
    out.add(
        fmt(
            "GC %.1f ms/s | %d collections | %d ms total",
            m.gcMsPerSec(), m.gcCountTotal(), m.gcTimeMsTotal()));
    out.addAll(env.adaptiveDetails());
    return out;
  }

  /** Compact lines for the HUD (plan section 44). */
  public static List<String> hud(ProfilerSnapshot s, Environment env) {
    List<String> out = new ArrayList<>();
    out.add("Hypernium");
    FrameSnapshot f = s.frame();
    if (f.isEmpty()) {
      out.add("collecting frames...");
    } else {
      out.add(fmt("FPS %.0f  1%% %.0f  0.1%% %.0f", f.avgFps(), f.low1Fps(), f.low01Fps()));
      out.add(fmt("Frame %.1f ms  worst %.1f ms", f.avgMs(), f.maxMs()));
    }
    if (!Double.isNaN(s.mainThreadMs())) {
      out.add(fmt("Main %.1f ms", s.mainThreadMs()));
    }
    out.add("GPU n/a");
    out.add("Tick " + tick(s.clientTick()) + " / srv " + tick(s.serverTick()));
    out.add("Chunks " + s.chunks() + "  Entities " + s.entities());
    MemorySnapshot m = s.memory();
    out.add(
        fmt("GC %.1f ms/s  Alloc ", m.gcMsPerSec())
            + (m.hasAllocationRate() ? fmt("%.0f MB/s", m.allocMbPerSec()) : "n/a"));
    out.add("Mode " + env.performanceMode() + "  " + env.adaptiveSummary());
    return out;
  }

  /** One line for {@code /hypernium stats}. */
  public static String oneLine(ProfilerSnapshot s) {
    FrameSnapshot f = s.frame();
    if (f.isEmpty()) {
      return "Hypernium: no frame data yet";
    }
    return fmt(
        "FPS %.0f | 1%% low %.0f | 0.1%% low %.0f | frame %.1f ms | %d spikes | heap %.0f MB",
        f.avgFps(), f.low1Fps(), f.low01Fps(), f.avgMs(), f.spikes(), s.memory().usedHeapMb());
  }

  private static String tick(TickSnapshot t) {
    return t.isAvailable() ? fmt("%.1f ms (max %.1f)", t.avgMs(), t.maxMs()) : "n/a";
  }

  private static String fmt(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }
}
