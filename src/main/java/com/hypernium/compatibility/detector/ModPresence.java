package com.hypernium.compatibility.detector;

import java.util.Optional;

/** Abstraction over "is this mod loaded?" so detection logic can be tested without Fabric. */
public interface ModPresence {

  /** The mod's version if it is loaded, empty otherwise. */
  Optional<String> version(String modId);
}
