package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 26.3 removed ItemInHandRenderer. This reproduces its renderItem method exactly as it
 * was in 26.2 so the viewmodel fallback paths keep working.
 */
public final class HandItemRenderer {
   public static final HandItemRenderer INSTANCE = new HandItemRenderer();

   private HandItemRenderer() {
   }

   public void renderItem(LivingEntity mob, ItemStack itemStack, ItemDisplayContext type, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords) {
      if (!itemStack.isEmpty()) {
         ItemStackRenderState renderState = new ItemStackRenderState();
         Minecraft.getInstance().getItemModelResolver().updateForTopItem(renderState, itemStack, type, mob.level(), mob, mob.getId() + type.ordinal());
         renderState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
      }
   }
}
