package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.compat.AvatarRenderState;
import dev.codex.tacticalarsenal.client.render.PlayerThirdPersonGripSolver;
import dev.codex.tacticalarsenal.gun.GunItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft 1.21.1 / 1.20.1 version of the 26.x third-person firearm hand posing. */
@Mixin({PlayerModel.class})
public abstract class PlayerModelMixin<T extends LivingEntity> {
   @Inject(method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"}, at = {@At("TAIL")})
   private void tacticalArsenal$poseFirearmHands(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callback) {
      AvatarRenderState state = AvatarRenderState.of(entity);
      ItemStack mainHand = state.getMainHandItemStack();
      if (mainHand.getItem() instanceof GunItem gun) {
         PlayerModel<?> model = (PlayerModel<?>)(Object)this;
         if (!gun.definition().id().equals("elite")) {
            PlayerThirdPersonGripSolver.poseSupportArm(model, state, gun.definition());
         } else {
            ModelPart right = model.getArm(HumanoidArm.RIGHT);
            ModelPart left = model.getArm(HumanoidArm.LEFT);
            right.xRot = model.head.xRot - 1.42F;
            right.yRot = model.head.yRot - 0.1F;
            right.zRot = 0.0F;
            left.xRot = model.head.xRot - 1.42F;
            left.yRot = model.head.yRot + 0.1F;
            left.zRot = 0.0F;
         }

         // In older versions the sleeves are separate parts copied from the arms inside setupAnim.
         model.leftSleeve.copyFrom(model.leftArm);
         model.rightSleeve.copyFrom(model.rightArm);
      }
   }
}
