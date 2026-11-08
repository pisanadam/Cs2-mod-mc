package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.client.animation.Bone;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.client.animation.Transform;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class GunViewmodelRenderer {
   private static final float IMPORTED_ACTION_TRANSLATION_SCALE = 0.1F;
   private static final float PRIMARY_GRIP_X = 0.48F;
   private static final float PRIMARY_GRIP_Y = -0.365F;
   private static final float PRIMARY_GRIP_Z = -0.642F;
   private static final float DUAL_GRIP_X = 0.3F;
   private static final float DUAL_GRIP_Y = -0.34F;
   private static final float DUAL_GRIP_Z = -0.74F;
   private static final float IMPORTED_WEAPON_SCALE = 0.95F;
   private static final float IMPORTED_WEAPON_PITCH = -14.0F;
   private static final float IMPORTED_WEAPON_YAW = 35.0F;
   private static final Map<GunItem, GunViewmodelRenderer.PartStacks> PARTS = new HashMap<>();

   private GunViewmodelRenderer() {
   }

   public static void render(
      ItemInHandRenderer itemRenderer,
      AbstractClientPlayer player,
      ItemStack original,
      float partialTick,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light
   ) {
      if (original.getItem() instanceof GunItem gun) {
         if (!gun.definition().hideViewmodelWhenZoomed() || !GunClientController.isScopeOverlayActive(partialTick)) {
            GunViewmodelRenderer.PartStacks parts = PARTS.computeIfAbsent(gun, GunViewmodelRenderer::createParts);
            Transform rootTransform = GunAnimationEngine.INSTANCE.rootTransform(partialTick);
            poses.pushPose();
            apply(poses, rootTransform);
            GunHandsRenderer.render(player, gun.definition(), partialTick, poses, collector, light);
            poses.popPose();
            if (!gun.definition().id().equals("elite")
               || !renderDualBerettas(player, gun, parts, rootTransform, partialTick, poses, collector, itemRenderer, light)) {
               String weaponId = gun.definition().id();
               poses.pushPose();
               apply(poses, rootTransform);
               if (Cs2MeshManager.hasMesh(weaponId)) {
                  applyImportedWeaponPose(poses, weaponId, player.getMainArm());
                  Cs2MeshManager.submitPart(weaponId, Cs2MeshManager.MeshPart.BODY, poses, collector, light);
                  poses.pushPose();
                  if (!weaponId.equals("revolver")) {
                     apply(poses, GunAnimationEngine.INSTANCE.boneTransform(Bone.MAGAZINE, partialTick));
                  }

                  Cs2MeshManager.submitPart(weaponId, Cs2MeshManager.MeshPart.MAGAZINE, poses, collector, light);
                  poses.popPose();
                  poses.pushPose();
                  applyImportedAction(poses, GunAnimationEngine.INSTANCE.boneTransform(Bone.ACTION, partialTick));
                  Cs2MeshManager.submitPart(weaponId, Cs2MeshManager.MeshPart.ACTION, poses, collector, light);
                  poses.popPose();
                  poses.popPose();
               } else {
                  poses.translate(0.37F, -0.34F, -0.58F);
                  itemRenderer.renderItem(player, parts.body, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses, collector, light);
                  poses.pushPose();
                  apply(poses, GunAnimationEngine.INSTANCE.boneTransform(Bone.MAGAZINE, partialTick));
                  itemRenderer.renderItem(player, parts.magazine, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses, collector, light);
                  poses.popPose();
                  poses.pushPose();
                  apply(poses, GunAnimationEngine.INSTANCE.boneTransform(Bone.ACTION, partialTick));
                  itemRenderer.renderItem(player, parts.action, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses, collector, light);
                  poses.popPose();
                  poses.popPose();
               }
            }
         }
      }
   }

   public static void renderOffhand(
      ItemInHandRenderer itemRenderer, AbstractClientPlayer player, ItemStack original, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      if (original.getItem() instanceof GunItem gun) {
         String var9 = gun.definition().id();
         HumanoidArm arm = player.getMainArm().getOpposite();
         if (Cs2MeshManager.hasMesh(var9)) {
            poses.pushPose();
            GunHandsRenderer.renderIdleGripHand(player, arm, poses, collector, light);
            applyImportedWeaponPose(poses, var9, arm);
            Cs2MeshManager.submit(var9, poses, collector, light);
            poses.popPose();
         } else {
            itemRenderer.renderItem(
               player,
               original,
               arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               poses,
               collector,
               light
            );
         }
      }
   }

   private static boolean renderDualBerettas(
      AbstractClientPlayer player,
      GunItem gun,
      GunViewmodelRenderer.PartStacks parts,
      Transform rootTransform,
      float partialTick,
      PoseStack poses,
      SubmitNodeCollector collector,
      ItemInHandRenderer itemRenderer,
      int light
   ) {
      String weaponId = gun.definition().id();
      if (Cs2MeshManager.hasMesh(weaponId)) {
         HumanoidArm firstArm = player.getMainArm();
         renderImportedBeretta(player, weaponId, firstArm, rootTransform, partialTick, poses, collector, light);
         renderImportedBeretta(player, weaponId, firstArm.getOpposite(), rootTransform, partialTick, poses, collector, light);
         return true;
      } else {
         renderFallbackBerettaPair(player, parts, rootTransform, partialTick, poses, collector, itemRenderer, light);
         return true;
      }
   }

   private static void renderImportedBeretta(
      AbstractClientPlayer player,
      String weaponId,
      HumanoidArm arm,
      Transform rootTransform,
      float partialTick,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light
   ) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      boolean positiveX = arm == HumanoidArm.LEFT;
      poses.pushPose();
      apply(poses, rootTransform);
      apply(poses, GunAnimationEngine.INSTANCE.dualWieldWeaponTransform(arm, partialTick));
      apply(poses, GunAnimationEngine.INSTANCE.dualWieldFireTransform(arm, partialTick));
      GunHandsRenderer.renderDualHand(player, arm, partialTick, poses, collector, light);
      poses.pushPose();
      applyImportedBerettaPose(poses, weaponId, arm, positiveX);
      Cs2MeshManager.submitPartHalf(weaponId, Cs2MeshManager.MeshPart.BODY, positiveX, poses, collector, light);
      poses.pushPose();
      apply(poses, GunAnimationEngine.INSTANCE.dualWieldBoneTransform(Bone.MAGAZINE, arm, partialTick));
      Cs2MeshManager.submitPartHalf(weaponId, Cs2MeshManager.MeshPart.MAGAZINE, positiveX, poses, collector, light);
      poses.popPose();
      poses.pushPose();
      applyImportedAction(poses, GunAnimationEngine.INSTANCE.dualWieldBoneTransform(Bone.ACTION, arm, partialTick));
      Cs2MeshManager.submitPartHalf(weaponId, Cs2MeshManager.MeshPart.ACTION, positiveX, poses, collector, light);
      poses.popPose();
      poses.popPose();
      poses.popPose();
   }

   static Vector3f firstPersonMuzzle(LocalPlayer player, GunItem gun, int patternIndex, float partialTick) {
      String weaponId = gun.definition().id();
      if (!Cs2MeshManager.hasMesh(weaponId)) {
         return null;
      } else {
         PoseStack poses = new PoseStack();
         applyTurnBob(poses, player, partialTick);
         Transform rootTransform = GunAnimationEngine.INSTANCE.rootTransform(partialTick);
         Vector3fc anchor;
         if (weaponId.equals("elite")) {
            HumanoidArm arm = (patternIndex & 1) == 0 ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean positiveX = arm == HumanoidArm.LEFT;
            apply(poses, rootTransform);
            apply(poses, GunAnimationEngine.INSTANCE.dualWieldWeaponTransform(arm, partialTick));
            apply(poses, GunAnimationEngine.INSTANCE.dualWieldFireTransform(arm, partialTick));
            applyImportedBerettaPose(poses, weaponId, arm, positiveX);
            anchor = Cs2MeshManager.muzzleAnchor(weaponId, positiveX);
         } else {
            apply(poses, rootTransform);
            applyImportedWeaponPose(poses, weaponId, player.getMainArm());
            anchor = Cs2MeshManager.muzzleAnchor(weaponId);
         }

         return anchor == null ? null : poses.last().pose().transformPosition(new Vector3f(anchor));
      }
   }

   private static void applyTurnBob(PoseStack poses, LocalPlayer player, float partialTick) {
      float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
      float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
      float pitchDegrees = (player.getViewXRot(partialTick) - xBob) * 0.1F;
      float yawDegrees = (player.getViewYRot(partialTick) - yBob) * 0.1F;
      poses.mulPose(new Quaternionf().rotationX((float)Math.toRadians(pitchDegrees)));
      poses.mulPose(new Quaternionf().rotationY((float)Math.toRadians(yawDegrees)));
   }

   static Vector3f primaryGripTarget(HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      return new Vector3f(side * 0.48F, -0.365F, -0.642F);
   }

   static Vector3f dualGripTarget(HumanoidArm arm) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      return new Vector3f(side * 0.3F, -0.34F, -0.74F);
   }

   static Vector3f supportGripTarget(GunDefinition gun, HumanoidArm mainArm) {
      Vector3fc grip = Cs2MeshManager.gripAnchor(gun.id());
      Vector3fc muzzle = Cs2MeshManager.muzzleAnchor(gun.id());
      if (grip != null && muzzle != null) {
         float distance = switch (gun.category()) {
            case PISTOL -> 0.045F;
            case SMG -> 0.15F;
            case RIFLE -> 0.23F;
            case SNIPER_RIFLE -> 0.27F;
            case SHOTGUN -> 0.28F;
            case MACHINE_GUN -> 0.25F;
         };
         float forwardSpan = Math.max(0.001F, muzzle.z() - grip.z());
         float fraction = Mth.clamp(distance / forwardSpan, 0.08F, 0.55F);
         Vector3f rawOffset = new Vector3f(muzzle).sub(grip).mul(fraction);
         rawOffset.y = rawOffset.y - (gun.category() == WeaponCategory.PISTOL ? 0.015F : 0.035F);
         rawOffset.mul(0.95F);
         rawOffset.rotateX((float)Math.toRadians(-14.0));
         float side = mainArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         rawOffset.rotateY((float)Math.toRadians(180.0F + side * 35.0F));
         return rawOffset.add(primaryGripTarget(mainArm));
      } else {
         Vector3f target = primaryGripTarget(mainArm);
         target.x *= 0.72F;
         target.y += 0.055F;
         target.z -= 0.15F;
         return target;
      }
   }

   private static void applyImportedWeaponPose(PoseStack poses, String weaponId, HumanoidArm arm) {
      Vector3fc grip = Cs2MeshManager.gripAnchor(weaponId);
      if (grip == null) {
         float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         poses.translate(side * 0.41F, -0.3F, -0.74F);
         poses.mulPose(new Quaternionf().rotationY((float)Math.toRadians(180.0F + side * 35.0F)));
         poses.mulPose(new Quaternionf().rotationX((float)Math.toRadians(-14.0)));
         poses.scale(0.95F, 0.95F, 0.95F);
         anchorImportedMesh(poses, weaponId);
      } else {
         Vector3f target = primaryGripTarget(arm);
         float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         poses.translate(target.x, target.y, target.z);
         poses.mulPose(new Quaternionf().rotationY((float)Math.toRadians(180.0F + side * 35.0F)));
         poses.mulPose(new Quaternionf().rotationX((float)Math.toRadians(-14.0)));
         poses.scale(0.95F, 0.95F, 0.95F);
         poses.translate(-grip.x(), -grip.y(), -grip.z());
      }
   }

   private static void applyImportedBerettaPose(PoseStack poses, String weaponId, HumanoidArm arm, boolean positiveX) {
      float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      Vector3fc grip = Cs2MeshManager.gripAnchor(weaponId, positiveX);
      if (grip == null) {
         anchorImportedMesh(poses, Cs2MeshManager.halfExtents(weaponId, positiveX));
      } else {
         Vector3f target = dualGripTarget(arm);
         poses.translate(target.x, target.y, target.z);
         poses.mulPose(new Quaternionf().rotationY((float)Math.toRadians(180.0F + side * 31.0F)));
         poses.mulPose(new Quaternionf().rotationX((float)Math.toRadians(-14.0)));
         poses.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(side * 3.0F)));
         poses.scale(0.83F, 0.83F, 0.83F);
         poses.translate(-grip.x(), -grip.y(), -grip.z());
      }
   }

   private static void renderFallbackBerettaPair(
      AbstractClientPlayer player,
      GunViewmodelRenderer.PartStacks parts,
      Transform rootTransform,
      float partialTick,
      PoseStack poses,
      SubmitNodeCollector collector,
      ItemInHandRenderer itemRenderer,
      int light
   ) {
      for (HumanoidArm arm : HumanoidArm.values()) {
         poses.pushPose();
         apply(poses, rootTransform);
         apply(poses, GunAnimationEngine.INSTANCE.dualWieldWeaponTransform(arm, partialTick));
         apply(poses, GunAnimationEngine.INSTANCE.dualWieldFireTransform(arm, partialTick));
         GunHandsRenderer.renderDualHand(player, arm, partialTick, poses, collector, light);
         poses.popPose();
      }

      poses.pushPose();
      apply(poses, rootTransform);
      itemRenderer.renderItem(player, parts.body, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses, collector, light);
      poses.pushPose();
      apply(poses, GunAnimationEngine.INSTANCE.boneTransform(Bone.MAGAZINE, partialTick));
      itemRenderer.renderItem(player, parts.magazine, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses, collector, light);
      poses.popPose();
      poses.pushPose();
      apply(poses, GunAnimationEngine.INSTANCE.boneTransform(Bone.ACTION, partialTick));
      itemRenderer.renderItem(player, parts.action, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, poses, collector, light);
      poses.popPose();
      poses.popPose();
   }

   private static void anchorImportedMesh(PoseStack poses, String weaponId) {
      anchorImportedMesh(poses, Cs2MeshManager.inventoryExtents(weaponId));
   }

   private static void anchorImportedMesh(PoseStack poses, Vector3fc[] extents) {
      if (extents.length != 0) {
         float minX = Float.POSITIVE_INFINITY;
         float minY = Float.POSITIVE_INFINITY;
         float minZ = Float.POSITIVE_INFINITY;
         float maxX = Float.NEGATIVE_INFINITY;
         float maxY = Float.NEGATIVE_INFINITY;
         float maxZ = Float.NEGATIVE_INFINITY;

         for (Vector3fc extent : extents) {
            minX = Math.min(minX, extent.x());
            minY = Math.min(minY, extent.y());
            minZ = Math.min(minZ, extent.z());
            maxX = Math.max(maxX, extent.x());
            maxY = Math.max(maxY, extent.y());
            maxZ = Math.max(maxZ, extent.z());
         }

         poses.translate(-(minX + maxX) * 0.5F, -(minY + maxY) * 0.5F, -(minZ + maxZ) * 0.5F);
      }
   }

   private static GunViewmodelRenderer.PartStacks createParts(GunItem gun) {
      String id = gun.definition().id();
      return new GunViewmodelRenderer.PartStacks(part(gun, id + "_body"), part(gun, id + "_magazine"), part(gun, id + "_action"));
   }

   private static ItemStack part(GunItem gun, String model) {
      ItemStack stack = new ItemStack(gun);
      stack.set(DataComponents.ITEM_MODEL, TacticalArsenal.id(model));
      return stack;
   }

   private static void apply(PoseStack poses, Transform transform) {
      poses.translate(transform.translation().x(), transform.translation().y(), transform.translation().z());
      poses.mulPose(new Quaternionf(transform.rotation()));
      poses.scale(transform.scale().x(), transform.scale().y(), transform.scale().z());
   }

   private static void applyImportedAction(PoseStack poses, Transform transform) {
      Vector3fc translation = transform.translation();
      poses.translate(translation.x() * 0.1F, translation.y() * 0.1F, -translation.z() * 0.1F);
      poses.mulPose(new Quaternionf(transform.rotation()));
      poses.scale(transform.scale().x(), transform.scale().y(), transform.scale().z());
   }

   private record PartStacks(ItemStack body, ItemStack magazine, ItemStack action) {
   }
}
