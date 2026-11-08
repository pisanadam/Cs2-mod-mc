package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.client.compat.ItemTransform;
import dev.codex.tacticalarsenal.client.compat.SubmitNodeCollector;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunDefinitions;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import dev.codex.tacticalarsenal.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Minecraft 1.21.1 / 1.20.1 version of the CS2 mesh item models. 26.x swaps the baked item model for
 * a mesh special-model; older versions have no item model API, so the same decisions and transforms
 * run inside a Fabric builtin item renderer. The owner entity (needed for the held transform and the
 * muzzle tracker) is captured by ItemRendererOwnerMixin.
 */
public final class Cs2InventoryItemModels {
   private static final ItemTransform THIRD_PERSON_ITEM_TRANSFORM = new ItemTransform(
      new Vector3f(0.0F, -90.0F, 55.0F), new Vector3f(0.0F, 0.0625F, -0.125F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform THIRD_PERSON_KNIFE_TRANSFORM = new ItemTransform(
      new Vector3f(0.0F, -90.0F, 55.0F), new Vector3f(0.0F, 0.25F, 0.03125F), new Vector3f(0.85F, 0.85F, 0.85F)
   );
   private static final ItemTransform FIRST_PERSON_KNIFE_TRANSFORM = new ItemTransform(
      new Vector3f(0.0F, -90.0F, 25.0F), new Vector3f(0.070625F, 0.2F, 0.070625F), new Vector3f(0.68F, 0.68F, 0.68F)
   );
   private static final ItemTransform PLAYER_PISTOL_RIGHT_TRANSFORM = new ItemTransform(
      new Vector3f(-115.947F, -79.9764F, 55.0F), new Vector3f(-0.00577F, -0.2371F, -0.2466F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform PLAYER_PISTOL_LEFT_TRANSFORM = new ItemTransform(
      new Vector3f(127.7081F, -78.59F, -60.4417F), new Vector3f(0.010056F, -0.277458F, 0.023295F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform PLAYER_LONG_GUN_RIGHT_TRANSFORM = new ItemTransform(
      new Vector3f(-115.1935F, -58.9876F, 55.0F), new Vector3f(-0.01708F, -0.2313F, -0.2499F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform PLAYER_LONG_GUN_LEFT_TRANSFORM = new ItemTransform(
      new Vector3f(126.7139F, -58.9655F, -55.0257F), new Vector3f(0.017135F, -0.260776F, 0.041822F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform COMBATANT_PISTOL_RIGHT_TRANSFORM = new ItemTransform(
      new Vector3f(-115.947F, -79.9764F, 55.0F), new Vector3f(-0.06827F, -0.236621F, -0.049782F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform COMBATANT_PISTOL_LEFT_TRANSFORM = new ItemTransform(
      new Vector3f(133.2272F, -79.9764F, -54.9999F), new Vector3f(-0.05673F, -0.27756F, 0.219639F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform COMBATANT_DUAL_PISTOL_RIGHT_TRANSFORM = new ItemTransform(
      new Vector3f(-115.7628F, -77.9531F, 55.0F), new Vector3f(-0.069419F, -0.235921F, -0.050237F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform COMBATANT_DUAL_PISTOL_LEFT_TRANSFORM = new ItemTransform(
      new Vector3f(133.0427F, -77.9531F, -55.0001F), new Vector3f(-0.055581F, -0.277027F, 0.220282F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform COMBATANT_LONG_GUN_RIGHT_TRANSFORM = new ItemTransform(
      new Vector3f(-115.1935F, -58.9876F, 55.0F), new Vector3f(-0.079581F, -0.230746F, -0.053038F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform COMBATANT_LONG_GUN_LEFT_TRANSFORM = new ItemTransform(
      new Vector3f(126.7439F, -58.9876F, -55.0F), new Vector3f(-0.045419F, -0.260248F, 0.238664F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   // Same values as Cs2KnifeThirdPersonScaleMixin in the 26.x version.
   private static final Vector3fc STANDARD_KNIFE_GRIP = new Vector3f(0.0F, 0.0127F, 0.0F);
   private static final Vector3fc SPECIAL_KNIFE_GRIP = new Vector3f(0.0F, 0.0254F, 0.0F);
   private static final ThreadLocal<LivingEntity> OWNER = new ThreadLocal<>();

   private Cs2InventoryItemModels() {
   }

   public static void initialize() {
      List<Identifier> fallbackModels = new ArrayList<>();
      for (GunDefinition gun : GunDefinitions.ALL) {
         BuiltinItemRendererRegistry.INSTANCE.register(ModItems.byId(gun.id()), new MeshItemRenderer(gun.id(), HeldPresentation.FIREARM));
         fallbackModels.add(TacticalArsenal.id("item/" + gun.id() + "_body"));
         fallbackModels.add(TacticalArsenal.id("item/" + gun.id() + "_magazine"));
         fallbackModels.add(TacticalArsenal.id("item/" + gun.id() + "_action"));
      }

      for (String knife : List.of("knife", "karambit", "butterfly")) {
         BuiltinItemRendererRegistry.INSTANCE.register(ModItems.knifeById(knife), new MeshItemRenderer(knife, HeldPresentation.KNIFE));
         fallbackModels.add(TacticalArsenal.id("item/" + knife + "_fallback"));
      }

      BuiltinItemRendererRegistry.INSTANCE.register(ModItems.CS_HELMET, new MeshItemRenderer("cs_helmet", HeldPresentation.NONE));
      BuiltinItemRendererRegistry.INSTANCE.register(ModItems.CS_KEVLAR, new MeshItemRenderer("cs_kevlar", HeldPresentation.NONE));
      fallbackModels.add(TacticalArsenal.id("item/cs_helmet_fallback"));
      fallbackModels.add(TacticalArsenal.id("item/cs_kevlar_fallback"));
      ModelLoadingPlugin.register(context -> context.addModels(fallbackModels));
   }

   public static void pushOwner(LivingEntity owner) {
      OWNER.set(owner);
   }

   public static void popOwner() {
      OWNER.remove();
   }

   static ItemTransform combatantFirearmTransform(WeaponCategory category, boolean leftHand) {
      if (category == WeaponCategory.PISTOL) {
         return leftHand ? COMBATANT_PISTOL_LEFT_TRANSFORM : COMBATANT_PISTOL_RIGHT_TRANSFORM;
      } else {
         return leftHand ? COMBATANT_LONG_GUN_LEFT_TRANSFORM : COMBATANT_LONG_GUN_RIGHT_TRANSFORM;
      }
   }

   static ItemTransform playerFirearmTransform(WeaponCategory category, boolean leftHand) {
      if (category == WeaponCategory.PISTOL) {
         return leftHand ? PLAYER_PISTOL_LEFT_TRANSFORM : PLAYER_PISTOL_RIGHT_TRANSFORM;
      } else {
         return leftHand ? PLAYER_LONG_GUN_LEFT_TRANSFORM : PLAYER_LONG_GUN_RIGHT_TRANSFORM;
      }
   }

   static ItemTransform combatantDualPistolTransform(boolean leftHand) {
      return leftHand ? COMBATANT_DUAL_PISTOL_LEFT_TRANSFORM : COMBATANT_DUAL_PISTOL_RIGHT_TRANSFORM;
   }

   private static void captureHeldMuzzles(String weaponId, int entityId, HumanoidArm mainArm, PoseStack poses) {
      Vector3fc negativeAnchor;
      Vector3fc positiveAnchor;
      if (weaponId.equals("elite")) {
         negativeAnchor = Cs2MeshManager.muzzleAnchor(weaponId, false);
         positiveAnchor = Cs2MeshManager.muzzleAnchor(weaponId, true);
      } else {
         negativeAnchor = Cs2MeshManager.muzzleAnchor(weaponId);
         positiveAnchor = negativeAnchor;
      }

      if (negativeAnchor != null && positiveAnchor != null) {
         Vector3f negative = poses.last().pose().transformPosition(new Vector3f(negativeAnchor));
         Vector3f positive = poses.last().pose().transformPosition(new Vector3f(positiveAnchor));
         if (Minecraft.getInstance().getEntityRenderDispatcher().camera != null) {
            //#if MC >= 1.21.11
            Vec3 camera = Minecraft.getInstance().getEntityRenderDispatcher().camera.position();
            //#else
            Vec3 camera = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            //#endif
            MuzzlePositionTracker.capture(
               entityId, weaponId, mainArm, camera.add(negative.x, negative.y, negative.z), camera.add(positive.x, positive.y, positive.z)
            );
         }
      }
   }

   private static Matrix4fc knifeThirdPersonLength(String weaponId, Matrix4fc original) {
      Vector3fc grip;
      float scale;
      switch (weaponId) {
         case "knife":
            grip = STANDARD_KNIFE_GRIP;
            scale = 1.404F;
            break;
         case "karambit":
            grip = SPECIAL_KNIFE_GRIP;
            scale = 2.171F;
            break;
         case "butterfly":
            grip = SPECIAL_KNIFE_GRIP;
            scale = 1.606F;
            break;
         default:
            return original;
      }

      return new Matrix4f(original).translate(grip.x(), grip.y(), grip.z()).scale(scale).translate(-grip.x(), -grip.y(), -grip.z());
   }

   private static enum HeldPresentation {
      NONE,
      FIREARM,
      KNIFE;
   }

   private static final class MeshItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
      private final String weaponId;
      private final HeldPresentation heldPresentation;

      private MeshItemRenderer(String weaponId, HeldPresentation heldPresentation) {
         this.weaponId = weaponId;
         this.heldPresentation = heldPresentation;
      }

      @Override
      public void render(ItemStack stack, ItemDisplayContext displayContext, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
         boolean gui = displayContext == ItemDisplayContext.GUI;
         boolean ground = displayContext == ItemDisplayContext.GROUND;
         boolean thirdPerson = this.heldPresentation != HeldPresentation.NONE
            && (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         boolean firstPersonKnife = this.heldPresentation == HeldPresentation.KNIFE
            && (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
         boolean held = thirdPerson || firstPersonKnife;
         boolean leftHand = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
         LivingEntity livingOwner = OWNER.get();
         // The vanilla item renderer already applied the (identity) display transform and a
         // (-0.5, -0.5, -0.5) offset; undo the offset so the 26.x layer transforms apply exactly.
         poses.pushPose();
         poses.translate(0.5F, 0.5F, 0.5F);
         if ((gui || ground || held) && Cs2MeshManager.hasMesh(this.weaponId)) {
            Matrix4fc localTransform;
            if (held) {
               ItemTransform heldTransform;
               if (firstPersonKnife) {
                  heldTransform = Cs2InventoryItemModels.FIRST_PERSON_KNIFE_TRANSFORM;
               } else if (this.heldPresentation == HeldPresentation.KNIFE) {
                  heldTransform = Cs2InventoryItemModels.THIRD_PERSON_KNIFE_TRANSFORM;
               } else if (livingOwner instanceof CombatantEntity) {
                  heldTransform = Cs2InventoryItemModels.combatantFirearmTransform(GunDefinitions.byId(this.weaponId).category(), leftHand);
               } else if (livingOwner instanceof AbstractClientPlayer) {
                  heldTransform = Cs2InventoryItemModels.playerFirearmTransform(GunDefinitions.byId(this.weaponId).category(), leftHand);
               } else {
                  heldTransform = Cs2InventoryItemModels.THIRD_PERSON_ITEM_TRANSFORM;
               }

               heldTransform.apply(leftHand, poses.last());
               localTransform = Cs2MeshManager.thirdPersonTransform(this.weaponId, leftHand);
               if (thirdPerson) {
                  localTransform = knifeThirdPersonLength(this.weaponId, localTransform);
               }
            } else {
               ItemTransform.NO_TRANSFORM.apply(leftHand, poses.last());
               localTransform = ground ? Cs2MeshManager.groundTransform(this.weaponId) : Cs2MeshManager.inventoryTransform(this.weaponId);
            }

            dev.codex.tacticalarsenal.client.compat.PoseCompat.mulPose(poses, localTransform);
            int meshLight = held ? light : LightTexture.FULL_BRIGHT;
            Cs2MeshManager.submit(this.weaponId, poses, new SubmitNodeCollector(buffers), meshLight);
            int entityId = thirdPerson && livingOwner != null ? livingOwner.getId() : -1;
            if (entityId >= 0 && GunDefinitions.byId(this.weaponId) != null) {
               Cs2InventoryItemModels.captureHeldMuzzles(this.weaponId, entityId, livingOwner.getMainArm(), poses);
            }
         } else {
            this.renderFallback(stack, displayContext, leftHand, poses, buffers, light, overlay);
         }

         poses.popPose();
      }

      private void renderFallback(
         ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poses, MultiBufferSource buffers, int light, int overlay
      ) {
         List<String> parts = this.heldPresentation == HeldPresentation.FIREARM
            ? List.of(this.weaponId + "_body", this.weaponId + "_magazine", this.weaponId + "_action")
            : List.of(this.weaponId + "_fallback");
         Minecraft minecraft = Minecraft.getInstance();
         for (String part : parts) {
            BakedModel model = minecraft.getModelManager().getModel(TacticalArsenal.id("item/" + part));
            if (model != null) {
               minecraft.getItemRenderer().render(stack, displayContext, leftHand, poses, buffers, light, overlay, model);
            }
         }
      }
   }
}
