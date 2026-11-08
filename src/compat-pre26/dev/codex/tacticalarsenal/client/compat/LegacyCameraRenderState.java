package dev.codex.tacticalarsenal.client.compat;

import dev.codex.tacticalarsenal.client.mixin.GameRendererFovInvoker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Mirror of the Minecraft 26.x CameraRenderState fields used by the tracer code, filled from the
 * live camera on versions that do not extract a camera render state.
 */
public class LegacyCameraRenderState {
   public Vec3 pos = Vec3.ZERO;
   public float hudFov;
   public Matrix4f projectionMatrix = new Matrix4f();
   public Matrix4f viewRotationMatrix = new Matrix4f();
   public LegacyCameraEntityRenderState entityRenderState = new LegacyCameraEntityRenderState();

   public static LegacyCameraRenderState capture(Camera camera, float partialTick) {
      Minecraft minecraft = Minecraft.getInstance();
      LegacyCameraRenderState state = new LegacyCameraRenderState();
      //#if MC >= 1.21.11
      state.pos = camera.position();
      //#else
      state.pos = camera.getPosition();
      //#endif
      GameRendererFovInvoker fovs = (GameRendererFovInvoker)minecraft.gameRenderer;
      float worldFov = (float)fovs.tacticalArsenal$getFov(camera, partialTick, true);
      state.hudFov = (float)fovs.tacticalArsenal$getFov(camera, partialTick, false);
      float aspect = (float)minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
      state.projectionMatrix.setPerspective((float)Math.toRadians(worldFov), aspect, 0.05F, 1000.0F);
      state.viewRotationMatrix.rotation(camera.rotation().conjugate(new Quaternionf()));
      //#if MC >= 1.21.11
      Entity entity = camera.entity();
      //#else
      Entity entity = camera.getEntity();
      //#endif
      LegacyCameraEntityRenderState entityState = state.entityRenderState;
      if (entity instanceof LivingEntity living) {
         entityState.isLiving = true;
         entityState.isSleeping = living.isSleeping();
         entityState.isDeadOrDying = living.isDeadOrDying();
         entityState.hurtDir = living.getHurtDir();
         entityState.hurtTime = living.hurtTime - partialTick;
         entityState.deathTime = living.deathTime + partialTick;
         entityState.hurtDuration = living.hurtDuration;
      }

      if (entity instanceof AbstractClientPlayer player) {
         entityState.isPlayer = true;
         //#if MC >= 1.21.11
         entityState.backwardsInterpolatedWalkDistance = player.avatarState().getBackwardsInterpolatedWalkDistance(partialTick);
         entityState.bob = player.avatarState().getInterpolatedBob(partialTick);
         //#else
         float walkDelta = player.walkDist - player.walkDistO;
         entityState.backwardsInterpolatedWalkDistance = -(player.walkDist + walkDelta * partialTick);
         entityState.bob = net.minecraft.util.Mth.lerp(partialTick, player.oBob, player.bob);
         //#endif
      }

      return state;
   }
}
