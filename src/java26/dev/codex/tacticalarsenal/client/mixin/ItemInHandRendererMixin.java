package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.render.GunHandsRenderer;
import dev.codex.tacticalarsenal.client.render.GunViewmodelRenderer;
import dev.codex.tacticalarsenal.client.settings.TacticalArsenalClientConfig;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Unique
   private boolean tacticalArsenal$affectsCurrentVanillaArm;
   @Unique
   private boolean tacticalArsenal$vanillaArmPosePushed;
   @Unique
   private boolean tacticalArsenal$knifeViewmodelPosePushed;

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void tacticalArsenal$submitGun(
      AbstractClientPlayer player,
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
      this.tacticalArsenal$affectsCurrentVanillaArm = isGrenade(stack);
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

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At("RETURN")}
   )
   private void tacticalArsenal$clearCurrentArmState(
      AbstractClientPlayer player,
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
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
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
      SubmitNodeCollector collector,
      int light,
      CallbackInfo callback
   ) {
      if (stack.getItem() instanceof KnifeItem knife) {
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
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
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
      PoseStack poses, SubmitNodeCollector collector, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo callback
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
      PoseStack poses, SubmitNodeCollector collector, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo callback
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
