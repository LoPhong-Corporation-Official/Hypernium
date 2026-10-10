package com.hypernium.adaptive.render;

/** The player's own values for the settings Hypernium has lowered; null = not touched. */
public record Baseline(Integer renderDistance, Double entityDistance, Integer particleLevel) {

  public boolean isEmpty() {
    return renderDistance == null && entityDistance == null && particleLevel == null;
  }
}
