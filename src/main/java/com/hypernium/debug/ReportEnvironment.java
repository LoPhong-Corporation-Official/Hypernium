package com.hypernium.debug;

import com.hypernium.Hypernium;
import com.hypernium.adaptive.controller.AdaptiveService;
import com.hypernium.compatibility.CapabilityRegistry;
import com.hypernium.core.config.HyperniumConfig;
import com.hypernium.profiler.PerformanceReport;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

/** Builds the report header (versions, modes, detected mods, capabilities) from runtime state. */
public final class ReportEnvironment {

  private ReportEnvironment() {}

  public static PerformanceReport.Environment current() {
    FabricLoader loader = FabricLoader.getInstance();
    HyperniumConfig config = Hypernium.config();

    String minecraft =
        loader
            .getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    String hypernium =
        loader
            .getModContainer(Hypernium.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    CapabilityRegistry capabilities = Hypernium.capabilities();
    AdaptiveService adaptive = Hypernium.adaptive();
    return new PerformanceReport.Environment(
        minecraft,
        hypernium,
        config.performanceMode().name().toLowerCase(Locale.ROOT),
        config.compatibilityMode().name().toLowerCase(Locale.ROOT),
        capabilities.detected().summary(),
        capabilities.describe(),
        capabilities.warnings(),
        adaptive != null ? adaptive.summary() : "adaptive off",
        adaptive != null ? adaptive.details() : List.of("Adaptive controller: not running"));
  }
}
