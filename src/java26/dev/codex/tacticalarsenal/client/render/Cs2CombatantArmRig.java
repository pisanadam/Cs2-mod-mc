package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class Cs2CombatantArmRig {
   private static final String RIGHT_UPPER = "right_upper_arm";
   private static final String RIGHT_LOWER = "right_lower_arm";
   private static final String RIGHT_HAND = "right_hand";
   private static final String LEFT_UPPER = "left_upper_arm";
   private static final String LEFT_LOWER = "left_lower_arm";
   private static final String LEFT_HAND = "left_hand";
   private static final Vector3fc RIGHT_UPPER_TO_ELBOW_SOURCE = new Vector3f(-0.19104218F, -0.22861698F, -0.02947535F);
   private static final Vector3fc RIGHT_ELBOW_TO_HAND_SOURCE = new Vector3f(-0.15903258F, -0.2169767F, 0.08288944F);
   private static final Vector3fc LEFT_UPPER_TO_ELBOW_SOURCE = new Vector3f(0.19104198F, -0.22861809F, -0.02947518F);
   private static final Vector3fc LEFT_ELBOW_TO_HAND_SOURCE = new Vector3f(0.15903185F, -0.21697778F, 0.08288925F);
   private static final Vector3fc RIGHT_UPPER_TO_ELBOW = minecraftVector(RIGHT_UPPER_TO_ELBOW_SOURCE);
   private static final Vector3fc RIGHT_ELBOW_TO_HAND = minecraftVector(RIGHT_ELBOW_TO_HAND_SOURCE);
   private static final Vector3fc LEFT_UPPER_TO_ELBOW = minecraftVector(LEFT_UPPER_TO_ELBOW_SOURCE);
   private static final Vector3fc LEFT_ELBOW_TO_HAND = minecraftVector(LEFT_ELBOW_TO_HAND_SOURCE);
   private static final float UPPER_LENGTH = RIGHT_UPPER_TO_ELBOW.length();
   private static final float LOWER_LENGTH = RIGHT_ELBOW_TO_HAND.length();
   private static final float IK_MARGIN = 0.004F;
   private static final float PISTOL_SUPPORT_SIDE = 0.05F;
   private static final float PISTOL_SUPPORT_FORWARD = 0.012F;
   private static final float FOREGRIP_SIDE = 0.02F;

   private Cs2CombatantArmRig() {
   }

   static boolean isReady(String meshPrefix) {
      return Cs2MeshManager.hasMesh(meshPrefix + "_right_upper_arm")
         && Cs2MeshManager.hasMesh(meshPrefix + "_right_lower_arm")
         && Cs2MeshManager.hasMesh(meshPrefix + "_right_hand")
         && Cs2MeshManager.hasMesh(meshPrefix + "_left_upper_arm")
         && Cs2MeshManager.hasMesh(meshPrefix + "_left_lower_arm")
         && Cs2MeshManager.hasMesh(meshPrefix + "_left_hand");
   }

   static void submitArms(
      String meshPrefix, HumanoidModel<CombatantRenderState> model, CombatantRenderState state, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      if (isReady(meshPrefix)) {
         ItemStack mainHand = state.getMainHandItemStack();
         if (mainHand.getItem() instanceof GunItem gun) {
            if (gun.definition().id().equals("elite")) {
               submitIkArm(meshPrefix, HumanoidArm.RIGHT, model.rightArm, heldGripTarget(model, state, gun, HumanoidArm.RIGHT, true), poses, collector, light);
               submitIkArm(meshPrefix, HumanoidArm.LEFT, model.leftArm, heldGripTarget(model, state, gun, HumanoidArm.LEFT, true), poses, collector, light);
            } else {
               HumanoidArm mainArm = state.mainArm;
               HumanoidArm supportArm = mainArm.getOpposite();
               ModelPart mainBone = model.getArm(mainArm);
               ModelPart supportBone = model.getArm(supportArm);
               PoseStack weaponTransform = heldWeaponTransform(model, state, gun, mainArm, false);
               Vector3fc gripAnchor = Cs2MeshManager.gripAnchor(gun.definition().id());
               if (weaponTransform != null && gripAnchor != null) {
                  Vector3f mainTarget = transformPoint(weaponTransform, gripAnchor);
                  Vector3f supportTarget = supportTarget(weaponTransform, gripAnchor, gun.definition().category(), supportArm);
                  submitIkArm(meshPrefix, mainArm, mainBone, mainTarget, poses, collector, light);
                  submitIkArm(meshPrefix, supportArm, supportBone, supportTarget, poses, collector, light);
               } else {
                  submitRigidArm(meshPrefix, mainArm, mainBone, poses, collector, light);
                  submitRigidArm(meshPrefix, supportArm, supportBone, poses, collector, light);
               }
            }
         } else {
            submitRigidArm(meshPrefix, HumanoidArm.RIGHT, model.rightArm, poses, collector, light);
            submitRigidArm(meshPrefix, HumanoidArm.LEFT, model.leftArm, poses, collector, light);
         }
      }
   }

   private static void submitRigidArm(String meshPrefix, HumanoidArm arm, ModelPart bone, PoseStack poses, SubmitNodeCollector collector, int light) {
      String side = sideName(arm);
      Vector3fc upperToElbow = sourceUpperToElbow(arm);
      Vector3fc elbowToHand = sourceElbowToHand(arm);
      poses.pushPose();
      bone.translateAndRotate(poses);
      orientSource2Character(poses);
      Cs2MeshManager.submit(meshPrefix + "_" + side + "_upper_arm", poses, collector, light);
      poses.translate(upperToElbow.x(), upperToElbow.y(), upperToElbow.z());
      Cs2MeshManager.submit(meshPrefix + "_" + side + "_lower_arm", poses, collector, light);
      poses.translate(elbowToHand.x(), elbowToHand.y(), elbowToHand.z());
      Cs2MeshManager.submit(meshPrefix + "_" + side + "_hand", poses, collector, light);
      poses.popPose();
   }

   private static void submitIkArm(
      String meshPrefix, HumanoidArm arm, ModelPart bone, Vector3fc requestedTarget, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      Vector3f shoulder = new Vector3f(bone.x / 16.0F, bone.y / 16.0F, bone.z / 16.0F);
      Cs2CombatantArmRig.ArmSolution solution = solve(arm, shoulder, requestedTarget);
      String side = sideName(arm);
      submitAbsolute(meshPrefix + "_" + side + "_upper_arm", solution.shoulder(), solution.upperRotation(), poses, collector, light);
      submitAbsolute(meshPrefix + "_" + side + "_lower_arm", solution.elbow(), solution.lowerRotation(), poses, collector, light);
      submitAbsolute(meshPrefix + "_" + side + "_hand", solution.hand(), solution.handRotation(), poses, collector, light);
   }

   private static Cs2CombatantArmRig.ArmSolution solve(HumanoidArm arm, Vector3fc shoulder, Vector3fc requestedTarget) {
      Vector3f towardTarget = new Vector3f(requestedTarget).sub(shoulder);
      float requestedDistance = towardTarget.length();
      if (requestedDistance < 1.0E-5F) {
         towardTarget.set(0.0F, 0.25F, -0.25F);
         requestedDistance = towardTarget.length();
      }

      towardTarget.div(requestedDistance);
      float minimumReach = Math.abs(UPPER_LENGTH - LOWER_LENGTH) + 0.004F;
      float maximumReach = UPPER_LENGTH + LOWER_LENGTH - 0.004F;
      float reach = Math.max(minimumReach, Math.min(maximumReach, requestedDistance));
      Vector3f hand = new Vector3f(shoulder).fma(reach, towardTarget);
      float shoulderProjection = (UPPER_LENGTH * UPPER_LENGTH - LOWER_LENGTH * LOWER_LENGTH + reach * reach) / (2.0F * reach);
      float bendHeight = (float)Math.sqrt(Math.max(0.0F, UPPER_LENGTH * UPPER_LENGTH - shoulderProjection * shoulderProjection));
      float side = arm == HumanoidArm.RIGHT ? -1.0F : 1.0F;
      Vector3f pole = new Vector3f(side, 0.42F, 0.16F);
      pole.fma(-pole.dot(towardTarget), towardTarget);
      if (pole.lengthSquared() < 1.0E-6F) {
         pole.set(0.0F, 1.0F, 0.0F).fma(-towardTarget.y, towardTarget);
      }

      pole.normalize();
      Vector3f elbow = new Vector3f(shoulder).fma(shoulderProjection, towardTarget).fma(bendHeight, pole);
      Vector3f desiredUpper = new Vector3f(elbow).sub(shoulder);
      Vector3f desiredLower = new Vector3f(hand).sub(elbow);
      Vector3fc bindUpper = upperToElbow(arm);
      Vector3fc bindLower = elbowToHand(arm);
      Vector3f bindNormal = new Vector3f(bindUpper).cross(bindLower).normalize();
      Vector3f desiredNormal = new Vector3f(desiredUpper).cross(desiredLower);
      if (desiredNormal.lengthSquared() < 1.0E-8F) {
         desiredNormal.set(pole).cross(towardTarget);
      }

      desiredNormal.normalize();
      Quaternionf upperRotation = alignSegment(bindUpper, desiredUpper, bindNormal, desiredNormal);
      Quaternionf lowerRotation = alignSegment(bindLower, desiredLower, bindNormal, desiredNormal);
      return new Cs2CombatantArmRig.ArmSolution(new Vector3f(shoulder), elbow, hand, upperRotation, lowerRotation, new Quaternionf(lowerRotation));
   }

   private static Quaternionf alignSegment(Vector3fc bindDirection, Vector3fc desiredDirection, Vector3fc bindNormal, Vector3fc desiredNormal) {
      Vector3f from = new Vector3f(bindDirection).normalize();
      Vector3f to = new Vector3f(desiredDirection).normalize();
      Quaternionf swing = new Quaternionf().rotationTo(from, to);
      Vector3f rotatedNormal = new Vector3f(bindNormal);
      swing.transform(rotatedNormal);
      rotatedNormal.fma(-rotatedNormal.dot(to), to).normalize();
      Vector3f targetNormal = new Vector3f(desiredNormal);
      targetNormal.fma(-targetNormal.dot(to), to).normalize();
      float sine = new Vector3f(rotatedNormal).cross(targetNormal).dot(to);
      float cosine = Math.max(-1.0F, Math.min(1.0F, rotatedNormal.dot(targetNormal)));
      float twistAngle = (float)Math.atan2(sine, cosine);
      return new Quaternionf().rotationAxis(twistAngle, to.x, to.y, to.z).mul(swing).normalize();
   }

   private static void submitAbsolute(String meshId, Vector3fc pivot, Quaternionf rotation, PoseStack poses, SubmitNodeCollector collector, int light) {
      poses.pushPose();
      poses.translate(pivot.x(), pivot.y(), pivot.z());
      poses.mulPose(rotation);
      orientSource2Character(poses);
      Cs2MeshManager.submit(meshId, poses, collector, light);
      poses.popPose();
   }

   private static Vector3f heldGripTarget(HumanoidModel<CombatantRenderState> model, CombatantRenderState state, GunItem gun, HumanoidArm arm, boolean dual) {
      PoseStack transform = heldWeaponTransform(model, state, gun, arm, dual);
      Vector3fc baseAnchor = dual
         ? Cs2MeshManager.gripAnchor(gun.definition().id(), arm == HumanoidArm.LEFT)
         : Cs2MeshManager.gripAnchor(gun.definition().id());
      if (transform != null && baseAnchor != null) {
         return transformPoint(transform, baseAnchor);
      } else {
         ModelPart bone = model.getArm(arm);
         return new Vector3f(bone.x / 16.0F, bone.y / 16.0F + UPPER_LENGTH + LOWER_LENGTH - 0.004F, bone.z / 16.0F);
      }
   }

   private static PoseStack heldWeaponTransform(
      HumanoidModel<CombatantRenderState> model, CombatantRenderState state, GunItem gun, HumanoidArm arm, boolean dual
   ) {
      if (!Cs2MeshManager.hasMesh(gun.definition().id())) {
         return null;
      } else {
         PoseStack transform = new PoseStack();
         model.translateToHand(state, arm, transform);
         applyVanillaHeldItemFrame(transform, arm);
         ItemTransform itemTransform = dual
            ? Cs2InventoryItemModels.combatantDualPistolTransform(arm == HumanoidArm.LEFT)
            : Cs2InventoryItemModels.combatantFirearmTransform(gun.definition().category(), arm == HumanoidArm.LEFT);
         itemTransform.apply(arm == HumanoidArm.LEFT, transform.last());
         transform.last()
            .mulPose(
               dual
                  ? Cs2MeshManager.thirdPersonHalfTransform(gun.definition().id(), arm == HumanoidArm.LEFT)
                  : Cs2MeshManager.thirdPersonTransform(gun.definition().id(), arm == HumanoidArm.LEFT)
            );
         return transform;
      }
   }

   private static Vector3f supportTarget(PoseStack weaponTransform, Vector3fc mainGrip, WeaponCategory category, HumanoidArm supportArm) {
      if (category == WeaponCategory.PISTOL) {
         Vector3f main = transformPoint(weaponTransform, mainGrip);
         float side = supportArm == HumanoidArm.LEFT ? 1.0F : -1.0F;
         return main.add(side * 0.05F, -0.006F, -0.012F);
      } else {
         float forward = switch (category) {
            case SMG -> 0.205F;
            case RIFLE -> 0.27F;
            case SNIPER_RIFLE -> 0.295F;
            case SHOTGUN -> 0.285F;
            case MACHINE_GUN -> 0.305F;
            case PISTOL -> 0.0F;
         };
         float side = supportArm == HumanoidArm.LEFT ? 1.0F : -1.0F;
         return transformPoint(weaponTransform, new Vector3f(mainGrip).add(side * 0.02F, 0.008F, forward));
      }
   }

   private static Vector3f transformPoint(PoseStack transform, Vector3fc point) {
      return transform.last().pose().transformPosition(new Vector3f(point));
   }

   private static void applyVanillaHeldItemFrame(PoseStack poses, HumanoidArm arm) {
      poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
      float side = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
      poses.translate(side / 16.0F, 0.125F, -0.625F);
   }

   private static void orientSource2Character(PoseStack poses) {
      poses.mulPose(new Quaternionf().rotationX((float) Math.PI));
   }

   private static Vector3fc sourceUpperToElbow(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? RIGHT_UPPER_TO_ELBOW_SOURCE : LEFT_UPPER_TO_ELBOW_SOURCE;
   }

   private static Vector3fc sourceElbowToHand(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? RIGHT_ELBOW_TO_HAND_SOURCE : LEFT_ELBOW_TO_HAND_SOURCE;
   }

   private static Vector3fc upperToElbow(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? RIGHT_UPPER_TO_ELBOW : LEFT_UPPER_TO_ELBOW;
   }

   private static Vector3fc elbowToHand(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? RIGHT_ELBOW_TO_HAND : LEFT_ELBOW_TO_HAND;
   }

   private static String sideName(HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? "right" : "left";
   }

   private static Vector3fc minecraftVector(Vector3fc source) {
      return new Vector3f(source.x(), -source.y(), -source.z());
   }

   private record ArmSolution(
      Vector3fc shoulder, Vector3fc elbow, Vector3fc hand, Quaternionf upperRotation, Quaternionf lowerRotation, Quaternionf handRotation
   ) {
   }
}
