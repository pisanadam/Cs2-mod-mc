package dev.codex.tacticalarsenal.knife;

import dev.codex.tacticalarsenal.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class KnifeServerSoundController {
   private KnifeServerSoundController() {
   }

   public static void broadcastEntityImpact(ServerLevel level, Player attacker, Entity target) {
      level.playSound(attacker, attacker.getX(), attacker.getEyeY(), attacker.getZ(), ModSounds.KNIFE_SWING, SoundSource.PLAYERS, 0.3F, 1.01F);
      boolean backstab = target instanceof LivingEntity living && isBehind(attacker, living);
      SoundEvent sound = backstab ? ModSounds.KNIFE_BACKSTAB_FLESH : ModSounds.KNIFE_HIT_FLESH;
      float volume = backstab ? 0.3F : 1.0F;
      float pitch = backstab ? 1.0F : 1.4F;
      level.playSound(attacker, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
   }

   private static boolean isBehind(Player attacker, LivingEntity target) {
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
}
