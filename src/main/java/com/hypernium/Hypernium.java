package com.hypernium;

import com.hypernium.adaptive.controller.AdaptiveService;
import com.hypernium.compatibility.CapabilityRegistry;
import com.hypernium.compatibility.detector.DetectedMods;
import com.hypernium.compatibility.detector.FabricModPresence;
import com.hypernium.core.config.ConfigManager;
import com.hypernium.core.config.HyperniumConfig;
import com.hypernium.core.lifecycle.HyperniumLog;
import com.hypernium.core.lifecycle.SubsystemManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Common entrypoint. Runs on both client and dedicated server. */
public final class Hypernium implements ModInitializer {

  public static final String MOD_ID = "hypernium";

  private static final ConfigManager CONFIG = new ConfigManager();
  private static final SubsystemManager SUBSYSTEMS = new SubsystemManager();

  private static volatile CapabilityRegistry capabilities;
  private static volatile AdaptiveService adaptive;

  public static HyperniumConfig config() {
    return CONFIG.get();
  }

  public static ConfigManager configManager() {
    return CONFIG;
  }

  public static SubsystemManager subsystems() {
    return SUBSYSTEMS;
  }

  /** The adaptive controller service, or null when it is not running (client only). */
  public static AdaptiveService adaptive() {
    return adaptive;
  }

  public static void setAdaptive(AdaptiveService service) {
    adaptive = service;
  }

  /** Who provides what, given the installed mods and the current config. Never null after init. */
  public static CapabilityRegistry capabilities() {
    return capabilities;
  }

  /**
   * Re-detects mods and re-resolves capabilities. Called at startup and after a config reload.
   * Existing subsystems are not started or stopped by this; later phases consult the registry
   * when they initialize.
   */
  public static CapabilityRegistry refreshCapabilities() {
    CapabilityRegistry registry =
        CapabilityRegistry.resolve(CONFIG.get(), DetectedMods.detect(new FabricModPresence()));
    capabilities = registry;
    return registry;
  }

  @Override
  public void onInitialize() {
    CONFIG.load();
    HyperniumConfig config = CONFIG.get();

    String version =
        FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    HyperniumLog.info("Hypernium {} initializing", version);
    HyperniumLog.info(
        "Performance mode: {}, compatibility mode: {}",
        config.performanceMode(),
        config.compatibilityMode());

    CapabilityRegistry registry = refreshCapabilities();
    for (String line : registry.describe()) {
      HyperniumLog.info("Capability {}", line);
    }
    for (String warning : registry.warnings()) {
      HyperniumLog.warn("{}", warning);
    }

    ServerLifecycleEvents.SERVER_STOPPED.register(server -> SUBSYSTEMS.shutdown());
    HyperniumLog.info("Hypernium initialized ({} subsystems running)", SUBSYSTEMS.runningCount());
  }
}
