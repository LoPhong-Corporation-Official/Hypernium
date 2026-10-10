package com.hypernium;

import com.hypernium.adaptive.client.FpsCapProbe;
import com.hypernium.adaptive.client.VanillaRenderSettings;
import com.hypernium.adaptive.controller.AdaptiveService;
import com.hypernium.adaptive.controller.ControllerSettings;
import com.hypernium.adaptive.controller.PerformanceController;
import com.hypernium.adaptive.render.AdaptiveRenderer;
import com.hypernium.adaptive.render.BaselineStore;
import com.hypernium.adaptive.render.RenderKnobs;
import com.hypernium.compatibility.Capability;
import com.hypernium.core.lifecycle.HyperniumLog;
import com.hypernium.debug.commands.HyperniumCommands;
import com.hypernium.debug.hud.PerformanceHud;
import com.hypernium.profiler.HyperniumProfiler;
import com.hypernium.profiler.ProfilerHooks;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client-only entrypoint: wires the profiler, HUD, keybinding and commands. */
public final class HyperniumClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    PerformanceHud hud = new PerformanceHud(Hypernium.config().profiler.hud);
    hud.register();
    new HyperniumCommands(hud).register();
    registerHudKey(hud);

    if (Hypernium.config().profiler.enabled) {
      startProfiler();
    } else {
      HyperniumLog.info("Profiler disabled in config");
    }

    ClientLifecycleEvents.CLIENT_STOPPING.register(
        client -> {
          // Hand the player's video settings back before anything is torn down.
          AdaptiveService adaptive = Hypernium.adaptive();
          if (adaptive != null) {
            adaptive.reset();
          }
          Hypernium.subsystems().shutdown();
        });
    HyperniumLog.info("Hypernium client side ready");
  }

  private static void registerHudKey(PerformanceHud hud) {
    KeyMapping.Category category =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Hypernium.MOD_ID, "main"));
    KeyMapping toggle =
        KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.hypernium.toggle_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                category));
    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (toggle.consumeClick()) {
            hud.toggle();
          }
        });
  }

  private static void startAdaptive(HyperniumProfiler profiler) {
    if (!Hypernium.capabilities().shouldRun(Capability.ADAPTIVE_CONTROL)) {
      HyperniumLog.info("Adaptive controller not started: {}",
          Hypernium.capabilities().get(Capability.ADAPTIVE_CONTROL).describe());
      return;
    }
    PerformanceController controller =
        new PerformanceController(ControllerSettings.from(Hypernium.config()));
    AdaptiveService service =
        new AdaptiveService(
            profiler, controller, () -> Minecraft.getInstance().level != null, FpsCapProbe::current);
    Hypernium.subsystems().register(service);
    Hypernium.setAdaptive(service);
    startAdaptiveRendering(service);

    // Leaving a world: back to full quality, so menus and the next world start clean.
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> service.reset());

    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          AdaptiveService active = Hypernium.adaptive();
          if (active == null) {
            return;
          }
          try {
            active.tick();
          } catch (Throwable t) {
            Hypernium.setAdaptive(null);
            Hypernium.subsystems().disable(active, t);
          }
        });
  }

  private static void startAdaptiveRendering(AdaptiveService service) {
    if (!Hypernium.capabilities().shouldRun(Capability.ADAPTIVE_RENDERING)) {
      HyperniumLog.info("Adaptive rendering not started: {}",
          Hypernium.capabilities().get(Capability.ADAPTIVE_RENDERING).describe());
      return;
    }
    AdaptiveRenderer renderer =
        new AdaptiveRenderer(
            new VanillaRenderSettings(),
            RenderKnobs.from(Hypernium.config()),
            new BaselineStore(
                FabricLoader.getInstance().getConfigDir().resolve("hypernium-baseline.properties")));
    service.addListener(renderer);
    service.addDetailsProvider(renderer::status);
    // Options are loaded by now; undo anything a crashed session left lowered.
    ClientLifecycleEvents.CLIENT_STARTED.register(client -> renderer.recoverFromCrash());
  }

  private static void startProfiler() {
    HyperniumProfiler profiler = new HyperniumProfiler();
    Hypernium.subsystems().register(profiler);
    ProfilerHooks.activate(profiler);
    startAdaptive(profiler);

    ClientTickEvents.START_CLIENT_TICK.register(client -> profiler.clientTickStart());
    ClientTickEvents.END_CLIENT_TICK.register(client -> profiler.clientTickEnd());
    ServerTickEvents.START_SERVER_TICK.register(server -> profiler.serverTickStart());
    ServerTickEvents.END_SERVER_TICK.register(server -> profiler.serverTickEnd());

    ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> profiler.chunkLoaded());
    ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> profiler.chunkUnloaded());
    ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> profiler.entityLoaded());
    ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> profiler.entityUnloaded());

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> profiler.resetSession());
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> profiler.resetSession());
  }
}
