package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Inject(
      method = {"calculateFov"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void tacticalArsenal$applyAdsFov(float partialTick, CallbackInfoReturnable<Float> callback) {
      GunDefinition gun = GunClientController.currentDefinition();
      int scopeLevel = GunClientController.currentScopeLevel();
      if (gun != null && scopeLevel != 0 && gun.canScope()) {
         float alpha = GunAnimationEngine.INSTANCE.ads(partialTick);
         if (!(alpha <= 0.001F)) {
            float original = callback.getReturnValueF();
            callback.setReturnValue(Mth.lerp(alpha, original, GunClientController.currentScopeFov(partialTick)));
         }
      }
   }
}
