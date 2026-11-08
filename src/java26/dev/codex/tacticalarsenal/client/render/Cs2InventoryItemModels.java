package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.gun.GunDefinitions;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBakeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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

   private Cs2InventoryItemModels() {
   }

   public static void initialize() {
      ModelLoadingPlugin.register(
         context -> context.modifyItemModelAfterBake()
            .register(
               (AfterBakeItem)(model, bakeContext) -> {
                  if (!bakeContext.itemId().getNamespace().equals("tactical_arsenal")) {
                     return model;
                  } else {
                     String itemId = bakeContext.itemId().getPath();
                     if (GunDefinitions.byId(itemId) != null) {
                        return new Cs2InventoryItemModels.GuiMeshItemModel(model, itemId, Cs2InventoryItemModels.HeldPresentation.FIREARM);
                     } else if (KnifeItem.isKnifeId(itemId)) {
                        return new Cs2InventoryItemModels.GuiMeshItemModel(model, itemId, Cs2InventoryItemModels.HeldPresentation.KNIFE);
                     } else {
                        return (ItemModel)(!itemId.equals("cs_helmet") && !itemId.equals("cs_kevlar")
                           ? model
                           : new Cs2InventoryItemModels.GuiMeshItemModel(model, itemId, Cs2InventoryItemModels.HeldPresentation.NONE));
                     }
                  }
               }
            )
      );
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

   private static void captureHeldMuzzles(Cs2InventoryItemModels.MeshRenderArgument argument, PoseStack poses) {
      Vector3fc negativeAnchor;
      Vector3fc positiveAnchor;
      if (argument.weaponId.equals("elite")) {
         negativeAnchor = Cs2MeshManager.muzzleAnchor(argument.weaponId, false);
         positiveAnchor = Cs2MeshManager.muzzleAnchor(argument.weaponId, true);
      } else {
         negativeAnchor = Cs2MeshManager.muzzleAnchor(argument.weaponId);
         positiveAnchor = negativeAnchor;
      }

      if (negativeAnchor != null && positiveAnchor != null) {
         Vector3f negative = poses.last().pose().transformPosition(new Vector3f(negativeAnchor));
         Vector3f positive = poses.last().pose().transformPosition(new Vector3f(positiveAnchor));
         if (Minecraft.getInstance().getEntityRenderDispatcher().camera != null) {
            Vec3 camera = Minecraft.getInstance().getEntityRenderDispatcher().camera.position();
            MuzzlePositionTracker.capture(
               argument.entityId,
               argument.weaponId,
               argument.mainArm,
               camera.add(negative.x, negative.y, negative.z),
               camera.add(positive.x, positive.y, positive.z)
            );
         }
      }
   }

   private static final class GuiMeshItemModel implements ItemModel {
      private final ItemModel fallback;
      private final String weaponId;
      private final Cs2InventoryItemModels.HeldPresentation heldPresentation;
      private final Cs2InventoryItemModels.MeshSpecialRenderer fullBrightRenderer;
      private final Cs2InventoryItemModels.MeshSpecialRenderer worldLitRenderer;

      private GuiMeshItemModel(ItemModel fallback, String weaponId, Cs2InventoryItemModels.HeldPresentation heldPresentation) {
         this.fallback = fallback;
         this.weaponId = weaponId;
         this.heldPresentation = heldPresentation;
         this.fullBrightRenderer = new Cs2InventoryItemModels.MeshSpecialRenderer(weaponId, true);
         this.worldLitRenderer = new Cs2InventoryItemModels.MeshSpecialRenderer(weaponId, false);
      }

      public void update(
         ItemStackRenderState state,
         ItemStack stack,
         ItemModelResolver resolver,
         ItemDisplayContext displayContext,
         ClientLevel level,
         ItemOwner owner,
         int seed
      ) {
         boolean gui = displayContext == ItemDisplayContext.GUI;
         boolean ground = displayContext == ItemDisplayContext.GROUND;
         boolean thirdPerson = this.heldPresentation != Cs2InventoryItemModels.HeldPresentation.NONE
            && (displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
         boolean firstPersonKnife = this.heldPresentation == Cs2InventoryItemModels.HeldPresentation.KNIFE
            && (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
         boolean held = thirdPerson || firstPersonKnife;
         LivingEntity livingOwner = owner == null ? null : owner.asLivingEntity();
         if ((gui || ground || held) && Cs2MeshManager.hasMesh(this.weaponId)) {
            state.appendModelIdentityElement(this);
            state.appendModelIdentityElement(Cs2MeshManager.renderIdentity(this.weaponId));
            LayerRenderState layer = state.newLayer();
            layer.setUsesBlockLight(held);
            //#if MC < 26.1
            dev.codex.tacticalarsenal.client.compat.LocalTransformHolder localTransform = new dev.codex.tacticalarsenal.client.compat.LocalTransformHolder();
            //#endif
            if (held) {
               boolean leftHand = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
               ItemTransform heldTransform;
               if (firstPersonKnife) {
                  heldTransform = Cs2InventoryItemModels.FIRST_PERSON_KNIFE_TRANSFORM;
               } else if (this.heldPresentation == Cs2InventoryItemModels.HeldPresentation.KNIFE) {
                  heldTransform = Cs2InventoryItemModels.THIRD_PERSON_KNIFE_TRANSFORM;
               } else if (livingOwner instanceof CombatantEntity) {
                  heldTransform = Cs2InventoryItemModels.combatantFirearmTransform(GunDefinitions.byId(this.weaponId).category(), leftHand);
               } else if (livingOwner instanceof Avatar) {
                  heldTransform = Cs2InventoryItemModels.playerFirearmTransform(GunDefinitions.byId(this.weaponId).category(), leftHand);
               } else {
                  heldTransform = Cs2InventoryItemModels.THIRD_PERSON_ITEM_TRANSFORM;
               }

               //#if MC >= 26.1
               layer.setItemTransform(heldTransform);
               layer.setLocalTransform(Cs2MeshManager.thirdPersonTransform(this.weaponId, leftHand));
               //#else
               layer.setTransform(heldTransform);
               localTransform.setLocalTransform(Cs2MeshManager.thirdPersonTransform(this.weaponId, leftHand));
               //#endif
            } else {
               //#if MC >= 26.1
               layer.setLocalTransform(ground ? Cs2MeshManager.groundTransform(this.weaponId) : Cs2MeshManager.inventoryTransform(this.weaponId));
               //#else
               localTransform.setLocalTransform(ground ? Cs2MeshManager.groundTransform(this.weaponId) : Cs2MeshManager.inventoryTransform(this.weaponId));
               //#endif
            }

            layer.setExtents(() -> Cs2MeshManager.inventoryExtents(this.weaponId));
            layer.setupSpecialModel(
               held ? this.worldLitRenderer : this.fullBrightRenderer,
               new Cs2InventoryItemModels.MeshRenderArgument(
                  this.weaponId,
                  thirdPerson && livingOwner != null ? livingOwner.getId() : -1,
                  livingOwner == null ? HumanoidArm.RIGHT : livingOwner.getMainArm()
                  //#if MC < 26.1
                  , localTransform.get()
                  //#endif
               )
            );
            if (stack.hasFoil()) {
               layer.setFoilType(FoilType.STANDARD);
               state.setAnimated();
               state.appendModelIdentityElement(FoilType.STANDARD);
            }
         } else {
            this.fallback.update(state, stack, resolver, displayContext, level, owner, seed);
         }
      }
   }

   private static enum HeldPresentation {
      NONE,
      FIREARM,
      KNIFE;
   }

   private record MeshRenderArgument(
      String weaponId,
      int entityId,
      HumanoidArm mainArm
      //#if MC < 26.1
      , Matrix4fc localTransform
      //#endif
   ) {
   }

   private static final class MeshSpecialRenderer implements SpecialModelRenderer<Cs2InventoryItemModels.MeshRenderArgument> {
      private final String weaponId;
      private final boolean fullBright;

      private MeshSpecialRenderer(String weaponId, boolean fullBright) {
         this.weaponId = weaponId;
         this.fullBright = fullBright;
      }

      public void submit(
         Cs2InventoryItemModels.MeshRenderArgument argument,
         //#if MC < 26.1
         ItemDisplayContext displayContext,
         //#endif
         PoseStack poses,
         SubmitNodeCollector collector,
         int light,
         int overlay,
         boolean foil,
         int outlineColor
      ) {
         int meshLight = this.fullBright ? Brightness.FULL_BRIGHT.pack() : light;
         //#if MC < 26.1
         if (argument == null) {
            return;
         }
         poses.pushPose();
         poses.mulPose(new org.joml.Matrix4f(argument.localTransform));
         //#endif
         Cs2MeshManager.submit(this.weaponId, poses, collector, meshLight);
         if (argument.entityId >= 0 && GunDefinitions.byId(argument.weaponId) != null) {
            Cs2InventoryItemModels.captureHeldMuzzles(argument, poses);
         }
         //#if MC < 26.1
         poses.popPose();
         //#endif
      }

      public void getExtents(Consumer<Vector3fc> visitor) {
         Cs2MeshManager.visitExtents(this.weaponId, visitor);
      }

      public Cs2InventoryItemModels.MeshRenderArgument extractArgument(ItemStack stack) {
         return new Cs2InventoryItemModels.MeshRenderArgument(
            this.weaponId,
            -1,
            HumanoidArm.RIGHT
            //#if MC < 26.1
            , Cs2MeshManager.inventoryTransform(this.weaponId)
            //#endif
         );
      }
   }
}
