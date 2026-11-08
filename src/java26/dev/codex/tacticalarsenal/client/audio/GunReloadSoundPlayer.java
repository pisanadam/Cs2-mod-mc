package dev.codex.tacticalarsenal.client.audio;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.client.animation.GunAnimationLibrary;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import dev.codex.tacticalarsenal.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class GunReloadSoundPlayer {
   private static final float CLIENT_TICK_SECONDS = 0.05F;
   private static GunItem activeGun;
   private static List<GunReloadSoundPlayer.Cue> cues = List.of();
   private static int nextCue;
   private static float elapsedSeconds;
   private static boolean active;

   private GunReloadSoundPlayer() {
   }

   public static void start(LocalPlayer player, GunItem gun, boolean empty, int reloadUnits) {
      reset();
      activeGun = gun;
      active = true;
      boolean localCs2Sounds = cueAvailable(gun.definition(), "fire");
      cues = gun.definition().reserveAsMagazines()
         ? (localCs2Sounds ? exactMagazineCues(gun.definition(), empty) : magazineCues(gun.definition(), empty))
         : (localCs2Sounds ? exactShellCues(gun.definition(), reloadUnits) : shellCues(gun.definition(), reloadUnits));
      playDueCues(player);
   }

   public static void tick(LocalPlayer player, GunItem heldGun, boolean reloading) {
      if (active) {
         if (heldGun != activeGun) {
            reset();
         } else {
            elapsedSeconds += 0.05F;
            playDueCues(player);
            if (!reloading) {
               reset();
            }
         }
      }
   }

   public static void reset() {
      activeGun = null;
      cues = List.of();
      nextCue = 0;
      elapsedSeconds = 0.0F;
      active = false;
   }

   private static void playDueCues(LocalPlayer player) {
      while (nextCue < cues.size() && cues.get(nextCue).timeSeconds <= elapsedSeconds + 1.0E-4F) {
         GunReloadSoundPlayer.Cue cue = cues.get(nextCue++);
         player.playSound(cue.sound, cue.volume, cue.pitch);
      }
   }

   private static List<GunReloadSoundPlayer.Cue> magazineCues(GunDefinition gun, boolean empty) {
      GunAnimationLibrary.ReloadTimeline timeline = GunAnimationLibrary.reloadTimeline(gun, empty);
      List<GunReloadSoundPlayer.Cue> result = new ArrayList<>();
      float tone = pitchOffset(gun);
      add(result, 0.0F, (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.18F, 1.42F + tone);
      add(result, timeline.clipOut() * 0.58F, SoundEvents.BUNDLE_REMOVE_ONE, 0.22F, 1.18F + tone);
      add(result, timeline.clipOut(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.42F, 1.34F + tone);
      add(result, timeline.clipOut() + 0.018F, SoundEvents.IRON_TRAPDOOR_OPEN, 0.1F, 1.86F + tone);
      add(result, timeline.dropMagazine(), SoundEvents.CHAIN_HIT, 0.13F, 1.72F + tone);
      add(result, Math.max(timeline.dropMagazine() + 0.08F, timeline.clipIn() - 0.24F), SoundEvents.BUNDLE_INSERT, 0.19F, 0.93F + tone);
      add(result, timeline.clipIn(), SoundEvents.ITEM_FRAME_ADD_ITEM, 0.38F, 1.22F + tone);
      add(result, timeline.clipIn() + 0.022F, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.13F, 1.73F + tone);
      add(result, timeline.addAmmo(), SoundEvents.STONE_BUTTON_CLICK_ON, 0.16F, 1.31F + tone);
      if (timeline.actionBack() > 0.0F) {
         add(result, timeline.actionBack(), SoundEvents.IRON_TRAPDOOR_OPEN, 0.15F, 1.43F + tone);
         add(result, Math.min(gun.reloadSeconds(), timeline.actionBack() + 0.075F), SoundEvents.SHEARS_SNIP, 0.16F, 0.88F + tone);
         add(result, timeline.actionForward(), SoundEvents.IRON_TRAPDOOR_CLOSE, 0.21F, 1.31F + tone);
         add(result, Math.min(gun.reloadSeconds(), timeline.actionForward() + 0.035F), SoundEvents.STONE_BUTTON_CLICK_OFF, 0.18F, 1.17F + tone);
      }

      return sorted(result);
   }

   private static List<GunReloadSoundPlayer.Cue> exactMagazineCues(GunDefinition gun, boolean empty) {
      if (gun.id().equals("elite")) {
         return exactDualBerettasCues(gun);
      } else {
         GunAnimationLibrary.ReloadTimeline timeline = GunAnimationLibrary.reloadTimeline(gun, empty);
         List<GunReloadSoundPlayer.Cue> result = new ArrayList<>();
         addExact(result, gun, "reload_start", 0.0F);
         addExact(result, gun, "clip_out", timeline.clipOut());
         addExact(result, gun, "clip_in", timeline.clipIn());
         addExact(result, gun, "clip_hit", Math.min(gun.reloadSeconds(), timeline.clipIn() + 0.065F));
         addExact(result, gun, "add_ammo", timeline.addAmmo());
         boolean cyclesAction = empty || gun.category() == WeaponCategory.MACHINE_GUN;
         if (cyclesAction) {
            float actionBack = timeline.actionBack() > 0.0F ? timeline.actionBack() : Math.max(timeline.addAmmo() + 0.05F, gun.reloadSeconds() * 0.78F);
            float actionForward = timeline.actionForward() > 0.0F ? timeline.actionForward() : Math.max(actionBack + 0.08F, gun.reloadSeconds() * 0.91F);
            addExact(result, gun, "action_back", Math.min(gun.reloadSeconds(), actionBack));
            addExact(result, gun, "action_forward", Math.min(gun.reloadSeconds(), actionForward));
         }

         return sorted(result);
      }
   }

   private static List<GunReloadSoundPlayer.Cue> exactDualBerettasCues(GunDefinition gun) {
      List<GunReloadSoundPlayer.Cue> result = new ArrayList<>();
      addExact(result, gun, "reload_start", 0.0F);
      addExact(result, gun, "clip_out", 0.267F);
      addExact(result, gun, "clip_out", 0.333F);
      addExact(result, gun, "clip_in", 1.667F);
      addExact(result, gun, "clip_hit", 2.8F);
      addExact(result, gun, "add_ammo", 2.933F);
      addExact(result, gun, "action_forward", 3.367F);
      addExact(result, gun, "action_forward", 3.533F);
      return sorted(result);
   }

   private static List<GunReloadSoundPlayer.Cue> shellCues(GunDefinition gun, int requestedShells) {
      int shells = Math.max(1, requestedShells);
      float cycle = GunAnimationLibrary.shellReloadCycle(gun);
      float tone = pitchOffset(gun);
      List<GunReloadSoundPlayer.Cue> result = new ArrayList<>();
      add(result, 0.0F, (SoundEvent)SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.17F, 1.36F + tone);

      for (int shell = 0; shell < shells; shell++) {
         float start = shell * cycle;
         add(result, start + cycle * 0.17F, SoundEvents.BUNDLE_REMOVE_ONE, 0.2F, 1.11F + tone);
         add(result, start + cycle * 0.58F, SoundEvents.ITEM_FRAME_ADD_ITEM, 0.34F, 1.35F + tone);
         add(result, start + cycle * 0.61F, SoundEvents.IRON_HIT, 0.09F, 1.79F + tone);
      }

      float duration = cycle * shells;
      add(result, Math.max(0.0F, duration - cycle * 0.16F), SoundEvents.IRON_TRAPDOOR_OPEN, 0.12F, 1.51F + tone);
      add(result, duration, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.18F, 1.36F + tone);
      return sorted(result);
   }

   private static List<GunReloadSoundPlayer.Cue> exactShellCues(GunDefinition gun, int requestedShells) {
      int shells = Math.max(1, requestedShells);
      float cycle = GunAnimationLibrary.shellReloadCycle(gun);
      List<GunReloadSoundPlayer.Cue> result = new ArrayList<>();
      addExact(result, gun, "reload_start", 0.0F);

      for (int shell = 0; shell < shells; shell++) {
         float start = shell * cycle;
         addExact(result, gun, "add_ammo", start + cycle * 0.17F);
         addExact(result, gun, "shell_insert", start + cycle * 0.58F);
      }

      float duration = cycle * shells;
      addExact(result, gun, "action_back", Math.max(0.0F, duration - cycle * 0.22F));
      addExact(result, gun, "action_forward", Math.max(0.0F, duration - cycle * 0.08F));
      return sorted(result);
   }

   private static float pitchOffset(GunDefinition gun) {
      return switch (gun.category()) {
         case PISTOL -> 0.1F;
         case SNIPER_RIFLE, MACHINE_GUN -> -0.1F;
         default -> 0.0F;
      };
   }

   private static void add(List<GunReloadSoundPlayer.Cue> cues, float time, SoundEvent sound, float volume, float pitch) {
      cues.add(new GunReloadSoundPlayer.Cue(Math.max(0.0F, time), sound, volume, pitch));
   }

   private static void addExact(List<GunReloadSoundPlayer.Cue> cues, GunDefinition gun, String cue, float time) {
      if (cueAvailable(gun, cue)) {
         add(cues, time, ModSounds.cue(gun.id(), cue), 1.0F, 1.0F);
      }
   }

   private static boolean cueAvailable(GunDefinition gun, String cue) {
      return Minecraft.getInstance().getResourceManager().getResource(TacticalArsenal.id("sounds/cs2/" + gun.id() + "/" + cue + "_1.ogg")).isPresent();
   }

   private static List<GunReloadSoundPlayer.Cue> sorted(List<GunReloadSoundPlayer.Cue> cues) {
      cues.sort(Comparator.comparingDouble(GunReloadSoundPlayer.Cue::timeSeconds));
      return List.copyOf(cues);
   }

   private record Cue(float timeSeconds, SoundEvent sound, float volume, float pitch) {
   }
}
