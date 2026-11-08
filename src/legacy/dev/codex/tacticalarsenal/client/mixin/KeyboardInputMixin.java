package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scoped movement speed. Minecraft 26.x scales ClientInput#getMoveVector; before 1.21.2 the player
 * reads the movement impulses directly, so the same ratio is applied to them.
 */
@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin {
   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void tacticalArsenal$applyScopedMoveSpeed(boolean sneaking, float sneakSpeed, CallbackInfo callback) {
      GunDefinition gun = GunClientController.currentDefinition();
      if (gun != null && GunClientController.isAiming()) {
         float ratio = Mth.clamp(gun.maximumMoveSpeed(true) / Math.max(1.0F, gun.maximumMoveSpeed(false)), 0.1F, 1.0F);
         Input input = (Input)(Object)this;
         input.leftImpulse *= ratio;
         input.forwardImpulse *= ratio;
      }
   }
}
