package com.hypernium.mixin.client;

import com.hypernium.profiler.ProfilerHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks render-loop iteration boundaries for the frame profiler. Lives in its own non-required
 * mixin config: if Minecraft changes this method, the hook is skipped and the profiler falls
 * back to the HUD pass instead of crashing the game.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftRunTickMixin {

  @Inject(method = "runTick", at = @At("HEAD"), require = 0)
  private void hypernium$frameStart(boolean renderLevel, CallbackInfo ci) {
    ProfilerHooks.frameStart();
  }

  @Inject(method = "runTick", at = @At("RETURN"), require = 0)
  private void hypernium$frameEnd(boolean renderLevel, CallbackInfo ci) {
    ProfilerHooks.frameEnd();
  }
}
