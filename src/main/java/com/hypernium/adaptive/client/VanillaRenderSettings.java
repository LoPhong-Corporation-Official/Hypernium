package com.hypernium.adaptive.client;

import com.hypernium.adaptive.render.RenderSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/** {@link RenderSettings} backed by the vanilla video options. Client thread only. */
public final class VanillaRenderSettings implements RenderSettings {

  private static Options options() {
    return Minecraft.getInstance().options;
  }

  @Override
  public int renderDistance() {
    return options().renderDistance().get();
  }

  @Override
  public void setRenderDistance(int chunks) {
    options().renderDistance().set(chunks);
  }

  @Override
  public double entityDistance() {
    return options().entityDistanceScaling().get();
  }

  @Override
  public void setEntityDistance(double scale) {
    options().entityDistanceScaling().set(scale);
  }

  /** Relies on the enum order All, Decreased, Minimal. */
  @Override
  public int particleLevel() {
    return options().particles().get().ordinal();
  }

  @Override
  public void setParticleLevel(int level) {
    ParticleStatus[] all = ParticleStatus.values();
    options().particles().set(all[Math.max(0, Math.min(level, all.length - 1))]);
  }
}
