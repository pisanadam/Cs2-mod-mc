package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.compat.KnifeStab;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third-person knife stab (26.x SPEAR arm pose + STAB attack animation) for players and combatants. */
@Mixin({HumanoidModel.class})
public abstract class HumanoidModelKnifeStabMixin<T extends LivingEntity> {
   @Shadow @Final public ModelPart head;
   @Shadow @Final public ModelPart body;
   @Shadow @Final public ModelPart rightArm;
   @Shadow @Final public ModelPart leftArm;
   @Shadow public float swimAmount;

   @Shadow
   public abstract ModelPart getArm(HumanoidArm arm);

   @Inject(method = {"setupAttackAnimation"}, at = {@At("HEAD")}, cancellable = true)
   private void tacticalArsenal$knifeStab(T entity, float ageInTicks, CallbackInfo callback) {
      HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
      if (entity.swinging && !entity.isUsingItem()) {
         boolean fallFlyingOrSwimming = entity.isFallFlying() || this.swimAmount > 0.0F;
         if (KnifeStab.isKnife(dev.codex.tacticalarsenal.client.compat.LegacyHands.heldByArm(entity, HumanoidArm.RIGHT))) {
            KnifeStab.spearArmPose(this.rightArm, this.head, true, fallFlyingOrSwimming);
         }

         if (KnifeStab.isKnife(dev.codex.tacticalarsenal.client.compat.LegacyHands.heldByArm(entity, HumanoidArm.LEFT))) {
            KnifeStab.spearArmPose(this.leftArm, this.head, false, fallFlyingOrSwimming);
         }
      }

      float attackTime = model.attackTime;
      HumanoidArm attackArm = KnifeStab.attackArm(entity);
      if (!(attackTime <= 0.0F) && KnifeStab.isKnife(dev.codex.tacticalarsenal.client.compat.LegacyHands.heldByArm(entity, attackArm))) {
         this.body.yRot = Mth.sin(Mth.sqrt(attackTime) * (float) (Math.PI * 2)) * 0.2F;
         if (attackArm == HumanoidArm.LEFT) {
            this.body.yRot *= -1.0F;
         }

         this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
         this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
         this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
         this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
         this.rightArm.yRot = this.rightArm.yRot + this.body.yRot;
         this.leftArm.yRot = this.leftArm.yRot + this.body.yRot;
         this.leftArm.xRot = this.leftArm.xRot + this.body.yRot;
         KnifeStab.thirdPersonAttackHand(model, attackTime, attackArm);
         callback.cancel();
      }
   }
}
