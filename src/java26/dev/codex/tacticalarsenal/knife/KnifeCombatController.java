package dev.codex.tacticalarsenal.knife;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public final class KnifeCombatController {
   private static final float FULL_ATTACK_THRESHOLD = 0.999F;
   private static final Map<Player, Long> NEXT_ALLOWED_HIT_TICKS = new WeakHashMap<>();
   private static boolean initialized;

   private KnifeCombatController() {
   }

   public static synchronized void initialize() {
      if (!initialized) {
         initialized = true;
         AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof KnifeItem)) {
               return InteractionResult.PASS;
            } else if (level.isClientSide()) {
               return InteractionResult.PASS;
            } else {
               long gameTime = level.getGameTime();
               long nextAllowed = NEXT_ALLOWED_HIT_TICKS.getOrDefault(player, Long.MIN_VALUE);
               if (gameTime >= nextAllowed && !(player.getAttackStrengthScale(0.0F) < 0.999F)) {
                  NEXT_ALLOWED_HIT_TICKS.put(player, gameTime + 10L);
                  if (level instanceof ServerLevel serverLevel) {
                     KnifeServerSoundController.broadcastEntityImpact(serverLevel, player, entity);
                  }

                  return InteractionResult.PASS;
               } else {
                  return InteractionResult.FAIL;
               }
            }
         });
      }
   }
}
