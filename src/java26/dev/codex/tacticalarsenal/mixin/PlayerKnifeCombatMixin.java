package dev.codex.tacticalarsenal.mixin;

import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerKnifeCombatMixin {
   @Inject(
      method = {"canCriticalAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$disableKnifeCriticals(Entity target, CallbackInfoReturnable<Boolean> callback) {
      Player player = (Player)(Object)this;
      if (player.getMainHandItem().getItem() instanceof KnifeItem) {
         callback.setReturnValue(false);
      }
   }
}
