package com.hypernium.debug.commands;

import com.hypernium.Hypernium;
import com.hypernium.debug.ReportEnvironment;
import com.hypernium.debug.hud.PerformanceHud;
import com.hypernium.profiler.HyperniumProfiler;
import com.hypernium.profiler.PerformanceReport;
import com.hypernium.profiler.ProfilerHooks;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/** Client commands from plan section 32. Only the Phase 1 subset is implemented. */
public final class HyperniumCommands {

  private final PerformanceHud hud;

  public HyperniumCommands(PerformanceHud hud) {
    this.hud = hud;
  }

  public void register() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("hypernium")
                    .executes(this::status)
                    .then(ClientCommandManager.literal("profiler").executes(this::profiler))
                    .then(ClientCommandManager.literal("stats").executes(this::stats))
                    .then(ClientCommandManager.literal("compat").executes(this::compat))
                    .then(
                        ClientCommandManager.literal("adaptive")
                            .executes(this::adaptive)
                            .then(ClientCommandManager.literal("reset").executes(this::adaptiveReset)))
                    .then(ClientCommandManager.literal("debug").executes(this::debug))
                    .then(ClientCommandManager.literal("reload").executes(this::reload))
                    .then(ClientCommandManager.literal("benchmark").executes(this::benchmark))));
  }

  private int status(CommandContext<FabricClientCommandSource> ctx) {
    var env = ReportEnvironment.current();
    send(
        ctx,
        "Hypernium "
            + env.hyperniumVersion()
            + " | mode "
            + env.performanceMode()
            + " | compatibility "
            + env.compatibilityMode());
    send(ctx, "Commands: profiler, stats, compat, adaptive, debug (toggle HUD), reload, benchmark");
    return 1;
  }

  private int profiler(CommandContext<FabricClientCommandSource> ctx) {
    HyperniumProfiler profiler = ProfilerHooks.profiler();
    if (profiler == null) {
      send(ctx, "Profiler is not running (disabled in config or after an error; see the log).");
      return 0;
    }
    for (String line : PerformanceReport.full(profiler.latest(), ReportEnvironment.current())) {
      send(ctx, line);
    }
    return 1;
  }

  private int stats(CommandContext<FabricClientCommandSource> ctx) {
    HyperniumProfiler profiler = ProfilerHooks.profiler();
    if (profiler == null) {
      send(ctx, "Profiler is not running.");
      return 0;
    }
    send(ctx, PerformanceReport.oneLine(profiler.latest()));
    return 1;
  }

  private int compat(CommandContext<FabricClientCommandSource> ctx) {
    var registry = Hypernium.capabilities();
    send(ctx, "Compatibility mode: " + registry.mode().name().toLowerCase(java.util.Locale.ROOT));
    send(ctx, "Mods: " + String.join(", ", registry.detected().summary()));
    for (String line : registry.describe()) {
      send(ctx, line);
    }
    for (String warning : registry.warnings()) {
      send(ctx, "Warning: " + warning);
    }
    return 1;
  }

  private int adaptive(CommandContext<FabricClientCommandSource> ctx) {
    var service = Hypernium.adaptive();
    if (service == null) {
      send(ctx, "Adaptive controller is not running (see /hypernium compat for why).");
      return 0;
    }
    for (String line : service.details()) {
      send(ctx, line);
    }
    return 1;
  }

  private int adaptiveReset(CommandContext<FabricClientCommandSource> ctx) {
    var service = Hypernium.adaptive();
    if (service == null) {
      send(ctx, "Adaptive controller is not running.");
      return 0;
    }
    service.reset();
    send(ctx, "Adaptive controller reset to full quality.");
    return 1;
  }

  private int debug(CommandContext<FabricClientCommandSource> ctx) {
    send(ctx, "Performance HUD " + (hud.toggle() ? "on" : "off"));
    return 1;
  }

  private int reload(CommandContext<FabricClientCommandSource> ctx) {
    Hypernium.configManager().load();
    hud.setVisible(Hypernium.config().profiler.hud);
    Hypernium.refreshCapabilities();
    var service = Hypernium.adaptive();
    if (service != null) {
      service.reconfigure(Hypernium.config());
    }
    send(ctx, "Config reloaded. Changes to profiler.enabled and rendering.* need a restart.");
    return 1;
  }

  private int benchmark(CommandContext<FabricClientCommandSource> ctx) {
    send(ctx, "Benchmark mode is planned for Phase 11.");
    return 1;
  }

  private static void send(CommandContext<FabricClientCommandSource> ctx, String message) {
    ctx.getSource().sendFeedback(Component.literal(message));
  }
}
