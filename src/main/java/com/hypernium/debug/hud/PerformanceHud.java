package com.hypernium.debug.hud;

import com.hypernium.debug.ReportEnvironment;
import com.hypernium.profiler.HyperniumProfiler;
import com.hypernium.profiler.PerformanceReport;
import com.hypernium.profiler.ProfilerHooks;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** Compact performance overlay (plan section 44). Off by default; toggled with F8. */
public final class PerformanceHud {

  private static final int PADDING = 3;
  private static final int MARGIN = 4;
  private static final int BACKGROUND = 0x90000000;
  private static final int TEXT = 0xFFFFFFFF;

  private volatile boolean visible;

  // Text is rebuilt only when the profiler publishes a new snapshot (about once per second).
  private Object lastSnapshotKey;
  private List<String> cachedLines = List.of();

  public PerformanceHud(boolean initiallyVisible) {
    this.visible = initiallyVisible;
  }

  public void register() {
    HudElementRegistry.attachElementBefore(
        VanillaHudElements.CHAT,
        Identifier.fromNamespaceAndPath("hypernium", "performance_hud"),
        this::render);
  }

  public boolean toggle() {
    visible = !visible;
    return visible;
  }

  public void setVisible(boolean value) {
    visible = value;
  }

  public boolean isVisible() {
    return visible;
  }

  private void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    // Frame-boundary fallback; a no-op once the render-loop hook is delivering frames.
    ProfilerHooks.hudFrame();

    HyperniumProfiler profiler = ProfilerHooks.profiler();
    if (!visible || profiler == null) {
      return;
    }
    try {
      Object key = profiler.latest();
      if (key != lastSnapshotKey) {
        lastSnapshotKey = key;
        cachedLines = PerformanceReport.hud(profiler.latest(), ReportEnvironment.current());
      }
      draw(graphics, cachedLines);
    } catch (Throwable t) {
      // A broken overlay must never take the game down; hide it and keep playing.
      visible = false;
    }
  }

  private static void draw(GuiGraphics graphics, List<String> lines) {
    Font font = Minecraft.getInstance().font;
    int lineHeight = font.lineHeight + 1;
    int width = 0;
    for (String line : lines) {
      width = Math.max(width, font.width(line));
    }
    int x = MARGIN;
    int y = MARGIN;
    graphics.fill(
        x - PADDING,
        y - PADDING,
        x + width + PADDING,
        y + lines.size() * lineHeight + PADDING - 1,
        BACKGROUND);
    for (int i = 0; i < lines.size(); i++) {
      graphics.drawString(font, lines.get(i), x, y + i * lineHeight, TEXT);
    }
  }
}
