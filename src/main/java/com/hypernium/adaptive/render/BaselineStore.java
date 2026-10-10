package com.hypernium.adaptive.render;

import com.hypernium.core.lifecycle.HyperniumLog;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Keeps the player's original values on disk while Hypernium has them lowered. If the game
 * crashes before they are restored, the next launch puts them back, so a crash can never leave
 * the player's options permanently reduced. Deleted once everything is restored.
 */
public final class BaselineStore {

  private final Path file;

  public BaselineStore(Path file) {
    this.file = file;
  }

  public void save(Baseline baseline) {
    Properties props = new Properties();
    if (baseline.renderDistance() != null) {
      props.setProperty("renderDistance", baseline.renderDistance().toString());
    }
    if (baseline.entityDistance() != null) {
      props.setProperty("entityDistance", baseline.entityDistance().toString());
    }
    if (baseline.particleLevel() != null) {
      props.setProperty("particleLevel", baseline.particleLevel().toString());
    }
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
        props.store(writer, "Hypernium: your original video settings while adaptive rendering is active");
      }
    } catch (IOException e) {
      HyperniumLog.warn("Could not write {}: {}", file, e.toString());
    }
  }

  /** Empty if there is no file or it cannot be read. */
  public Optional<Baseline> load() {
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    Properties props = new Properties();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      props.load(reader);
      Baseline baseline =
          new Baseline(
              parseInt(props.getProperty("renderDistance")),
              parseDouble(props.getProperty("entityDistance")),
              parseInt(props.getProperty("particleLevel")));
      return baseline.isEmpty() ? Optional.empty() : Optional.of(baseline);
    } catch (IOException | RuntimeException e) {
      HyperniumLog.warn("Ignoring unreadable {}: {}", file, e.toString());
      return Optional.empty();
    }
  }

  public void delete() {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      HyperniumLog.warn("Could not delete {}: {}", file, e.toString());
    }
  }

  private static Integer parseInt(String value) {
    return value == null ? null : Integer.valueOf(value.trim());
  }

  private static Double parseDouble(String value) {
    return value == null ? null : Double.valueOf(value.trim());
  }
}
