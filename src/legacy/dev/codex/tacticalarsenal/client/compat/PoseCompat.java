package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** PoseStack operations that only exist on PoseStack.Pose in Minecraft 26.x. */
public final class PoseCompat {
   private PoseCompat() {
   }

   /** Same as 26.x {@code poses.last().mulPose(matrix)}; always applied to the top pose. */
   public static void mulPose(PoseStack poses, Matrix4fc matrix) {
      //#if MC >= 1.20.5
      poses.mulPose(new Matrix4f(matrix));
      //#else
      PoseStack.Pose pose = poses.last();
      pose.pose().mul(matrix);
      pose.normal().set(new Matrix4f(pose.pose()).normal(new Matrix4f()).get3x3(new org.joml.Matrix3f()));
      //#endif
   }

   /** Uniform-scale-aware scale of a single pose, as 26.x Pose#scale does. */
   public static void scale(PoseStack.Pose pose, float x, float y, float z) {
      pose.pose().scale(x, y, z);
      if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
         if (x < 0.0F || y < 0.0F || z < 0.0F) {
            pose.normal().scale(Math.signum(x), Math.signum(y), Math.signum(z));
         }
      } else {
         pose.normal().scale(1.0F / x, 1.0F / y, 1.0F / z);
      }
   }
}
