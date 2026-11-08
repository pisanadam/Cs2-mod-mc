package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.compat.KnifeStab;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Held-knife stab motion in third person (26.x SpearAnimations.thirdPersonAttackItem). */
@Mixin({ItemInHandLayer.class})
public abstract class ItemInHandLayerKnifeStabMixin {
   /** 26.x clears a player's main-hand item state while the dual Berettas layer draws them. */
   @Inject(method = {"renderArmWithItem"}, at = {@At("HEAD")}, cancellable = true)
   private void tacticalArsenal$hidePlayerDualPistols(
      LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo callback
   ) {
      if (entity instanceof net.minecraft.client.player.AbstractClientPlayer
         && arm == entity.getMainArm()
         && dev.codex.tacticalarsenal.client.render.PlayerDualPistolLayer.isDualPistol(stack)
         && dev.codex.tacticalarsenal.client.render.PlayerDualPistolLayer.isReady()) {
         callback.cancel();
      }
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")}
   )
   private void tacticalArsenal$stabItem(
      LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo callback
   ) {
      float attackTime = ((RenderLayer<?, ?>)(Object)this).getParentModel().attackTime;
      if (attackTime > 0.0F && KnifeStab.attackArm(entity) == arm && KnifeStab.isKnife(stack)) {
         KnifeStab.thirdPersonAttackItem(attackTime, poses);
      }
   }
}
