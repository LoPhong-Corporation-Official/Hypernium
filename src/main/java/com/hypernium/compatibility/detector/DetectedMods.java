package com.hypernium.compatibility.detector;

import com.hypernium.compatibility.OptimizationMod;
import java.util.Collections;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Snapshot of which known optimization mods are loaded, with their versions. */
public final class DetectedMods {

  private final Map<OptimizationMod, String> versions;

  private DetectedMods(Map<OptimizationMod, String> versions) {
    this.versions = Collections.unmodifiableMap(versions);
  }

  public static DetectedMods detect(ModPresence presence) {
    Map<OptimizationMod, String> found = new EnumMap<>(OptimizationMod.class);
    for (OptimizationMod mod : OptimizationMod.values()) {
      presence.version(mod.modId()).ifPresent(version -> found.put(mod, version));
    }
    return new DetectedMods(found);
  }

  public static DetectedMods of(Map<OptimizationMod, String> versions) {
    Map<OptimizationMod, String> copy = new EnumMap<>(OptimizationMod.class);
    copy.putAll(versions);
    return new DetectedMods(copy);
  }

  public static DetectedMods none() {
    return new DetectedMods(new EnumMap<>(OptimizationMod.class));
  }

  public boolean has(OptimizationMod mod) {
    return versions.containsKey(mod);
  }

  /** Version string, or null if the mod is not loaded. */
  public String version(OptimizationMod mod) {
    return versions.get(mod);
  }

  public boolean isEmpty() {
    return versions.isEmpty();
  }

  /** "Sodium 0.6.13" or "Lithium (not found)" for each known mod, in declaration order. */
  public List<String> summary() {
    List<String> out = new ArrayList<>();
    for (OptimizationMod mod : OptimizationMod.values()) {
      out.add(
          has(mod)
              ? mod.displayName() + " " + versions.get(mod)
              : mod.displayName() + " (not found)");
    }
    return out;
  }
}
