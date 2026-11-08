package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Quaternionf;

final class Cs2CombatantSkinLayer extends RenderLayer<CombatantRenderState, HumanoidModel<CombatantRenderState>> {
   private static final List<String> PART_NAMES = List.of(
      "head", "body", "right_upper_arm", "right_lower_arm", "right_hand", "left_upper_arm", "left_lower_arm", "left_hand", "right_leg", "left_leg"
   );
   private final String meshPrefix;

   Cs2CombatantSkinLayer(RenderLayerParent<CombatantRenderState, HumanoidModel<CombatantRenderState>> parent, String meshPrefix) {
      super(parent);
      this.meshPrefix = meshPrefix;
   }

   static boolean isReady(String meshPrefix) {
      for (String partName : PART_NAMES) {
         if (!Cs2MeshManager.hasMesh(meshPrefix + "_" + partName)) {
            return false;
         }
      }

      return true;
   }

   public void submit(PoseStack poses, SubmitNodeCollector collector, int light, CombatantRenderState state, float yRot, float xRot) {
      if (isReady(this.meshPrefix) && (!state.isInvisible || !state.isInvisibleToPlayer)) {
         HumanoidModel<CombatantRenderState> model = (HumanoidModel<CombatantRenderState>)this.getParentModel();
         this.submitPart("head", model.head, poses, collector, light);
         this.submitPart("body", model.body, poses, collector, light);
         Cs2CombatantArmRig.submitArms(this.meshPrefix, model, state, poses, collector, light);
         this.submitPart("right_leg", model.rightLeg, poses, collector, light);
         this.submitPart("left_leg", model.leftLeg, poses, collector, light);
      }
   }

   private void submitPart(String partName, ModelPart bone, PoseStack poses, SubmitNodeCollector collector, int light) {
      poses.pushPose();
      bone.translateAndRotate(poses);
      orientSource2Character(poses);
      Cs2MeshManager.submit(this.meshPrefix + "_" + partName, poses, collector, light);
      poses.popPose();
   }

   private static void orientSource2Character(PoseStack poses) {
      poses.mulPose(new Quaternionf().rotationX((float) Math.PI));
   }
}
