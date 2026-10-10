# Hypernium

Adaptive performance optimization framework for Minecraft Java Edition 1.21.11 (Fabric).
See [PLAN.md](PLAN.md) for the full development plan.

## Status

Phase 0 (Foundation): project skeleton, config, logging, subsystem lifecycle with fallback.
Phase 1 (Profiler): frame time, 1% / 0.1% lows, spikes, client/server tick time, chunk and entity
counts, heap/GC/allocation, F8 HUD, `/hypernium profiler|stats|debug|reload`.
Phase 2 (Compatibility): detects Sodium, Lithium, FerriteCore, ModernFix and EntityCulling and
resolves a capability registry (ACTIVE / PROVIDED_EXTERNALLY / DISABLED / UNAVAILABLE / EXPERIMENTAL)
respecting `compatibility.mode` (auto, standalone, integration, safe). `/hypernium compat` shows it.
Phase 3 (Controller): bottleneck classifier (render/cpu/memory/gc, each with a confidence level),
policy engine with separate render and simulation domains, hysteresis and cooldown
(`adaptive.*` in the config). `/hypernium adaptive` shows its state. It only computes policies for
now; Phase 4 applies them to the game.
Phase 4 (Adaptive rendering): applies the render policy to entity distance and particle density
(and render distance if `rendering.adaptRenderDistance` is on). Remembers your own settings,
respects changes you make, restores on leaving a world, and recovers after a crash
(`config/hypernium-baseline.properties`). LOD is reported but not applied: vanilla has no LOD.
GPU time, draw calls and chunk build times are reported as "unavailable" until later phases.

## Build

Requires JDK 21.

    gradle wrapper        # once, if gradlew is missing
    ./gradlew build       # jar in build/libs/
    ./gradlew test        # unit tests for the profiler core
    ./gradlew runClient   # launch Minecraft with Hypernium

Phase 0 is done when the game launches and the log shows `[Hypernium] Hypernium initialized`.
Version numbers live in `gradle.properties`; check https://fabricmc.net/develop if they need updating.

## Config

`config/hypernium.json` is created on first launch. Experimental features default to off.
