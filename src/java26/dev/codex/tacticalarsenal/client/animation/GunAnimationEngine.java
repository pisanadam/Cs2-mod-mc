package dev.codex.tacticalarsenal.client.animation;

import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class GunAnimationEngine {
   public static final GunAnimationEngine INSTANCE = new GunAnimationEngine();
   private final ClipPlayer equipPlayer = new ClipPlayer();
   private final ClipPlayer firePlayer = new ClipPlayer();
   private final ClipPlayer reloadPlayer = new ClipPlayer();
   private final CriticalSpring recoilPitch = new CriticalSpring(18.0F);
   private final CriticalSpring recoilYaw = new CriticalSpring(20.0F);
   private final CriticalSpring recoilBack = new CriticalSpring(24.0F);
   private GunItem currentGun;
   private GunAnimationLibrary.AnimationSet animations;
   private float previousAds;
   private float ads;
   private float previousMovement;
   private float movement;
   private float previousSprint;
   private float sprint;
   private float idleTime;
   private float strideTime;
   private HumanoidArm lastDualWieldFireArm = HumanoidArm.RIGHT;

   private GunAnimationEngine() {
   }

   public void tick(GunItem gun, boolean aiming, boolean sprinting, float movementAmount) {
      if (gun != this.currentGun) {
         this.currentGun = gun;
         this.reset();
         if (gun != null) {
            this.animations = GunAnimationLibrary.create(gun.definition());
            this.equipPlayer.play(this.animations.equip());
         }
      }

      if (gun != null) {
         float deltaSeconds = 0.05F;
         this.previousAds = this.ads;
         float targetAds = aiming && !this.reloadPlayer.isPlaying() ? 1.0F : 0.0F;
         float halfLife = Math.max(0.012F, gun.definition().zoomSeconds() * 0.55F);
         float blend = 1.0F - (float)Math.exp(-Math.log(2.0) * deltaSeconds / halfLife);
         this.ads = this.ads + (targetAds - this.ads) * blend;
         this.previousMovement = this.movement;
         this.previousSprint = this.sprint;
         float targetMovement = Mth.clamp(movementAmount, 0.0F, 1.0F);
         float locomotionBlend = 1.0F - (float)Math.exp(-Math.log(2.0) * deltaSeconds / 0.065F);
         this.movement = this.movement + (targetMovement - this.movement) * locomotionBlend;
         float targetSprint = sprinting && !aiming && !this.reloadPlayer.isPlaying() ? 1.0F : 0.0F;
         float sprintHalfLife = targetSprint > this.sprint ? 0.075F : 0.11F;
         float sprintBlend = 1.0F - (float)Math.exp(-Math.log(2.0) * deltaSeconds / sprintHalfLife);
         this.sprint = this.sprint + (targetSprint - this.sprint) * sprintBlend;
         this.equipPlayer.tick(deltaSeconds);
         this.firePlayer.tick(deltaSeconds);
         this.reloadPlayer.tick(deltaSeconds);
         this.recoilPitch.tick(0.0F, deltaSeconds);
         this.recoilYaw.tick(0.0F, deltaSeconds);
         this.recoilBack.tick(0.0F, deltaSeconds);
         this.idleTime += deltaSeconds;
         this.strideTime = this.strideTime + deltaSeconds * Mth.lerp(this.movement, 3.8F, 8.2F) * this.movement;
      }
   }

   public void onFire(GunDefinition gun, int sequence, boolean scoped, HumanoidArm mainArm) {
      if (this.animations != null && !this.reloadPlayer.isPlaying()) {
         if (gun.id().equals("elite")) {
            this.lastDualWieldFireArm = (sequence & 1) == 0 ? mainArm : mainArm.getOpposite();
         }

         this.firePlayer.play(this.animations.fire());
         float pitch = gun.recoilPitchDegrees(scoped, sequence);
         float side = gun.recoilYawDegrees(scoped, sequence);
         this.recoilPitch.impulse(pitch * 7.5F);
         this.recoilYaw.impulse(side * 6.0F);
         this.recoilBack.impulse(Math.min(2.4F, 0.75F + pitch * 0.45F));
      }
   }

   public void onReload(boolean empty, int reloadUnits) {
      if (this.animations != null && !this.reloadPlayer.isPlaying()) {
         this.firePlayer.stop();
         if (this.currentGun != null && !this.currentGun.definition().reserveAsMagazines()) {
            this.reloadPlayer.play(GunAnimationLibrary.shellReload(this.currentGun.definition(), reloadUnits));
         } else {
            this.reloadPlayer.play(empty ? this.animations.emptyReload() : this.animations.reload());
         }
      }
   }

   public boolean isReloading() {
      return this.reloadPlayer.isPlaying();
   }

   public void cancelReload() {
      this.reloadPlayer.stop();
   }

   public float reloadProgress(float partialTick) {
      return this.reloadPlayer.isPlaying() ? this.reloadPlayer.progress(partialTick) : 0.0F;
   }

   public float ads(float partialTick) {
      return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), this.previousAds, this.ads);
   }

   public float sprint(float partialTick) {
      return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), this.previousSprint, this.sprint);
   }

   public Transform dualWieldFireTransform(HumanoidArm arm, float partialTick) {
      return this.currentGun != null && this.currentGun.definition().id().equals("elite") && arm == this.lastDualWieldFireArm
         ? this.firePlayer.sample(Bone.ROOT, partialTick)
         : Transform.IDENTITY;
   }

   public Transform dualWieldWeaponTransform(HumanoidArm arm, float partialTick) {
      if (this.currentGun != null && this.currentGun.definition().id().equals("elite")) {
         Bone bone = arm == HumanoidArm.RIGHT ? Bone.RIGHT_WEAPON : Bone.LEFT_WEAPON;
         return this.reloadPlayer.sample(bone, partialTick);
      } else {
         return Transform.IDENTITY;
      }
   }

   public Transform dualWieldHandTransform(HumanoidArm arm, float partialTick) {
      if (this.currentGun != null && this.currentGun.definition().id().equals("elite")) {
         Bone bone = arm == HumanoidArm.RIGHT ? Bone.RIGHT_HAND : Bone.LEFT_HAND;
         return this.reloadPlayer.sample(bone, partialTick);
      } else {
         return Transform.IDENTITY;
      }
   }

   public Transform dualWieldBoneTransform(Bone bone, HumanoidArm arm, float partialTick) {
      Bone reloadBone = switch (bone) {
         case MAGAZINE -> arm == HumanoidArm.RIGHT ? Bone.RIGHT_MAGAZINE : Bone.LEFT_MAGAZINE;
         case ACTION -> arm == HumanoidArm.RIGHT ? Bone.RIGHT_ACTION : Bone.LEFT_ACTION;
         default -> bone;
      };
      Transform result = this.reloadPlayer.sample(reloadBone, partialTick);
      if (bone == Bone.ACTION && arm == this.lastDualWieldFireArm) {
         result = result.then(this.firePlayer.sample(bone, partialTick));
      }

      return result;
   }

   public Transform rootTransform(float partialTick) {
      if (this.currentGun == null) {
         return Transform.IDENTITY;
      } else {
         float renderIdleTime = this.idleTime + partialTick / 20.0F;
         float renderMovement = Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), this.previousMovement, this.movement);
         float renderSprint = this.sprint(partialTick);
         float renderStride = this.strideTime + partialTick / 20.0F * Mth.lerp(renderMovement, 3.8F, 8.2F) * renderMovement;
         Transform idle = Transform.of(
            (float)Math.sin(renderIdleTime * 1.35F) * 0.004F,
            (float)Math.cos(renderIdleTime * 1.7F) * 0.006F,
            0.0F,
            (float)Math.sin(renderIdleTime * 1.1F) * 0.18F,
            (float)Math.cos(renderIdleTime * 1.3F) * 0.16F,
            0.0F
         );
         float walkBlend = renderMovement * (1.0F - renderSprint);
         float walkSin = (float)Math.sin(renderStride);
         float walkCos = (float)Math.cos(renderStride * 2.0F);
         Transform walk = Transform.of(
            walkSin * 0.01F * walkBlend,
            -Math.abs(walkCos) * 0.009F * walkBlend,
            0.0F,
            walkCos * 0.65F * walkBlend,
            walkSin * 1.15F * walkBlend,
            -walkSin * 0.85F * walkBlend
         );
         float runSin = (float)Math.sin(renderStride * 1.12F);
         float runCos = (float)Math.cos(renderStride * 2.24F);
         Transform loweredSprintPose = Transform.blend(Transform.IDENTITY, Transform.of(0.13F, -0.13F, 0.08F, 12.0F, 19.0F, 10.0F), renderSprint);
         Transform sprintSway = Transform.of(
            runSin * 0.032F * renderSprint,
            -Math.abs(runCos) * 0.036F * renderSprint,
            0.012F * runCos * renderSprint,
            runCos * 2.6F * renderSprint,
            runSin * 4.8F * renderSprint,
            -runSin * 4.2F * renderSprint
         );
         String weaponId = this.currentGun.definition().id();
         Transform aimedViewmodel = !weaponId.equals("aug") && !weaponId.equals("sg556")
            ? Transform.of(-0.315F, 0.165F, -0.22F, 0.0F, -4.0F, 0.0F)
            : Transform.of(-0.12F, 0.07F, -0.1F, 0.0F, -1.5F, 0.0F);
         Transform adsPose = Transform.blend(Transform.IDENTITY, aimedViewmodel, this.ads(partialTick));
         Transform spring = Transform.of(
            0.0F,
            0.0F,
            this.recoilBack.sample(partialTick) * 0.018F,
            -this.recoilPitch.sample(partialTick) * 0.12F,
            this.recoilYaw.sample(partialTick) * 0.1F,
            0.0F
         );
         Transform fireRoot = weaponId.equals("elite") ? Transform.IDENTITY : this.firePlayer.sample(Bone.ROOT, partialTick);
         return idle.then(walk)
            .then(loweredSprintPose)
            .then(sprintSway)
            .then(adsPose)
            .then(this.equipPlayer.sample(Bone.ROOT, partialTick))
            .then(this.reloadPlayer.sample(Bone.ROOT, partialTick))
            .then(fireRoot)
            .then(spring);
      }
   }

   public Transform boneTransform(Bone bone, float partialTick) {
      Transform result = this.reloadPlayer.sample(bone, partialTick);
      if (bone == Bone.ACTION || bone == Bone.LEFT_HAND || bone == Bone.RIGHT_HAND) {
         result = result.then(this.firePlayer.sample(bone, partialTick));
      }

      return result;
   }

   private void reset() {
      this.animations = null;
      this.equipPlayer.stop();
      this.firePlayer.stop();
      this.reloadPlayer.stop();
      this.recoilPitch.reset();
      this.recoilYaw.reset();
      this.recoilBack.reset();
      this.previousAds = 0.0F;
      this.ads = 0.0F;
      this.previousMovement = 0.0F;
      this.movement = 0.0F;
      this.previousSprint = 0.0F;
      this.sprint = 0.0F;
      this.idleTime = 0.0F;
      this.strideTime = 0.0F;
      this.lastDualWieldFireArm = HumanoidArm.RIGHT;
   }
}
