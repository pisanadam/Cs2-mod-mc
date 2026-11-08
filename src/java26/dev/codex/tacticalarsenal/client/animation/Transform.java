package dev.codex.tacticalarsenal.client.animation;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class Transform {
   public static final Transform IDENTITY = new Transform(new Vector3f(), new Quaternionf(), new Vector3f(1.0F));
   private final Vector3f translation;
   private final Quaternionf rotation;
   private final Vector3f scale;

   public Transform(Vector3fc translation, Quaternionfc rotation, Vector3fc scale) {
      this.translation = new Vector3f(translation);
      this.rotation = new Quaternionf(rotation).normalize();
      this.scale = new Vector3f(scale);
   }

   public static Transform of(float x, float y, float z, float pitch, float yaw, float roll) {
      float radians = (float) (Math.PI / 180.0);
      return new Transform(new Vector3f(x, y, z), new Quaternionf().rotationXYZ(pitch * radians, yaw * radians, roll * radians), new Vector3f(1.0F));
   }

   public Vector3fc translation() {
      return this.translation;
   }

   public Quaternionfc rotation() {
      return this.rotation;
   }

   public Vector3fc scale() {
      return this.scale;
   }

   public Transform then(Transform next) {
      return new Transform(
         new Vector3f(this.translation).add(next.translation), new Quaternionf(this.rotation).mul(next.rotation), new Vector3f(this.scale).mul(next.scale)
      );
   }

   public static Transform blend(Transform from, Transform to, float alpha) {
      float value = Math.max(0.0F, Math.min(1.0F, alpha));
      return new Transform(
         new Vector3f(from.translation).lerp(to.translation, value),
         new Quaternionf(from.rotation).slerp(to.rotation, value),
         new Vector3f(from.scale).lerp(to.scale, value)
      );
   }
}
