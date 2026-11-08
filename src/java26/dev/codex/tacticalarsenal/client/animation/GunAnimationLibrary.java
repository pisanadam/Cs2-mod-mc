package dev.codex.tacticalarsenal.client.animation;

import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import java.util.ArrayList;
import java.util.List;

public final class GunAnimationLibrary {
   private GunAnimationLibrary() {
   }

   public static GunAnimationLibrary.AnimationSet create(GunDefinition gun) {
      return new GunAnimationLibrary.AnimationSet(equip(gun), fire(gun), reload(gun, false), reload(gun, true));
   }

   private static AnimationClip equip(GunDefinition gun) {
      float duration = gun.deploySeconds();
      return AnimationClip.builder(gun.id() + "/equip", duration)
         .track(
            Bone.ROOT,
            key(0.0F, Transform.of(0.34F, -0.52F, 0.34F, 30.0F, -25.0F, 18.0F), Easing.EASE_OUT_CUBIC),
            key(duration * 0.72F, Transform.of(-0.02F, 0.015F, 0.0F, -1.0F, 1.5F, 0.0F), Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .build();
   }

   private static AnimationClip fire(GunDefinition gun) {
      if (gun.id().equals("awp") || gun.id().equals("ssg08")) {
         return boltFire(gun);
      } else {
         return gun.category() == WeaponCategory.SHOTGUN && !gun.id().equals("xm1014")
            ? pumpFire(gun)
            : slideFire(gun, gun.category() == WeaponCategory.PISTOL ? 0.34F : 0.2F);
      }
   }

   private static AnimationClip slideFire(GunDefinition gun, float actionTravel) {
      float duration = Math.max(gun.cycleTimeSeconds(), 0.12F);
      float kick = Math.min(0.13F, gun.recoilPitchDegrees(false, 0) * 0.042F);
      float actionBack = Math.min(duration * 0.28F, 0.035F);
      float actionForward = Math.min(duration * 0.82F, gun.id().equals("desert_eagle") ? 0.19F : 0.09F);
      return AnimationClip.builder(gun.id() + "/fire", duration)
         .track(
            Bone.ROOT,
            key(0.0F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(
               Math.min(0.03F, duration * 0.25F), Transform.of(0.0F, 0.02F, kick, -gun.recoilPitchDegrees(false, 0) * 0.34F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC
            ),
            key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
         )
         .track(
            Bone.ACTION,
            key(0.0F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(actionBack, Transform.of(0.0F, 0.0F, actionTravel, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(actionForward, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .build();
   }

   private static AnimationClip boltFire(GunDefinition gun) {
      float duration = gun.cycleTimeSeconds();
      return AnimationClip.builder(gun.id() + "/fire", duration)
         .track(
            Bone.ROOT,
            key(0.0F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(0.055F, Transform.of(0.0F, 0.035F, 0.14F, -7.5F, 0.8F, -1.0F), Easing.EASE_OUT_CUBIC),
            key(0.28F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.ACTION,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(0.34F, Transform.of(0.0F, 0.0F, 0.02F, 0.0F, 0.0F, 28.0F), Easing.EASE_OUT_CUBIC),
            key(0.54F, Transform.of(0.0F, 0.0F, 0.38F, 0.0F, 0.0F, 28.0F), Easing.EASE_OUT_CUBIC),
            key(0.9F, Transform.of(0.0F, 0.0F, 0.04F, 0.0F, 0.0F, 28.0F), Easing.EASE_IN_CUBIC),
            key(1.08F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.RIGHT_HAND,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(0.2F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(0.34F, Transform.of(-0.09F, 0.11F, 0.07F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(0.54F, Transform.of(-0.071F, 0.102F, 0.098F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(0.9F, Transform.of(-0.088F, 0.109F, 0.073F, 0.0F, 0.0F, 0.0F), Easing.EASE_IN_CUBIC),
            key(1.08F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .build();
   }

   private static AnimationClip pumpFire(GunDefinition gun) {
      float duration = Math.max(0.34F, gun.cycleTimeSeconds());
      float back = Math.min(duration * 0.42F, 0.34F);
      float forward = Math.min(duration * 0.74F, back + 0.28F);
      return AnimationClip.builder(gun.id() + "/fire", duration)
         .track(
            Bone.ROOT,
            key(0.0F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(0.055F, Transform.of(0.0F, 0.04F, 0.13F, -7.0F, 1.4F, -1.5F), Easing.EASE_OUT_CUBIC),
            key(0.26F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.ACTION,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(back, Transform.of(0.0F, 0.0F, 0.34F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(forward, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.LEFT_HAND,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(back, Transform.of(0.019F, -0.008F, 0.027F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(forward, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .build();
   }

   public static AnimationClip shellReload(GunDefinition gun, int requestedShells) {
      int shells = Math.max(1, requestedShells);
      float cycle = shellReloadCycle(gun);
      float duration = cycle * shells;
      List<Keyframe> root = new ArrayList<>();
      List<Keyframe> supportHand = new ArrayList<>();
      root.add(key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP));
      supportHand.add(key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP));

      for (int shell = 0; shell < shells; shell++) {
         float start = shell * cycle;
         root.add(key(start + cycle * 0.18F, Transform.of(0.08F, -0.06F, 0.08F, 10.0F, 18.0F, 6.0F), Easing.SMOOTH_STEP));
         root.add(key(start + cycle * 0.58F, Transform.of(0.12F, -0.02F, 0.05F, 5.0F, 13.0F, 3.0F), Easing.EASE_IN_CUBIC));
         root.add(key(start + cycle * 0.82F, Transform.of(0.07F, -0.04F, 0.07F, 8.0F, 17.0F, 5.0F), Easing.EASE_OUT_CUBIC));
         supportHand.add(key(start + cycle * 0.12F, Transform.of(0.08F, -0.2F, 0.04F, 18.0F, 10.0F, 8.0F), Easing.SMOOTH_STEP));
         supportHand.add(key(start + cycle * 0.42F, Transform.of(0.17F, -0.31F, 0.01F, 27.0F, 18.0F, 15.0F), Easing.EASE_IN_CUBIC));
         supportHand.add(key(start + cycle * 0.61F, Transform.of(0.1F, -0.08F, -0.04F, 8.0F, 7.0F, 5.0F), Easing.EASE_OUT_CUBIC));
         supportHand.add(key(start + cycle * 0.84F, Transform.of(0.05F, -0.03F, 0.0F, 3.0F, 2.0F, 2.0F), Easing.SMOOTH_STEP));
      }

      root.add(key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP));
      supportHand.add(key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP));
      return AnimationClip.builder(gun.id() + "/reload_shells_" + shells, duration)
         .track(Bone.ROOT, root.toArray(Keyframe[]::new))
         .track(Bone.LEFT_HAND, supportHand.toArray(Keyframe[]::new))
         .track(
            Bone.ACTION,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(Math.max(0.0F, duration - cycle * 0.28F), Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(Math.max(0.0F, duration - cycle * 0.16F), Transform.of(0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
         )
         .build();
   }

   public static float shellReloadCycle(GunDefinition gun) {
      return Math.max(0.2F, gun.reloadSeconds());
   }

   private static AnimationClip reload(GunDefinition gun, boolean empty) {
      if (gun.id().equals("elite")) {
         return dualBerettasReload(gun, empty);
      } else {
         GunAnimationLibrary.ReloadTimeline timeline = reloadTimeline(gun, empty);
         float duration = gun.reloadSeconds();
         float settle = Math.min(duration, Math.max(timeline.actionForward + 0.18F, duration - 0.16F));
         AnimationClip.Builder builder = AnimationClip.builder(gun.id() + (empty ? "/reload_empty" : "/reload"), duration)
            .track(
               Bone.ROOT,
               key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
               key(Math.min(0.18F, timeline.clipOut * 0.6F), Transform.of(0.08F, -0.05F, 0.1F, 8.0F, 20.0F, 7.0F), Easing.SMOOTH_STEP),
               key(timeline.clipIn, Transform.of(0.11F, -0.03F, 0.08F, 5.0F, 16.0F, 4.0F), Easing.SMOOTH_STEP),
               key(settle, Transform.of(0.03F, 0.0F, 0.02F, 1.0F, 4.0F, 0.0F), Easing.SMOOTH_STEP),
               key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
            )
            .track(
               Bone.MAGAZINE,
               key(0.0F, Transform.IDENTITY, Easing.LINEAR),
               key(timeline.clipOut, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
               key(timeline.dropMagazine, Transform.of(0.02F, -0.72F, 0.08F, 4.0F, 0.0F, 10.0F), Easing.EASE_OUT_CUBIC),
               key(Math.max(timeline.dropMagazine + 0.08F, timeline.clipIn - 0.34F), Transform.of(0.4F, -1.3F, 0.3F, 12.0F, -8.0F, 28.0F), Easing.SMOOTH_STEP),
               key(
                  Math.max(timeline.dropMagazine + 0.1F, timeline.clipIn - 0.18F), Transform.of(0.28F, -0.46F, 0.18F, 8.0F, -6.0F, 18.0F), Easing.EASE_IN_CUBIC
               ),
               key(timeline.clipIn, Transform.of(0.02F, -0.16F, 0.02F, 2.0F, 0.0F, 3.0F), Easing.EASE_IN_CUBIC),
               key(timeline.addAmmo, Transform.IDENTITY, Easing.SMOOTH_STEP),
               key(duration, Transform.IDENTITY, Easing.LINEAR)
            );
         List<Keyframe> supportHand = new ArrayList<>();
         supportHand.add(key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP));
         supportHand.add(key(Math.min(timeline.clipOut * 0.62F, duration * 0.12F), Transform.of(0.09F, -0.06F, -0.02F, 8.0F, 10.0F, 7.0F), Easing.SMOOTH_STEP));
         supportHand.add(key(timeline.clipOut, Transform.of(0.16F, -0.13F, 0.02F, 18.0F, 17.0F, 14.0F), Easing.EASE_IN_CUBIC));
         supportHand.add(key(timeline.dropMagazine, Transform.of(0.22F, -0.39F, 0.12F, 31.0F, 24.0F, 25.0F), Easing.EASE_OUT_CUBIC));
         supportHand.add(
            key(Math.max(timeline.dropMagazine + 0.08F, timeline.clipIn - 0.34F), Transform.of(0.3F, -0.72F, 0.24F, 40.0F, 30.0F, 34.0F), Easing.SMOOTH_STEP)
         );
         supportHand.add(
            key(Math.max(timeline.dropMagazine + 0.1F, timeline.clipIn - 0.18F), Transform.of(0.23F, -0.38F, 0.13F, 28.0F, 22.0F, 25.0F), Easing.EASE_IN_CUBIC)
         );
         supportHand.add(key(timeline.clipIn, Transform.of(0.12F, -0.09F, 0.01F, 13.0F, 12.0F, 10.0F), Easing.EASE_IN_CUBIC));
         supportHand.add(key(timeline.addAmmo, Transform.of(0.06F, -0.02F, -0.02F, 5.0F, 5.0F, 4.0F), Easing.SMOOTH_STEP));
         if (timeline.actionBack > 0.0F) {
            supportHand.add(
               key(Math.max(timeline.addAmmo + 0.05F, timeline.actionBack - 0.18F), Transform.of(0.15F, 0.16F, -0.04F, -17.0F, 22.0F, 9.0F), Easing.SMOOTH_STEP)
            );
            supportHand.add(key(timeline.actionBack, Transform.of(0.19F, 0.24F, -0.08F, -28.0F, 27.0F, 14.0F), Easing.EASE_IN_CUBIC));
            supportHand.add(
               key(Math.min(duration, timeline.actionBack + 0.075F), Transform.of(0.25F, 0.17F, 0.12F, -18.0F, 31.0F, 17.0F), Easing.EASE_OUT_CUBIC)
            );
            supportHand.add(key(timeline.actionForward, Transform.of(0.18F, 0.22F, -0.05F, -25.0F, 25.0F, 13.0F), Easing.EASE_IN_CUBIC));
            supportHand.add(key(Math.min(duration, timeline.actionForward + 0.13F), Transform.IDENTITY, Easing.SMOOTH_STEP));
         } else {
            supportHand.add(key(Math.min(duration, timeline.addAmmo + 0.28F), Transform.IDENTITY, Easing.SMOOTH_STEP));
         }

         supportHand.add(key(duration, Transform.IDENTITY, Easing.LINEAR));
         builder.track(Bone.LEFT_HAND, supportHand.toArray(Keyframe[]::new));
         builder.track(
            Bone.RIGHT_HAND,
            key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(Math.min(0.16F, timeline.clipOut * 0.55F), Transform.of(-0.02F, 0.01F, 0.015F, -2.0F, -4.0F, -2.0F), Easing.SMOOTH_STEP),
            key(settle, Transform.of(-0.01F, 0.005F, 0.008F, -1.0F, -2.0F, -1.0F), Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
         );
         if (timeline.actionBack > 0.0F) {
            builder.track(
               Bone.ACTION,
               key(0.0F, Transform.IDENTITY, Easing.LINEAR),
               key(timeline.actionBack, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
               key(
                  Math.min(duration, timeline.actionBack + 0.075F),
                  Transform.of(0.0F, 0.0F, timeline.actionTravel, 0.0F, 0.0F, timeline.actionRotation),
                  Easing.EASE_OUT_CUBIC
               ),
               key(
                  timeline.actionForward,
                  Transform.of(0.0F, 0.0F, timeline.actionTravel * 0.12F, 0.0F, 0.0F, timeline.actionRotation * 0.25F),
                  Easing.EASE_IN_CUBIC
               ),
               key(Math.min(duration, timeline.actionForward + 0.1F), Transform.IDENTITY, Easing.SMOOTH_STEP),
               key(duration, Transform.IDENTITY, Easing.LINEAR)
            );
         }

         return builder.build();
      }
   }

   private static AnimationClip dualBerettasReload(GunDefinition gun, boolean empty) {
      float duration = gun.reloadSeconds();
      AnimationClip.Builder builder = AnimationClip.builder(gun.id() + (empty ? "/reload_empty_dual" : "/reload_dual"), duration)
         .track(
            Bone.ROOT,
            key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(0.2F, Transform.of(0.0F, -0.025F, 0.035F, 2.5F, 0.0F, 0.0F), Easing.SMOOTH_STEP),
            key(1.15F, Transform.of(0.0F, -0.055F, 0.07F, 5.0F, 0.0F, 0.0F), Easing.SMOOTH_STEP),
            key(2.45F, Transform.of(0.0F, -0.04F, 0.055F, 3.5F, 0.0F, 0.0F), Easing.SMOOTH_STEP),
            key(3.6F, Transform.of(0.0F, -0.01F, 0.015F, 1.0F, 0.0F, 0.0F), Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
         )
         .track(
            Bone.LEFT_WEAPON,
            key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(0.2F, Transform.of(-0.025F, -0.01F, 0.025F, 3.0F, 5.0F, -6.0F), Easing.SMOOTH_STEP),
            key(0.43F, Transform.of(-0.06F, -0.08F, 0.055F, 9.0F, 12.0F, -15.0F), Easing.EASE_OUT_CUBIC),
            key(1.18F, Transform.of(-0.115F, -0.245F, 0.145F, 18.0F, 24.0F, -29.0F), Easing.SMOOTH_STEP),
            key(1.5F, Transform.of(-0.09F, -0.165F, 0.1F, 13.0F, 18.0F, -22.0F), Easing.EASE_IN_CUBIC),
            key(1.67F, Transform.of(-0.04F, -0.065F, 0.045F, 6.0F, 8.0F, -10.0F), Easing.EASE_OUT_CUBIC),
            key(2.1F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(3.18F, Transform.of(-0.02F, 0.005F, 0.025F, -2.0F, 5.0F, -5.0F), Easing.SMOOTH_STEP),
            key(3.37F, Transform.of(-0.035F, 0.018F, 0.045F, -4.0F, 7.0F, -8.0F), Easing.EASE_OUT_CUBIC),
            key(3.55F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.RIGHT_WEAPON,
            key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(0.2F, Transform.of(0.025F, -0.01F, 0.025F, 3.0F, -5.0F, 6.0F), Easing.SMOOTH_STEP),
            key(0.43F, Transform.of(0.055F, -0.06F, 0.05F, 8.0F, -11.0F, 14.0F), Easing.EASE_OUT_CUBIC),
            key(1.67F, Transform.of(0.035F, -0.08F, 0.055F, 8.0F, -8.0F, 11.0F), Easing.SMOOTH_STEP),
            key(2.35F, Transform.of(0.105F, -0.235F, 0.14F, 18.0F, -23.0F, 28.0F), Easing.SMOOTH_STEP),
            key(2.7F, Transform.of(0.08F, -0.155F, 0.095F, 13.0F, -17.0F, 21.0F), Easing.EASE_IN_CUBIC),
            key(2.8F, Transform.of(0.04F, -0.07F, 0.05F, 7.0F, -9.0F, 11.0F), Easing.EASE_OUT_CUBIC),
            key(3.1F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(3.35F, Transform.of(0.02F, 0.005F, 0.025F, -2.0F, -5.0F, 5.0F), Easing.SMOOTH_STEP),
            key(3.53F, Transform.of(0.035F, 0.018F, 0.045F, -4.0F, -7.0F, 8.0F), Easing.EASE_OUT_CUBIC),
            key(3.68F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.LEFT_HAND,
            key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(0.43F, Transform.of(-0.015F, -0.015F, 0.01F, 3.0F, 5.0F, -5.0F), Easing.SMOOTH_STEP),
            key(1.5F, Transform.of(-0.035F, -0.035F, 0.02F, 7.0F, 10.0F, -11.0F), Easing.EASE_IN_CUBIC),
            key(1.67F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
         )
         .track(
            Bone.RIGHT_HAND,
            key(0.0F, Transform.IDENTITY, Easing.SMOOTH_STEP),
            key(0.43F, Transform.of(0.015F, -0.015F, 0.01F, 3.0F, -5.0F, 5.0F), Easing.SMOOTH_STEP),
            key(2.7F, Transform.of(0.035F, -0.035F, 0.02F, 7.0F, -10.0F, 11.0F), Easing.EASE_IN_CUBIC),
            key(2.8F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(duration, Transform.IDENTITY, Easing.SMOOTH_STEP)
         )
         .track(
            Bone.LEFT_MAGAZINE,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(0.267F, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
            key(0.433F, Transform.of(-0.015F, -0.4F, 0.08F, 4.0F, 0.0F, -9.0F), Easing.EASE_OUT_CUBIC),
            key(0.8F, Transform.of(-0.12F, -1.35F, 0.22F, 12.0F, 7.0F, -28.0F), Easing.EASE_OUT_CUBIC),
            key(1.25F, Transform.of(-0.18F, -1.55F, 0.28F, 15.0F, 9.0F, -34.0F), Easing.LINEAR),
            key(1.5F, Transform.of(-0.06F, -0.54F, 0.13F, 8.0F, 4.0F, -17.0F), Easing.EASE_IN_CUBIC),
            key(1.6F, Transform.of(-0.025F, -0.19F, 0.045F, 3.0F, 1.0F, -6.0F), Easing.EASE_IN_CUBIC),
            key(1.667F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.RIGHT_MAGAZINE,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(0.333F, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
            key(0.433F, Transform.of(0.015F, -0.4F, 0.08F, 4.0F, 0.0F, 9.0F), Easing.EASE_OUT_CUBIC),
            key(0.8F, Transform.of(0.12F, -1.35F, 0.22F, 12.0F, -7.0F, 28.0F), Easing.EASE_OUT_CUBIC),
            key(2.4F, Transform.of(0.18F, -1.55F, 0.28F, 15.0F, -9.0F, 34.0F), Easing.LINEAR),
            key(2.7F, Transform.of(0.06F, -0.54F, 0.13F, 8.0F, -4.0F, 17.0F), Easing.EASE_IN_CUBIC),
            key(2.733F, Transform.of(0.025F, -0.19F, 0.045F, 3.0F, -1.0F, 6.0F), Easing.EASE_IN_CUBIC),
            key(2.8F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         );
      builder.track(
            Bone.LEFT_ACTION,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(3.18F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(3.28F, Transform.of(0.0F, 0.0F, 0.34F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(3.367F, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         )
         .track(
            Bone.RIGHT_ACTION,
            key(0.0F, Transform.IDENTITY, Easing.LINEAR),
            key(3.34F, Transform.IDENTITY, Easing.EASE_OUT_CUBIC),
            key(3.46F, Transform.of(0.0F, 0.0F, 0.34F, 0.0F, 0.0F, 0.0F), Easing.EASE_OUT_CUBIC),
            key(3.533F, Transform.IDENTITY, Easing.EASE_IN_CUBIC),
            key(duration, Transform.IDENTITY, Easing.LINEAR)
         );
      return builder.build();
   }

   public static GunAnimationLibrary.ReloadTimeline reloadTimeline(GunDefinition gun, boolean empty) {
      String var2 = gun.id();

      return switch (var2) {
         case "ak47" -> new GunAnimationLibrary.ReloadTimeline(0.367F, 0.6F, 1.067F, 1.1F, empty ? 1.6F : 0.0F, empty ? 1.76F : 0.0F, 0.3F, 0.0F);
         case "m4a1_silencer" -> new GunAnimationLibrary.ReloadTimeline(0.433F, 0.633F, 1.167F, 1.367F, empty ? 2.0F : 0.0F, empty ? 2.16F : 0.0F, 0.2F, 0.0F);
         case "awp" -> new GunAnimationLibrary.ReloadTimeline(0.267F, 0.667F, 1.533F, 2.0F, empty ? 2.633F : 0.0F, empty ? 2.967F : 0.0F, 0.4F, 28.0F);
         case "desert_eagle" -> empty
            ? new GunAnimationLibrary.ReloadTimeline(0.2F, 0.5F, 0.933F, 1.033F, 1.4F, 1.5F, 0.34F, 0.0F)
            : new GunAnimationLibrary.ReloadTimeline(0.133F, 0.4F, 0.7F, 0.767F, 0.0F, 0.0F, 0.34F, 0.0F);
         default -> genericTimeline(gun, empty);
      };
   }

   private static GunAnimationLibrary.ReloadTimeline genericTimeline(GunDefinition gun, boolean empty) {
      float duration = gun.reloadSeconds();
      return new GunAnimationLibrary.ReloadTimeline(
         duration * 0.18F,
         duration * 0.29F,
         duration * 0.56F,
         duration * 0.64F,
         empty ? duration * 0.79F : 0.0F,
         empty ? duration * 0.88F : 0.0F,
         gun.category() == WeaponCategory.PISTOL ? 0.32F : 0.22F,
         0.0F
      );
   }

   private static Keyframe key(float time, Transform transform, Easing easing) {
      return new Keyframe(time, transform, easing);
   }

   public record AnimationSet(AnimationClip equip, AnimationClip fire, AnimationClip reload, AnimationClip emptyReload) {
   }

   public record ReloadTimeline(
      float clipOut, float dropMagazine, float clipIn, float addAmmo, float actionBack, float actionForward, float actionTravel, float actionRotation
   ) {
   }
}
