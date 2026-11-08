package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.animation.AnimationClip;
import dev.codex.tacticalarsenal.client.animation.GunAnimationLibrary;
import dev.codex.tacticalarsenal.client.animation.GunReloadAnimationLibrary;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GunAnimationLibrary.class})
public abstract class GunAnimationLibraryMixin {
   @Inject(
      method = {"reload"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void tacticalArsenal$useWeaponReload(GunDefinition gun, boolean empty, CallbackInfoReturnable<AnimationClip> callback) {
      if (!gun.id().equals("elite")) {
         AnimationClip clip = GunReloadAnimationLibrary.clip(gun.id(), empty);
         if (clip != null) {
            callback.setReturnValue(clip);
         }
      }
   }

   @Inject(
      method = {"shellReload"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void tacticalArsenal$useWeaponShellReload(GunDefinition gun, int requestedShells, CallbackInfoReturnable<AnimationClip> callback) {
      AnimationClip clip = GunReloadAnimationLibrary.shellReload(gun.id(), Math.max(1, requestedShells));
      if (clip != null) {
         callback.setReturnValue(clip);
      }
   }

   @Inject(
      method = {"reloadTimeline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void tacticalArsenal$syncReloadEvents(GunDefinition gun, boolean empty, CallbackInfoReturnable<GunAnimationLibrary.ReloadTimeline> callback) {
      if (!gun.id().equals("ak47") && !gun.id().equals("m4a1_silencer") && !gun.id().equals("awp") && !gun.id().equals("desert_eagle")) {
         float duration = empty ? gun.emptyReloadAnimationSeconds() : gun.reloadAnimationSeconds();
         if (!(duration > 0.0F) || !Float.isFinite(duration)) {
            duration = gun.reloadSeconds();
         }

         float addAmmo = empty ? gun.emptyReloadInsertSeconds() : gun.reloadInsertSeconds();
         if (!(addAmmo > 0.0F) || !Float.isFinite(addAmmo)) {
            addAmmo = duration * 0.64F;
         }

         addAmmo = Math.min(duration, addAmmo);
         float clipOut = Math.min(addAmmo * 0.46F, duration * 0.24F);
         float dropMagazine = Math.min(addAmmo * 0.68F, duration * 0.36F);
         float clipIn = Math.max(dropMagazine + 0.05F, addAmmo - Math.max(0.055F, duration * 0.055F));
         clipIn = Math.min(addAmmo, clipIn);
         boolean cycleAction = empty || gun.category() == WeaponCategory.MACHINE_GUN;
         float actionBack = cycleAction ? Math.min(duration, Math.max(addAmmo + 0.05F, duration * 0.78F)) : 0.0F;
         float actionForward = cycleAction ? Math.min(duration, Math.max(actionBack + 0.08F, duration * 0.91F)) : 0.0F;
         float actionTravel = gun.category() == WeaponCategory.PISTOL ? 0.32F : 0.22F;
         String var13 = gun.id();

         float actionRotation = switch (var13) {
            case "ssg08" -> 24.0F;
            case "revolver" -> 18.0F;
            default -> 0.0F;
         };
         callback.setReturnValue(
            new GunAnimationLibrary.ReloadTimeline(clipOut, dropMagazine, clipIn, addAmmo, actionBack, actionForward, actionTravel, actionRotation)
         );
      }
   }
}
