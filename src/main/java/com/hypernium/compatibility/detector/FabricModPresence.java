package com.hypernium.compatibility.detector;

import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;

/** {@link ModPresence} backed by Fabric Loader. */
public final class FabricModPresence implements ModPresence {

  @Override
  public Optional<String> version(String modId) {
    return FabricLoader.getInstance()
        .getModContainer(modId)
        .map(container -> container.getMetadata().getVersion().getFriendlyString());
  }
}
