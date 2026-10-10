package com.hypernium.profiler.frame;

import java.util.Arrays;

/**
 * Fixed-size window of recent frame durations.
 *
 * <p>{@link #record(long)} is called once per frame and does not allocate. {@link #snapshot()}
 * sorts a copy of the window and allocates, so call it at a low rate (once per second), never
 * per frame. Not thread-safe: record and snapshot from the same thread.
 */
public final class FrameStats {

  private static final double NANOS_PER_SECOND = 1_000_000_000.0;
  private static final double NANOS_PER_MS = 1_000_000.0;
  /** A frame counts as a spike when it is both clearly above typical and clearly visible. */
  private static final double SPIKE_MEDIAN_FACTOR = 2.5;

  private static final long SPIKE_MIN_NANOS = 20_000_000L;

  private final long[] ring;
  private final long[] scratch;
  private int head;
  private int size;

  public FrameStats(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be >= 1");
    }
    this.ring = new long[capacity];
    this.scratch = new long[capacity];
  }

  /** Hot path: runs once per frame. Keep allocation-free. */
  public void record(long frameNanos) {
    ring[head] = frameNanos;
    head++;
    if (head == ring.length) {
      head = 0;
    }
    if (size < ring.length) {
      size++;
    }
  }

  public void clear() {
    head = 0;
    size = 0;
  }

  public int size() {
    return size;
  }

  public FrameSnapshot snapshot() {
    int n = size;
    if (n == 0) {
      return FrameSnapshot.EMPTY;
    }
    // Entries [0, n) are always valid: while the ring is filling, head == size.
    System.arraycopy(ring, 0, scratch, 0, n);
    Arrays.sort(scratch, 0, n);

    double sum = 0;
    for (int i = 0; i < n; i++) {
      sum += scratch[i];
    }
    double avgNs = sum / n;
    long medianNs = scratch[n / 2];
    long maxNs = scratch[n - 1];

    long spikeThresholdNs = Math.max((long) (medianNs * SPIKE_MEDIAN_FACTOR), SPIKE_MIN_NANOS);
    int spikes = 0;
    for (int i = n - 1; i >= 0 && scratch[i] > spikeThresholdNs; i--) {
      spikes++;
    }

    return new FrameSnapshot(
        n,
        avgNs / NANOS_PER_MS,
        medianNs / NANOS_PER_MS,
        maxNs / NANOS_PER_MS,
        toFps(avgNs),
        toFps(medianNs),
        toFps(meanOfSlowest(n, 0.01)),
        toFps(meanOfSlowest(n, 0.001)),
        spikes,
        spikeThresholdNs / NANOS_PER_MS);
  }

  /** Mean duration of the slowest {@code fraction} of frames (at least one frame). */
  private double meanOfSlowest(int n, double fraction) {
    int count = Math.max(1, (int) Math.ceil(n * fraction));
    double sum = 0;
    for (int i = n - count; i < n; i++) {
      sum += scratch[i];
    }
    return sum / count;
  }

  private static double toFps(double nanos) {
    return nanos <= 0 ? 0 : NANOS_PER_SECOND / nanos;
  }
}
