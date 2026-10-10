package com.hypernium.adaptive.controller;

import com.hypernium.adaptive.classifier.BottleneckResult;
import com.hypernium.adaptive.policy.AdaptivePolicy;
import com.hypernium.adaptive.policy.PolicyEngine;
import com.hypernium.core.config.HyperniumConfig;
import com.hypernium.core.lifecycle.HyperniumLog;
import com.hypernium.core.lifecycle.Subsystem;
import com.hypernium.profiler.HyperniumProfiler;
import com.hypernium.profiler.ProfilerSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Runs the {@link PerformanceController} against the profiler once per new snapshot (about once a
 * second) and tells listeners when the policy changes.
 *
 * <p>The service only decides. Listeners (adaptive rendering since Phase 4) apply the policy to the
 * game; with no listener the game is not modified.
 */
public final class AdaptiveService implements Subsystem {

  public static final String ID = "adaptive";

  private final HyperniumProfiler profiler;
  private final PerformanceController controller;
  private final BooleanSupplier inWorld;
  private final IntSupplier fpsCap;
  private final List<Consumer<AdaptivePolicy>> listeners = new CopyOnWriteArrayList<>();

  private final List<Supplier<List<String>>> detailProviders = new CopyOnWriteArrayList<>();

  private ProfilerSnapshot lastSeen;

  public AdaptiveService(
      HyperniumProfiler profiler,
      PerformanceController controller,
      BooleanSupplier inWorld,
      IntSupplier fpsCap) {
    this.profiler = profiler;
    this.controller = controller;
    this.inWorld = inWorld;
    this.fpsCap = fpsCap;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    listeners.clear();
  }

  public void addListener(Consumer<AdaptivePolicy> listener) {
    listeners.add(listener);
  }

  /** Lets a policy consumer (e.g. the renderer) add its own lines to {@link #details()}. */
  public void addDetailsProvider(Supplier<List<String>> provider) {
    detailProviders.add(provider);
  }

  /** Call every client tick; does real work only when the profiler published a new snapshot. */
  public void tick() {
    ProfilerSnapshot snapshot = profiler.latest();
    if (snapshot == lastSeen) {
      return;
    }
    lastSeen = snapshot;
    Decision decision = controller.evaluate(snapshot, inWorld.getAsBoolean(), fpsCap.getAsInt());
    if (decision.changedPolicy()) {
      AdaptivePolicy policy = decision.policy();
      HyperniumLog.info(
          "Adaptive {}: render level {}, simulation level {} ({})",
          decision.action(),
          policy.renderLevel(),
          policy.simLevel(),
          decision.bottleneck().describe());
      for (Consumer<AdaptivePolicy> listener : listeners) {
        listener.accept(policy);
      }
    }
  }

  public void reconfigure(HyperniumConfig config) {
    controller.configure(ControllerSettings.from(config));
    notifyListeners();
  }

  public void reset() {
    controller.reset();
    notifyListeners();
  }

  private void notifyListeners() {
    AdaptivePolicy policy = controller.policy();
    for (Consumer<AdaptivePolicy> listener : listeners) {
      listener.accept(policy);
    }
  }

  public AdaptivePolicy policy() {
    return controller.policy();
  }

  /** Short form for the HUD, e.g. "R1 S0 render-bound". */
  public String summary() {
    Decision d = controller.lastDecision();
    return "R"
        + d.policy().renderLevel()
        + " S"
        + d.policy().simLevel()
        + " "
        + d.bottleneck().bottleneck().label();
  }

  /** Full state for {@code /hypernium adaptive} and the report. */
  public List<String> details() {
    Decision d = controller.lastDecision();
    AdaptivePolicy p = d.policy();
    ControllerSettings settings = controller.settings();
    BottleneckResult b = d.bottleneck();
    List<String> out = new ArrayList<>();
    out.add(
        String.format(
            Locale.ROOT,
            "Adaptive controller (%s mode): render level %d/%d, simulation level %d/%d",
            settings.mode().name().toLowerCase(Locale.ROOT),
            p.renderLevel(),
            PolicyEngine.maxRenderLevel(settings.mode()),
            p.simLevel(),
            PolicyEngine.maxSimLevel(settings.mode())));
    out.add(
        String.format(
            Locale.ROOT,
            "Policy: render distance x%.2f, entity distance x%.2f, particles x%.2f, LOD +%d, "
                + "chunk scheduling x%.2f, simulation x%.2f",
            p.renderDistanceScale(),
            p.entityDistanceScale(),
            p.particleScale(),
            p.lodBias(),
            p.chunkSchedulingScale(),
            p.simulationScale()));
    out.add("Bottleneck: " + b.describe());
    out.add("Last decision: " + d.action() + " - " + d.reason());
    if (detailProviders.isEmpty()) {
      out.add("Policies are computed but nothing applies them (adaptive rendering is not running).");
    }
    for (Supplier<List<String>> provider : detailProviders) {
      out.addAll(provider.get());
    }
    return out;
  }
}
