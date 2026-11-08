package dev.codex.tacticalarsenal.client.render;

import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

final class CombatantHumanoidModel extends HumanoidModel<CombatantRenderState> {
   private static final float VANILLA_RIGHT_ARM_X = -5.0F;
   private static final float VANILLA_LEFT_ARM_X = 5.0F;
   private static final float VANILLA_ARM_Y = 2.0F;
   private static final float VANILLA_RIGHT_LEG_X = -1.9F;
   private static final float VANILLA_LEFT_LEG_X = 1.9F;
   private static final float VANILLA_LEG_Y = 12.0F;
   private static final float HEAD_X = 0.0F;
   private static final float HEAD_Y = -2.64317F;
   private static final float HEAD_Z = -0.15672F;
   private static final float RIGHT_ARM_X = -2.91794F;
   private static final float RIGHT_ARM_Y = -1.16794F;
   private static final float RIGHT_ARM_Z = -0.05405F;
   private static final float LEFT_ARM_X = 2.91794F;
   private static final float LEFT_ARM_Y = -1.16792F;
   private static final float LEFT_ARM_Z = -0.05405F;
   private static final float RIGHT_LEG_X = -1.50133F;
   private static final float RIGHT_LEG_Y = 8.0176F;
   private static final float RIGHT_LEG_Z = -0.62245F;
   private static final float LEFT_LEG_X = 1.50133F;
   private static final float LEFT_LEG_Y = 8.01757F;
   private static final float LEFT_LEG_Z = -0.62245F;
   private static final float PISTOL_MAIN_PITCH = -1.42F;
   private static final float PISTOL_SUPPORT_PITCH = -1.38F;
   private static final float PISTOL_MAIN_YAW = 0.1F;
   private static final float PISTOL_SUPPORT_YAW = 0.18F;
   private static final float DUAL_PISTOL_PITCH = -1.42F;
   private static final float DUAL_PISTOL_YAW = 0.12F;
   private static final float LONG_GUN_MAIN_PITCH = -1.47F;
   private static final float LONG_GUN_SUPPORT_PITCH = -1.5F;
   private static final float LONG_GUN_MAIN_YAW = 0.3F;
   private static final float LONG_GUN_SUPPORT_YAW = 0.58F;
   private final String meshPrefix;

   CombatantHumanoidModel(ModelPart root, String meshPrefix) {
      super(root);
      this.meshPrefix = meshPrefix;
      rebase(this.head, 0.0F, -2.64317F, -0.15672F);
      rebase(this.hat, 0.0F, -2.64317F, -0.15672F);
      rebase(this.body, 0.0F, 0.0F, 0.0F);
      rebase(this.rightArm, -2.91794F, -1.16794F, -0.05405F);
      rebase(this.leftArm, 2.91794F, -1.16792F, -0.05405F);
      rebase(this.rightLeg, -1.50133F, 8.0176F, -0.62245F);
      rebase(this.leftLeg, 1.50133F, 8.01757F, -0.62245F);
   }

   private static void rebase(ModelPart part, float x, float y, float z) {
      part.setPos(x, y, z);
      part.setInitialPose(part.storePose());
   }

   public void setupAnim(CombatantRenderState state) {
      super.setupAnim(state);
      if (!Cs2CombatantSkinLayer.isReady(this.meshPrefix)) {
         this.restoreVanillaPivots();
      }

      ItemStack mainHand = state.getMainHandItemStack();
      if (mainHand.getItem() instanceof GunItem gun) {
         if (gun.definition().id().equals("elite")) {
            this.poseDualPistols(state);
         } else if (gun.definition().category() == WeaponCategory.PISTOL) {
            this.poseTwoHandedAim(state, -1.42F, -1.38F, 0.1F, 0.18F);
         } else {
            this.poseTwoHandedAim(state, -1.47F, -1.5F, 0.3F, 0.58F);
         }
      }
   }

   private void restoreVanillaPivots() {
      this.head.setPos(0.0F, 0.0F, 0.0F);
      this.hat.setPos(0.0F, 0.0F, 0.0F);
      this.body.setPos(0.0F, 0.0F, 0.0F);
      this.rightArm.setPos(-5.0F, 2.0F, 0.0F);
      this.leftArm.setPos(5.0F, 2.0F, 0.0F);
      this.rightLeg.setPos(-1.9F, 12.0F, 0.0F);
      this.leftLeg.setPos(1.9F, 12.0F, 0.0F);
   }

   private void poseDualPistols(CombatantRenderState state) {
      float crouchPitch = state.isCrouching ? 0.4F : 0.0F;
      this.rightArm.xRot = this.head.xRot + -1.42F + crouchPitch;
      this.rightArm.yRot = this.head.yRot - 0.12F;
      this.rightArm.zRot = 0.0F;
      this.leftArm.xRot = this.head.xRot + -1.42F + crouchPitch;
      this.leftArm.yRot = this.head.yRot + 0.12F;
      this.leftArm.zRot = 0.0F;
   }

   private void poseTwoHandedAim(CombatantRenderState state, float mainPitch, float supportPitch, float mainYaw, float supportYaw) {
      HumanoidArm mainArm = state.mainArm;
      ModelPart main = this.getArm(mainArm);
      ModelPart support = this.getArm(mainArm.getOpposite());
      float side = mainArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      float crouchPitch = state.isCrouching ? 0.4F : 0.0F;
      main.xRot = this.head.xRot + mainPitch + crouchPitch;
      main.yRot = this.head.yRot - side * mainYaw;
      main.zRot = 0.0F;
      support.xRot = this.head.xRot + supportPitch + crouchPitch;
      support.yRot = this.head.yRot + side * supportYaw;
      support.zRot = 0.0F;
   }
}
