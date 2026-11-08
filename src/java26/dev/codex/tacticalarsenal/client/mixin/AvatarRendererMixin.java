package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.render.PlayerDualPistolLayer;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AvatarRenderer.class})
public abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
   protected AvatarRendererMixin(Context context, PlayerModel model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void tacticalArsenal$addDualPistolLayer(Context context, boolean slim, CallbackInfo callback) {
      this.addLayer(new PlayerDualPistolLayer(this));
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void tacticalArsenal$poseFirearm(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo callback) {
      if (avatar.getMainHandItem().getItem() instanceof GunItem gun) {
         ArmPose pose = gun.definition().category() == WeaponCategory.PISTOL ? ArmPose.BOW_AND_ARROW : ArmPose.CROSSBOW_HOLD;
         if (avatar.getMainArm() == HumanoidArm.RIGHT) {
            state.rightArmPose = pose;
         } else {
            state.leftArmPose = pose;
         }

         if (PlayerDualPistolLayer.isDualPistol(state.getMainHandItemStack()) && PlayerDualPistolLayer.isReady()) {
            state.getMainHandItemState().clear();
         }
      }
   }
}
