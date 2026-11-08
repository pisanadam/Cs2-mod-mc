package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Before Minecraft 26.1 the world FOV is computed by GameRenderer#getFov(camera, partialTick, true)
 * instead of Camera#calculateFov; the ADS zoom is applied to exactly that value.
 */
@Mixin({GameRenderer.class})
public abstract class CameraMixin {
   @Inject(
      method = {"getFov"},
      at = {@At("RETURN")},
      cancellable = true
   )
   //#if MC >= 1.21.2
   private void tacticalArsenal$applyAdsFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> callback) {
   //#else
   private void tacticalArsenal$applyAdsFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> callback) {
   //#endif
      if (!useFovSetting) {
         return;
      }

      GunDefinition gun = GunClientController.currentDefinition();
      int scopeLevel = GunClientController.currentScopeLevel();
      if (gun != null && scopeLevel != 0 && gun.canScope()) {
         float alpha = GunAnimationEngine.INSTANCE.ads(partialTick);
         if (!(alpha <= 0.001F)) {
            //#if MC >= 1.21.2
            float original = callback.getReturnValueF();
            callback.setReturnValue(Mth.lerp(alpha, original, GunClientController.currentScopeFov(partialTick)));
            //#else
            float original = (float)callback.getReturnValueD();
            callback.setReturnValue((double)Mth.lerp(alpha, original, GunClientController.currentScopeFov(partialTick)));
            //#endif
         }
      }
   }
}
