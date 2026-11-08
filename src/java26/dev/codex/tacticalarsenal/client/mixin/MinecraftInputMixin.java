package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.GunClientController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftInputMixin {
   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$cancelVanillaAttack(CallbackInfoReturnable<Boolean> callback) {
      if (GunClientController.isHoldingGun()) {
         callback.setReturnValue(true);
      }
   }

   @Inject(
      method = {"continueAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$cancelBlockBreaking(boolean down, CallbackInfo callback) {
      if (GunClientController.isHoldingGun()) {
         callback.cancel();
      }
   }

   @Inject(
      method = {"startUseItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$cancelVanillaUse(CallbackInfo callback) {
      if (GunClientController.isHoldingGun()) {
         callback.cancel();
      }
   }
}
