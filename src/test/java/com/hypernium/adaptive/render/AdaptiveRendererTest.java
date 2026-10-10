package com.hypernium.adaptive.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypernium.adaptive.policy.AdaptivePolicy;
import com.hypernium.adaptive.policy.PolicyEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdaptiveRendererTest {

  private static final RenderKnobs ALL = new RenderKnobs(true, true, true);
  private static final RenderKnobs DEFAULT = new RenderKnobs(false, true, true);

  private final Path file = tempFile();
  private final FakeRenderSettings settings = new FakeRenderSettings();

  private static Path tempFile() {
    try {
      return Files.createTempDirectory("hypernium-test").resolve("baseline.properties");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private AdaptiveRenderer renderer(RenderKnobs knobs) {
    return new AdaptiveRenderer(settings, knobs, new BaselineStore(file));
  }

  private static AdaptivePolicy level(int renderLevel) {
    return PolicyEngine.build(renderLevel, 0, false);
  }

  @Test
  void loweringAndRestoringReturnsTheExactOriginalValues() {
    var r = renderer(ALL);
    r.accept(level(2)); // x0.8 distance, x0.7 entities, x0.6 particles
    assertEquals(13, settings.renderDistance); // 16 * 0.8 = 12.8 -> 13
    assertEquals(0.7, settings.entityDistance, 1e-9);
    assertEquals(1, settings.particleLevel);

    r.accept(level(0));
    assertEquals(16, settings.renderDistance);
    assertEquals(1.0, settings.entityDistance, 1e-9);
    assertEquals(0, settings.particleLevel);
    assertFalse(Files.exists(file), "baseline file is removed once restored");
  }

  @Test
  void renderDistanceIsLeftAloneUnlessTheUserEnabledIt() {
    var r = renderer(DEFAULT);
    r.accept(level(3));
    assertEquals(16, settings.renderDistance);
    assertEquals(0.55, settings.entityDistance, 1e-9);
  }

  @Test
  void aNeutralPolicyWithNothingHeldWritesNothing() {
    var r = renderer(ALL);
    r.accept(level(0));
    r.accept(level(0));
    assertEquals(0, settings.writes);
  }

  @Test
  void aValueTheUserChangedBecomesTheNewBaseline() {
    var r = renderer(ALL);
    r.accept(level(1)); // 16 -> 14
    assertEquals(14, settings.renderDistance);

    settings.renderDistance = 24; // the player raises it in the video menu
    r.accept(level(2)); // 24 * 0.8 = 19.2 -> 19
    assertEquals(19, settings.renderDistance);

    r.accept(level(0));
    assertEquals(24, settings.renderDistance, "restores the player's latest choice, not the old one");
  }

  @Test
  void neverRaisesAnythingAboveTheUsersOwnSetting() {
    settings.renderDistance = 4;
    settings.entityDistance = 0.5;
    settings.particleLevel = 2; // already Minimal
    var r = renderer(ALL);
    r.accept(level(1));
    r.accept(level(4));
    assertEquals(4, settings.renderDistance);
    assertEquals(0.5, settings.entityDistance, 1e-9);
    assertEquals(2, settings.particleLevel);
  }

  @Test
  void renderDistanceNeverDropsBelowTheFloor() {
    var r = renderer(ALL);
    r.accept(level(4)); // 16 * 0.6 = 9.6 -> 10, floor is 5
    assertEquals(10, settings.renderDistance);
    settings.renderDistance = 6;
    r.accept(level(4)); // user lowered it to 6: 6 * 0.6 = 3.6, clamped to the floor but not above 6
    assertEquals(5, settings.renderDistance);
  }

  @Test
  void entityDistanceStopsAtTheVanillaMinimum() {
    settings.entityDistance = 0.8;
    var r = renderer(ALL);
    r.accept(level(4)); // 0.8 * 0.4 = 0.32 -> clamped to 0.5
    assertEquals(0.5, settings.entityDistance, 1e-9);
  }

  @Test
  void particlesFollowTheScaleBands() {
    assertEquals(0, AdaptiveRenderer.particleLevelFor(0.8));
    assertEquals(1, AdaptiveRenderer.particleLevelFor(0.6));
    assertEquals(1, AdaptiveRenderer.particleLevelFor(0.4));
    assertEquals(2, AdaptiveRenderer.particleLevelFor(0.2));
  }

  @Test
  void aCrashedSessionIsUndoneOnTheNextLaunch() {
    var first = renderer(ALL);
    first.accept(level(3)); // lowered, file written
    assertTrue(Files.exists(file));

    // "Crash": the options now hold the lowered values, a new renderer starts fresh.
    var second = renderer(ALL);
    second.recoverFromCrash();
    assertEquals(16, settings.renderDistance);
    assertEquals(1.0, settings.entityDistance, 1e-9);
    assertEquals(0, settings.particleLevel);
    assertFalse(Files.exists(file));
  }

  @Test
  void aFailingBackendNeverPropagatesAnException() {
    settings.failOnWrite = true;
    var r = renderer(ALL);
    r.accept(level(2)); // must not throw
  }

  @Test
  void statusSaysWhyLodIsNotApplied() {
    var r = renderer(DEFAULT);
    r.accept(level(2));
    String all = String.join("\n", r.status());
    assertTrue(all.contains("LOD: +2 wanted, not applied"));
    assertTrue(all.contains("Render distance: off in config"));
    assertTrue(all.contains("your setting: 100%"));
  }
}
