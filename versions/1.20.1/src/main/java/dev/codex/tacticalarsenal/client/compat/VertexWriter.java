package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Minecraft 1.20.1 has the old fixed-order vertex API. This accepts the 26.x style calls used by the
 * mod (addVertex/setColor/setUv/setOverlay/setLight/setNormal in any order) and emits each vertex in
 * the order the old buffers expect. Normals are normalised, like 26.x does for scaled poses.
 */
public final class VertexWriter {
   private final com.mojang.blaze3d.vertex.VertexConsumer consumer;
   private boolean pending;
   private float x;
   private float y;
   private float z;
   private int color = -1;
   private boolean hasUv;
   private float u;
   private float v;
   private boolean hasOverlay;
   private int overlay;
   private boolean hasLight;
   private int light;
   private boolean hasNormal;
   private float nx;
   private float ny;
   private float nz;

   public VertexWriter(com.mojang.blaze3d.vertex.VertexConsumer consumer) {
      this.consumer = consumer;
   }

   public VertexWriter addVertex(PoseStack.Pose pose, float x, float y, float z) {
      this.flush();
      Vector4f position = pose.pose().transform(new Vector4f(x, y, z, 1.0F));
      this.x = position.x();
      this.y = position.y();
      this.z = position.z();
      this.pending = true;
      return this;
   }

   public VertexWriter setColor(int argb) {
      this.color = argb;
      return this;
   }

   public VertexWriter setUv(float u, float v) {
      this.u = u;
      this.v = v;
      this.hasUv = true;
      return this;
   }

   public VertexWriter setOverlay(int overlay) {
      this.overlay = overlay;
      this.hasOverlay = true;
      return this;
   }

   public VertexWriter setLight(int light) {
      this.light = light;
      this.hasLight = true;
      return this;
   }

   public VertexWriter setNormal(PoseStack.Pose pose, float x, float y, float z) {
      Vector3f normal = pose.normal().transform(new Vector3f(x, y, z));
      if (normal.lengthSquared() > 0.0F) {
         normal.normalize();
      }

      this.nx = normal.x();
      this.ny = normal.y();
      this.nz = normal.z();
      this.hasNormal = true;
      return this;
   }

   public void flush() {
      if (!this.pending) {
         return;
      }

      com.mojang.blaze3d.vertex.VertexConsumer vertex = this.consumer.vertex(this.x, this.y, this.z)
         .color(this.color >> 16 & 0xFF, this.color >> 8 & 0xFF, this.color & 0xFF, this.color >>> 24);
      if (this.hasUv) {
         vertex = vertex.uv(this.u, this.v);
      }

      if (this.hasOverlay) {
         vertex = vertex.overlayCoords(this.overlay);
      }

      if (this.hasLight) {
         vertex = vertex.uv2(this.light);
      }

      if (this.hasNormal) {
         vertex = vertex.normal(this.nx, this.ny, this.nz);
      }

      vertex.endVertex();
      this.pending = false;
      this.color = -1;
      this.hasUv = false;
      this.hasOverlay = false;
      this.hasLight = false;
      this.hasNormal = false;
   }
}
