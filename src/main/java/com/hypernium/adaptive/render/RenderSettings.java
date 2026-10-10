package com.hypernium.adaptive.render;

/**
 * The video settings adaptive rendering is allowed to change. Implemented on top of vanilla
 * options in the client; faked in tests. Call from the client thread only.
 */
public interface RenderSettings {

  /** Render distance in chunks. */
  int renderDistance();

  void setRenderDistance(int chunks);

  /** The "Entity Distance" slider as a multiplier (1.0 = 100%). */
  double entityDistance();

  void setEntityDistance(double scale);

  /** 0 = All, 1 = Decreased, 2 = Minimal. */
  int particleLevel();

  void setParticleLevel(int level);
}
