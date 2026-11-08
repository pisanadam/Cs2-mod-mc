package dev.codex.tacticalarsenal.client.compat;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Stand-in for the Minecraft 26.x item layer "local transform" on versions whose item layers only
 * accept a display transform. The stored matrix is applied by the mesh renderer itself.
 */
public final class LocalTransformHolder {
   private final Matrix4f transform = new Matrix4f();

   public void setLocalTransform(Matrix4fc transform) {
      this.transform.set(transform);
   }

   public Matrix4fc get() {
      return new Matrix4f(this.transform);
   }
}
