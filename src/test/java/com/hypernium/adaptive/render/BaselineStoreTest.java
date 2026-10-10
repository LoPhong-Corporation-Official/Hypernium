package com.hypernium.adaptive.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BaselineStoreTest {

  private static Path tempFile() {
    try {
      return Files.createTempDirectory("hypernium-store").resolve("b.properties");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void roundTripKeepsUntouchedSettingsNull() {
    var store = new BaselineStore(tempFile());
    store.save(new Baseline(null, 1.5, 1));
    var loaded = store.load().orElseThrow();
    assertEquals(null, loaded.renderDistance());
    assertEquals(1.5, loaded.entityDistance(), 1e-9);
    assertEquals(1, loaded.particleLevel().intValue());
  }

  @Test
  void missingFileMeansNothingToRecover() {
    assertFalse(new BaselineStore(tempFile()).load().isPresent());
  }

  @Test
  void corruptFileIsIgnored() throws IOException {
    Path file = tempFile();
    Files.writeString(file, "renderDistance=sixteen\n");
    assertFalse(new BaselineStore(file).load().isPresent());
  }

  @Test
  void deleteRemovesTheFile() {
    Path file = tempFile();
    var store = new BaselineStore(file);
    store.save(new Baseline(12, null, null));
    assertTrue(Files.exists(file));
    store.delete();
    assertFalse(Files.exists(file));
  }
}
