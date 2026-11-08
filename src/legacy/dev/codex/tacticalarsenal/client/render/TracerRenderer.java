package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.TracerStyle;
import dev.codex.tacticalarsenal.network.TracerPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import dev.codex.tacticalarsenal.client.compat.TracerRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import dev.codex.tacticalarsenal.client.compat.LegacyCameraEntityRenderState;
import dev.codex.tacticalarsenal.client.compat.LegacyCameraRenderState;
import dev.codex.tacticalarsenal.client.compat.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TracerRenderer {
   private static final int MAX_ACTIVE_TRACERS = 256;
   private static final TracerRenderer.TracerRenderProfile PISTOL_PROFILE = new TracerRenderer.TracerRenderProfile(
      343.0, 17.15, 2.5, 0.065, 3.0F, 1.15F, 16242014, 16774619, 0.46F, 0.98F
   );
   private static final TracerRenderer.TracerRenderProfile SMG_PROFILE = new TracerRenderer.TracerRenderProfile(
      410.0, 13.0, 2.5, 0.07, 2.8F, 1.05F, 15051888, 16769444, 0.46F, 0.98F
   );
   private static final TracerRenderer.TracerRenderProfile ASSAULT_RIFLE_PROFILE = new TracerRenderer.TracerRenderProfile(
      390.5, 22.86, 3.0, 0.09, 3.6F, 1.35F, 16242014, 16774619, 0.48F, 0.98F
   );
   private static final TracerRenderer.TracerRenderProfile ASSAULT_RIFLE_AUG_PROFILE = new TracerRenderer.TracerRenderProfile(
      430.0, 18.0, 3.0, 0.085, 3.2F, 1.2F, 15051888, 16769189, 0.48F, 0.98F
   );
   private static final TracerRenderer.TracerRenderProfile SNIPER_RIFLE_PROFILE = new TracerRenderer.TracerRenderProfile(
      571.5, 17.15, 4.0, 0.1, 5.0F, 1.9F, 16235614, 16774619, 0.56F, 1.0F
   );
   private static final TracerRenderer.TracerRenderProfile SSG_SNIPER_PROFILE = new TracerRenderer.TracerRenderProfile(
      571.5, 17.15, 3.5, 0.1, 4.2F, 1.55F, 16235614, 16773321, 0.52F, 0.99F
   );
   private static final TracerRenderer.TracerRenderProfile SCAR_SNIPER_PROFILE = new TracerRenderer.TracerRenderProfile(
      571.5, 17.15, 3.8, 0.1, 4.6F, 1.7F, 16235614, 16773842, 0.54F, 0.99F
   );
   private static final TracerRenderer.TracerRenderProfile MACHINE_GUN_PROFILE = new TracerRenderer.TracerRenderProfile(
      360.0, 12.0, 3.0, 0.11, 4.8F, 1.8F, 16765242, 16774307, 0.54F, 0.98F
   );
   private static final TracerRenderer.TracerRenderProfile SHOTGUN_PROFILE = new TracerRenderer.TracerRenderProfile(
      466.7, 17.15, 3.0, 0.075, 4.0F, 1.5F, 16221789, 16772315, 0.5F, 0.98F
   );
   private static final Deque<TracerRenderer.ActiveTracer> ACTIVE = new ArrayDeque<>();

   private TracerRenderer() {
   }

   public static void initialize() {
      ClientPlayNetworking.registerGlobalReceiver(TracerPayload.TYPE, (payload, context) -> accept(payload));
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> clear());
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> clear());
      // Before 1.21.9 there is no separate extraction pass: sample and draw in one world-render callback.
      WorldRenderEvents.AFTER_ENTITIES.register(TracerRenderer::render);
   }

   private static void accept(TracerPayload payload) {
      Vec3 start = payload.start();
      Vec3 end = payload.end();
      if (finite(start) && finite(end) && !(start.distanceToSqr(end) < 1.0E-6)) {
         synchronized (ACTIVE) {
            while (ACTIVE.size() >= 256) {
               ACTIVE.removeFirst();
            }

            ACTIVE.addLast(
               new TracerRenderer.ActiveTracer(
                  start, end, payload.style(), payload.shooterEntityId(), payload.weaponId(), payload.patternIndex(), System.nanoTime()
               )
            );
         }
      }
   }

   private static void clear() {
      synchronized (ACTIVE) {
         ACTIVE.clear();
      }

      MuzzlePositionTracker.clear();
   }

   private static void render(WorldRenderContext context) {
      long now = System.nanoTime();
      //#if MC >= 1.21
      float cameraPartialTick = context.tickCounter().getGameTimeDeltaPartialTick(true);
      //#else
      float cameraPartialTick = context.tickDelta();
      //#endif
      LegacyCameraRenderState cameraState = LegacyCameraRenderState.capture(context.camera(), cameraPartialTick);
      Minecraft minecraft = Minecraft.getInstance();
      TracerRenderer.MuzzleFrame muzzleFrame = new TracerRenderer.MuzzleFrame(
         minecraft.player,
         minecraft.options.getCameraType().isFirstPerson(),
         cameraPartialTick,
         cameraState,
         cameraMotion(cameraState)
      );
      List<TracerRenderer.TracerSegment> segments = new ArrayList<>();
      synchronized (ACTIVE) {
         Iterator<TracerRenderer.ActiveTracer> iterator = ACTIVE.iterator();

         while (iterator.hasNext()) {
            TracerRenderer.ActiveTracer tracer = iterator.next();
            TracerRenderer.TracerSegment segment = tracer.sample(now, muzzleFrame);
            if (segment == null) {
               iterator.remove();
            } else {
               segments.add(segment);
            }
         }
      }

      if (!segments.isEmpty() && context.consumers() != null) {
         Vec3 camera = cameraState.pos;
         PoseStack poses = context.matrixStack();
         SubmitNodeCollector collector = new SubmitNodeCollector(context.consumers());
         // Same draw order as 26.x: every glow line, then every core line.
         for (TracerRenderer.TracerSegment segment : segments) {
            TracerRenderer.TracerRenderProfile profile = profile(segment.style);
            int color = withAlpha(profile.glowRgb, segment.opacity * profile.glowOpacity);
            collector.submitCustomGeometry(poses, TracerRenderTypes.linesTranslucent(profile.glowWidth), (pose, buffer) ->
               writeLine(pose, buffer, segment.start, segment.end, camera, color, profile.glowWidth));
         }

         for (TracerRenderer.TracerSegment segment : segments) {
            TracerRenderer.TracerRenderProfile profile = profile(segment.style);
            int color = withAlpha(profile.coreRgb, segment.opacity * profile.coreOpacity);
            collector.submitCustomGeometry(poses, TracerRenderTypes.linesTranslucent(profile.coreWidth), (pose, buffer) ->
               writeLine(pose, buffer, segment.start, segment.end, camera, color, profile.coreWidth));
         }
      }
   }

   private static Vec3 resolveMuzzle(TracerRenderer.ActiveTracer tracer, TracerRenderer.MuzzleFrame frame) {
      LocalPlayer player = frame.player;
      if (player != null
         && frame.firstPerson
         && tracer.shooterEntityId == player.getId()
         && player.getMainHandItem().getItem() instanceof GunItem gun
         && gun.definition().id().equals(tracer.weaponId)) {
         if (gun.definition().hideViewmodelWhenZoomed() && GunClientController.isScopeOverlayActive(frame.partialTick)) {
            Vec3 scopedStart = scopedFirstPersonStart(tracer, frame);
            if (scopedStart != null) {
               return scopedStart;
            }
         }

         Vector3f gunPoint = GunViewmodelRenderer.firstPersonMuzzle(player, gun, tracer.patternIndex, frame.partialTick);
         if (gunPoint != null) {
            Vec3 worldPoint = firstPersonWorldPoint(gunPoint, frame);
            if (worldPoint != null) {
               return worldPoint;
            }
         }
      }

      Vec3 thirdPerson = MuzzlePositionTracker.resolve(tracer.shooterEntityId, tracer.weaponId, tracer.patternIndex);
      return thirdPerson == null ? tracer.serverStart : thirdPerson;
   }

   private static Vec3 scopedFirstPersonStart(TracerRenderer.ActiveTracer tracer, TracerRenderer.MuzzleFrame frame) {
      LegacyCameraRenderState camera = frame.cameraState;
      if (camera != null && camera.pos != null) {
         Vec3 path = tracer.end.subtract(camera.pos);
         double length = path.length();
         if (Double.isFinite(length) && !(length < 1.0E-6)) {
            Vec3 start = camera.pos.add(path.scale(0.35 / length));
            return finite(start) ? start : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Vec3 firstPersonWorldPoint(Vector3f gunPoint, TracerRenderer.MuzzleFrame frame) {
      LegacyCameraRenderState camera = frame.cameraState;
      if (camera != null && camera.pos != null && camera.projectionMatrix != null && camera.viewRotationMatrix != null) {
         Vector3f hudPoint = frame.cameraMotion.transformPosition(gunPoint, new Vector3f());
         double hudTangent = Math.tan(Math.toRadians(camera.hudFov) * 0.5);
         double denominator = camera.projectionMatrix.m11() * hudTangent;
         if (Double.isFinite(denominator) && !(Math.abs(denominator) < 1.0E-6)) {
            float fovScale = (float)(1.0 / denominator);
            if (Float.isFinite(fovScale) && !(fovScale <= 0.0F)) {
               hudPoint.x *= fovScale;
               hudPoint.y *= fovScale;
               Matrix4f inverseCameraMotion = new Matrix4f(frame.cameraMotion).invert();
               Vector3f cameraPoint = inverseCameraMotion.transformPosition(hudPoint, new Vector3f());
               Vector3f worldOffset = new Matrix4f(camera.viewRotationMatrix).invert().transformPosition(cameraPoint, new Vector3f());
               return Float.isFinite(worldOffset.x) && Float.isFinite(worldOffset.y) && Float.isFinite(worldOffset.z)
                  ? camera.pos.add(worldOffset.x, worldOffset.y, worldOffset.z)
                  : null;
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Matrix4f cameraMotion(LegacyCameraRenderState camera) {
      PoseStack poses = new PoseStack();
      if (camera != null && camera.entityRenderState != null) {
         LegacyCameraEntityRenderState entity = camera.entityRenderState;
         if (entity.isLiving) {
            if (entity.isDeadOrDying) {
               float death = Math.min(entity.deathTime, 20.0F);
               poses.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (death + 200.0F)));
            }

            if (entity.hurtTime >= 0.0F && entity.hurtDuration > 0) {
               float hurt = entity.hurtTime / entity.hurtDuration;
               hurt *= hurt;
               hurt *= hurt;
               hurt = Mth.sin(hurt * (float) Math.PI);
               float direction = entity.hurtDir;
               poses.mulPose(Axis.YP.rotationDegrees(-direction));
               double strength = (Double)Minecraft.getInstance().options.damageTiltStrength().get();
               poses.mulPose(Axis.ZP.rotationDegrees((float)(-hurt * 14.0 * strength)));
               poses.mulPose(Axis.YP.rotationDegrees(direction));
            }
         }

         if (entity.isPlayer && (Boolean)Minecraft.getInstance().options.bobView().get()) {
            float walk = entity.backwardsInterpolatedWalkDistance;
            float bob = entity.bob;
            float walkSin = Mth.sin(walk * (float) Math.PI);
            float walkCos = Mth.cos(walk * (float) Math.PI);
            poses.translate(walkSin * bob * 0.5F, -Math.abs(walkCos * bob), 0.0F);
            poses.mulPose(Axis.ZP.rotationDegrees(walkSin * bob * 3.0F));
            poses.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(walk * (float) Math.PI - 0.2F) * bob) * 5.0F));
         }

         return new Matrix4f(poses.last().pose());
      } else {
         return new Matrix4f();
      }
   }

   private static TracerRenderer.TracerRenderProfile profile(TracerStyle style) {
      return switch (style) {
         case PISTOL -> PISTOL_PROFILE;
         case SMG -> SMG_PROFILE;
         case ASSAULT_RIFLE -> ASSAULT_RIFLE_PROFILE;
         case ASSAULT_RIFLE_AUG -> ASSAULT_RIFLE_AUG_PROFILE;
         case SNIPER_RIFLE -> SNIPER_RIFLE_PROFILE;
         case SSG_SNIPER -> SSG_SNIPER_PROFILE;
         case SCAR_SNIPER -> SCAR_SNIPER_PROFILE;
         case MACHINE_GUN -> MACHINE_GUN_PROFILE;
         case SHOTGUN -> SHOTGUN_PROFILE;
      };
   }

   private static void writeLine(Pose pose, VertexConsumer buffer, Vec3 start, Vec3 end, Vec3 camera, int color, float width) {
      float dx = (float)(end.x - start.x);
      float dy = (float)(end.y - start.y);
      float dz = (float)(end.z - start.z);
      float startX = (float)(start.x - camera.x);
      float startY = (float)(start.y - camera.y);
      float startZ = (float)(start.z - camera.z);
      float endX = (float)(end.x - camera.x);
      float endY = (float)(end.y - camera.y);
      float endZ = (float)(end.z - camera.z);
      buffer.addVertex(pose, startX, startY, startZ).setColor(color).setNormal(pose, dx, dy, dz);
      buffer.addVertex(pose, endX, endY, endZ).setColor(color).setNormal(pose, dx, dy, dz);
   }

   private static int withAlpha(int rgb, float opacity) {
      int alpha = Mth.clamp((int)(opacity * 255.0F), 0, 255);
      return alpha << 24 | rgb;
   }

   private static boolean finite(Vec3 point) {
      return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
   }

   private static final class ActiveTracer {
      private final Vec3 serverStart;
      private final Vec3 end;
      private final TracerStyle style;
      private final int shooterEntityId;
      private final String weaponId;
      private final int patternIndex;
      private final long bornNanos;
      private Vec3 resolvedStart;

      private ActiveTracer(Vec3 serverStart, Vec3 end, TracerStyle style, int shooterEntityId, String weaponId, int patternIndex, long bornNanos) {
         this.serverStart = serverStart;
         this.end = end;
         this.style = style;
         this.shooterEntityId = shooterEntityId;
         this.weaponId = weaponId;
         this.patternIndex = patternIndex;
         this.bornNanos = bornNanos;
      }

      private TracerRenderer.TracerSegment sample(long nowNanos, TracerRenderer.MuzzleFrame frame) {
         if (this.resolvedStart == null) {
            this.resolvedStart = TracerRenderer.resolveMuzzle(this, frame);
            Vec3 originalPath = this.end.subtract(this.serverStart);
            Vec3 resolvedPath = this.end.subtract(this.resolvedStart);
            if (resolvedPath.dot(originalPath) <= 0.0) {
               return null;
            }
         }

         double ageSeconds = Math.max(0.0, (nowNanos - this.bornNanos) / 1.0E9);
         Vec3 path = this.end.subtract(this.resolvedStart);
         double length = path.length();
         if (length < 0.001) {
            return null;
         } else {
            TracerRenderer.TracerRenderProfile profile = TracerRenderer.profile(this.style);
            double travelSeconds = Math.max(0.0, (length - profile.initialHead) / profile.speed);
            if (ageSeconds > travelSeconds + profile.fadeSeconds) {
               return null;
            } else {
               double headDistance = Math.min(length, profile.initialHead + ageSeconds * profile.speed);
               double tailDistance = Math.max(0.0, headDistance - profile.tailLength);
               float opacity = ageSeconds <= travelSeconds ? 1.0F : Mth.clamp((float)(1.0 - (ageSeconds - travelSeconds) / profile.fadeSeconds), 0.0F, 1.0F);
               Vec3 direction = path.scale(1.0 / length);
               return new TracerRenderer.TracerSegment(
                  this.resolvedStart.add(direction.scale(tailDistance)), this.resolvedStart.add(direction.scale(headDistance)), opacity, this.style
               );
            }
         }
      }
   }

   private record MuzzleFrame(LocalPlayer player, boolean firstPerson, float partialTick, LegacyCameraRenderState cameraState, Matrix4f cameraMotion) {
   }

   private record TracerRenderProfile(
      double speed,
      double tailLength,
      double initialHead,
      double fadeSeconds,
      float glowWidth,
      float coreWidth,
      int glowRgb,
      int coreRgb,
      float glowOpacity,
      float coreOpacity
   ) {
   }

   private record TracerSegment(Vec3 start, Vec3 end, float opacity, TracerStyle style) {
   }
}
