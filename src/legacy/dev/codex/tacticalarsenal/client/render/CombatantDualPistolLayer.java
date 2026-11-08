package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.codex.tacticalarsenal.client.compat.ItemTransform;
import dev.codex.tacticalarsenal.client.compat.PoseCompat;
import dev.codex.tacticalarsenal.client.compat.SubmitNodeCollector;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Minecraft 1.21.1 / 1.20.1 version of the combatant dual Berettas layer (same transforms as 26.x). */
final class CombatantDualPistolLayer extends RenderLayer<CombatantEntity, HumanoidModel<CombatantEntity>> {
   CombatantDualPistolLayer(RenderLayerParent<CombatantEntity, HumanoidModel<CombatantEntity>> parent) {
      super(parent);
   }

   static boolean isDualPistol(ItemStack stack) {
      return stack.getItem() instanceof GunItem gun && gun.definition().id().equals("elite");
   }

   static boolean isReady() {
      return Cs2MeshManager.hasMesh("elite");
   }

   @Override
   public void render(
      PoseStack poses, MultiBufferSource buffers, int light, CombatantEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
      float ageInTicks, float yRot, float xRot
   ) {
      CombatantRenderState state = CombatantRenderState.of(entity);
      SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
      if (isDualPistol(state.getMainHandItemStack()) && isReady()) {
         Vec3 rightMuzzle = this.submitHalf(HumanoidArm.RIGHT, false, poses, collector, light);
         Vec3 leftMuzzle = this.submitHalf(HumanoidArm.LEFT, true, poses, collector, light);
         if (rightMuzzle != null && leftMuzzle != null && state.entityId >= 0) {
            MuzzlePositionTracker.capture(state.entityId, "elite", state.mainArm, rightMuzzle, leftMuzzle);
         }
      }
   }

   private Vec3 submitHalf(HumanoidArm arm, boolean positiveX, PoseStack poses, SubmitNodeCollector collector, int light) {
      poses.pushPose();
      this.getParentModel().translateToHand(arm, poses);
      applyVanillaHeldItemFrame(poses, arm);
      ItemTransform transform = Cs2InventoryItemModels.combatantDualPistolTransform(arm == HumanoidArm.LEFT);
      transform.apply(arm == HumanoidArm.LEFT, poses.last());
      PoseCompat.mulPose(poses, Cs2MeshManager.thirdPersonHalfTransform("elite", positiveX));
      Vec3 muzzle = transformedMuzzle(poses, positiveX);
      Cs2MeshManager.submitPartHalf("elite", Cs2MeshManager.MeshPart.ALL, positiveX, poses, collector, light);
      poses.popPose();
      return muzzle;
   }

   private static void applyVanillaHeldItemFrame(PoseStack poses, HumanoidArm arm) {
      poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
      float side = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
      poses.translate(side / 16.0F, 0.125F, -0.625F);
   }

   private static Vec3 transformedMuzzle(PoseStack poses, boolean positiveX) {
      Vector3fc anchor = Cs2MeshManager.muzzleAnchor("elite", positiveX);
      if (anchor != null && Minecraft.getInstance().getEntityRenderDispatcher().camera != null) {
         Vector3f relative = poses.last().pose().transformPosition(new Vector3f(anchor));
         Vec3 camera = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
         return camera.add(relative.x, relative.y, relative.z);
      } else {
         return null;
      }
   }
}
