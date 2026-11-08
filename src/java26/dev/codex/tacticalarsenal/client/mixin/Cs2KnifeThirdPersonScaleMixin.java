package dev.codex.tacticalarsenal.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"dev.codex.tacticalarsenal.client.render.Cs2InventoryItemModels$GuiMeshItemModel"}
)
public abstract class Cs2KnifeThirdPersonScaleMixin {
   private static final Vector3fc STANDARD_GRIP = new Vector3f(0.0F, 0.0127F, 0.0F);
   private static final Vector3fc SPECIAL_GRIP = new Vector3f(0.0F, 0.0254F, 0.0F);
   private static final float STANDARD_SCALE = 1.404F;
   private static final float KARAMBIT_SCALE = 2.171F;
   private static final float BUTTERFLY_SCALE = 1.606F;
   @Shadow
   @Final
   private String weaponId;
   @Unique
   private ItemDisplayContext tacticalArsenal$currentDisplayContext;

   @Inject(
      method = {"update"},
      at = {@At("HEAD")}
   )
   private void tacticalArsenal$captureDisplayContext(
      ItemStackRenderState state,
      ItemStack stack,
      ItemModelResolver resolver,
      ItemDisplayContext displayContext,
      ClientLevel level,
      ItemOwner owner,
      int seed,
      CallbackInfo callback
   ) {
      this.tacticalArsenal$currentDisplayContext = displayContext;
   }

   @Inject(
      method = {"update"},
      at = {@At("RETURN")}
   )
   private void tacticalArsenal$clearDisplayContext(
      ItemStackRenderState state,
      ItemStack stack,
      ItemModelResolver resolver,
      ItemDisplayContext displayContext,
      ClientLevel level,
      ItemOwner owner,
      int seed,
      CallbackInfo callback
   ) {
      this.tacticalArsenal$currentDisplayContext = null;
   }

   @ModifyArg(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;setLocalTransform(Lorg/joml/Matrix4fc;)V",
         ordinal = 0
      ),
      index = 0,
      require = 1
   )
   private Matrix4fc tacticalArsenal$normalizeThirdPersonKnifeLength(Matrix4fc original) {
      ItemDisplayContext displayContext = this.tacticalArsenal$currentDisplayContext;
      if (displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
         return original;
      } else {
         String var5 = this.weaponId;
         Vector3fc grip;
         float scale;
         switch (var5) {
            case "knife":
               grip = STANDARD_GRIP;
               scale = 1.404F;
               break;
            case "karambit":
               grip = SPECIAL_GRIP;
               scale = 2.171F;
               break;
            case "butterfly":
               grip = SPECIAL_GRIP;
               scale = 1.606F;
               break;
            default:
               return original;
         }

         return new Matrix4f(original).translate(grip.x(), grip.y(), grip.z()).scale(scale).translate(-grip.x(), -grip.y(), -grip.z());
      }
   }
}
