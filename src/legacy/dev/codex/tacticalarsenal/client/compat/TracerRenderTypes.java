package dev.codex.tacticalarsenal.client.compat;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderType;

/**
 * Minecraft 26.x draws tracers with a per-vertex line width in screen pixels. Older versions take
 * the width per render type (same shader maths), so one translucent line type is kept per width.
 */
public final class TracerRenderTypes extends RenderType {
   private static final Map<Float, RenderType> BY_WIDTH = new HashMap<>();

   private TracerRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
   }

   public static RenderType linesTranslucent(float width) {
      return BY_WIDTH.computeIfAbsent(width, w -> create(
         "tactical_arsenal_lines_translucent_" + w,
         DefaultVertexFormat.POSITION_COLOR_NORMAL,
         VertexFormat.Mode.LINES,
         1536,
         false,
         false,
         CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(OptionalDouble.of(w)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
      ));
   }
}
