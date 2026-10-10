package com.hypernium.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypernium.core.lifecycle.HyperniumLog;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Loads and saves {@code config/hypernium.json}. Never throws: a broken file means defaults. */
public final class ConfigManager {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final Path file;
  private volatile HyperniumConfig config = new HyperniumConfig();

  public ConfigManager() {
    this(FabricLoader.getInstance().getConfigDir().resolve("hypernium.json"));
  }

  public ConfigManager(Path file) {
    this.file = file;
  }

  public HyperniumConfig get() {
    return config;
  }

  /** (Re)loads from disk and rewrites the file so new options appear for the user. */
  public synchronized void load() {
    HyperniumConfig loaded = null;
    if (Files.exists(file)) {
      try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
        loaded = GSON.fromJson(reader, HyperniumConfig.class);
      } catch (IOException | RuntimeException e) {
        // Keep the unreadable file around instead of silently overwriting the user's edits.
        HyperniumLog.error("Could not read {}; using defaults for this session", file, e);
        config = new HyperniumConfig();
        config.normalize();
        return;
      }
    }
    if (loaded == null) {
      loaded = new HyperniumConfig();
    }
    loaded.normalize();
    config = loaded;
    save();
  }

  public synchronized void save() {
    try {
      Files.createDirectories(file.getParent());
      try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
        GSON.toJson(config, writer);
      }
    } catch (IOException e) {
      HyperniumLog.error("Could not write {}", file, e);
    }
  }
}
