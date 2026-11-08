package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.render.Cs2MeshManager;
import dev.codex.tacticalarsenal.gun.GunDefinitions;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Cs2MeshManager.class})
public abstract class Cs2MeshManagerFirearmScaleMixin {
   private static final float FIREARM_SCALE_MULTIPLIER = 1.5F;
   private static final float UPRIGHT_ROLL_CORRECTION_RADIANS = (float) Math.PI;
   private static final float KARAMBIT_REVERSE_GRIP_RADIANS = (float)Math.toRadians(-57.18F);
   private static final float ELITE_HALF_GRIP_SEPARATION = 0.508F;
   @Shadow
   @Final
   private static Map<String, Vector3fc> THIRD_PERSON_GRIP_ANCHORS;

   @Inject(
      method = {"thirdPersonTransform(Ljava/lang/String;Z)Lorg/joml/Matrix4fc;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void tacticalArsenal$orientAndScaleHeldFirearm(String weaponId, boolean leftHand, CallbackInfoReturnable<Matrix4fc> callback) {
      if (weaponId.equals("karambit")) {
         Vector3fc knifeGrip = THIRD_PERSON_GRIP_ANCHORS.get(weaponId);
         if (knifeGrip != null) {
            Matrix4f corrected = new Matrix4f((Matrix4fc)callback.getReturnValue())
               .translate(knifeGrip.x(), knifeGrip.y(), knifeGrip.z())
               .rotateX(KARAMBIT_REVERSE_GRIP_RADIANS);
            callback.setReturnValue(corrected.translate(-knifeGrip.x(), -knifeGrip.y(), -knifeGrip.z()));
         }
      } else if (GunDefinitions.byId(weaponId) != null) {
         Vector3fc grip = THIRD_PERSON_GRIP_ANCHORS.get(weaponId);
         if (grip != null) {
            callback.setReturnValue(orientAndScaleAroundGrip((Matrix4fc)callback.getReturnValue(), grip));
         }
      }
   }

   @Inject(
      method = {"thirdPersonHalfTransform(Ljava/lang/String;Z)Lorg/joml/Matrix4fc;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void tacticalArsenal$orientAndScaleHeldFirearmHalf(String weaponId, boolean positiveX, CallbackInfoReturnable<Matrix4fc> callback) {
      if (GunDefinitions.byId(weaponId) != null) {
         Vector3fc baseGrip = THIRD_PERSON_GRIP_ANCHORS.get(weaponId);
         if (baseGrip != null) {
            Vector3fc physicalGrip = (Vector3fc)(weaponId.equals("elite") && positiveX
               ? new Vector3f(baseGrip.x() + 0.508F, baseGrip.y(), baseGrip.z())
               : baseGrip);
            callback.setReturnValue(orientAndScaleAroundGrip((Matrix4fc)callback.getReturnValue(), physicalGrip));
         }
      }
   }

   private static Matrix4fc orientAndScaleAroundGrip(Matrix4fc original, Vector3fc grip) {
      return new Matrix4f(original).translate(grip.x(), grip.y(), grip.z()).rotateZ((float) Math.PI).scale(1.5F).translate(-grip.x(), -grip.y(), -grip.z());
   }
}
