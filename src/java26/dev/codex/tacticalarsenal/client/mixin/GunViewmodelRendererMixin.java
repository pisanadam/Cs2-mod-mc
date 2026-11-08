package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.client.animation.GunReloadAnimationLibrary;
import dev.codex.tacticalarsenal.client.animation.Transform;
import dev.codex.tacticalarsenal.client.render.GunViewmodelRenderer;
import dev.codex.tacticalarsenal.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({GunViewmodelRenderer.class})
public abstract class GunViewmodelRendererMixin {
   @Shadow
   private static void applyImportedWeaponPose(PoseStack poses, String weaponId, HumanoidArm arm) {
      throw new AssertionError();
   }

   @ModifyConstant(
      method = {"render"},
      constant = {@Constant(
         stringValue = "revolver"
      )}
   )
   private static String tacticalArsenal$animateRevolverCylinder(String ignored) {
      return "__tactical_arsenal_never_skip_revolver_cylinder__";
   }

   @ModifyArg(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/codex/tacticalarsenal/client/render/GunViewmodelRenderer;apply(Lcom/mojang/blaze3d/vertex/PoseStack;Ldev/codex/tacticalarsenal/client/animation/Transform;)V",
         ordinal = 2
      ),
      index = 1,
      require = 1
   )
   private static Transform tacticalArsenal$conjugateImportedMagazine(Transform transform) {
      return tacticalArsenal$conjugateExactReloadChild(transform, false);
   }

   @ModifyArg(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/codex/tacticalarsenal/client/render/GunViewmodelRenderer;applyImportedAction(Lcom/mojang/blaze3d/vertex/PoseStack;Ldev/codex/tacticalarsenal/client/animation/Transform;)V"
      ),
      index = 1,
      require = 1
   )
   private static Transform tacticalArsenal$conjugateImportedAction(Transform transform) {
      return tacticalArsenal$conjugateExactReloadChild(transform, true);
   }

   @Unique
   private static Transform tacticalArsenal$conjugateExactReloadChild(Transform encoded, boolean actionEncoding) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && GunAnimationEngine.INSTANCE.isReloading() && player.getMainHandItem().getItem() instanceof GunItem gun) {
         String weaponId = gun.definition().id();
         if (!weaponId.equals("elite") && GunReloadAnimationLibrary.hasClip(weaponId)) {
            Vector3f sourceTranslation = new Vector3f(encoded.translation());
            if (actionEncoding) {
               sourceTranslation.set(sourceTranslation.x() * 0.1F, sourceTranslation.y() * 0.1F, -sourceTranslation.z() * 0.1F);
            }

            Matrix4f source = new Matrix4f().translate(sourceTranslation).rotate(new Quaternionf(encoded.rotation())).scale(encoded.scale());
            PoseStack importedPose = new PoseStack();
            applyImportedWeaponPose(importedPose, weaponId, player.getMainArm());
            Matrix4f imported = new Matrix4f(importedPose.last().pose());
            Matrix4f conjugated = new Matrix4f(imported).invert().mul(source).mul(imported);
            Vector3f translation = conjugated.getTranslation(new Vector3f());
            if (actionEncoding) {
               translation.set(translation.x() * 10.0F, translation.y() * 10.0F, -translation.z() * 10.0F);
            }

            return new Transform(translation, conjugated.getUnnormalizedRotation(new Quaternionf()).normalize(), conjugated.getScale(new Vector3f()));
         } else {
            return encoded;
         }
      } else {
         return encoded;
      }
   }
}
