package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The 26.x ItemInHandRenderer#renderItem call shape on Minecraft 1.21.1 / 1.20.1. */
public final class LegacyHands {
   private LegacyHands() {
   }

   private static final java.util.Map<ItemStack, net.minecraft.resources.Identifier> MODEL_OVERRIDES = new java.util.IdentityHashMap<>();

   /** Stand-in for the 26.x ITEM_MODEL component used by the viewmodel fallback part stacks. */
   public static void useModel(ItemStack stack, net.minecraft.resources.Identifier model) {
      MODEL_OVERRIDES.put(stack, model);
   }

   public static net.minecraft.client.model.geom.ModelPart arm(net.minecraft.client.model.HumanoidModel<?> model, net.minecraft.world.entity.HumanoidArm arm) {
      return arm == net.minecraft.world.entity.HumanoidArm.LEFT ? model.leftArm : model.rightArm;
   }

   public static ItemStack heldByArm(LivingEntity entity, net.minecraft.world.entity.HumanoidArm arm) {
      return arm == entity.getMainArm() ? entity.getMainHandItem() : entity.getOffhandItem();
   }

   public static void renderItem(
      ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
      net.minecraft.resources.Identifier override = MODEL_OVERRIDES.get(stack);
      if (override != null) {
         net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
         minecraft.getItemRenderer().render(
            stack, context, leftHand, poses, collector.buffers(), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            minecraft.getModelManager().getModel(override)
         );
      } else {
         renderer.renderItem(entity, stack, context, leftHand, poses, collector.buffers(), light);
      }
   }
}
