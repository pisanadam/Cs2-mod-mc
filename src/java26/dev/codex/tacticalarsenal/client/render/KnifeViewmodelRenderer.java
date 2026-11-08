package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.KnifeClientController;
import dev.codex.tacticalarsenal.client.animation.KnifeAnimationLibrary;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class KnifeViewmodelRenderer {
   private static final float VIEWMODEL_MAX_LENGTH = 0.4F;
   private static final float ATTACK_TRANSLATION_FACTOR = 0.12F;
   private static final float MAX_ATTACK_TRANSLATION = 0.08F;
   private static final String[] BUTTERFLY_PART_IDS = new String[]{"butterfly_body", "butterfly_blade", "butterfly_rear", "butterfly_lock"};

   private KnifeViewmodelRenderer() {
   }

   public static void render(
      ItemInHandRenderer itemRenderer,
      AbstractClientPlayer player,
      ItemStack stack,
      InteractionHand hand,
      float equipProgress,
      PoseStack poses,
      SubmitNodeCollector collector,
      int light
   ) {
      if (stack.getItem() instanceof KnifeItem knife) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
         KnifeClientController.Playback playback = hand == InteractionHand.MAIN_HAND ? KnifeClientController.playback(knife) : null;
         KnifeAnimationLibrary.Pose animation = playback == null
            ? KnifeAnimationLibrary.sample("", 0.0F)
            : KnifeAnimationLibrary.sample(playback.clipId(), playback.elapsedSeconds()).blendToIdentity(playback.blendOutProgress());
         animation = viewSafe(animation, side);
         poses.pushPose();
         poses.translate(side * 0.2F, -0.1F - Math.min(1.0F, Math.max(0.0F, equipProgress)) * 0.04F, -1.08F);
         KnifeViewmodelRenderer.KnifeLayout layout = createLayout(knife.id(), side);
         if (layout != null) {
            if (!renderImportedKnife(knife.id(), animation, layout, poses, collector, light)) {
               renderFallback(itemRenderer, player, stack, arm, poses, collector, light);
            }
         } else {
            renderFallback(itemRenderer, player, stack, arm, poses, collector, light);
         }

         poses.popPose();
      }
   }

   private static boolean renderImportedKnife(
      String id, KnifeAnimationLibrary.Pose animation, KnifeViewmodelRenderer.KnifeLayout layout, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      if (id.equals("butterfly") && hasButterflyParts()) {
         renderTrack(BUTTERFLY_PART_IDS[0], animation.body(), layout.model, poses, collector, light);
         renderTrack(BUTTERFLY_PART_IDS[1], animation.blade(), layout.model, poses, collector, light);
         renderTrack(BUTTERFLY_PART_IDS[2], animation.rear(), layout.model, poses, collector, light);
         renderTrack(BUTTERFLY_PART_IDS[3], animation.lock(), layout.model, poses, collector, light);
         return true;
      } else if (!Cs2MeshManager.hasMesh(id)) {
         return false;
      } else {
         renderTrack(id, animation.body(), layout.model, poses, collector, light);
         return true;
      }
   }

   private static void renderTrack(
      String meshId, KnifeAnimationLibrary.Transform animation, Matrix4fc model, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      poses.pushPose();
      poses.mulPose(model);
      apply(poses, animation);
      Cs2MeshManager.submit(meshId, poses, collector, light);
      poses.popPose();
   }

   private static boolean hasButterflyParts() {
      for (String part : BUTTERFLY_PART_IDS) {
         if (!Cs2MeshManager.hasMesh(part)) {
            return false;
         }
      }

      return true;
   }

   private static KnifeViewmodelRenderer.KnifeLayout createLayout(String id, float side) {
      Vector3fc[] extents = Cs2MeshManager.inventoryExtents(id);
      if (extents.length == 0) {
         return null;
      } else {
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

         float largest = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
         float scale = 0.4F / Math.max(1.0E-4F, largest);
         Vector3fc grip = Cs2MeshManager.gripAnchor(id);
         float gripX = grip == null ? (minX + maxX) * 0.5F : grip.x();
         float gripY = grip == null ? (minY + maxY) * 0.5F : grip.y();
         float gripZ = grip == null ? (minZ + maxZ) * 0.5F : grip.z();

         float zDegrees = switch (id) {
            case "karambit" -> -side * 60.0F;
            case "butterfly" -> side * 70.0F;
            default -> side * 24.0F;
         };
         Matrix4f model = new Matrix4f()
            .translate(side * 0.1F, 0.025F, 0.08F)
            .rotateY((float)Math.toRadians(-side * 55.0F))
            .rotateX((float)Math.toRadians(-28.0))
            .rotateZ((float)Math.toRadians(zDegrees))
            .scale(scale)
            .translate(-gripX, -gripY, -gripZ);
         return new KnifeViewmodelRenderer.KnifeLayout(model);
      }
   }

   private static KnifeAnimationLibrary.Pose viewSafe(KnifeAnimationLibrary.Pose source, float side) {
      return new KnifeAnimationLibrary.Pose(
         viewSafe(source.body(), side),
         viewSafe(source.blade(), side),
         viewSafe(source.rear(), side),
         viewSafe(source.lock(), side),
         source.handL(),
         source.handR()
      );
   }

   private static KnifeAnimationLibrary.Transform viewSafe(KnifeAnimationLibrary.Transform source, float side) {
      float x = clamp(source.translation().x() * 0.12F);
      float y = clamp(source.translation().y() * 0.12F);
      float z = clamp(source.translation().z() * 0.12F);
      Quaternionf rotation = new Quaternionf(source.rotation());
      if (side < 0.0F) {
         x = -x;
         rotation.set(rotation.x(), -rotation.y(), -rotation.z(), rotation.w());
      }

      return new KnifeAnimationLibrary.Transform(new Vector3f(x, y, z), rotation.normalize());
   }

   private static float clamp(float value) {
      return Math.max(-0.08F, Math.min(0.08F, value));
   }

   private static void apply(PoseStack poses, KnifeAnimationLibrary.Transform transform) {
      poses.translate(transform.translation().x(), transform.translation().y(), transform.translation().z());
      poses.mulPose(new Quaternionf(transform.rotation()));
   }

   private static void renderFallback(
      ItemInHandRenderer itemRenderer, AbstractClientPlayer player, ItemStack stack, HumanoidArm arm, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      poses.pushPose();
      poses.translate((arm == HumanoidArm.RIGHT ? 1.0F : -1.0F) * 0.1F, 0.025F, 0.08F);
      ItemDisplayContext context = arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
      itemRenderer.renderItem(player, stack, context, poses, collector, light);
      poses.popPose();
   }

   private record KnifeLayout(Matrix4fc model) {
   }
}
