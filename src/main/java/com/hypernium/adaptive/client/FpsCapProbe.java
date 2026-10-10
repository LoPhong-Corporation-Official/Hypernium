package com.hypernium.adaptive.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * Finds the highest FPS the game can currently reach because of the frame limiter or vsync, so
 * the controller does not mistake a deliberate 60 FPS cap for poor performance.
 */
public final class FpsCapProbe {

  /** The options screen's "Unlimited" value. */
  private static final int UNLIMITED_FRAMERATE = 260;

  private static final int FALLBACK_REFRESH_RATE = 60;

  private FpsCapProbe() {}

  /** Client thread only. Returns 0 when there is no cap. */
  public static int current() {
    Options options = Minecraft.getInstance().options;
    int cap = Integer.MAX_VALUE;

    int limit = options.framerateLimit().get();
    if (limit < UNLIMITED_FRAMERATE) {
      cap = limit;
    }
    if (options.enableVsync().get()) {
      // Uses the primary monitor; a window on another display may differ. Capping the thresholds
      // too low only makes the controller more conservative, never more aggressive.
      GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
      cap = Math.min(cap, mode != null ? mode.refreshRate() : FALLBACK_REFRESH_RATE);
    }
    return cap == Integer.MAX_VALUE ? 0 : cap;
  }
}
