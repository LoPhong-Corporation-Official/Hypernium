package com.hypernium.profiler;

/** Where frame boundaries come from. The mixin is exact; the HUD callback is a fallback. */
public enum FrameSource {
  NONE("none"),
  RENDER_LOOP("render loop hook"),
  HUD_FALLBACK("HUD fallback (no frames while HUD is hidden)");

  private final String label;

  FrameSource(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
