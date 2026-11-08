package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientInput.class})
public abstract class KeyboardInputMixin {
   @Inject(
      method = {"getMoveVector"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void tacticalArsenal$applyScopedMoveSpeed(CallbackInfoReturnable<Vec2> callback) {
      GunDefinition gun = GunClientController.currentDefinition();
      if (gun != null && GunClientController.isAiming()) {
         float ratio = gun.maximumMoveSpeed(true) / Math.max(1.0F, gun.maximumMoveSpeed(false));
         callback.setReturnValue(((Vec2)callback.getReturnValue()).scale(Mth.clamp(ratio, 0.1F, 1.0F)));
      }
   }
}
