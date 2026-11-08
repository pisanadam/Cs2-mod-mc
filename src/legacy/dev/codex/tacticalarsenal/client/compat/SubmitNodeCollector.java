package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Minecraft 1.21.1 / 1.20.1 stand-in for the 26.x SubmitNodeCollector. Geometry is written straight
 * into the frame's buffer source instead of being queued, which produces the same image.
 */
public final class SubmitNodeCollector {
   private final MultiBufferSource buffers;

   public SubmitNodeCollector(MultiBufferSource buffers) {
      this.buffers = buffers;
   }

   public MultiBufferSource buffers() {
      return this.buffers;
   }

   public void submitCustomGeometry(PoseStack poses, RenderType renderType, CustomGeometryRenderer renderer) {
      com.mojang.blaze3d.vertex.VertexConsumer consumer = this.buffers.getBuffer(renderType);
      //#if MC >= 1.20.5
      renderer.render(poses.last(), consumer);
      //#else
      VertexWriter writer = new VertexWriter(consumer);
      renderer.render(poses.last(), writer);
      writer.flush();
      //#endif
   }

   @FunctionalInterface
   public interface CustomGeometryRenderer {
      //#if MC >= 1.20.5
      void render(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer);
      //#else
      void render(PoseStack.Pose pose, VertexWriter buffer);
      //#endif
   }
}
