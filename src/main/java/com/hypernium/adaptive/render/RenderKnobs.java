package com.hypernium.adaptive.render;

import com.hypernium.core.config.HyperniumConfig;

/** Which settings the user allows Hypernium to adapt. */
public record RenderKnobs(boolean renderDistance, boolean entityDistance, boolean particles) {

  public static RenderKnobs from(HyperniumConfig config) {
    return new RenderKnobs(
        config.rendering.adaptRenderDistance,
        config.rendering.adaptEntityDistance,
        config.rendering.adaptParticles);
  }
}
