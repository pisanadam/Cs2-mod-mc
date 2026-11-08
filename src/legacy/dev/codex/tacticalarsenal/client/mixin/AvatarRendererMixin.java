package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.render.PlayerDualPistolLayer;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 1.21.1 / 1.20.1 version of the 26.x AvatarRenderer hooks (dual pistol layer + gun arm pose). */
@Mixin({PlayerRenderer.class})
public abstract class AvatarRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   protected AvatarRendererMixin(Context context, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
      super(context, model, shadowRadius);
   }

   @Inject(method = {"<init>"}, at = {@At("TAIL")})
   private void tacticalArsenal$addDualPistolLayer(Context context, boolean slim, CallbackInfo callback) {
      this.addLayer(new PlayerDualPistolLayer((PlayerRenderer)(Object)this));
   }

   @Inject(method = {"setModelProperties"}, at = {@At("TAIL")})
   private void tacticalArsenal$poseFirearm(AbstractClientPlayer player, CallbackInfo callback) {
      if (player.getMainHandItem().getItem() instanceof GunItem gun) {
         ArmPose pose = gun.definition().category() == WeaponCategory.PISTOL ? ArmPose.BOW_AND_ARROW : ArmPose.CROSSBOW_HOLD;
         PlayerModel<AbstractClientPlayer> model = this.getModel();
         if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = pose;
         } else {
            model.leftArmPose = pose;
         }
      }
   }
}
