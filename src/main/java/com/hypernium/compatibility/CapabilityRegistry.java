package com.hypernium.compatibility;

import com.hypernium.compatibility.Capability.Implementation;
import com.hypernium.compatibility.detector.DetectedMods;
import com.hypernium.core.config.CompatibilityMode;
import com.hypernium.core.config.HyperniumConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides, for every {@link Capability}, whether Hypernium runs its own implementation or leaves
 * the job to another mod (plan sections 15-17, 47). Immutable: recompute it after a config reload.
 *
 * <p>Rules, in order:
 *
 * <ol>
 *   <li>A provider mod is installed and the mode is not STANDALONE: PROVIDED_EXTERNALLY.
 *   <li>Hypernium has no implementation yet: UNAVAILABLE.
 *   <li>Feature can conflict and mode is SAFE: DISABLED. In INTEGRATION mode a missing provider
 *       is never replaced: DISABLED.
 *   <li>Turned off in config: DISABLED.
 *   <li>Otherwise ACTIVE, or EXPERIMENTAL for experimental implementations.
 * </ol>
 */
public final class CapabilityRegistry {

  /** Everything the decision depends on. Tests can override {@code implementations}. */
  public record Inputs(
      CompatibilityMode mode,
      DetectedMods detected,
      Set<Capability> enabledByConfig,
      Map<Capability, Implementation> implementations) {}

  private final CompatibilityMode mode;
  private final DetectedMods detected;
  private final Map<Capability, CapabilityEntry> entries;
  private final List<String> warnings;

  private CapabilityRegistry(
      CompatibilityMode mode,
      DetectedMods detected,
      Map<Capability, CapabilityEntry> entries,
      List<String> warnings) {
    this.mode = mode;
    this.detected = detected;
    this.entries = Collections.unmodifiableMap(entries);
    this.warnings = List.copyOf(warnings);
  }

  /** Normal entry point: derive the inputs from the loaded config and detected mods. */
  public static CapabilityRegistry resolve(HyperniumConfig config, DetectedMods detected) {
    return resolve(
        new Inputs(
            config.compatibilityMode(), detected, enabledByConfig(config), Map.of()));
  }

  public static CapabilityRegistry resolve(Inputs in) {
    Map<Capability, CapabilityEntry> entries = new EnumMap<>(Capability.class);
    List<String> warnings = new ArrayList<>();
    for (Capability capability : Capability.values()) {
      entries.put(capability, resolveOne(capability, in, warnings));
    }
    if (in.mode() == CompatibilityMode.INTEGRATION && in.detected().isEmpty()) {
      warnings.add(
          "INTEGRATION mode is set but no supported optimization mod was found; "
              + "Hypernium will not replace their features.");
    }
    return new CapabilityRegistry(in.mode(), in.detected(), entries, warnings);
  }

  /** Which capabilities the user's config allows Hypernium to run. */
  static Set<Capability> enabledByConfig(HyperniumConfig config) {
    Set<Capability> enabled = EnumSet.allOf(Capability.class);
    if (!config.profiler.enabled) {
      enabled.remove(Capability.PROFILER);
    }
    if (!config.adaptive.enabled) {
      enabled.remove(Capability.ADAPTIVE_CONTROL);
    }
    if (!config.rendering.enabled) {
      enabled.remove(Capability.ADAPTIVE_RENDERING);
    }
    if (!config.chunk.predictiveLoading) {
      enabled.remove(Capability.PREDICTIVE_LOADING);
    }
    if (!config.experimental.customRenderer) {
      enabled.remove(Capability.CHUNK_RENDERING);
    }
    if (!config.experimental.greedyMeshing) {
      enabled.remove(Capability.CHUNK_MESHING);
    }
    return enabled;
  }

  private static CapabilityEntry resolveOne(Capability c, Inputs in, List<String> warnings) {
    CompatibilityMode mode = in.mode();
    DetectedMods detected = in.detected();
    Implementation impl = in.implementations().getOrDefault(c, c.defaultImplementation());
    boolean configured = in.enabledByConfig().contains(c);

    List<OptimizationMod> present = c.providers().stream().filter(detected::has).toList();

    if (mode != CompatibilityMode.STANDALONE && !present.isEmpty()) {
      if (impl == Implementation.EXPERIMENTAL && configured) {
        warnings.add(
            c.label()
                + ": Hypernium's experimental implementation is not used because "
                + names(present, detected)
                + " provides it.");
      }
      return new CapabilityEntry(
          c, CapabilityState.PROVIDED_EXTERNALLY, names(present, detected), "");
    }

    if (impl == Implementation.PLANNED) {
      return new CapabilityEntry(
          c,
          CapabilityState.UNAVAILABLE,
          "",
          "not implemented yet (planned for phase " + c.plannedPhase() + ")");
    }

    if (c.conflictRisk() && mode == CompatibilityMode.SAFE) {
      return new CapabilityEntry(c, CapabilityState.DISABLED, "Hypernium", "SAFE compatibility mode");
    }
    if (c.conflictRisk() && mode == CompatibilityMode.INTEGRATION && !c.providers().isEmpty()) {
      return new CapabilityEntry(
          c,
          CapabilityState.DISABLED,
          "Hypernium",
          "INTEGRATION mode does not replace a missing provider");
    }
    if (!configured) {
      return new CapabilityEntry(c, CapabilityState.DISABLED, "Hypernium", "turned off in config");
    }

    if (mode == CompatibilityMode.STANDALONE && c.conflictRisk()) {
      List<OptimizationMod> installed =
          c.providers().stream().filter(detected::has).toList();
      if (!installed.isEmpty()) {
        warnings.add(
            c.label()
                + ": STANDALONE mode runs Hypernium's own implementation even though "
                + names(installed, detected)
                + " is installed; they may conflict. Use AUTO to let it handle this.");
      }
    }
    CapabilityState state =
        impl == Implementation.EXPERIMENTAL ? CapabilityState.EXPERIMENTAL : CapabilityState.ACTIVE;
    return new CapabilityEntry(c, state, "Hypernium", "");
  }

  private static String names(List<OptimizationMod> mods, DetectedMods detected) {
    StringBuilder sb = new StringBuilder();
    for (OptimizationMod mod : mods) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(mod.displayName()).append(' ').append(detected.version(mod));
    }
    return sb.toString();
  }

  public CompatibilityMode mode() {
    return mode;
  }

  public DetectedMods detected() {
    return detected;
  }

  public CapabilityEntry get(Capability capability) {
    return entries.get(capability);
  }

  public CapabilityState state(Capability capability) {
    return entries.get(capability).state();
  }

  public List<CapabilityEntry> entries() {
    return List.copyOf(entries.values());
  }

  public List<String> warnings() {
    return warnings;
  }

  /** True if Hypernium's own implementation of this capability should be running. */
  public boolean shouldRun(Capability capability) {
    CapabilityState state = state(capability);
    return state == CapabilityState.ACTIVE || state == CapabilityState.EXPERIMENTAL;
  }

  public boolean isProvidedExternally(Capability capability) {
    return state(capability) == CapabilityState.PROVIDED_EXTERNALLY;
  }

  /** One line per capability, for the report, command and startup log. */
  public List<String> describe() {
    List<String> lines = new ArrayList<>();
    for (CapabilityEntry entry : entries.values()) {
      lines.add(entry.describe());
    }
    return lines;
  }
}
