package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.joml.Quaternionf;

public final class Cs2ArmorRenderer implements ArmorRenderer {
   private static final Cs2ArmorRenderer INSTANCE = new Cs2ArmorRenderer();
   private static final String HELMET_MESH = "cs_helmet";
   private static final String KEVLAR_MESH = "cs_kevlar";
   private static final float HELMET_SCALE = 2.45F;
   private static final float HELMET_X = -0.0077F;
   private static final float HELMET_Y = -0.2603F;
   private static final float HELMET_Z = 0.0052F;
   private static final float KEVLAR_SCALE = 1.44F;
   private static final float KEVLAR_X = -0.0045F;
   private static final float KEVLAR_Y = 0.3627F;
   private static final float KEVLAR_Z = 0.0031F;

   private Cs2ArmorRenderer() {
   }

   public static void initialize() {
      ArmorRenderer.register(INSTANCE, new ItemLike[]{ModItems.CS_HELMET, ModItems.CS_KEVLAR});
   }

   public void render(
      PoseStack poses,
      SubmitNodeCollector collector,
      ItemStack stack,
      HumanoidRenderState state,
      EquipmentSlot slot,
      int light,
      HumanoidModel<HumanoidRenderState> parentModel
   ) {
      if (slot == EquipmentSlot.HEAD && stack.getItem() == ModItems.CS_HELMET) {
         renderHelmet(poses, collector, light, parentModel);
      } else if (slot == EquipmentSlot.CHEST && stack.getItem() == ModItems.CS_KEVLAR) {
         renderKevlar(poses, collector, light, parentModel);
      }
   }

   private static void renderHelmet(PoseStack poses, SubmitNodeCollector collector, int light, HumanoidModel<HumanoidRenderState> parentModel) {
      poses.pushPose();
      parentModel.head.translateAndRotate(poses);
      poses.translate(-0.0077F, -0.2603F, 0.0052F);
      orientSource2Equipment(poses);
      poses.scale(2.45F, 2.45F, 2.45F);
      Cs2MeshManager.submit("cs_helmet", poses, collector, light);
      poses.popPose();
   }

   private static void renderKevlar(PoseStack poses, SubmitNodeCollector collector, int light, HumanoidModel<HumanoidRenderState> parentModel) {
      poses.pushPose();
      parentModel.body.translateAndRotate(poses);
      poses.translate(-0.0045F, 0.3627F, 0.0031F);
      orientSource2Equipment(poses);
      poses.scale(1.44F, 1.44F, 1.44F);
      Cs2MeshManager.submit("cs_kevlar", poses, collector, light);
      poses.popPose();
   }

   private static void orientSource2Equipment(PoseStack poses) {
      poses.mulPose(new Quaternionf().rotationZ((float) Math.PI));
      poses.mulPose(new Quaternionf().rotationY((float) (-Math.PI / 2)));
   }
}
