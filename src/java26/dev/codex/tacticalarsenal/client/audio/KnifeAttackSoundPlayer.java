package dev.codex.tacticalarsenal.client.audio;

import dev.codex.tacticalarsenal.client.KnifeClientController;
import dev.codex.tacticalarsenal.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class KnifeAttackSoundPlayer {
   private static final long WALL_DELAY_NANOS = 90000000L;
   private static long pendingWallNanos = Long.MIN_VALUE;

   private KnifeAttackSoundPlayer() {
   }

   public static void play(LocalPlayer player, KnifeClientController.Contact contact) {
      player.playSound(ModSounds.KNIFE_SWING, 0.3F, 1.01F);
      switch (contact) {
         case MISS:
         default:
            break;
         case WALL:
            pendingWallNanos = System.nanoTime() + 90000000L;
            break;
         case HIT:
            player.playSound(ModSounds.KNIFE_HIT_FLESH, 1.0F, 1.4F);
            break;
         case BACKSTAB:
            player.playSound(ModSounds.KNIFE_BACKSTAB_FLESH, 0.3F, 1.0F);
      }
   }

   public static void tick() {
      if (pendingWallNanos != Long.MIN_VALUE && System.nanoTime() >= pendingWallNanos) {
         pendingWallNanos = Long.MIN_VALUE;
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            player.playSound(ModSounds.KNIFE_HIT_WALL, 1.0F, 1.0F);
         }
      }
   }
}
