package dev.codex.tacticalarsenal.npc;

import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunVisualProfile;
import dev.codex.tacticalarsenal.gun.TracerStyle;
import dev.codex.tacticalarsenal.network.TracerPayload;
import dev.codex.tacticalarsenal.registry.ModSounds;
import dev.codex.tacticalarsenal.server.BallisticsService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class CombatantBallistics {
   private static final double ENTITY_CORRIDOR_INFLATION = 1.25;
   private static final double TRACER_BROADCAST_RANGE_SQR = 36864.0;

   private CombatantBallistics() {
   }

   public static boolean hasFriendlySafeLine(ServerLevel level, CombatantEntity shooter, LivingEntity intendedTarget) {
      Vec3 start = shooter.getEyePosition();
      Vec3 targetPoint = new Vec3(intendedTarget.getX(), intendedTarget.getBoundingBox().minY + intendedTarget.getBbHeight() * 0.62, intendedTarget.getZ());
      double intendedDistance = start.distanceTo(targetPoint);
      AABB corridor = new AABB(start, targetPoint).inflate(0.35);

      for (Entity entity : level.getEntities(
         shooter, corridor, entityx -> entityx instanceof LivingEntity living && living.isAlive() && shooter.isFriendlyTo(living)
      )) {
         Optional<Vec3> intersection = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.05F).clip(start, targetPoint);
         if (intersection.isPresent() && start.distanceTo(intersection.get()) < intendedDistance - 0.15) {
            return false;
         }
      }

      return true;
   }

   public static void fire(ServerLevel level, CombatantEntity shooter, GunDefinition gun, Vec3 aimPoint, int patternIndex, float skillError) {
      Vec3 eye = shooter.getEyePosition();
      Vec3 forward = aimPoint.subtract(eye);
      if (!(forward.lengthSqr() < 1.0E-8)) {
         forward = forward.normalize();
         float movementFactor = Mth.clamp(
            (float)(shooter.getDeltaMovement().horizontalDistance() * 20.0 * 52.493F / Math.max(1.0F, gun.maximumMoveSpeed(false))), 0.0F, 1.0F
         );
         float spread = gun.spread(false) + gun.standingInaccuracy(false) + gun.movementInaccuracy(false) * movementFactor + Math.max(0.0F, skillError);
         if (!shooter.onGround()) {
            spread += gun.jumpInaccuracy(false);
         }

         GunVisualProfile visual = GunVisualProfile.forShot(gun, patternIndex);
         Vec3 muzzle = muzzlePosition(shooter, eye, forward, visual);
         playShotEffects(level, shooter, gun, muzzle);

         for (int projectile = 0; projectile < gun.projectiles(); projectile++) {
            Vec3 direction = applySpread(shooter, forward, spread, patternIndex * 31 + projectile);
            Vec3 terminal = trace(level, shooter, gun, eye, eye.add(direction.scale(gun.maximumRange())));
            if (projectile == 0 || visual.tracerStyle() == TracerStyle.SHOTGUN) {
               sendTracer(level, shooter, gun.id(), patternIndex, muzzle, terminal, visual.tracerStyle());
            }
         }
      }
   }

   private static Vec3 applySpread(CombatantEntity shooter, Vec3 forward, float radius, int patternIndex) {
      Vec3 worldUp = Math.abs(forward.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp).normalize();
      Vec3 up = right.cross(forward).normalize();
      double angle = shooter.getRandom().nextDouble() * Math.PI * 2.0 + patternIndex * 2.399963;
      double sampledRadius = Math.sqrt(shooter.getRandom().nextDouble()) * radius;
      return forward.add(right.scale(Math.cos(angle) * sampledRadius)).add(up.scale(Math.sin(angle) * sampledRadius)).normalize();
   }

   private static Vec3 trace(ServerLevel level, CombatantEntity shooter, GunDefinition gun, Vec3 start, Vec3 requestedEnd) {
      BlockHitResult blockHit = level.clip(new ClipContext(start, requestedEnd, Block.COLLIDER, Fluid.NONE, shooter));
      Vec3 physicalEnd = blockHit.getType() == Type.BLOCK ? blockHit.getLocation() : requestedEnd;
      double blockDistance = start.distanceTo(physicalEnd);
      List<CombatantBallistics.EntityIntersection> intersections = new ArrayList<>();
      AABB corridor = new AABB(start, physicalEnd).inflate(1.25);

      for (Entity entity : level.getEntities(shooter, corridor, entityx -> entityx instanceof LivingEntity living && living.isAlive() && living.isPickable())) {
         AABB hitbox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.03F);
         hitbox.clip(start, physicalEnd)
            .ifPresent(position -> intersections.add(new CombatantBallistics.EntityIntersection((LivingEntity)entity, position, start.distanceTo(position))));
      }

      intersections.sort(Comparator.comparingDouble(CombatantBallistics.EntityIntersection::distance));
      if (!intersections.isEmpty() && !(intersections.getFirst().distance() > blockDistance)) {
         CombatantBallistics.EntityIntersection hit = intersections.getFirst();
         if (shooter.isFriendlyTo(hit.entity())) {
            return hit.position();
         } else {
            BallisticsService.applyDirectHit(level, shooter, hit.entity(), gun, hit.position(), hit.distance(), 1.0F);
            return hit.position();
         }
      } else {
         if (blockHit.getType() == Type.BLOCK) {
            level.sendParticles(ParticleTypes.SMOKE, physicalEnd.x, physicalEnd.y, physicalEnd.z, 2, 0.03, 0.03, 0.03, 0.01);
         }

         return physicalEnd;
      }
   }

   private static Vec3 muzzlePosition(CombatantEntity shooter, Vec3 eye, Vec3 forward, GunVisualProfile visual) {
      Vec3 right = Vec3.directionFromRotation(0.0F, shooter.getYRot() + 90.0F);
      Vec3 up = right.cross(forward).normalize();
      float heldSide = shooter.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      return eye.add(forward.scale(visual.muzzleForward())).add(right.scale(visual.muzzleSide() * heldSide)).add(up.scale(visual.muzzleUp()));
   }

   private static void playShotEffects(ServerLevel level, CombatantEntity shooter, GunDefinition gun, Vec3 muzzle) {
      level.playSound(
         null,
         shooter.getX(),
         shooter.getEyeY(),
         shooter.getZ(),
         ModSounds.fire(gun.id()),
         shooter.combatantTeam() == CombatantTeam.NATURAL ? SoundSource.HOSTILE : SoundSource.PLAYERS,
         ModSounds.fireVolume(gun.id()),
         ModSounds.firePitch(gun.id(), shooter.getRandom())
      );
      if (!gun.suppressed()) {
         level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, gun.suppressed() ? 1 : 2, 0.02, 0.02, 0.02, 0.01);
   }

   private static void sendTracer(ServerLevel level, CombatantEntity shooter, String weaponId, int patternIndex, Vec3 start, Vec3 end, TracerStyle style) {
      if (!(start.distanceToSqr(end) < 1.0E-6)) {
         TracerPayload payload = TracerPayload.between(shooter.getId(), weaponId, patternIndex, start, end, style);

         for (ServerPlayer viewer : level.players()) {
            if ((viewer.position().distanceToSqr(start) <= 36864.0 || viewer.position().distanceToSqr(end) <= 36864.0)
               && ServerPlayNetworking.canSend(viewer, TracerPayload.TYPE)) {
               ServerPlayNetworking.send(viewer, payload);
            }
         }
      }
   }

   private record EntityIntersection(LivingEntity entity, Vec3 position, double distance) {
   }
}
