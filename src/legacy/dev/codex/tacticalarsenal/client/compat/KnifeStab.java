package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * In Minecraft 26.x the knives carry the STAB swing animation (8 ticks) instead of the default
 * swing. Older versions have no swing animation types, so this reproduces the 26.x "stab" maths
 * (SpearAnimations and the SPEAR arm pose, without the kinetic-weapon parts knives do not use).
 */
public final class KnifeStab {
   public static final int KNIFE_SWING_TICKS = 8;
   private static final int DEFAULT_SWING_TICKS = 6;

   private KnifeStab() {
   }

   public static boolean isKnife(ItemStack stack) {
      return stack.getItem() instanceof KnifeItem;
   }

   public static int swingDurationOffset() {
      return KNIFE_SWING_TICKS - DEFAULT_SWING_TICKS;
   }

   /** Arm that the current swing uses, as 26.x ArmedEntityRenderState#attackArm. */
   public static HumanoidArm attackArm(LivingEntity entity) {
      return entity.swingingArm != InteractionHand.OFF_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
   }

   public static boolean stabs(LivingEntity entity) {
      return isKnife(entity.getItemInHand(entity.swingingArm == null ? InteractionHand.MAIN_HAND : entity.swingingArm));
   }

   private static float progress(float time, float start, float end) {
      return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
   }

   private static float inOutSine(float x) {
      return -(Mth.cos((float) Math.PI * x) - 1.0F) / 2.0F;
   }

   private static float outBack(float x) {
      float d = x - 1.0F;
      return 1.0F + 2.70158F * d * d * d + 1.70158F * d * d;
   }

   private static float inOutExpo(float x) {
      if (x < 0.5F) {
         return x == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0 * x - 10.0) / 2.0);
      } else {
         return x == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0 * x + 10.0)) / 2.0);
      }
   }

   private static float inQuad(float x) {
      return x * x;
   }

   /** 26.x ArmPose.SPEAR arm placement (SpearAnimations.thirdPersonHandUse) for a knife stab. */
   public static void spearArmPose(ModelPart arm, ModelPart head, boolean rightArm, boolean fallFlyingOrSwimming) {
      int invert = rightArm ? 1 : -1;
      arm.yRot = -0.1F * invert + head.yRot;
      arm.xRot = (float) (-Math.PI / 2) + head.xRot + 0.8F;
      if (fallFlyingOrSwimming) {
         arm.xRot -= 0.9599311F;
      }

      arm.yRot = (float) (Math.PI / 180.0) * Mth.clamp((180.0F / (float)Math.PI) * arm.yRot, -60.0F, 60.0F);
      arm.xRot = (float) (Math.PI / 180.0) * Mth.clamp((180.0F / (float)Math.PI) * arm.xRot, -120.0F, 30.0F);
   }

   /** SpearAnimations.thirdPersonAttackHand. */
   public static void thirdPersonAttackHand(HumanoidModel<?> model, float attackTime, HumanoidArm arm) {
      model.rightArm.yRot = model.rightArm.yRot - model.body.yRot;
      model.leftArm.yRot = model.leftArm.yRot - model.body.yRot;
      model.leftArm.xRot = model.leftArm.xRot - model.body.yRot;
      float prepare = inOutSine(progress(attackTime, 0.0F, 0.05F));
      float attack = inQuad(progress(attackTime, 0.05F, 0.2F));
      float retract = inOutExpo(progress(attackTime, 0.4F, 1.0F));
      (arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm).xRot += (90.0F * prepare - 120.0F * attack + 30.0F * retract) * (float) (Math.PI / 180.0);
   }

   /** SpearAnimations.thirdPersonAttackItem (knives have no forward movement). */
   public static void thirdPersonAttackItem(float attackTime, PoseStack poses) {
      if (!(attackTime <= 0.0F)) {
         float attack = inQuad(progress(attackTime, 0.05F, 0.2F));
         float retract = inOutExpo(progress(attackTime, 0.4F, 1.0F));
         poses.rotateAround(Axis.XN.rotationDegrees(70.0F * (attack - retract)), 0.0F, -0.125F, 0.125F);
      }
   }

   /** SpearAnimations.firstPersonAttack. */
   public static void firstPersonAttack(float attack, PoseStack poses, int invert) {
      float startingAmount = inOutSine(progress(attack, 0.0F, 0.05F));
      float middleAmount = outBack(progress(attack, 0.05F, 0.2F));
      float endingAmount = inOutExpo(progress(attack, 0.4F, 1.0F));
      poses.translate(invert * 0.1F * (startingAmount - middleAmount), -0.075F * (startingAmount - endingAmount), 0.65F * (startingAmount - middleAmount));
      poses.mulPose(Axis.XP.rotationDegrees(-70.0F * (startingAmount - endingAmount)));
      poses.translate(0.0, 0.0, -0.25 * (endingAmount - middleAmount));
   }
}
