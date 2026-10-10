package com.hypernium.profiler.tick;

/**
 * Times one tick loop (client tick, server tick, ...).
 *
 * <p>{@link #begin()} and {@link #end()} must be called from the thread that owns the loop. The
 * summary is published through volatile fields so any thread (e.g. the render thread) can read
 * {@link #snapshot()} without locking.
 */
public final class TickTimer {

  private static final double NANOS_PER_MS = 1_000_000.0;

  private final long[] ring;
  private int head;
  private int size;
  private long startNanos;
  private boolean running;

  private volatile TickSnapshot published = TickSnapshot.EMPTY;

  public TickTimer() {
    this(100);
  }

  public TickTimer(int window) {
    this.ring = new long[window];
  }

  public void begin() {
    startNanos = System.nanoTime();
    running = true;
  }

  public void end() {
    if (!running) {
      return;
    }
    running = false;
    record(System.nanoTime() - startNanos);
  }

  /** Visible for tests; normally driven by {@link #begin()}/{@link #end()}. */
  void record(long durationNanos) {
    ring[head] = durationNanos;
    head++;
    if (head == ring.length) {
      head = 0;
    }
    if (size < ring.length) {
      size++;
    }
    long sum = 0;
    long max = 0;
    for (int i = 0; i < size; i++) {
      sum += ring[i];
      max = Math.max(max, ring[i]);
    }
    published = new TickSnapshot(sum / (double) size / NANOS_PER_MS, max / NANOS_PER_MS, size);
  }

  public void reset() {
    head = 0;
    size = 0;
    running = false;
    published = TickSnapshot.EMPTY;
  }

  public TickSnapshot snapshot() {
    return published;
  }
}
