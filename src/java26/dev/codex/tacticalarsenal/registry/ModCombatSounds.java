package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ModCombatSounds {
   public static final SoundEvent BODY = register("combat.bullet.body");
   public static final SoundEvent KEVLAR = register("combat.bullet.kevlar");
   public static final SoundEvent HEADSHOT = register("combat.bullet.headshot");
   public static final SoundEvent HEADSHOT_ARMOR = register("combat.bullet.headshot_armor");
   public static final SoundEvent HEADSHOT_ARMOR_FLESH = register("combat.bullet.headshot_armor_flesh");

   private ModCombatSounds() {
   }

   public static void initialize() {
   }

   public static void playBulletHit(ServerLevel level, Vec3 position, boolean headshot, boolean armored, boolean killed) {
      float deathScale = killed ? 1.12F : 1.0F;
      if (headshot) {
         if (armored) {
            play(level, position, HEADSHOT_ARMOR, 0.7F * deathScale, 1.0F);
            play(level, position, HEADSHOT_ARMOR_FLESH, 0.24F * deathScale, 1.0F);
         } else {
            play(level, position, HEADSHOT, 0.55F * deathScale, 1.0F);
         }
      } else {
         play(level, position, BODY, 0.72F * deathScale, 1.0F);
         if (armored) {
            play(level, position, KEVLAR, 0.88F * deathScale, 1.0F);
         }
      }
   }

   private static SoundEvent register(String path) {
      Identifier id = TacticalArsenal.id(path);
      return (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   private static void play(ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {
      level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, volume, pitch);
   }
}
