package dev.codex.tacticalarsenal.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Gives the tracer code the same world/hand FOV values that Minecraft 26.x exposes in CameraRenderState. */
@Mixin(GameRenderer.class)
public interface GameRendererFovInvoker {
   @Invoker("getFov")
   //#if MC >= 1.21.2
   float tacticalArsenal$getFov(Camera camera, float partialTick, boolean useFovSetting);
   //#else
   double tacticalArsenal$getFov(Camera camera, float partialTick, boolean useFovSetting);
   //#endif
}
