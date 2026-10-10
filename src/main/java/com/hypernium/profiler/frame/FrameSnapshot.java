package com.hypernium.profiler.frame;

/**
 * Immutable summary of recent frame times. FPS values are derived from frame times, never
 * sampled separately, so averages and lows are consistent with each other.
 *
 * @param low1Fps FPS equivalent of the mean of the slowest 1% of frames
 * @param low01Fps FPS equivalent of the mean of the slowest 0.1% of frames
 * @param spikes frames slower than {@code spikeThresholdMs} in the window
 */
public record FrameSnapshot(
    int frames,
    double avgMs,
    double medianMs,
    double maxMs,
    double avgFps,
    double medianFps,
    double low1Fps,
    double low01Fps,
    int spikes,
    double spikeThresholdMs) {

  public static final FrameSnapshot EMPTY =
      new FrameSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

  public boolean isEmpty() {
    return frames == 0;
  }
}
