package com.hypernium.profiler;

import com.hypernium.core.lifecycle.Subsystem;
import com.hypernium.profiler.frame.FrameStats;
import com.hypernium.profiler.memory.MemoryProfiler;
import com.hypernium.profiler.tick.TickTimer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central performance profiler (plan sections 8-14).
 *
 * <p>Threading: the frame methods must be called from the render thread. Tick timers are driven
 * by their own loops. {@link #latest()} can be read from any thread.
 *
 * <p>Hot-path rule: {@link #frameStart()}, {@link #frameEnd()} and {@link #hudFrame()} run every
 * frame and must not allocate or log. The once-per-second {@link #refresh(long)} may allocate.
 */
public final class HyperniumProfiler implements Subsystem {

  public static final String ID = "profiler";

  private static final long REFRESH_INTERVAL_NANOS = 1_000_000_000L;
  /** Rolling window of recent frames used for averages and 1% / 0.1% lows. */
  private static final int FRAME_WINDOW = 4096;

  private final FrameStats frames = new FrameStats(FRAME_WINDOW);
  private final FrameStats mainThread = new FrameStats(512);
  private final MemoryProfiler memory = new MemoryProfiler();
  private final TickTimer clientTick = new TickTimer();
  private final TickTimer serverTick = new TickTimer();
  private final AtomicInteger entities = new AtomicInteger();
  private final AtomicInteger chunks = new AtomicInteger();

  private volatile boolean enabled;
  private volatile FrameSource frameSource = FrameSource.NONE;
  private volatile ProfilerSnapshot latest = ProfilerSnapshot.EMPTY;

  // Render-thread only.
  private long lastFrameStart;
  private long iterationStart;
  private long lastRefresh;
  private int intervalFrames;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void start() {
    enabled = true;
    memory.sample(System.nanoTime()); // establish a baseline for the first interval
  }

  @Override
  public void stop() {
    enabled = false;
  }

  // ---- frame (render thread) ---------------------------------------------------------------

  /** Called at the start of every render-loop iteration (exact source). */
  public void frameStart() {
    if (!enabled) {
      return;
    }
    frameSource = FrameSource.RENDER_LOOP;
    long now = System.nanoTime();
    iterationStart = now;
    markFrame(now);
  }

  /** Called at the end of every render-loop iteration. */
  public void frameEnd() {
    if (!enabled || iterationStart == 0) {
      return;
    }
    mainThread.record(System.nanoTime() - iterationStart);
  }

  /** Fallback frame boundary from the HUD pass; ignored once the render-loop hook is live. */
  public void hudFrame() {
    if (!enabled || frameSource == FrameSource.RENDER_LOOP) {
      return;
    }
    frameSource = FrameSource.HUD_FALLBACK;
    markFrame(System.nanoTime());
  }

  private void markFrame(long now) {
    if (lastFrameStart != 0) {
      frames.record(now - lastFrameStart);
      intervalFrames++;
    }
    lastFrameStart = now;
    if (now - lastRefresh >= REFRESH_INTERVAL_NANOS) {
      refresh(now);
    }
  }

  private void refresh(long now) {
    double intervalSeconds = lastRefresh == 0 ? 0 : (now - lastRefresh) / 1_000_000_000.0;
    double recentFps = intervalSeconds > 0 ? intervalFrames / intervalSeconds : 0;
    intervalFrames = 0;
    lastRefresh = now;
    boolean exact = frameSource == FrameSource.RENDER_LOOP;
    double mainMs = exact && mainThread.size() > 0 ? mainThread.snapshot().avgMs() : Double.NaN;
    latest =
        new ProfilerSnapshot(
            frames.snapshot(),
            recentFps,
            now,
            mainMs,
            clientTick.snapshot(),
            serverTick.snapshot(),
            entities.get(),
            chunks.get(),
            memory.sample(now),
            frameSource);
  }

  // ---- ticks -------------------------------------------------------------------------------

  public void clientTickStart() {
    if (enabled) {
      clientTick.begin();
    }
  }

  public void clientTickEnd() {
    if (enabled) {
      clientTick.end();
    }
  }

  public void serverTickStart() {
    if (enabled) {
      serverTick.begin();
    }
  }

  public void serverTickEnd() {
    if (enabled) {
      serverTick.end();
    }
  }

  // ---- counters ----------------------------------------------------------------------------

  public void entityLoaded() {
    entities.incrementAndGet();
  }

  public void entityUnloaded() {
    entities.updateAndGet(v -> Math.max(0, v - 1));
  }

  public void chunkLoaded() {
    chunks.incrementAndGet();
  }

  public void chunkUnloaded() {
    chunks.updateAndGet(v -> Math.max(0, v - 1));
  }

  /** Call when joining or leaving a world: counters restart and old frames are discarded. */
  public void resetSession() {
    entities.set(0);
    chunks.set(0);
    clientTick.reset();
    serverTick.reset();
  }

  /** Render thread only. Drops frame history, e.g. before a benchmark. */
  public void clearFrameHistory() {
    frames.clear();
    mainThread.clear();
    lastFrameStart = 0;
    intervalFrames = 0;
  }

  public ProfilerSnapshot latest() {
    return latest;
  }
}
