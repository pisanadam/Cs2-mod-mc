package dev.codex.tacticalarsenal.client;

import dev.codex.tacticalarsenal.client.compat.ClientCompat;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.client.audio.GunReloadSoundPlayer;
import dev.codex.tacticalarsenal.gun.FireMode;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import dev.codex.tacticalarsenal.network.FirePayload;
import dev.codex.tacticalarsenal.network.ReloadPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class GunClientController {
   private static final float SCOPE_OVERLAY_ADS_THRESHOLD = 0.72F;
   private static final float SCOPE_OVERLAY_EXIT_THRESHOLD = 0.02F;
   private static final float ONE_CENTIMETRE_AT_ONE_METRE = 0.01F;
   private static final long PATTERN_RESET_NANOS = 400000000L;
   static final Category CATEGORY = Category.register(TacticalArsenal.id("weapons"));
   private static final KeyMapping RELOAD = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tactical_arsenal.reload", Type.KEYSYM, 82, CATEGORY));
   private static GunItem currentGun;
   private static boolean previousAttack;
   private static boolean previousUse;
   private static double automaticBudget = 1.0;
   private static long nextSemiShotNanos;
   private static long lastLocalShotNanos;
   private static int shotSequence;
   private static float accuracyPenalty;
   private static int scopeLevel;
   private static int visualScopeLevel;
   private static float previousVisualScopeFov = 90.0F;
   private static float visualScopeFov = 90.0F;
   private static float targetVisualScopeFov = 90.0F;
   private static boolean scopeOverlayLatched;
   private static boolean burstMode;
   private static int burstShotsRemaining;
   private static long nextBurstShotNanos;
   private static long nextBurstAllowedNanos;
   private static long revolverChargeStartNanos;
   private static long nextRevolverShotNanos;
   private static long revolverCycleReadyNanos;

   private GunClientController() {
   }

   public static void initialize() {
      ClientTickEvents.END_CLIENT_TICK.register(GunClientController::tick);
   }

   public static boolean isHoldingGun() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof GunItem;
   }

   public static GunDefinition currentDefinition() {
      return currentGun == null ? null : currentGun.definition();
   }

   public static boolean isAiming() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.player != null
         && ClientCompat.screen(minecraft) == null
         && currentGun != null
         && currentGun.definition().canScope()
         && scopeLevel > 0
         && !GunAnimationEngine.INSTANCE.isReloading();
   }

   public static int currentScopeLevel() {
      return currentGun != null && currentGun.definition().canScope() ? visualScopeLevel : 0;
   }

   public static float currentScopeFov(float partialTick) {
      return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousVisualScopeFov, visualScopeFov);
   }

   public static boolean isScopeOverlayActive(float partialTick) {
      float ads = GunAnimationEngine.INSTANCE.ads(partialTick);
      if (!scopeOverlayLatched && scopeLevel > 0 && currentScopeLevel() > 0 && ads >= 0.72F) {
         scopeOverlayLatched = true;
      }

      if (scopeOverlayLatched && (currentScopeLevel() == 0 || ads <= 0.02F)) {
         scopeOverlayLatched = false;
      }

      return scopeOverlayLatched;
   }

   public static boolean isBurstMode() {
      return currentGun != null && currentGun.definition().burstCapable() && burstMode;
   }

   public static boolean scopedShotConeExceedsOneCentimetre() {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && currentGun != null && player.getMainHandItem().getItem() == currentGun && currentGun.definition().canScope() && scopeLevel > 0) {
         GunDefinition gun = currentGun.definition();
         boolean scoped = true;
         boolean alternateFireMode = burstMode && gun.burstCapable();
         float inaccuracy = player.isCrouching() ? gun.crouchingInaccuracy(scoped, alternateFireMode) : gun.standingInaccuracy(scoped, alternateFireMode);
         double sourceSpeed = player.getDeltaMovement().horizontalDistance() * 20.0 * 52.493F;
         float moveFactor = Mth.clamp((float)(sourceSpeed / Math.max(1.0F, gun.maximumMoveSpeed(scoped))), 0.0F, 1.0F);
         inaccuracy += gun.movementInaccuracy(scoped, alternateFireMode) * moveFactor;
         if (!player.onGround()) {
            float verticalFactor = Mth.clamp((float)(Math.abs(player.getDeltaMovement().y) / 0.42), 0.0F, 1.0F);
            float jumpPhase = Mth.lerp(verticalFactor, gun.accuracy().jumpApex(), gun.accuracy().jumpInitial());
            inaccuracy += Math.max(gun.jumpInaccuracy(scoped, alternateFireMode), jumpPhase);
         }

         inaccuracy += recoveredAccuracyPenalty(player, gun, System.nanoTime());
         float maximumOffset = Math.max(0.0F, gun.spread(scoped, alternateFireMode) + inaccuracy);
         return maximumOffset >= 0.01F;
      } else {
         return false;
      }
   }

   private static void tick(Minecraft minecraft) {
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.level != null) {
         ItemStack stack = player.getMainHandItem();
         GunItem gun = stack.getItem() instanceof GunItem value ? value : null;
         if (gun != currentGun) {
            if (currentGun != null && currentGun.definition().revolver() && revolverChargeStartNanos != 0L) {
               ClientPlayNetworking.send(new FirePayload(false, false, false));
            }

            currentGun = gun;
            previousAttack = false;
            previousUse = false;
            automaticBudget = 1.0;
            nextSemiShotNanos = 0L;
            resetPredictedAccuracy();
            scopeLevel = 0;
            visualScopeLevel = 0;
            resetVisualScopeFov(gun == null ? null : gun.definition());
            scopeOverlayLatched = false;
            burstMode = false;
            burstShotsRemaining = 0;
            nextBurstShotNanos = 0L;
            nextBurstAllowedNanos = 0L;
            resetRevolverTrigger();
            revolverCycleReadyNanos = 0L;
            GunReloadSoundPlayer.reset();
         }

         boolean use = gun != null && ClientCompat.screen(minecraft) == null && minecraft.options.keyUse.isDown();
         if (gun != null && use && !previousUse && !GunAnimationEngine.INSTANCE.isReloading()) {
            if (gun.definition().canScope()) {
               int previousLevel = scopeLevel;
               scopeLevel = (scopeLevel + 1) % (gun.definition().zoomLevels() + 1);
               if (scopeLevel > 0) {
                  float nextFov = gun.definition().zoomFov(scopeLevel);
                  boolean fullyUnscoped = visualScopeLevel == 0 || GunAnimationEngine.INSTANCE.ads(1.0F) <= 0.001F;
                  if (previousLevel == 0 && fullyUnscoped) {
                     previousVisualScopeFov = nextFov;
                     visualScopeFov = nextFov;
                  }

                  targetVisualScopeFov = nextFov;
                  visualScopeLevel = scopeLevel;
               }
            } else if (gun.definition().burstCapable()) {
               burstMode = !burstMode;
               burstShotsRemaining = 0;
            }
         }

         previousUse = use;
         boolean aiming = gun != null && isAiming();
         double horizontalSpeed = Math.sqrt(
            player.getDeltaMovement().x * player.getDeltaMovement().x + player.getDeltaMovement().z * player.getDeltaMovement().z
         );
         float movementAmount = Mth.clamp((float)(horizontalSpeed / 0.13), 0.0F, 1.0F);
         boolean sprinting = player.isSprinting() && movementAmount > 0.08F;
         GunAnimationEngine.INSTANCE.tick(gun, aiming, sprinting, movementAmount);
         previousVisualScopeFov = visualScopeFov;
         float zoomHalfLife = Math.max(0.012F, gun == null ? 0.05F : gun.definition().zoomSeconds() * 0.55F);
         float zoomBlend = 1.0F - (float)Math.exp(-Math.log(2.0) * 0.05F / zoomHalfLife);
         visualScopeFov = visualScopeFov + (targetVisualScopeFov - visualScopeFov) * zoomBlend;
         if (scopeLevel == 0 && GunAnimationEngine.INSTANCE.ads(1.0F) <= 0.001F) {
            visualScopeLevel = 0;
            scopeOverlayLatched = false;
         }

         GunReloadSoundPlayer.tick(player, gun, GunAnimationEngine.INSTANCE.isReloading());
         if (gun == null) {
            previousAttack = false;
         } else {
            while (RELOAD.consumeClick()) {
               startReload(gun, stack);
            }

            boolean attack = ClientCompat.screen(minecraft) == null && minecraft.options.keyAttack.isDown();
            if (GunAnimationEngine.INSTANCE.isReloading()) {
               burstShotsRemaining = 0;
               resetRevolverTrigger();
               if (!canInterruptShellReload(gun, stack, attack)) {
                  previousAttack = attack;
                  return;
               }

               GunAnimationEngine.INSTANCE.cancelReload();
               GunReloadSoundPlayer.reset();
               automaticBudget = 1.0;
            }

            if (gun.definition().revolver()) {
               tickRevolver(gun, stack, attack, aiming);
            } else if (burstMode && gun.definition().burstCapable()) {
               tickBurst(gun, stack, attack, aiming);
            } else if (gun.definition().fireMode() == FireMode.AUTOMATIC) {
               tickAutomatic(gun, stack, attack, aiming);
            } else if (attack && !previousAttack) {
               trySemiAutomatic(gun, stack, aiming);
            }

            previousAttack = attack;
         }
      } else {
         currentGun = null;
         previousAttack = false;
         previousUse = false;
         resetPredictedAccuracy();
         scopeLevel = 0;
         visualScopeLevel = 0;
         resetVisualScopeFov(null);
         scopeOverlayLatched = false;
         burstMode = false;
         burstShotsRemaining = 0;
         resetRevolverTrigger();
         revolverCycleReadyNanos = 0L;
         GunAnimationEngine.INSTANCE.tick(null, false, false, 0.0F);
         GunReloadSoundPlayer.reset();
      }
   }

   private static boolean canInterruptShellReload(GunItem gun, ItemStack stack, boolean attack) {
      GunDefinition definition = gun.definition();
      return attack && !previousAttack && definition.category() == WeaponCategory.SHOTGUN && !definition.reserveAsMagazines() && gun.ammo(stack) > 0
         ? definition.fireMode() == FireMode.AUTOMATIC || System.nanoTime() >= nextSemiShotNanos
         : false;
   }

   private static void tickRevolver(GunItem gun, ItemStack stack, boolean attack, boolean aiming) {
      if (attack && gun.ammo(stack) > 0) {
         long now = System.nanoTime();
         if (!previousAttack || revolverChargeStartNanos == 0L) {
            revolverChargeStartNanos = now;
            nextRevolverShotNanos = Math.max(now + gun.definition().revolverFireDelayNanos(), revolverCycleReadyNanos);
         }

         ClientPlayNetworking.send(new FirePayload(aiming, false, true));
         if (now >= nextRevolverShotNanos) {
            playPredictedShot(gun, stack, aiming, false);
            revolverCycleReadyNanos = now + gun.definition().shotIntervalNanos();
            nextRevolverShotNanos = revolverCycleReadyNanos;
         }
      } else {
         if (!attack && previousAttack) {
            ClientPlayNetworking.send(new FirePayload(false, false, false));
         }

         resetRevolverTrigger();
      }
   }

   private static void resetRevolverTrigger() {
      revolverChargeStartNanos = 0L;
      nextRevolverShotNanos = 0L;
   }

   private static void tickBurst(GunItem gun, ItemStack stack, boolean attack, boolean aiming) {
      long now = System.nanoTime();
      if (attack && !previousAttack && burstShotsRemaining == 0 && now >= nextBurstAllowedNanos && gun.ammo(stack) > 0) {
         burstShotsRemaining = Math.min(3, gun.ammo(stack));
         nextBurstShotNanos = now;
         nextBurstAllowedNanos = now + (long)(gun.definition().burstCycleTimeSeconds() * 1.0E9);
      }

      if (burstShotsRemaining > 0 && now >= nextBurstShotNanos) {
         ClientPlayNetworking.send(new FirePayload(aiming, true, true));
         playPredictedShot(gun, stack, aiming, true);
         burstShotsRemaining--;
         nextBurstShotNanos = nextBurstShotNanos + (long)(gun.definition().burstShotIntervalSeconds() * 1.0E9);
      }
   }

   private static void tickAutomatic(GunItem gun, ItemStack stack, boolean attack, boolean aiming) {
      if (attack && gun.ammo(stack) > 0) {
         if (!previousAttack) {
            automaticBudget = 1.0;
         } else {
            automaticBudget = Math.min(2.0, automaticBudget + gun.definition().roundsPerMinute() / 1200.0);
         }

         ClientPlayNetworking.send(new FirePayload(aiming, false, true));
         if (automaticBudget >= 1.0) {
            automaticBudget--;
            playPredictedShot(gun, stack, aiming, false);
         }
      } else {
         automaticBudget = 1.0;
      }
   }

   private static void trySemiAutomatic(GunItem gun, ItemStack stack, boolean aiming) {
      long now = System.nanoTime();
      if (gun.ammo(stack) > 0 && now >= nextSemiShotNanos) {
         nextSemiShotNanos = now + gun.definition().shotIntervalNanos();
         ClientPlayNetworking.send(new FirePayload(aiming, false, true));
         playPredictedShot(gun, stack, aiming, false);
      }
   }

   private static void playPredictedShot(GunItem gun, ItemStack stack, boolean scoped, boolean alternateFireMode) {
      if (gun.ammo(stack) > 0) {
         long now = System.nanoTime();
         LocalPlayer player = Minecraft.getInstance().player;
         float recoveredPenalty = recoveredAccuracyPenalty(player, gun.definition(), now);
         if (now - lastLocalShotNanos > 400000000L) {
            shotSequence = 0;
         }

         lastLocalShotNanos = now;
         int sequence = shotSequence;
         GunAnimationEngine.INSTANCE.onFire(gun.definition(), sequence, scoped, player.getMainArm());
         applyCameraRecoil(player, gun.definition(), sequence, scoped);
         accuracyPenalty = Math.min(0.75F, recoveredPenalty + gun.definition().fireInaccuracy(scoped, alternateFireMode));
         shotSequence = sequence + 1;
         if (gun.definition().unzoomsAfterShot()) {
            scopeLevel = 0;
         }
      }
   }

   private static float recoveredAccuracyPenalty(LocalPlayer player, GunDefinition gun, long now) {
      if (player != null && lastLocalShotNanos != 0L && !(accuracyPenalty <= 0.0F)) {
         float elapsedSeconds = Math.max(0.0F, (float)(now - lastLocalShotNanos) / 1.0E9F);
         float recovery = Math.max(0.01F, gun.recoverySeconds(player.isCrouching(), shotSequence));
         return accuracyPenalty * (float)Math.exp(-Math.log(10.0) * elapsedSeconds / recovery);
      } else {
         return 0.0F;
      }
   }

   private static void resetPredictedAccuracy() {
      lastLocalShotNanos = 0L;
      shotSequence = 0;
      accuracyPenalty = 0.0F;
   }

   private static void startReload(GunItem gun, ItemStack stack) {
      if (!GunAnimationEngine.INSTANCE.isReloading() && gun.ammo(stack) < gun.definition().magazineSize() && gun.reserveAmmo(stack) > 0) {
         boolean empty = gun.ammo(stack) == 0;
         int reloadUnits = gun.plannedReloadUnits(stack);
         scopeLevel = 0;
         burstShotsRemaining = 0;
         resetRevolverTrigger();
         revolverCycleReadyNanos = 0L;
         GunAnimationEngine.INSTANCE.onReload(empty, reloadUnits);
         GunReloadSoundPlayer.start(Minecraft.getInstance().player, gun, empty, reloadUnits);
         ClientPlayNetworking.send(ReloadPayload.INSTANCE);
      }
   }

   private static void applyCameraRecoil(LocalPlayer player, GunDefinition gun, int sequence, boolean scoped) {
      if (player != null) {
         float pitch = gun.recoilPitchDegrees(scoped, sequence);
         float yaw = gun.recoilYawDegrees(scoped, sequence);
         player.setXRot(Mth.clamp(player.getXRot() - pitch * 0.55F, -90.0F, 90.0F));
         player.setYRot(player.getYRot() + yaw * 0.65F);
      }
   }

   private static void resetVisualScopeFov(GunDefinition gun) {
      float fov = gun != null && gun.canScope() ? gun.zoomFov(1) : 90.0F;
      previousVisualScopeFov = fov;
      visualScopeFov = fov;
      targetVisualScopeFov = fov;
   }
}
