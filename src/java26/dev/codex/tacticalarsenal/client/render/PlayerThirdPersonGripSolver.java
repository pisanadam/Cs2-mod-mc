package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class PlayerThirdPersonGripSolver {
   private static final float MODEL_UNIT = 0.0625F;
   private static final float MIN_CONTACT_FRACTION = 0.04F;
   private static final float MAX_CONTACT_FRACTION = 0.7F;
   private static final float EPSILON = 1.0E-6F;

   private PlayerThirdPersonGripSolver() {
   }

   public static boolean poseSupportArm(PlayerModel model, AvatarRenderState state, GunDefinition gun) {
      String weaponId = gun.id();
      if (!weaponId.equals("elite") && Cs2MeshManager.hasMesh(weaponId)) {
         Vector3fc rawGrip = Cs2MeshManager.gripAnchor(weaponId);
         Vector3fc rawMuzzle = Cs2MeshManager.muzzleAnchor(weaponId);
         if (rawGrip != null && rawMuzzle != null) {
            HumanoidArm mainArm = state.mainArm == null ? HumanoidArm.RIGHT : state.mainArm;
            HumanoidArm supportArm = mainArm.getOpposite();
            ModelPart mainPart = model.getArm(mainArm);
            ModelPart supportPart = model.getArm(supportArm);
            PoseStack weaponPoses = new PoseStack();
            model.translateToHand(state, mainArm, weaponPoses);
            applyVanillaHeldItemFrame(weaponPoses, mainArm);
            boolean leftHand = mainArm == HumanoidArm.LEFT;
            ItemTransform itemTransform = Cs2InventoryItemModels.playerFirearmTransform(gun.category(), leftHand);
            itemTransform.apply(leftHand, weaponPoses.last());
            weaponPoses.last().mulPose(Cs2MeshManager.thirdPersonTransform(weaponId, leftHand));
            PoseStack rootPoses = new PoseStack();
            model.root().translateAndRotate(rootPoses);
            Matrix4f rootInverse = new Matrix4f(rootPoses.last().pose()).invert();
            Matrix4fc weaponMatrix = weaponPoses.last().pose();
            Vector3f grip = toRootSpace(weaponMatrix, rootInverse, rawGrip);
            Vector3f muzzle = toRootSpace(weaponMatrix, rootInverse, rawMuzzle);
            Vector3f mainShoulder = shoulder(mainPart);
            Vector3f supportShoulder = shoulder(supportPart);
            Vector3f mainPalmLocal = armLocalPoint(mainPart, grip);
            if (finite(mainPalmLocal) && !(mainPalmLocal.lengthSquared() < 1.0E-6F)) {
               Vector3f supportPalmLocal = new Vector3f(-mainPalmLocal.x, mainPalmLocal.y, mainPalmLocal.z);
               float reach = supportPalmLocal.length();
               float rawAxisLength = new Vector3f(rawMuzzle).sub(rawGrip).length();
               if (Float.isFinite(rawAxisLength) && !(rawAxisLength < 1.0E-6F)) {
                  float desiredFraction = Mth.clamp(supportDistance(gun.category()) / rawAxisLength, 0.08F, 0.55F);
                  float underside = gun.category() == WeaponCategory.PISTOL ? 0.015F : 0.035F;
                  Vector3f rawUnderGrip = new Vector3f(rawGrip).add(0.0F, -underside, 0.0F);
                  Vector3f rawUnderMuzzle = new Vector3f(rawMuzzle).add(0.0F, -underside, 0.0F);
                  Vector3f lineStart = toRootSpace(weaponMatrix, rootInverse, rawUnderGrip);
                  Vector3f lineEnd = toRootSpace(weaponMatrix, rootInverse, rawUnderMuzzle);
                  Vector3f line = lineEnd.sub(lineStart, new Vector3f());
                  float contactFraction = reachableContactFraction(lineStart, line, supportShoulder, reach, desiredFraction);
                  Vector3f contact = new Vector3f(lineStart).fma(contactFraction, line);
                  Vector3f shoulderToContact = contact.sub(supportShoulder, new Vector3f());
                  if (finite(shoulderToContact) && !(shoulderToContact.lengthSquared() < 1.0E-6F)) {
                     Quaternionf palmToContact = new Quaternionf().rotationTo(supportPalmLocal, shoulderToContact);
                     Vector3f euler = palmToContact.getEulerAnglesZYX(new Vector3f());
                     if (!finite(euler)) {
                        return false;
                     } else {
                        supportPart.setRotation(euler.x, euler.y, euler.z);
                        return true;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void applyVanillaHeldItemFrame(PoseStack poses, HumanoidArm arm) {
      poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
      float side = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
      poses.translate(side * 0.0625F, 0.125F, -0.625F);
   }

   private static Vector3f toRootSpace(Matrix4fc weaponMatrix, Matrix4fc rootInverse, Vector3fc rawPoint) {
      Vector3f rendered = weaponMatrix.transformPosition(new Vector3f(rawPoint));
      return rootInverse.transformPosition(rendered);
   }

   private static Vector3f shoulder(ModelPart arm) {
      return new Vector3f(arm.x * 0.0625F, arm.y * 0.0625F, arm.z * 0.0625F);
   }

   private static Vector3f armLocalPoint(ModelPart arm, Vector3fc rootPoint) {
      Matrix4f armMatrix = new Matrix4f()
         .translate(arm.x * 0.0625F, arm.y * 0.0625F, arm.z * 0.0625F)
         .rotate(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot));
      return armMatrix.invert().transformPosition(new Vector3f(rootPoint));
   }

   private static float supportDistance(WeaponCategory category) {
      return switch (category) {
         case PISTOL -> 0.045F;
         case SMG -> 0.15F;
         case RIFLE -> 0.23F;
         case SNIPER_RIFLE -> 0.27F;
         case SHOTGUN -> 0.28F;
         case MACHINE_GUN -> 0.25F;
      };
   }

   private static float reachableContactFraction(Vector3fc lineStart, Vector3fc line, Vector3fc shoulder, float reach, float desiredFraction) {
      Vector3f offset = new Vector3f(lineStart).sub(shoulder);
      float a = line.lengthSquared();
      if (Float.isFinite(a) && !(a < 1.0E-6F)) {
         float b = 2.0F * offset.dot(line);
         float c = offset.lengthSquared() - reach * reach;
         float discriminant = b * b - 4.0F * a * c;
         if (Float.isFinite(discriminant) && !(discriminant < 0.0F)) {
            float root = (float)Math.sqrt(discriminant);
            float first = (-b - root) / (2.0F * a);
            float second = (-b + root) / (2.0F * a);
            boolean firstValid = inContactRange(first);
            boolean secondValid = inContactRange(second);
            if (firstValid && secondValid) {
               return Math.abs(first - desiredFraction) <= Math.abs(second - desiredFraction) ? first : second;
            } else if (firstValid) {
               return first;
            } else {
               return secondValid ? second : nearestContactFraction(offset, line, a);
            }
         } else {
            return nearestContactFraction(offset, line, a);
         }
      } else {
         return desiredFraction;
      }
   }

   private static float nearestContactFraction(Vector3fc offset, Vector3fc line, float lineLengthSquared) {
      float projected = -offset.dot(line) / lineLengthSquared;
      return Float.isFinite(projected) ? Mth.clamp(projected, 0.04F, 0.7F) : 0.04F;
   }

   private static boolean inContactRange(float value) {
      return Float.isFinite(value) && value >= 0.04F && value <= 0.7F;
   }

   private static boolean finite(Vector3fc vector) {
      return Float.isFinite(vector.x()) && Float.isFinite(vector.y()) && Float.isFinite(vector.z());
   }
}
