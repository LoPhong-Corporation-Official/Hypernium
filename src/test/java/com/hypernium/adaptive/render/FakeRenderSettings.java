package com.hypernium.adaptive.render;

/** In-memory settings that count how often Hypernium writes to them. */
final class FakeRenderSettings implements RenderSettings {

  int renderDistance = 16;
  double entityDistance = 1.0;
  int particleLevel = 0;
  int writes;
  boolean failOnWrite;

  @Override
  public int renderDistance() {
    return renderDistance;
  }

  @Override
  public void setRenderDistance(int chunks) {
    write();
    renderDistance = chunks;
  }

  @Override
  public double entityDistance() {
    return entityDistance;
  }

  @Override
  public void setEntityDistance(double scale) {
    write();
    entityDistance = scale;
  }

  @Override
  public int particleLevel() {
    return particleLevel;
  }

  @Override
  public void setParticleLevel(int level) {
    write();
    particleLevel = level;
  }

  private void write() {
    if (failOnWrite) {
      throw new IllegalStateException("settings unavailable");
    }
    writes++;
  }
}
