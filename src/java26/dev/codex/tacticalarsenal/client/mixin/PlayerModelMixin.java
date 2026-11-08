package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.render.PlayerThirdPersonGripSolver;
import dev.codex.tacticalarsenal.gun.GunItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerModel.class})
public abstract class PlayerModelMixin {
   @Inject(
      method = {"setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V"},
      at = {@At("TAIL")}
   )
   private void tacticalArsenal$poseFirearmHands(AvatarRenderState state, CallbackInfo callback) {
      ItemStack mainHand = state.getMainHandItemStack();
      if (mainHand.getItem() instanceof GunItem gun) {
         PlayerModel var8 = (PlayerModel)(Object)this;
         if (!gun.definition().id().equals("elite")) {
            PlayerThirdPersonGripSolver.poseSupportArm(var8, state, gun.definition());
         } else {
            ModelPart right = var8.getArm(HumanoidArm.RIGHT);
            ModelPart left = var8.getArm(HumanoidArm.LEFT);
            right.xRot = var8.head.xRot - 1.42F;
            right.yRot = var8.head.yRot - 0.1F;
            right.zRot = 0.0F;
            left.xRot = var8.head.xRot - 1.42F;
            left.yRot = var8.head.yRot + 0.1F;
            left.zRot = 0.0F;
         }
      }
   }
}
