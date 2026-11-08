package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Copy of the Minecraft 26.x ItemTransform record (same maths) for Minecraft 1.21.1 / 1.20.1. */
public record ItemTransform(Vector3fc rotation, Vector3fc translation, Vector3fc scale) {
   public static final ItemTransform NO_TRANSFORM = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

   public void apply(boolean applyLeftHandFix, PoseStack.Pose pose) {
      if (this == NO_TRANSFORM) {
         pose.pose().translate(-0.5F, -0.5F, -0.5F);
      } else {
         float translationX;
         float rotY;
         float rotZ;
         if (applyLeftHandFix) {
            translationX = -this.translation.x();
            rotY = -this.rotation.y();
            rotZ = -this.rotation.z();
         } else {
            translationX = this.translation.x();
            rotY = this.rotation.y();
            rotZ = this.rotation.z();
         }

         pose.pose().translate(translationX, this.translation.y(), this.translation.z());
         Quaternionf rotation = new Quaternionf()
            .rotationXYZ(this.rotation.x() * (float) (Math.PI / 180.0), rotY * (float) (Math.PI / 180.0), rotZ * (float) (Math.PI / 180.0));
         pose.pose().rotate(rotation);
         pose.normal().rotate(rotation);
         PoseCompat.scale(pose, this.scale.x(), this.scale.y(), this.scale.z());
         pose.pose().translate(-0.5F, -0.5F, -0.5F);
      }
   }
}
