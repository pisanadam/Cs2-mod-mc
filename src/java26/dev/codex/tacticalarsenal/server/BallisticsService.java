package dev.codex.tacticalarsenal.server;

import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunVisualProfile;
import dev.codex.tacticalarsenal.gun.TracerStyle;
import dev.codex.tacticalarsenal.network.TracerPayload;
import dev.codex.tacticalarsenal.registry.ModCombatSounds;
import dev.codex.tacticalarsenal.registry.ModDamageTypes;
import dev.codex.tacticalarsenal.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BallisticsService {
   private static final double TRACE_STEP = 0.08;
   private static final int MAX_SURFACES = 4;

   private BallisticsService() {
   }

   public static void fire(
      ServerPlayer shooter, GunDefinition gun, boolean requestedScope, boolean alternateFireMode, int patternIndex, float fireAccuracyPenalty
   ) {
      ServerLevel level = shooter.level();
      boolean scoped = requestedScope && gun.canScope();
      Vec3 start = shooter.getEyePosition();
      BallisticsService.AccuracySample accuracy = calculateAccuracy(shooter, gun, scoped, alternateFireMode, fireAccuracyPenalty);
      Vec3 effectDirection = shooter.getLookAngle();
      float heldSide = shooter.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      GunVisualProfile visualProfile = GunVisualProfile.forShot(gun, patternIndex);
      Vec3 muzzle = muzzlePosition(shooter, start, effectDirection, visualProfile, heldSide);
      playShotEffects(level, shooter, muzzle, gun);
      BallisticsService.ShotHitEvents hitEvents = new BallisticsService.ShotHitEvents();
      int tracerFrequency = gun.tracerFrequency(scoped, alternateFireMode);
      boolean showTracer = tracerFrequency > 0 && Math.floorMod(patternIndex, tracerFrequency) == 0;

      for (int projectile = 0; projectile < gun.projectiles(); projectile++) {
         Vec3 direction = applySpread(shooter, effectDirection, accuracy.spread, accuracy.inaccuracy, patternIndex * 31 + projectile);
         Vec3 end = start.add(direction.scale(gun.maximumRange()));
         Vec3 terminal = trace(level, shooter, gun, start, end, direction, hitEvents);
         boolean showProjectileTracer = showTracer && (visualProfile.tracerStyle() == TracerStyle.SHOTGUN || projectile == 0);
         if (showProjectileTracer && hasForwardTracerPath(start, muzzle, terminal, direction)) {
            sendTracer(level, shooter, gun.id(), patternIndex, muzzle, terminal, visualProfile.tracerStyle());
         }
      }

      hitEvents.play(level);
   }

   public static BallisticsService.BulletHitResult applyDirectHit(
      ServerLevel level, LivingEntity shooter, LivingEntity target, GunDefinition gun, Vec3 hitPosition, double distanceBlocks, float energyScale
   ) {
      return applyBulletDamage(level, shooter, target, gun, hitPosition, distanceBlocks, energyScale, true);
   }

   private static BallisticsService.AccuracySample calculateAccuracy(
      ServerPlayer shooter, GunDefinition gun, boolean scoped, boolean alternateFireMode, float fireAccuracyPenalty
   ) {
      float inaccuracy = shooter.isCrouching() ? gun.crouchingInaccuracy(scoped, alternateFireMode) : gun.standingInaccuracy(scoped, alternateFireMode);
      double sourceSpeed = shooter.getDeltaMovement().horizontalDistance() * 20.0 * 52.493F;
      float moveFactor = Mth.clamp((float)(sourceSpeed / Math.max(1.0F, gun.maximumMoveSpeed(scoped))), 0.0F, 1.0F);
      inaccuracy += gun.movementInaccuracy(scoped, alternateFireMode) * moveFactor;
      if (!shooter.onGround()) {
         float verticalFactor = Mth.clamp((float)(Math.abs(shooter.getDeltaMovement().y) / 0.42), 0.0F, 1.0F);
         float jumpPhase = Mth.lerp(verticalFactor, gun.accuracy().jumpApex(), gun.accuracy().jumpInitial());
         inaccuracy += Math.max(gun.jumpInaccuracy(scoped, alternateFireMode), jumpPhase);
      }

      inaccuracy += Math.max(0.0F, fireAccuracyPenalty);
      return new BallisticsService.AccuracySample(gun.spread(scoped, alternateFireMode), inaccuracy);
   }

   private static Vec3 applySpread(ServerPlayer shooter, Vec3 forward, float spread, float inaccuracy, int patternIndex) {
      Vec3 worldUp = Math.abs(forward.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(worldUp).normalize();
      Vec3 up = right.cross(forward).normalize();
      double inaccuracyAngle = shooter.getRandom().nextDouble() * Math.PI * 2.0 + patternIndex * 2.399963;
      double spreadAngle = shooter.getRandom().nextDouble() * Math.PI * 2.0;
      double inaccuracyRadius = shooter.getRandom().nextDouble() * inaccuracy;
      double spreadRadius = shooter.getRandom().nextDouble() * spread;
      double x = Math.cos(inaccuracyAngle) * inaccuracyRadius + Math.cos(spreadAngle) * spreadRadius;
      double y = Math.sin(inaccuracyAngle) * inaccuracyRadius + Math.sin(spreadAngle) * spreadRadius;
      return forward.add(right.scale(x)).add(up.scale(y)).normalize();
   }

   private static Vec3 trace(
      ServerLevel level, ServerPlayer shooter, GunDefinition gun, Vec3 start, Vec3 end, Vec3 direction, BallisticsService.ShotHitEvents hitEvents
   ) {
      List<BallisticsService.EntityIntersection> entityHits = collectEntityHits(level, shooter, start, end);
      Set<Integer> damagedEntities = new HashSet<>();
      int entityCursor = 0;
      float remainingPenetration = gun.penetrationPower();
      int surfaces = 0;
      BlockPos previousSolid = null;
      double totalDistance = start.distanceTo(end);

      for (double distance = 0.0; distance <= totalDistance; distance += 0.08) {
         while (entityCursor < entityHits.size() && entityHits.get(entityCursor).distance <= distance) {
            BallisticsService.EntityIntersection hit = entityHits.get(entityCursor++);
            if (!(remainingPenetration <= 0.0F) && damagedEntities.add(hit.entity.getId())) {
               float energyScale = Mth.clamp(remainingPenetration / gun.penetrationPower(), 0.35F, 1.0F);
               BallisticsService.BulletHitResult result = applyBulletDamage(level, shooter, hit.entity, gun, hit.position, hit.distance, energyScale, false);
               if (result.accepted()) {
                  hitEvents.accept(hit.entity, hit.position, result.region(), result.armored(), result.killed());
               }

               remainingPenetration -= 0.55F;
               if (remainingPenetration <= 0.0F) {
                  return hit.position;
               }
            }
         }

         Vec3 point = start.add(direction.scale(distance));
         BlockPos blockPos = BlockPos.containing(point);
         BlockState state = level.getBlockState(blockPos);
         if (!insideCollision(level, blockPos, state, point)) {
            previousSolid = null;
         } else {
            if (!blockPos.equals(previousSolid)) {
               surfaces++;
               previousSolid = blockPos.immutable();
               if (surfaces > 4) {
                  return point;
               }
            }

            remainingPenetration -= penetrationCost(level, blockPos, state) * 0.08F;
            if (remainingPenetration <= 0.0F) {
               level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 3, 0.04, 0.04, 0.04, 0.01);
               return point;
            }
         }
      }

      return end;
   }

   private static BallisticsService.BulletHitResult applyBulletDamage(
      ServerLevel level,
      LivingEntity shooter,
      LivingEntity target,
      GunDefinition gun,
      Vec3 hitPosition,
      double distanceBlocks,
      float energyScale,
      boolean immediateFeedback
   ) {
      BallisticsService.HitRegion region = hitRegion(target, hitPosition);
      boolean armored = isArmored(target, region);
      float damage = gun.damageAtDistance(distanceBlocks) * region.multiplier(gun) * Mth.clamp(energyScale, 0.0F, 1.0F);
      if (armored) {
         damage *= gun.armoredDamageFactor();
      }

      boolean accepted = dev.codex.tacticalarsenal.compat.CommonCompat.hurt(target, level, ModDamageTypes.bullet(level, shooter), damage);
      boolean killed = accepted && !target.isAlive();
      if (accepted) {
         level.sendParticles(ParticleTypes.CRIT, hitPosition.x, hitPosition.y, hitPosition.z, 4, 0.08, 0.08, 0.08, 0.04);
         if (immediateFeedback) {
            ModCombatSounds.playBulletHit(level, hitPosition, region == BallisticsService.HitRegion.HEAD, armored, killed);
         }
      }

      return new BallisticsService.BulletHitResult(accepted, region, armored, damage, killed);
   }

   private static List<BallisticsService.EntityIntersection> collectEntityHits(ServerLevel level, ServerPlayer shooter, Vec3 start, Vec3 end) {
      AABB corridor = new AABB(start, end).inflate(1.5);
      List<BallisticsService.EntityIntersection> hits = new ArrayList<>();

      for (Entity entity : level.getEntities(shooter, corridor, entityx -> entityx instanceof LivingEntity && entityx.isPickable() && entityx.isAlive())) {
         AABB hitbox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.03F);
         Optional<Vec3> intersection = hitbox.clip(start, end);
         intersection.ifPresent(position -> hits.add(new BallisticsService.EntityIntersection((LivingEntity)entity, position, start.distanceTo(position))));
      }

      hits.sort(Comparator.comparingDouble(BallisticsService.EntityIntersection::distance));
      return hits;
   }

   private static boolean insideCollision(ServerLevel level, BlockPos pos, BlockState state, Vec3 point) {
      if (state.isAir()) {
         return false;
      } else {
         VoxelShape shape = state.getCollisionShape(level, pos);
         if (shape.isEmpty()) {
            return false;
         } else {
            for (AABB box : shape.toAabbs()) {
               if (box.move(pos).inflate(0.01).contains(point)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static float penetrationCost(ServerLevel level, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(level, pos);
      if (hardness < 0.0F) {
         return Float.POSITIVE_INFINITY;
      } else if (state.is(BlockTags.LEAVES)) {
         return 0.05F;
      } else if (state.getBlock() instanceof HalfTransparentBlock) {
         return 0.15F;
      } else if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) {
         return 0.25F;
      } else if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) {
         return 0.9F;
      } else {
         return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? Math.max(1.2F, hardness * 1.5F) : Math.max(0.4F, hardness * 0.8F);
      }
   }

   private static BallisticsService.HitRegion hitRegion(LivingEntity entity, Vec3 hitPosition) {
      AABB bounds = entity.getBoundingBox();
      double height = bounds.getYsize();
      double width = Math.max(bounds.getXsize(), bounds.getZsize());
      if (Double.isFinite(height) && Double.isFinite(width) && !(height < 0.01) && !(width < 0.01) && isFinite(hitPosition)) {
         double normalizedHeight = Mth.clamp((hitPosition.y - bounds.minY) / height, 0.0, 1.0);
         double eyeHeight = Mth.clamp((entity.getEyeY() - bounds.minY) / height, 0.0, 1.0);
         double headStart = Mth.clamp(eyeHeight - 0.12, 0.68, 0.84);
         double legEnd = Mth.clamp(eyeHeight * 0.38, 0.24, 0.36);
         if (normalizedHeight >= headStart) {
            return BallisticsService.HitRegion.HEAD;
         } else {
            return normalizedHeight < legEnd ? BallisticsService.HitRegion.LEGS : BallisticsService.HitRegion.TORSO;
         }
      } else {
         return BallisticsService.HitRegion.TORSO;
      }
   }

   private static boolean isFinite(Vec3 position) {
      return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
   }

   private static boolean isArmored(LivingEntity entity, BallisticsService.HitRegion region) {
      if (region == BallisticsService.HitRegion.HEAD) {
         return providesArmor(entity.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD);
      } else {
         return region == BallisticsService.HitRegion.LEGS
            ? false
            : providesArmor(entity.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST)
               || providesArmor(entity.getItemBySlot(EquipmentSlot.BODY), EquipmentSlot.BODY);
      }
   }

   private static boolean providesArmor(ItemStack stack, EquipmentSlot slot) {
      if (stack.isEmpty()) {
         return false;
      } else {
         boolean[] armored = new boolean[]{false};
         stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ARMOR) && modifier.amount() > 0.0) {
               armored[0] = true;
            }
         });
         return armored[0];
      }
   }

   private static void playShotEffects(ServerLevel level, ServerPlayer shooter, Vec3 muzzle, GunDefinition gun) {
      level.playSound(
         null,
         shooter.getX(),
         shooter.getEyeY(),
         shooter.getZ(),
         ModSounds.fire(gun.id()),
         SoundSource.PLAYERS,
         ModSounds.fireVolume(gun.id()),
         ModSounds.firePitch(gun.id(), shooter.getRandom())
      );
      if (!gun.suppressed()) {
         sendMuzzleParticles(level, shooter, ParticleTypes.FLAME, muzzle, 1, 0.0, 0.0);
      }

      sendMuzzleParticles(level, shooter, ParticleTypes.SMOKE, muzzle, gun.suppressed() ? 1 : 2, 0.02, 0.01);
   }

   private static Vec3 muzzlePosition(ServerPlayer shooter, Vec3 start, Vec3 direction, GunVisualProfile visualProfile, float heldSide) {
      Vec3 right = Vec3.directionFromRotation(0.0F, shooter.getYRot() + 90.0F);
      Vec3 up = right.cross(direction).normalize();
      return start.add(direction.scale(visualProfile.muzzleForward()))
         .add(right.scale(visualProfile.muzzleSide() * heldSide))
         .add(up.scale(visualProfile.muzzleUp()));
   }

   private static boolean hasForwardTracerPath(Vec3 eye, Vec3 muzzle, Vec3 terminal, Vec3 direction) {
      double terminalForward = terminal.subtract(eye).dot(direction);
      double muzzleForward = muzzle.subtract(eye).dot(direction);
      return terminalForward > muzzleForward + 0.05 && muzzle.distanceToSqr(terminal) >= 1.0E-6;
   }

   private static void sendTracer(ServerLevel level, ServerPlayer shooter, String weaponId, int patternIndex, Vec3 start, Vec3 end, TracerStyle style) {
      TracerPayload payload = TracerPayload.between(shooter.getId(), weaponId, patternIndex, start, end, style);
      double rangeSquared = 36864.0;

      for (ServerPlayer viewer : level.players()) {
         boolean nearPath = viewer == shooter || viewer.position().distanceToSqr(start) <= rangeSquared || viewer.position().distanceToSqr(end) <= rangeSquared;
         if (nearPath && ServerPlayNetworking.canSend(viewer, TracerPayload.TYPE)) {
            ServerPlayNetworking.send(viewer, payload);
         }
      }
   }

   private static <T extends ParticleOptions> void sendMuzzleParticles(
      ServerLevel level, ServerPlayer shooter, T particle, Vec3 position, int count, double spread, double speed
   ) {
      for (ServerPlayer viewer : level.players()) {
         if (viewer != shooter) {
            //#if MC >= 1.21.4
            level.sendParticles(viewer, particle, false, false, position.x, position.y, position.z, count, spread, spread, spread, speed);
            //#else
            level.sendParticles(viewer, particle, false, position.x, position.y, position.z, count, spread, spread, spread, speed);
            //#endif
         }
      }
   }

   private record AcceptedHit(Vec3 position, BallisticsService.HitRegion region, boolean armored, boolean killed) {
      private int priority() {
         return (this.killed ? 4 : 0) + (this.region == BallisticsService.HitRegion.HEAD ? 2 : 0) + (this.armored ? 1 : 0);
      }
   }

   private record AccuracySample(float spread, float inaccuracy) {
   }

   public record BulletHitResult(boolean accepted, BallisticsService.HitRegion region, boolean armored, float damage, boolean killed) {
   }

   private record EntityIntersection(LivingEntity entity, Vec3 position, double distance) {
   }

   public static enum HitRegion {
      HEAD,
      TORSO,
      LEGS;

      private float multiplier(GunDefinition gun) {
         return switch (this) {
            case HEAD -> gun.headMultiplier();
            case TORSO -> 1.0F;
            case LEGS -> 0.75F;
         };
      }
   }

   private static final class ShotHitEvents {
      private final Map<Integer, BallisticsService.AcceptedHit> acceptedByEntity = new LinkedHashMap<>();

      private void accept(LivingEntity entity, Vec3 position, BallisticsService.HitRegion region, boolean armored, boolean killed) {
         BallisticsService.AcceptedHit accepted = new BallisticsService.AcceptedHit(position, region, armored, killed);
         this.acceptedByEntity
            .merge(entity.getId(), accepted, (previous, next) -> (BallisticsService.AcceptedHit)(next.priority() > previous.priority() ? next : previous));
      }

      private void play(ServerLevel level) {
         for (BallisticsService.AcceptedHit hit : this.acceptedByEntity.values()) {
            ModCombatSounds.playBulletHit(level, hit.position, hit.region == BallisticsService.HitRegion.HEAD, hit.armored, hit.killed);
         }
      }
   }
}
