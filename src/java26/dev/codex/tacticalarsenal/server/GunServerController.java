package dev.codex.tacticalarsenal.server;

import dev.codex.tacticalarsenal.gun.FireMode;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class GunServerController {
   private static final long PATTERN_RESET_NANOS = 400000000L;
   private static final Map<UUID, GunServerController.PlayerGunState> PLAYER_STATES = new HashMap<>();

   private GunServerController() {
   }

   public static void initialize() {
      ServerTickEvents.END_SERVER_TICK.register(GunServerController::tick);
   }

   public static void handleFire(ServerPlayer player, boolean aiming, boolean requestedBurstMode, boolean triggerHeld) {
      if (!triggerHeld) {
         GunServerController.PlayerGunState state = PLAYER_STATES.get(player.getUUID());
         if (state != null) {
            state.revolverChargeStartNanos = 0L;
            state.lastRevolverRequestTick = -1L;
         }
      } else if (player.isAlive() && !player.isSpectator()) {
         ItemStack stack = player.getMainHandItem();
         if (stack.getItem() instanceof GunItem gun) {
            GunServerController.PlayerGunState var17 = PLAYER_STATES.computeIfAbsent(player.getUUID(), ignored -> new GunServerController.PlayerGunState());
            GunDefinition definition = gun.definition();
            boolean interruptingShellReload = false;
            if (var17.reload != null) {
               if (var17.reload.stack == stack && var17.reload.gun == gun) {
                  interruptingShellReload = definition.category() == WeaponCategory.SHOTGUN && !definition.reserveAsMagazines() && gun.ammo(stack) > 0;
                  if (!interruptingShellReload) {
                     return;
                  }
               } else {
                  var17.reload = null;
               }
            }

            if (var17.lastGun != gun) {
               var17.lastGun = gun;
               var17.lastAutomaticRequestTick = -1L;
               var17.automaticBudget = 1.0;
               var17.nextAllowedSemiShotNanos = 0L;
               var17.patternIndex = 0;
               var17.accuracyPenalty = 0.0F;
               var17.burstShots = 0;
               var17.nextAllowedBurstShotNanos = 0L;
               var17.nextAllowedBurstSequenceNanos = 0L;
               var17.revolverChargeStartNanos = 0L;
               var17.lastRevolverRequestTick = -1L;
               var17.nextAllowedRevolverShotNanos = 0L;
            }

            long now = System.nanoTime();
            if (gun.ammo(stack) <= 0) {
               var17.burstShots = 0;
               var17.revolverChargeStartNanos = 0L;
            } else {
               boolean burstMode = requestedBurstMode && definition.burstCapable();
               if (definition.revolver()) {
                  if (!allowRevolverShot(player, definition, var17, now)) {
                     return;
                  }
               } else if (burstMode) {
                  if (!allowBurstShot(definition, var17, now)) {
                     return;
                  }
               } else if (definition.fireMode() == FireMode.AUTOMATIC) {
                  long tick = player.level().getGameTime();
                  long elapsedTicks = var17.lastAutomaticRequestTick < 0L ? Long.MAX_VALUE : tick - var17.lastAutomaticRequestTick;
                  if (elapsedTicks > 2L) {
                     var17.automaticBudget = 1.0;
                  } else if (elapsedTicks > 0L) {
                     var17.automaticBudget = Math.min(2.0, var17.automaticBudget + (float)elapsedTicks * definition.roundsPerMinute() / 1200.0);
                  }

                  var17.lastAutomaticRequestTick = tick;
                  if (var17.automaticBudget < 1.0) {
                     return;
                  }

                  var17.automaticBudget--;
               } else {
                  if (now < var17.nextAllowedSemiShotNanos) {
                     return;
                  }

                  var17.nextAllowedSemiShotNanos = now + definition.shotIntervalNanos();
               }

               if (gun.consumeRound(stack)) {
                  if (interruptingShellReload) {
                     var17.reload = null;
                  }

                  float accuracyPenalty = recoverAccuracyPenalty(player, definition, var17, now);
                  if (now - var17.lastShotNanos > 400000000L) {
                     var17.patternIndex = 0;
                  }

                  var17.lastShotNanos = now;
                  boolean scoped = aiming && definition.canScope();
                  BallisticsService.fire(player, definition, scoped, burstMode, var17.patternIndex, accuracyPenalty);
                  var17.accuracyPenalty = Math.min(0.75F, accuracyPenalty + definition.fireInaccuracy(scoped, burstMode));
                  var17.patternIndex++;
               }
            }
         }
      }
   }

   private static boolean allowRevolverShot(ServerPlayer player, GunDefinition gun, GunServerController.PlayerGunState state, long now) {
      long tick = player.level().getGameTime();
      long elapsedTicks = state.lastRevolverRequestTick < 0L ? Long.MAX_VALUE : tick - state.lastRevolverRequestTick;
      if (elapsedTicks > 2L || state.revolverChargeStartNanos == 0L) {
         state.revolverChargeStartNanos = now;
      }

      state.lastRevolverRequestTick = tick;
      if (now - state.revolverChargeStartNanos >= gun.revolverFireDelayNanos() && now >= state.nextAllowedRevolverShotNanos) {
         state.nextAllowedRevolverShotNanos = now + gun.shotIntervalNanos();
         return true;
      } else {
         return false;
      }
   }

   private static boolean allowBurstShot(GunDefinition gun, GunServerController.PlayerGunState state, long now) {
      if (state.burstShots == 0) {
         if (now < state.nextAllowedBurstSequenceNanos) {
            return false;
         }

         state.burstSequenceStartNanos = now;
      } else if (now < state.nextAllowedBurstShotNanos) {
         return false;
      }

      state.burstShots++;
      long burstIntervalNanos = (long)(gun.burstShotIntervalSeconds() * 1.0E9);
      state.nextAllowedBurstShotNanos = state.burstSequenceStartNanos + state.burstShots * burstIntervalNanos;
      if (state.burstShots >= 3) {
         state.nextAllowedBurstSequenceNanos = state.burstSequenceStartNanos + (long)(gun.burstCycleTimeSeconds() * 1.0E9);
         state.burstShots = 0;
      }

      return true;
   }

   public static void handleReload(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() instanceof GunItem gun && gun.ammo(stack) < gun.definition().magazineSize() && gun.reserveAmmo(stack) > 0) {
         GunServerController.PlayerGunState state = PLAYER_STATES.computeIfAbsent(player.getUUID(), ignored -> new GunServerController.PlayerGunState());
         if (state.reload == null) {
            boolean empty = gun.ammo(stack) == 0;
            GunDefinition definition = gun.definition();
            int reloadUnits = gun.plannedReloadUnits(stack);
            if (reloadUnits > 0) {
               float insertSeconds = empty ? definition.emptyReloadInsertSeconds() : definition.reloadInsertSeconds();
               long start = player.level().getGameTime();
               long insertGameTime = start + (long)Math.ceil(insertSeconds * 20.0F);
               float totalSeconds = definition.reserveAsMagazines() ? definition.reloadSeconds() : definition.reloadSeconds() * reloadUnits;
               long finishGameTime = start + (long)Math.ceil(totalSeconds * 20.0F);
               state.burstShots = 0;
               state.revolverChargeStartNanos = 0L;
               state.lastRevolverRequestTick = -1L;
               state.reload = new GunServerController.ReloadState(gun, stack, insertGameTime, finishGameTime, reloadUnits);
            }
         }
      }
   }

   private static void tick(MinecraftServer server) {
      Iterator<Entry<UUID, GunServerController.PlayerGunState>> iterator = PLAYER_STATES.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, GunServerController.PlayerGunState> entry = iterator.next();
         ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
         if (player == null) {
            iterator.remove();
         } else {
            GunServerController.PlayerGunState state = entry.getValue();
            if (state.reload != null) {
               ItemStack stack = player.getMainHandItem();
               if (stack == state.reload.stack && stack.getItem() == state.reload.gun) {
                  long gameTime = player.level().getGameTime();

                  while (state.reload.remainingUnits > 0 && gameTime >= state.reload.nextInsertGameTime) {
                     boolean inserted = state.reload.gun.reloadOneUnit(stack);
                     int remaining = inserted ? state.reload.remainingUnits - 1 : 0;
                     long intervalTicks = Math.max(1L, (long)Math.ceil(state.reload.gun.definition().reloadSeconds() * 20.0F));
                     state.reload = new GunServerController.ReloadState(
                        state.reload.gun, state.reload.stack, state.reload.nextInsertGameTime + intervalTicks, state.reload.finishGameTime, remaining
                     );
                  }

                  if (gameTime >= state.reload.finishGameTime) {
                     while (state.reload.remainingUnits > 0 && state.reload.gun.reloadOneUnit(stack)) {
                        state.reload = new GunServerController.ReloadState(
                           state.reload.gun, state.reload.stack, state.reload.nextInsertGameTime, state.reload.finishGameTime, state.reload.remainingUnits - 1
                        );
                     }

                     state.reload = null;
                  }
               } else {
                  state.reload = null;
               }
            }
         }
      }
   }

   private static float recoverAccuracyPenalty(ServerPlayer player, GunDefinition gun, GunServerController.PlayerGunState state, long now) {
      if (state.lastShotNanos != 0L && !(state.accuracyPenalty <= 0.0F)) {
         float elapsedSeconds = Math.max(0.0F, (float)(now - state.lastShotNanos) / 1.0E9F);
         float recovery = Math.max(0.01F, gun.recoverySeconds(player.isCrouching(), state.patternIndex));
         return state.accuracyPenalty * (float)Math.exp(-Math.log(10.0) * elapsedSeconds / recovery);
      } else {
         return 0.0F;
      }
   }

   private static final class PlayerGunState {
      private GunItem lastGun;
      private long lastAutomaticRequestTick = -1L;
      private double automaticBudget = 1.0;
      private long nextAllowedSemiShotNanos;
      private long lastShotNanos;
      private int patternIndex;
      private float accuracyPenalty;
      private int burstShots;
      private long burstSequenceStartNanos;
      private long nextAllowedBurstShotNanos;
      private long nextAllowedBurstSequenceNanos;
      private long revolverChargeStartNanos;
      private long lastRevolverRequestTick = -1L;
      private long nextAllowedRevolverShotNanos;
      private GunServerController.ReloadState reload;
   }

   private record ReloadState(GunItem gun, ItemStack stack, long nextInsertGameTime, long finishGameTime, int remainingUnits) {
   }
}
