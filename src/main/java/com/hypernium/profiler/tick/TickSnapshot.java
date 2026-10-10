package com.hypernium.profiler.tick;

/** Duration summary of the most recent ticks of one tick loop. */
public record TickSnapshot(double avgMs, double maxMs, int samples) {

  public static final TickSnapshot EMPTY = new TickSnapshot(0, 0, 0);

  /** False until at least one tick completed, e.g. no integrated server in multiplayer. */
  public boolean isAvailable() {
    return samples > 0;
  }
}
