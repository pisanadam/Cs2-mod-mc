package dev.codex.tacticalarsenal.client.render;

import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** Minecraft 1.21.1 / 1.20.1 version of the combatant model (same pivots and gun poses as 26.x). */
final class CombatantHumanoidModel extends HumanoidModel<CombatantEntity> {
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

   @Override
   public void prepareMobModel(CombatantEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
      // 26.x HumanoidMobRenderer/CombatantRenderer#getArmPose: guns are held with CROSSBOW_HOLD on the
      // main arm; the off arm and other items use the default (EMPTY) pose.
      this.rightArmPose = armPose(entity, HumanoidArm.RIGHT);
      this.leftArmPose = armPose(entity, HumanoidArm.LEFT);
      super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
   }

   private static ArmPose armPose(CombatantEntity entity, HumanoidArm arm) {
      if (arm != entity.getMainArm()) {
         return ArmPose.EMPTY;
      } else {
         return entity.getMainHandItem().getItem() instanceof GunItem ? ArmPose.CROSSBOW_HOLD : ArmPose.EMPTY;
      }
   }

   @Override
   public void setupAnim(CombatantEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      // Same starting point as 26.x: every part begins at its (rebased) initial pose each frame.
      this.head.resetPose();
      this.hat.resetPose();
      this.body.resetPose();
      this.rightArm.resetPose();
      this.leftArm.resetPose();
      this.rightLeg.resetPose();
      this.leftLeg.resetPose();
      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      this.applyRebasedPivots();
      CombatantRenderState state = CombatantRenderState.of(entity);
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

   /**
    * Older HumanoidModel#setupAnim writes absolute vanilla pivot positions every frame, while 26.x
    * starts from the (rebased) initial pose and only offsets it. Put the 26.x result back.
    */
   private void applyRebasedPivots() {
      float rightArmX = this.rightArm.x;
      float rightArmZ = this.rightArm.z;
      float leftArmX = this.leftArm.x;
      float leftArmZ = this.leftArm.z;
      for (ModelPart part : new ModelPart[]{this.head, this.hat, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg}) {
         net.minecraft.client.model.geom.PartPose initial = part.getInitialPose();
         part.setPos(initial.x, initial.y, initial.z);
      }

      if (this.attackTime > 0.0F) {
         // The attack swing places the arms absolutely in 26.x as well.
         this.rightArm.x = rightArmX;
         this.rightArm.z = rightArmZ;
         this.leftArm.x = leftArmX;
         this.leftArm.z = leftArmZ;
      }

      if (this.crouching) {
         this.rightLeg.z += 4.0F;
         this.leftLeg.z += 4.0F;
         this.head.y += 4.2F;
         this.hat.y += 4.2F;
         this.body.y += 3.2F;
         this.leftArm.y += 3.2F;
         this.rightArm.y += 3.2F;
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
