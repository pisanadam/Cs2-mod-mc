package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunDefinitions;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public final class ModSounds {
   public static final String RELOAD_START = "reload_start";
   public static final String CLIP_OUT = "clip_out";
   public static final String CLIP_IN = "clip_in";
   public static final String CLIP_HIT = "clip_hit";
   public static final String ADD_AMMO = "add_ammo";
   public static final String ACTION_BACK = "action_back";
   public static final String ACTION_FORWARD = "action_forward";
   public static final String SHELL_INSERT = "shell_insert";
   public static final SoundEvent KNIFE_SWING = register("weapon.knife.swing");
   public static final SoundEvent KNIFE_HIT_FLESH = register("weapon.knife.hit_flesh");
   public static final SoundEvent KNIFE_HIT_WALL = register("weapon.knife.hit_wall");
   public static final SoundEvent KNIFE_BACKSTAB_FLESH = register("weapon.knife.backstab_flesh");
   private static final String FIRE = "fire";
   private static final String[] CUES = new String[]{
      "fire", "reload_start", "clip_out", "clip_in", "clip_hit", "add_ammo", "action_back", "action_forward", "shell_insert"
   };
   private static final Map<String, Map<String, SoundEvent>> WEAPONS = new LinkedHashMap<>();
   private static final Map<String, ModSounds.FireProfile> FIRE_PROFILES = new LinkedHashMap<>();

   private ModSounds() {
   }

   public static void initialize() {
   }

   private static SoundEvent register(String path) {
      Identifier id = TacticalArsenal.id(path);
      return (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   public static SoundEvent fire(String weaponId) {
      return cue(weaponId, "fire");
   }

   public static SoundEvent cue(String weaponId, String cue) {
      Map<String, SoundEvent> weapon = WEAPONS.get(weaponId);
      if (weapon != null && weapon.containsKey(cue)) {
         return weapon.get(cue);
      } else {
         throw new IllegalArgumentException("Unknown Tactical Arsenal sound: " + weaponId + "/" + cue);
      }
   }

   public static float fireVolume(String weaponId) {
      return profile(weaponId).volume();
   }

   public static float firePitch(String weaponId, RandomSource random) {
      ModSounds.FireProfile profile = profile(weaponId);
      return profile.pitch() + profile.randomMin() + random.nextFloat() * (profile.randomMax() - profile.randomMin());
   }

   private static ModSounds.FireProfile profile(String weaponId) {
      ModSounds.FireProfile profile = FIRE_PROFILES.get(weaponId);
      if (profile == null) {
         throw new IllegalArgumentException("Unknown Tactical Arsenal fire profile: " + weaponId);
      } else {
         return profile;
      }
   }

   private static ModSounds.FireProfile fireProfile(String weaponId) {
      float pitch = switch (weaponId) {
         case "glock" -> 0.9F;
         case "elite", "galilar" -> 1.1F;
         case "m4a4" -> 1.2F;
         default -> 1.0F;
      };

      float volume = switch (weaponId) {
         case "ak47" -> 1.1F;
         case "m4a1_silencer", "tec9", "cz75a", "revolver", "p90", "bizon", "xm1014", "ssg08" -> 0.8F;
         case "usp_silencer" -> 0.6F;
         case "desert_eagle", "p250", "ump45", "nova", "mag7", "sawedoff", "famas", "sg556", "aug" -> 0.9F;
         case "mac10" -> 0.65F;
         case "mp9" -> 0.75F;
         case "m249" -> 1.7F;
         default -> 1.0F;
      };
      float randomMin;
      float randomMax;
      if (weaponId.equals("glock")) {
         randomMin = -0.03F;
         randomMax = 0.0F;
      } else {
         float variance = switch (weaponId) {
            case "hkp2000", "tec9" -> 0.05F;
            case "sg556" -> 0.04F;
            case "awp", "desert_eagle", "elite", "cz75a", "mac10", "mp9", "mp7", "mp5sd", "ump45", "p90", "bizon", "xm1014", "sawedoff", "galilar", "m4a4", "ssg08", "aug", "g3sg1", "scar20" -> 0.03F;
            case "p250" -> 0.02F;
            case "fiveseven", "famas" -> 0.01F;
            default -> 0.0F;
         };
         randomMin = -variance;
         randomMax = variance;
      }

      return new ModSounds.FireProfile(volume, pitch, randomMin, randomMax);
   }

   static {
      for (GunDefinition gun : GunDefinitions.ALL) {
         Map<String, SoundEvent> cues = new LinkedHashMap<>();

         for (String cue : CUES) {
            Identifier id = TacticalArsenal.id("weapon." + gun.id() + "." + cue);
            SoundEvent event = (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
            cues.put(cue, event);
         }

         WEAPONS.put(gun.id(), Map.copyOf(cues));
         FIRE_PROFILES.put(gun.id(), fireProfile(gun.id()));
      }
   }

   private record FireProfile(float volume, float pitch, float randomMin, float randomMax) {
   }
}
