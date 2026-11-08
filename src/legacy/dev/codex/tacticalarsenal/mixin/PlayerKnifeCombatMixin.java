package dev.codex.tacticalarsenal.mixin;

import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Knives never land critical hits (26.x cancels Player#canCriticalAttack). Before 1.21.2 the
 * critical check is inline in Player#attack; making its "on a ladder" test report true while a
 * knife is held disables the critical exactly the same way.
 */
@Mixin({Player.class})
public abstract class PlayerKnifeCombatMixin {
   @Redirect(method = {"attack"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onClimbable()Z"))
   private boolean tacticalArsenal$disableKnifeCriticals(Player player) {
      return player.getMainHandItem().getItem() instanceof KnifeItem || player.onClimbable();
   }
}
