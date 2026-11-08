package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.render.Cs2InventoryItemModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the CS2 mesh item renderer which entity holds the item (26.x passes it as the item owner). */
@Mixin({ItemRenderer.class})
public abstract class ItemRendererOwnerMixin {
   @Inject(
      method = {"renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V"},
      at = {@At("HEAD")}
   )
   private void tacticalArsenal$pushOwner(
      LivingEntity owner, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poses, MultiBufferSource buffers, Level level, int light, int overlay, int seed, CallbackInfo callback
   ) {
      Cs2InventoryItemModels.pushOwner(owner);
   }

   @Inject(
      method = {"renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V"},
      at = {@At("RETURN")}
   )
   private void tacticalArsenal$popOwner(
      LivingEntity owner, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poses, MultiBufferSource buffers, Level level, int light, int overlay, int seed, CallbackInfo callback
   ) {
      Cs2InventoryItemModels.popOwner();
   }
}
