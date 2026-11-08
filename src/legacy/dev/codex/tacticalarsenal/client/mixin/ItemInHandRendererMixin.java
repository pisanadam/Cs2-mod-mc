package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.compat.KnifeStab;
import dev.codex.tacticalarsenal.client.compat.SubmitNodeCollector;
import dev.codex.tacticalarsenal.client.render.GunHandsRenderer;
import dev.codex.tacticalarsenal.client.render.GunViewmodelRenderer;
import dev.codex.tacticalarsenal.client.settings.TacticalArsenalClientConfig;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.21.1 / 1.20.1 version of the first-person hooks (same hooks as 26.x), plus the 26.x
 * knife stab swing which older versions do not have.
 */
@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Unique
   private boolean tacticalArsenal$affectsCurrentVanillaArm;
   @Unique
   private boolean tacticalArsenal$vanillaArmPosePushed;
   @Unique
   private boolean tacticalArsenal$knifeViewmodelPosePushed;
   @Unique
   private ItemStack tacticalArsenal$currentStack = ItemStack.EMPTY;

   @Inject(method = {"renderArmWithItem"}, at = {@At("HEAD")}, cancellable = true)
   private void tacticalArsenal$submitGun(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      CallbackInfo callback
   ) {
      this.tacticalArsenal$currentStack = stack;
      this.tacticalArsenal$affectsCurrentVanillaArm = isGrenade(stack);
      SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
      if (stack.getItem() instanceof GunItem && hand == InteractionHand.OFF_HAND) {
         if (!(player.getMainHandItem().getItem() instanceof GunItem)) {
            GunViewmodelRenderer.renderOffhand((ItemInHandRenderer)(Object)this, player, stack, poses, collector, light);
         }

         callback.cancel();
      } else if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof GunItem) {
         callback.cancel();
      } else {
         if (stack.getItem() instanceof GunItem && hand == InteractionHand.MAIN_HAND) {
            GunViewmodelRenderer.render((ItemInHandRenderer)(Object)this, player, stack, partialTick, poses, collector, light);
            callback.cancel();
         }
      }
   }

   @Inject(method = {"renderArmWithItem"}, at = {@At("RETURN")})
   private void tacticalArsenal$clearCurrentArmState(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      CallbackInfo callback
   ) {
      this.tacticalArsenal$affectsCurrentVanillaArm = false;
      this.tacticalArsenal$currentStack = ItemStack.EMPTY;
   }

   /** 26.x knives use the STAB swing: drop the default swing offset for them... */
   @Redirect(
      method = {"renderArmWithItem"},
      slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isAutoSpinAttack()Z")),
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 1)
   )
   private void tacticalArsenal$knifeSwingOffset(PoseStack poses, float x, float y, float z) {
      if (!KnifeStab.isKnife(this.tacticalArsenal$currentStack)) {
         poses.translate(x, y, z);
      }
   }

   /** ...and replace the default swing rotation with the stab motion. */
   @Redirect(
      method = {"renderArmWithItem"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V")
   )
   private void tacticalArsenal$knifeStab(ItemInHandRenderer renderer, PoseStack poses, HumanoidArm arm, float attack) {
      if (KnifeStab.isKnife(this.tacticalArsenal$currentStack)) {
         KnifeStab.firstPersonAttack(attack, poses, arm == HumanoidArm.RIGHT ? 1 : -1);
      } else {
         ((ItemInHandRendererInvoker)renderer).tacticalArsenal$applyItemArmAttackTransform(poses, arm, attack);
      }
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
         ordinal = 1
      )},
      require = 1
   )
   private void tacticalArsenal$renderKnifeHandAtPhysicalGrip(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      CallbackInfo callback
   ) {
      if (stack.getItem() instanceof KnifeItem knife) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         GunHandsRenderer.renderKnifeHand(player, knife.id(), arm, poses, new SubmitNodeCollector(buffers), light);
         poses.pushPose();
         GunHandsRenderer.applyKnifeViewmodelTransform(knife.id(), arm, poses);
         this.tacticalArsenal$knifeViewmodelPosePushed = true;
      }
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
         ordinal = 1,
         shift = Shift.AFTER
      )},
      require = 1
   )
   private void tacticalArsenal$restoreKnifeViewmodelPose(
      AbstractClientPlayer player,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      CallbackInfo callback
   ) {
      if (this.tacticalArsenal$knifeViewmodelPosePushed) {
         poses.popPose();
         this.tacticalArsenal$knifeViewmodelPosePushed = false;
      }
   }

   @Inject(method = {"renderPlayerArm"}, at = {@At("HEAD")}, cancellable = true)
   private void tacticalArsenal$prepareKnifeOrGrenadeArm(
      PoseStack poses, MultiBufferSource buffers, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo callback
   ) {
      if (this.tacticalArsenal$affectsCurrentVanillaArm) {
         if (!TacticalArsenalClientConfig.hideHands() && !(TacticalArsenalClientConfig.handScale() <= 0.0F)) {
            float scale = TacticalArsenalClientConfig.handScale();
            if (scale < 1.0F) {
               poses.pushPose();
               poses.scale(scale, scale, scale);
               this.tacticalArsenal$vanillaArmPosePushed = true;
            }
         } else {
            callback.cancel();
         }
      }
   }

   @Inject(method = {"renderPlayerArm"}, at = {@At("RETURN")})
   private void tacticalArsenal$finishKnifeOrGrenadeArm(
      PoseStack poses, MultiBufferSource buffers, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo callback
   ) {
      if (this.tacticalArsenal$vanillaArmPosePushed) {
         poses.popPose();
         this.tacticalArsenal$vanillaArmPosePushed = false;
      }
   }

   @Unique
   private static boolean isGrenade(ItemStack stack) {
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (!id.getNamespace().equals("tactical_arsenal")) {
         return false;
      } else {
         String path = id.getPath();
         return path.contains("grenade") || path.equals("molotov") || path.equals("incendiary") || path.equals("flashbang") || path.equals("decoy");
      }
   }
}
