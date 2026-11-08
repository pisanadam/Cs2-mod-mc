package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.compat.HandItemRenderer;
import dev.codex.tacticalarsenal.client.render.GunHandsRenderer;
import dev.codex.tacticalarsenal.client.render.GunViewmodelRenderer;
import dev.codex.tacticalarsenal.client.settings.TacticalArsenalClientConfig;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.3 version of the first-person hooks. In 26.3 ItemInHandRenderer became
 * FirstPersonHandsAndItemsRenderer and works from an extracted render state; the hooks and what they
 * do are the same as in 26.2. First-person hands always belong to the local player.
 */
@Mixin({FirstPersonHandsAndItemsRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Unique
   private boolean tacticalArsenal$affectsCurrentVanillaArm;
   @Unique
   private boolean tacticalArsenal$vanillaArmPosePushed;
   @Unique
   private boolean tacticalArsenal$knifeViewmodelPosePushed;

   @Unique
   private static AbstractClientPlayer tacticalArsenal$player() {
      return Minecraft.getInstance().player;
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$submitGun(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo callback
   ) {
      AbstractClientPlayer player = tacticalArsenal$player();
      if (player == null) {
         return;
      }
      this.tacticalArsenal$affectsCurrentVanillaArm = isGrenade(stack);
      if (stack.getItem() instanceof GunItem && hand == InteractionHand.OFF_HAND) {
         if (!(player.getMainHandItem().getItem() instanceof GunItem)) {
            GunViewmodelRenderer.renderOffhand(HandItemRenderer.INSTANCE, player, stack, poses, collector, light);
         }

         callback.cancel();
      } else if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof GunItem) {
         callback.cancel();
      } else {
         if (stack.getItem() instanceof GunItem && hand == InteractionHand.MAIN_HAND) {
            GunViewmodelRenderer.render(HandItemRenderer.INSTANCE, player, stack, partialTick, poses, collector, light);
            callback.cancel();
         }
      }
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At("RETURN")}
   )
   private void tacticalArsenal$clearCurrentArmState(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo callback
   ) {
      this.tacticalArsenal$affectsCurrentVanillaArm = false;
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
         ordinal = 1
      )},
      require = 1
   )
   private void tacticalArsenal$renderKnifeHandAtPhysicalGrip(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo callback
   ) {
      AbstractClientPlayer player = tacticalArsenal$player();
      if (player != null && stack.getItem() instanceof KnifeItem knife) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         GunHandsRenderer.renderKnifeHand(player, knife.id(), arm, poses, collector, light);
         poses.pushPose();
         GunHandsRenderer.applyKnifeViewmodelTransform(knife.id(), arm, poses);
         this.tacticalArsenal$knifeViewmodelPosePushed = true;
      }
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
         ordinal = 1,
         shift = Shift.AFTER
      )},
      require = 1
   )
   private void tacticalArsenal$restoreKnifeViewmodelPose(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo callback
   ) {
      if (this.tacticalArsenal$knifeViewmodelPosePushed) {
         poses.popPose();
         this.tacticalArsenal$knifeViewmodelPosePushed = false;
      }
   }

   @Inject(
      method = {"renderPlayerArm"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$prepareKnifeOrGrenadeArm(
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      float equipProgress,
      float swingProgress,
      HumanoidArm arm,
      PlayerRenderState playerState,
      CallbackInfo callback
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

   @Inject(
      method = {"renderPlayerArm"},
      at = {@At("RETURN")}
   )
   private void tacticalArsenal$finishKnifeOrGrenadeArm(
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      float equipProgress,
      float swingProgress,
      HumanoidArm arm,
      PlayerRenderState playerState,
      CallbackInfo callback
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
