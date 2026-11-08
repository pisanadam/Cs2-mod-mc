package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.codex.tacticalarsenal.client.animation.Bone;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.client.animation.Transform;
import dev.codex.tacticalarsenal.client.settings.HandRenderMode;
import dev.codex.tacticalarsenal.client.settings.TacticalArsenalClientConfig;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
//#if MC >= 1.21.11
import net.minecraft.client.model.geom.PartPose;
//#endif
//#if MC >= 1.21.11
import net.minecraft.client.model.player.PlayerModel;
//#endif
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
//#if MC >= 1.21.11
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
//#endif
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#endif
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
//#if MC >= 1.21.11
import net.minecraft.world.entity.player.PlayerModelPart;
//#endif
//#if MC >= 1.21.11
import net.minecraft.world.entity.player.PlayerModelType;
//#endif
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GunHandsRenderer {
   private GunHandsRenderer() {
   }

   public static void render(AbstractClientPlayer player, GunDefinition gun, float partialTick, PoseStack poses, SubmitNodeCollector collector, int light) {
      if (!handsUnavailable(player) && !gun.id().equals("elite")) {
         HumanoidArm gripArm = player.getMainArm();
         if (TacticalArsenalClientConfig.handRenderMode() == HandRenderMode.LEGACY_2_2) {
            renderCs2GripHand(player, gripArm, partialTick, poses, collector, light);
            if (GunAnimationEngine.INSTANCE.isReloading()) {
               renderCs2SupportHand(player, gripArm.getOpposite(), gun, partialTick, poses, collector, light);
            }
         } else {
            renderCs2GripHand(player, gripArm, partialTick, poses, collector, light);
            renderCs2SupportHand(player, gripArm.getOpposite(), gun, partialTick, poses, collector, light);
         }
      }
   }

   public static void renderDualHand(AbstractClientPlayer player, HumanoidArm arm, float partialTick, PoseStack poses, SubmitNodeCollector collector, int light) {
      if (!handsUnavailable(player)) {
         poses.pushPose();
         apply(poses, GunAnimationEngine.INSTANCE.dualWieldHandTransform(arm, partialTick));
         renderArmAtGrip(player, arm, dualGripTarget(arm), 30.0F, -4.0F, 60.0F, poses, collector, light);
         poses.popPose();
      }
   }

   public static void renderIdleGripHand(AbstractClientPlayer player, HumanoidArm arm, PoseStack poses, SubmitNodeCollector collector, int light) {
      if (!handsUnavailable(player)) {
         poses.pushPose();
         renderArmAtGrip(player, arm, primaryGripTarget(arm), -8.0F, -7.0F, -8.0F, poses, collector, light);
         poses.popPose();
      }
   }

   public static void renderKnifeHand(AbstractClientPlayer player, String knifeId, HumanoidArm arm, PoseStack poses, SubmitNodeCollector collector, int light) {
      if (!handsUnavailable(player)) {
         Vector3f gripTarget = knifeGripTarget(knifeId, arm);
         float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         poses.pushPose();
         poses.translate(gripTarget.x, gripTarget.y, gripTarget.z);
         float handRoll = knifeId.equals("karambit") ? 0.0F : knifeViewmodelRoll(knifeId);
         poses.mulPose(Axis.ZP.rotationDegrees(side * handRoll));
         poses.translate(-gripTarget.x, -gripTarget.y, -gripTarget.z);
         renderArmAtGrip(player, arm, gripTarget, 45.0F, 0.0F, 105.0F, poses, collector, light);
         poses.popPose();
      }
   }

   public static void applyKnifeViewmodelTransform(String knifeId, HumanoidArm arm, PoseStack poses) {
      Vector3f sourceGrip = legacyKnifeGripTarget(arm);
      Vector3f targetGrip = knifeGripTarget(knifeId, arm);
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      float scale = knifeViewmodelScale(knifeId);
      poses.translate(targetGrip.x, targetGrip.y, targetGrip.z);
      poses.mulPose(Axis.ZP.rotationDegrees(side * knifeViewmodelRoll(knifeId)));
      poses.scale(scale, scale, scale);
      poses.translate(-sourceGrip.x, -sourceGrip.y, -sourceGrip.z);
   }

   private static Vector3f legacyKnifeGripTarget(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? new Vector3f(0.070625F, 0.047506F, 0.015122F) : new Vector3f(-0.070625F, 0.144501F, 0.223103F);
   }

   private static Vector3f knifeGripTarget(String knifeId, HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      return new Vector3f(-side * 0.13F, 0.22F, -0.025F);
   }

   private static float knifeViewmodelScale(String knifeId) {
      return switch (knifeId) {
         case "karambit" -> 2.17F;
         case "butterfly" -> 1.61F;
         default -> 1.4F;
      };
   }

   private static float knifeViewmodelRoll(String knifeId) {
      return switch (knifeId) {
         case "butterfly" -> 35.0F;
         case "karambit" -> -100.0F;
         default -> 8.0F;
      };
   }

   private static boolean handsUnavailable(AbstractClientPlayer player) {
      return player.isInvisible() || TacticalArsenalClientConfig.hideHands() || TacticalArsenalClientConfig.handScale() <= 0.0F;
   }

   private static void renderCs2GripHand(
      AbstractClientPlayer player, HumanoidArm arm, float partialTick, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      poses.pushPose();
      apply(poses, mirroredHandTransform(Bone.RIGHT_HAND, player.getMainArm(), partialTick));
      renderArmAtGrip(player, arm, primaryGripTarget(arm), -8.0F, -7.0F, -8.0F, poses, collector, light);
      poses.popPose();
   }

   private static void renderCs2SupportHand(
      AbstractClientPlayer player, HumanoidArm arm, GunDefinition gun, float partialTick, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      boolean compact = switch (gun.category()) {
         case PISTOL, SMG -> true;
         default -> false;
      };
      poses.pushPose();
      apply(poses, mirroredHandTransform(Bone.LEFT_HAND, player.getMainArm(), partialTick));
      renderArmAtGrip(
         player,
         arm,
         supportGripTarget(gun, arm.getOpposite()),
         compact ? -17.0F : -12.0F,
         compact ? -24.0F : -17.0F,
         compact ? -15.0F : -17.0F,
         poses,
         collector,
         light
      );
      poses.popPose();
   }

   private static Transform mirroredHandTransform(Bone bone, HumanoidArm mainArm, float partialTick) {
      Transform transform = GunAnimationEngine.INSTANCE.boneTransform(bone, partialTick);
      return mainArm == HumanoidArm.RIGHT
         ? transform
         : new Transform(
            new Vector3f(-transform.translation().x(), transform.translation().y(), transform.translation().z()),
            new Quaternionf(transform.rotation().x(), -transform.rotation().y(), -transform.rotation().z(), transform.rotation().w()),
            transform.scale()
         );
   }

   private static Vector3f primaryGripTarget(HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      return new Vector3f(side * 0.48F, -0.365F, -0.642F);
   }

   private static Vector3f dualGripTarget(HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      return new Vector3f(side * 0.3F, -0.34F, -0.74F);
   }

   private static Vector3f supportGripTarget(GunDefinition gun, HumanoidArm mainArm) {
      Vector3fc grip = Cs2MeshManager.gripAnchor(gun.id());
      Vector3fc muzzle = Cs2MeshManager.muzzleAnchor(gun.id());
      if (grip != null && muzzle != null) {
         float distance = switch (gun.category()) {
            case PISTOL -> 0.045F;
            case SMG -> 0.15F;
            case RIFLE -> 0.23F;
            case SNIPER_RIFLE -> 0.27F;
            case SHOTGUN -> 0.28F;
            case MACHINE_GUN -> 0.25F;
         };
         float forwardSpan = Math.max(0.001F, muzzle.z() - grip.z());
         float fraction = Mth.clamp(distance / forwardSpan, 0.08F, 0.55F);
         Vector3f rawOffset = new Vector3f(muzzle).sub(grip).mul(fraction);
         rawOffset.y = rawOffset.y - (gun.category() == WeaponCategory.PISTOL ? 0.015F : 0.035F);
         rawOffset.mul(0.95F);
         rawOffset.rotateX((float)Math.toRadians(-14.0));
         float side = mainArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         rawOffset.rotateY((float)Math.toRadians(180.0F + side * 35.0F));
         return rawOffset.add(primaryGripTarget(mainArm));
      } else {
         Vector3f target = primaryGripTarget(mainArm);
         target.x *= 0.72F;
         target.y += 0.055F;
         target.z -= 0.15F;
         return target;
      }
   }

   private static void renderArmAtGrip(
      AbstractClientPlayer player,
      HumanoidArm arm,
      Vector3f target,
      float xRot,
      float yRot,
      float zRot,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light
   ) {
      renderArmAtGrip(player, arm, target, xRot, yRot, zRot, TacticalArsenalClientConfig.handScale(), poses, collector, light);
   }

   private static void renderArmAtGrip(
      AbstractClientPlayer player,
      HumanoidArm arm,
      Vector3f target,
      float xRot,
      float yRot,
      float zRot,
      float thickness,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light
   ) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      Vector3f palm = palmAnchor(player, arm, thickness);
      poses.translate(target.x, target.y, target.z);
      poses.mulPose(Axis.XP.rotationDegrees(xRot));
      poses.mulPose(Axis.YP.rotationDegrees(side * yRot));
      poses.mulPose(Axis.ZP.rotationDegrees(side * zRot));
      poses.translate(-palm.x, -palm.y, -palm.z);
      renderVanillaArm(player, arm, thickness, poses, collector, light);
   }

   private static Vector3f palmAnchor(AbstractClientPlayer player, HumanoidArm arm, float thickness) {
      //#if MC >= 1.21.11
      return calculateVanillaPalmAnchor(arm, thickness, player.getSkin().model() == PlayerModelType.SLIM);
      //#elif MC >= 1.20.2
      return calculateVanillaPalmAnchor(arm, thickness, player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM);
      //#else
      return calculateVanillaPalmAnchor(arm, thickness, "slim".equals(player.getModelName()));
      //#endif
   }

   private static void renderVanillaArm(
      AbstractClientPlayer player, HumanoidArm arm, float thickness, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      boolean right = arm == HumanoidArm.RIGHT;
      applyVanillaArmCameraPose(poses, arm);
      //#if MC < 1.21.11
      // Before 1.21.9 the player renderer draws the sleeve as a separate part; both get the thickness.
      net.minecraft.client.renderer.entity.player.PlayerRenderer legacyRenderer =
         (net.minecraft.client.renderer.entity.player.PlayerRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
      net.minecraft.client.model.PlayerModel<AbstractClientPlayer> legacyModel = legacyRenderer.getModel();
      ModelPart legacyArm = right ? legacyModel.rightArm : legacyModel.leftArm;
      ModelPart legacySleeve = right ? legacyModel.rightSleeve : legacyModel.leftSleeve;
      float[] armScale = {legacyArm.xScale, legacyArm.yScale, legacyArm.zScale};
      float[] sleeveScale = {legacySleeve.xScale, legacySleeve.yScale, legacySleeve.zScale};
      legacyArm.xScale *= thickness;
      legacyArm.yScale *= thickness;
      legacyArm.zScale *= thickness;
      legacySleeve.xScale *= thickness;
      legacySleeve.yScale *= thickness;
      legacySleeve.zScale *= thickness;

      try {
         if (right) {
            legacyRenderer.renderRightHand(poses, collector.buffers(), light, player);
         } else {
            legacyRenderer.renderLeftHand(poses, collector.buffers(), light, player);
         }
      } finally {
         legacyArm.xScale = armScale[0];
         legacyArm.yScale = armScale[1];
         legacyArm.zScale = armScale[2];
         legacySleeve.xScale = sleeveScale[0];
         legacySleeve.yScale = sleeveScale[1];
         legacySleeve.zScale = sleeveScale[2];
      }
      //#else
      //#if MC >= 26.3
      AvatarRenderer<AbstractClientPlayer> renderer = (AvatarRenderer<AbstractClientPlayer>)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
      //#else
      AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
      //#endif
      PlayerModel model = (PlayerModel)renderer.getModel();
      ModelPart armPart = right ? model.rightArm : model.leftArm;
      PartPose originalPose = armPart.getInitialPose();
      armPart.setInitialPose(
         new PartPose(
            originalPose.x(),
            originalPose.y(),
            originalPose.z(),
            originalPose.xRot(),
            originalPose.yRot(),
            originalPose.zRot(),
            originalPose.xScale() * thickness,
            originalPose.yScale() * thickness,
            originalPose.zScale() * thickness
         )
      );
      Identifier skin = player.getSkin().body().texturePath();

      try {
         if (right) {
            renderer.renderRightHand(poses, collector, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
         } else {
            renderer.renderLeftHand(poses, collector, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
         }
      } finally {
         armPart.setInitialPose(originalPose);
      }
      //#endif
   }

   private static void applyVanillaArmCameraPose(PoseStack poses, HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      poses.translate(side * 0.64000005F, -0.6F, -0.71999997F);
      poses.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
      poses.translate(side * -1.0F, 3.6F, 3.5F);
      poses.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
      poses.mulPose(Axis.XP.rotationDegrees(200.0F));
      poses.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
      poses.translate(side * 5.6F, 0.0F, 0.0F);
   }

   private static Vector3f calculateVanillaPalmAnchor(HumanoidArm arm, float thickness, boolean slim) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      PoseStack poses = new PoseStack();
      applyVanillaArmCameraPose(poses, arm);
      poses.translate(-side * 5.0F / 16.0F, 0.125F, 0.0F);
      poses.mulPose(new Quaternionf().rotationZ(side * 0.1F));
      poses.scale(thickness, thickness, thickness);
      float palmX = slim ? -side / 32.0F : -side / 16.0F;
      return poses.last().pose().transformPosition(new Vector3f(palmX, 0.5F, 0.0F));
   }

   private static void apply(PoseStack poses, Transform transform) {
      poses.translate(transform.translation().x(), transform.translation().y(), transform.translation().z());
      poses.mulPose(new Quaternionf(transform.rotation()));
      poses.scale(transform.scale().x(), transform.scale().y(), transform.scale().z());
   }
}
