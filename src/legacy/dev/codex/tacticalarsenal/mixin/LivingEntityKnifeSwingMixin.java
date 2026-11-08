package dev.codex.tacticalarsenal.mixin;

import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Knives swing in 8 ticks (their 26.x swing animation component) instead of the default 6. */
@Mixin({LivingEntity.class})
public abstract class LivingEntityKnifeSwingMixin {
   @Inject(method = {"getCurrentSwingDuration"}, at = {@At("RETURN")}, cancellable = true)
   private void tacticalArsenal$knifeSwingDuration(CallbackInfoReturnable<Integer> callback) {
      LivingEntity entity = (LivingEntity)(Object)this;
      InteractionHand hand = entity.swingingArm != null ? entity.swingingArm : InteractionHand.MAIN_HAND;
      if (entity.getItemInHand(hand).getItem() instanceof KnifeItem) {
         callback.setReturnValue(callback.getReturnValueI() + 2);
      }
   }
}
