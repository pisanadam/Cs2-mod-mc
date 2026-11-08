package dev.codex.tacticalarsenal.client;

import dev.codex.tacticalarsenal.client.animation.KnifeAnimationLibrary;
import dev.codex.tacticalarsenal.client.audio.KnifeAttackSoundPlayer;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class KnifeClientController {
   private static final float ATTACK_BLEND_OUT_SECONDS = 0.2F;
   private static final long NANOS_PER_SECOND = 1000000000L;
   private static KnifeItem currentKnife;
   private static KnifeClientController.ActiveClip activeClip;
   private static long nextAllowedAttackTick = Long.MIN_VALUE;
   private static int nextAttackVariant;

   private KnifeClientController() {
   }

   public static void initialize() {
      KnifeAnimationLibrary.initialize();
      ClientTickEvents.END_CLIENT_TICK.register(KnifeClientController::tick);
   }

   public static boolean isHoldingKnife() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof KnifeItem;
   }

   public static boolean tryStartAttack() {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.level != null && player.getMainHandItem().getItem() instanceof KnifeItem knife) {
         synchronizeHeldKnife(knife);
         if (player.isSpectator()) {
            return true;
         } else {
            long gameTime = minecraft.level.getGameTime();
            if (!player.isHandsBusy() && !(player.getAttackStrengthScale(0.0F) < 0.999F) && gameTime >= nextAllowedAttackTick) {
               KnifeClientController.Contact contact = predictedContact(minecraft, player);
               int variant = nextAttackVariant + 1;
               nextAttackVariant = (nextAttackVariant + 1) % 2;
               String clipId = knife.id() + "_" + contact.clipName + variant;
               float duration = KnifeAnimationLibrary.durationSeconds(clipId, 1.1667F);
               activeClip = new KnifeClientController.ActiveClip(knife, clipId, System.nanoTime(), duration);
               nextAllowedAttackTick = gameTime + contact.cooldownTicks;
               KnifeAttackSoundPlayer.play(player, contact);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return true;
      }
   }

   public static KnifeClientController.Playback playback(KnifeItem knife) {
      KnifeClientController.ActiveClip clip = activeClip;
      if (knife != null && clip != null && clip.knife == knife) {
         float elapsed = elapsedSeconds(clip.startedNanos);
         float blendOut = (elapsed - clip.durationSeconds) / 0.2F;
         if (blendOut >= 1.0F) {
            activeClip = null;
            return null;
         } else {
            return new KnifeClientController.Playback(clip.clipId, Math.min(elapsed, clip.durationSeconds), clip.durationSeconds, Math.max(0.0F, blendOut));
         }
      } else {
         return null;
      }
   }

   private static void tick(Minecraft minecraft) {
      LocalPlayer player = minecraft.player;
      KnifeItem held = player != null && player.getMainHandItem().getItem() instanceof KnifeItem knife ? knife : null;
      synchronizeHeldKnife(held);
      expireFinishedClip();
      KnifeAttackSoundPlayer.tick();
   }

   private static void synchronizeHeldKnife(KnifeItem held) {
      if (held != currentKnife) {
         currentKnife = held;
         activeClip = null;
         nextAllowedAttackTick = Long.MIN_VALUE;
         nextAttackVariant = 0;
      }
   }

   private static void expireFinishedClip() {
      KnifeClientController.ActiveClip clip = activeClip;
      if (clip != null) {
         float end = clip.durationSeconds + 0.2F;
         if (elapsedSeconds(clip.startedNanos) >= end) {
            activeClip = null;
         }
      }
   }

   private static KnifeClientController.Contact predictedContact(Minecraft minecraft, LocalPlayer player) {
      HitResult hit = minecraft.hitResult;
      if (!(hit instanceof EntityHitResult entityHit)) {
         return hit instanceof BlockHitResult blockHit
               && hit.getType() == Type.BLOCK
               && minecraft.level != null
               && !minecraft.level.getBlockState(blockHit.getBlockPos()).isAir()
            ? KnifeClientController.Contact.WALL
            : KnifeClientController.Contact.MISS;
      } else {
         return entityHit.getEntity() instanceof LivingEntity living && isBehind(player, living)
            ? KnifeClientController.Contact.BACKSTAB
            : KnifeClientController.Contact.HIT;
      }
   }

   private static boolean isBehind(LocalPlayer attacker, LivingEntity target) {
      Vec3 targetForward = Vec3.directionFromRotation(0.0F, target.getYRot());
      Vec3 targetToAttacker = attacker.position().subtract(target.position());
      double horizontalLengthSquared = targetToAttacker.x * targetToAttacker.x + targetToAttacker.z * targetToAttacker.z;
      if (horizontalLengthSquared < 1.0E-8) {
         return false;
      } else {
         double inverseLength = 1.0 / Math.sqrt(horizontalLengthSquared);
         double dot = targetForward.x * targetToAttacker.x * inverseLength + targetForward.z * targetToAttacker.z * inverseLength;
         return dot < -0.5;
      }
   }

   private static float elapsedSeconds(long startedNanos) {
      return (float)(System.nanoTime() - startedNanos) / 1.0E9F;
   }

   private record ActiveClip(KnifeItem knife, String clipId, long startedNanos, float durationSeconds) {
   }

   public static enum Contact {
      MISS("attack", 8),
      WALL("hit", 10),
      HIT("hit", 10),
      BACKSTAB("backstab", 10);

      private final String clipName;
      private final int cooldownTicks;

      private Contact(String clipName, int cooldownTicks) {
         this.clipName = clipName;
         this.cooldownTicks = cooldownTicks;
      }
   }

   public record Playback(String clipId, float elapsedSeconds, float clipDurationSeconds, float blendOutProgress) {
   }
}
