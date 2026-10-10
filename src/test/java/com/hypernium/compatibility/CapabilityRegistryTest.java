package com.hypernium.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypernium.compatibility.Capability.Implementation;
import com.hypernium.compatibility.detector.DetectedMods;
import com.hypernium.compatibility.detector.ModPresence;
import com.hypernium.core.config.CompatibilityMode;
import com.hypernium.core.config.HyperniumConfig;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CapabilityRegistryTest {

  private static final Map<OptimizationMod, String> SODIUM =
      Map.of(OptimizationMod.SODIUM, "0.6.13");

  /** Registry where Hypernium's own chunk renderer exists and the user switched it on. */
  private static CapabilityRegistry withExperimentalRenderer(
      CompatibilityMode mode, DetectedMods mods, boolean enabledInConfig) {
    var enabled = EnumSet.allOf(Capability.class);
    if (!enabledInConfig) {
      enabled.remove(Capability.CHUNK_RENDERING);
    }
    return CapabilityRegistry.resolve(
        new CapabilityRegistry.Inputs(
            mode, mods, enabled, Map.of(Capability.CHUNK_RENDERING, Implementation.EXPERIMENTAL)));
  }

  @Test
  void autoModeLeavesSodiumFeaturesToSodium() {
    var config = new HyperniumConfig();
    var registry = CapabilityRegistry.resolve(config, DetectedMods.of(SODIUM));

    assertEquals(CapabilityState.PROVIDED_EXTERNALLY, registry.state(Capability.CHUNK_RENDERING));
    assertEquals(CapabilityState.PROVIDED_EXTERNALLY, registry.state(Capability.CHUNK_MESHING));
    assertTrue(registry.get(Capability.CHUNK_RENDERING).provider().contains("Sodium 0.6.13"));
    assertFalse(registry.shouldRun(Capability.CHUNK_RENDERING));
    // Sodium says nothing about entities.
    assertEquals(CapabilityState.UNAVAILABLE, registry.state(Capability.ENTITY_CULLING));
  }

  @Test
  void withoutOtherModsOnlyImplementedFeaturesRun() {
    var registry = CapabilityRegistry.resolve(new HyperniumConfig(), DetectedMods.none());

    assertEquals(CapabilityState.ACTIVE, registry.state(Capability.PROFILER));
    assertTrue(registry.shouldRun(Capability.PROFILER));
    assertEquals(CapabilityState.UNAVAILABLE, registry.state(Capability.CHUNK_RENDERING));
    assertTrue(registry.get(Capability.CHUNK_RENDERING).reason().contains("phase 9"));
  }

  @Test
  void profilerOffInConfigIsDisabled() {
    var config = new HyperniumConfig();
    config.profiler.enabled = false;
    var registry = CapabilityRegistry.resolve(config, DetectedMods.none());

    assertEquals(CapabilityState.DISABLED, registry.state(Capability.PROFILER));
    assertFalse(registry.shouldRun(Capability.PROFILER));
  }

  @Test
  void standaloneIgnoresInstalledMods() {
    var config = new HyperniumConfig();
    config.compatibility.mode = "standalone";
    var registry = CapabilityRegistry.resolve(config, DetectedMods.of(SODIUM));

    assertFalse(registry.isProvidedExternally(Capability.CHUNK_RENDERING));
  }

  @Test
  void memoryOptimizationListsEveryProvider() {
    var mods =
        DetectedMods.of(
            Map.of(OptimizationMod.FERRITECORE, "8.0.0", OptimizationMod.MODERNFIX, "5.20.0"));
    var registry = CapabilityRegistry.resolve(new HyperniumConfig(), mods);
    String provider = registry.get(Capability.MEMORY_OPTIMIZATION).provider();

    assertTrue(provider.contains("FerriteCore 8.0.0"));
    assertTrue(provider.contains("ModernFix 5.20.0"));
  }

  @Test
  void ownExperimentalFeatureRunsWhenNothingProvidesIt() {
    var registry =
        withExperimentalRenderer(CompatibilityMode.AUTO, DetectedMods.none(), true);
    assertEquals(CapabilityState.EXPERIMENTAL, registry.state(Capability.CHUNK_RENDERING));
    assertTrue(registry.shouldRun(Capability.CHUNK_RENDERING));
  }

  @Test
  void ownExperimentalFeatureStaysOffUnlessConfigured() {
    var registry =
        withExperimentalRenderer(CompatibilityMode.AUTO, DetectedMods.none(), false);
    assertEquals(CapabilityState.DISABLED, registry.state(Capability.CHUNK_RENDERING));
  }

  @Test
  void sodiumWinsOverExperimentalRendererInAuto() {
    var registry =
        withExperimentalRenderer(CompatibilityMode.AUTO, DetectedMods.of(SODIUM), true);
    assertEquals(CapabilityState.PROVIDED_EXTERNALLY, registry.state(Capability.CHUNK_RENDERING));
    assertEquals(1, registry.warnings().size());
  }

  @Test
  void safeModeDisablesConflictingFeaturesButNotTheProfiler() {
    var registry =
        withExperimentalRenderer(CompatibilityMode.SAFE, DetectedMods.none(), true);
    assertEquals(CapabilityState.DISABLED, registry.state(Capability.CHUNK_RENDERING));
    assertEquals(CapabilityState.ACTIVE, registry.state(Capability.PROFILER));
  }

  @Test
  void integrationNeverReplacesAMissingProvider() {
    var registry =
        withExperimentalRenderer(CompatibilityMode.INTEGRATION, DetectedMods.none(), true);
    assertEquals(CapabilityState.DISABLED, registry.state(Capability.CHUNK_RENDERING));
    // And it tells the user why nothing is being integrated.
    assertEquals(1, registry.warnings().size());
  }

  @Test
  void standaloneWarnsWhenItWouldRunNextToTheRealThing() {
    var registry =
        withExperimentalRenderer(CompatibilityMode.STANDALONE, DetectedMods.of(SODIUM), true);
    assertEquals(CapabilityState.EXPERIMENTAL, registry.state(Capability.CHUNK_RENDERING));
    assertEquals(1, registry.warnings().size());
    assertTrue(registry.warnings().get(0).contains("Sodium"));
  }

  @Test
  void detectionReadsVersionsFromModPresence() {
    ModPresence presence =
        id -> id.equals("lithium") ? Optional.of("0.15.0") : Optional.empty();
    DetectedMods mods = DetectedMods.detect(presence);

    assertTrue(mods.has(OptimizationMod.LITHIUM));
    assertEquals("0.15.0", mods.version(OptimizationMod.LITHIUM));
    assertFalse(mods.has(OptimizationMod.SODIUM));
    assertTrue(mods.summary().contains("Sodium (not found)"));
  }
}
