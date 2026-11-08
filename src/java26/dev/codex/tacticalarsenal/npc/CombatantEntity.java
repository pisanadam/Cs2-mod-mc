package dev.codex.tacticalarsenal.npc;

import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.gun.WeaponCategory;
import dev.codex.tacticalarsenal.registry.ModItems;
import dev.codex.tacticalarsenal.registry.ModSounds;
import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class CombatantEntity extends Zombie {
   private static final double ACQUISITION_RANGE = 56.0;
   private static final double ACQUISITION_RANGE_SQR = 3136.0;
   private static final int TARGET_SCAN_TICKS = 8;
   private static final int HIT_MELEE_COOLDOWN_TICKS = 10;
   private static final int MAX_COVER_CANDIDATES = 32;
   private static final String PRIMARY_STACK_TAG = "TacticalPrimary";
   private static final String PISTOL_STACK_TAG = "TacticalPistol";
   private static final String KNIFE_STACK_TAG = "TacticalKnife";
   private static final String ACTIVE_SLOT_TAG = "TacticalActiveSlot";
   private final CombatantTeam combatantTeam;
   private final CombatantTier combatantTier;
   private ItemStack primaryStack = ItemStack.EMPTY;
   private ItemStack pistolStack = ItemStack.EMPTY;
   private ItemStack knifeStack = ItemStack.EMPTY;
   private CombatantEntity.WeaponSlot activeSlot = CombatantEntity.WeaponSlot.KNIFE;
   private CombatantEntity.TacticalState tacticalState = CombatantEntity.TacticalState.ENGAGE;
   private BlockPos coverPosition;
   private BlockPos peekPosition;
   private long stateUntilGameTime;
   private long nextCoverSearchGameTime;
   private long underFireUntilGameTime;
   private long reactionUntilGameTime;
   private long nextShotGameTime;
   private long reloadCompleteGameTime;
   private long burstPauseUntilGameTime;
   private long nextMeleeGameTime;
   private long nextIdleMoveGameTime;
   private int shotPatternIndex;
   private int burstShots;
   private int strafeDirection = 1;

   public CombatantEntity(EntityType<? extends CombatantEntity> type, Level level, CombatantTeam team, CombatantTier tier) {
      super(type, level);
      this.combatantTeam = team;
      this.combatantTier = tier;
      this.xpReward = 0;
      this.setPersistenceRequired();
      this.getNavigation().setCanOpenDoors(true);
      if (!level.isClientSide()) {
         this.equipFreshLoadout();
      }
   }

   public static Builder createAttributes() {
      return Zombie.createAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.FOLLOW_RANGE, 56.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 30.0)
         .add(Attributes.ARMOR, 0.0)
         .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
   }

   public CombatantTeam combatantTeam() {
      return this.combatantTeam;
   }

   public CombatantTier combatantTier() {
      return this.combatantTier;
   }

   public boolean isFriendlyTo(LivingEntity entity) {
      if (entity == this) {
         return true;
      } else {
         return entity instanceof CombatantEntity combatant
            ? combatant.combatantTeam == this.combatantTeam
            : entity instanceof Player && this.combatantTeam == CombatantTeam.PLAYER;
      }
   }

   protected void registerGoals() {
   }

   protected boolean isSunSensitive() {
      return false;
   }

   public boolean canBeSeenAsEnemy() {
      return super.canBeSeenAsEnemy();
   }

   public boolean isInvertedHealAndHarm() {
      return this.combatantTeam == CombatantTeam.NATURAL && super.isInvertedHealAndHarm();
   }

   protected SoundEvent getAmbientSound() {
      return this.combatantTeam == CombatantTeam.NATURAL ? super.getAmbientSound() : null;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.combatantTeam == CombatantTeam.NATURAL ? super.getHurtSound(source) : SoundEvents.PLAYER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return this.combatantTeam == CombatantTeam.NATURAL ? super.getDeathSound() : SoundEvents.PLAYER_DEATH;
   }

   protected boolean convertsInWater() {
      return false;
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public boolean shouldDropExperience() {
      return false;
   }

   protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
      this.setBaby(false);
      this.equipFreshLoadout();
      return result;
   }

   protected boolean considersEntityAsAlly(Entity entity) {
      return entity instanceof LivingEntity living && this.isFriendlyTo(living);
   }

   public boolean canAttack(LivingEntity target) {
      return this.isEnemy(target) && super.canAttack(target);
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      Entity attacker = source.getEntity();
      if (attacker instanceof CombatantEntity combatant && this.isFriendlyTo(combatant)) {
         return false;
      } else {
         boolean hurt = super.hurtServer(level, source, amount);
         if (hurt && attacker instanceof LivingEntity living && this.isEnemy(living)) {
            boolean acquiredNewTarget = this.getTarget() != living;
            this.setTarget(living);
            long now = level.getGameTime();
            if (acquiredNewTarget) {
               this.onTargetAcquired(now);
            }

            this.underFireUntilGameTime = now + 60L;
            this.nextCoverSearchGameTime = Math.min(this.nextCoverSearchGameTime, now);
         }

         return hurt;
      }
   }

   protected void customServerAiStep(ServerLevel level) {
      super.customServerAiStep(level);
      long now = level.getGameTime();
      LivingEntity target = this.getTarget();
      if (!this.isValidTarget(target)) {
         this.setTarget(null);
         target = null;
      }

      if (target == null && Math.floorMod(now + this.getId(), 8) == 0L) {
         target = this.findTarget(level);
         if (target != null) {
            this.setTarget(target);
            this.onTargetAcquired(now);
         }
      }

      this.setAggressive(target != null);
      if (target == null) {
         this.tickWithoutTarget(level, now);
      } else {
         this.getLookControl().setLookAt(target, 42.0F, 42.0F);
         if (!this.prepareActiveWeapon(level, now)) {
            this.tickTacticalMovement(level, target, now, false);
         } else {
            ItemStack active = this.activeStack();
            if (active.getItem() instanceof GunItem gun) {
               this.tickRangedCombat(level, target, gun, active, now);
            } else {
               this.tickKnifeRush(level, target, now);
            }
         }
      }
   }

   private void tickRangedCombat(ServerLevel level, LivingEntity target, GunItem gun, ItemStack stack, long now) {
      boolean hasLineOfSight = this.getSensing().hasLineOfSight(target);
      this.tickTacticalMovement(level, target, now, hasLineOfSight);
      if (hasLineOfSight
         && now >= this.reactionUntilGameTime
         && now >= this.nextShotGameTime
         && now >= this.burstPauseUntilGameTime
         && this.tacticalState != CombatantEntity.TacticalState.SEEK_COVER
         && this.tacticalState != CombatantEntity.TacticalState.HOLD_COVER
         && CombatantBallistics.hasFriendlySafeLine(level, this, target)) {
         Vec3 aimPoint = this.chooseAimPoint(target);
         if (gun.consumeRound(stack)) {
            GunDefinition definition = gun.definition();

            float skillError = switch (this.combatantTier) {
               case TIER_1 -> 0.03F;
               case TIER_2 -> 0.017F;
               case TIER_3 -> 0.009F;
            };
            CombatantBallistics.fire(level, this, definition, aimPoint, this.shotPatternIndex, skillError);
            this.shotPatternIndex++;
            this.burstShots++;
            int cycleTicks = Math.max(1, Mth.ceil(definition.cycleTimeSeconds() * 20.0F));
            this.nextShotGameTime = now + cycleTicks;

            int burstLimit = switch (this.combatantTier) {
               case TIER_1 -> 3;
               case TIER_2 -> 5;
               case TIER_3 -> 7;
            };
            if (definition.category() != WeaponCategory.PISTOL && this.burstShots >= burstLimit) {
               this.burstShots = 0;

               this.burstPauseUntilGameTime = now + switch (this.combatantTier) {
                  case TIER_1 -> 11L;
                  case TIER_2 -> 7L;
                  case TIER_3 -> 4L;
               };
            }
         }
      }
   }

   private void tickKnifeRush(ServerLevel level, LivingEntity target, long now) {
      this.tacticalState = CombatantEntity.TacticalState.KNIFE_RUSH;
      this.setShiftKeyDown(false);
      this.getNavigation().moveTo(target, 1.38);
      if (!(this.distanceToSqr(target) > 7.0225) && now >= this.nextMeleeGameTime && this.getSensing().hasLineOfSight(target)) {
         this.getNavigation().stop();
         this.swing(InteractionHand.MAIN_HAND, true);
         this.nextMeleeGameTime = now + 10L;
         SoundSource soundSource = this.combatantTeam == CombatantTeam.NATURAL ? SoundSource.HOSTILE : SoundSource.PLAYERS;
         level.playSound(null, this.getX(), this.getEyeY(), this.getZ(), ModSounds.KNIFE_SWING, soundSource, 0.3F, 1.01F);
         boolean hurt = target.hurtServer(level, this.damageSources().mobAttack(this), 30.0F);
         if (hurt) {
            level.playSound(null, target.getX(), target.getEyeY(), target.getZ(), ModSounds.KNIFE_HIT_FLESH, soundSource, 1.0F, 1.4F);
         }
      }
   }

   private boolean prepareActiveWeapon(ServerLevel level, long now) {
      ItemStack active = this.activeStack();
      if (active.getItem() instanceof GunItem gun) {
         if (gun.ammo(active) > 0) {
            this.reloadCompleteGameTime = 0L;
            return true;
         } else if (gun.reserveAmmo(active) <= 0) {
            this.advanceToBackupWeapon();
            return true;
         } else if (this.reloadCompleteGameTime == 0L) {
            this.reloadCompleteGameTime = now + Math.max(1L, (long)Mth.ceil(gun.definition().reloadSeconds() * 20.0F));
            level.playSound(
               null, this.getX(), this.getEyeY(), this.getZ(), ModSounds.cue(gun.definition().id(), "reload_start"), SoundSource.PLAYERS, 0.9F, 1.0F
            );
            return false;
         } else if (now < this.reloadCompleteGameTime) {
            return false;
         } else {
            this.reloadCompleteGameTime = 0L;
            if (!gun.reloadOneUnit(active)) {
               this.advanceToBackupWeapon();
               return true;
            } else {
               return gun.ammo(active) > 0;
            }
         }
      } else {
         return true;
      }
   }

   private void advanceToBackupWeapon() {
      if (this.activeSlot != CombatantEntity.WeaponSlot.PRIMARY
         || this.pistolStack.isEmpty()
         || !(this.pistolStack.getItem() instanceof GunItem pistol && (pistol.ammo(this.pistolStack) > 0 || pistol.reserveAmmo(this.pistolStack) > 0))) {
         this.setActiveSlot(CombatantEntity.WeaponSlot.KNIFE);
      } else {
         this.setActiveSlot(CombatantEntity.WeaponSlot.PISTOL);
      }

      this.reloadCompleteGameTime = 0L;
      this.nextShotGameTime = 0L;
      this.burstPauseUntilGameTime = 0L;
      this.burstShots = 0;
   }

   private void tickTacticalMovement(ServerLevel level, LivingEntity target, long now, boolean hasLineOfSight) {
      boolean pressured = now < this.underFireUntilGameTime || this.getHealth() <= this.getMaxHealth() * 0.55F;
      if (pressured
         && hasLineOfSight
         && now >= this.nextCoverSearchGameTime
         && this.tacticalState != CombatantEntity.TacticalState.SEEK_COVER
         && this.tacticalState != CombatantEntity.TacticalState.HOLD_COVER) {
         this.nextCoverSearchGameTime = now + 45L;
         BlockPos cover = this.findCover(level, target);
         if (cover != null) {
            this.coverPosition = cover;
            this.peekPosition = null;
            this.tacticalState = CombatantEntity.TacticalState.SEEK_COVER;
         }
      }

      switch (this.tacticalState) {
         case ENGAGE:
         case REPOSITION:
            this.setShiftKeyDown(false);
            if (!hasLineOfSight) {
               this.tacticalState = CombatantEntity.TacticalState.REPOSITION;
               this.getNavigation().moveTo(target, 1.2);
               return;
            }

            this.tacticalState = CombatantEntity.TacticalState.ENGAGE;
            this.keepCombatDistance(target);
            break;
         case SEEK_COVER:
            if (this.coverPosition == null) {
               this.tacticalState = CombatantEntity.TacticalState.REPOSITION;
               return;
            }

            this.setShiftKeyDown(false);
            this.moveTo(this.coverPosition, 1.22);
            if (this.distanceToSqr(Vec3.atBottomCenterOf(this.coverPosition)) <= 2.0) {
               this.getNavigation().stop();
               this.setShiftKeyDown(true);
               this.peekPosition = this.findPeekPosition(level, target, this.coverPosition);
               this.tacticalState = CombatantEntity.TacticalState.HOLD_COVER;
               this.stateUntilGameTime = now + 8L + this.getRandom().nextInt(8);
            }

            return;
         case HOLD_COVER:
            this.getNavigation().stop();
            this.setShiftKeyDown(true);
            if (now >= this.stateUntilGameTime) {
               if (this.peekPosition != null) {
                  this.tacticalState = CombatantEntity.TacticalState.PEEK;
                  this.stateUntilGameTime = now + 24L + this.getRandom().nextInt(18);
               } else {
                  this.tacticalState = CombatantEntity.TacticalState.REPOSITION;
               }
            }

            return;
         case PEEK:
            this.setShiftKeyDown(false);
            if (this.peekPosition != null) {
               this.moveTo(this.peekPosition, 1.12);
            }

            if (now >= this.stateUntilGameTime || !this.isValidTarget(target)) {
               this.tacticalState = CombatantEntity.TacticalState.SEEK_COVER;
               this.peekPosition = null;
            }
         case KNIFE_RUSH:
      }
   }

   private void keepCombatDistance(LivingEntity target) {
      double distance = Math.sqrt(this.distanceToSqr(target));
      double preferredMin = this.activeSlot == CombatantEntity.WeaponSlot.PISTOL ? 7.0 : 11.0;
      double preferredMax = this.activeSlot == CombatantEntity.WeaponSlot.PISTOL ? 15.0 : 24.0;
      if (distance > preferredMax) {
         this.getNavigation().moveTo(target, 1.14);
      } else {
         this.getNavigation().stop();
         if (distance < preferredMin) {
            this.getMoveControl().strafe(-0.58F, this.strafeDirection * 0.42F);
         } else {
            if (this.getRandom().nextInt(36) == 0) {
               this.strafeDirection = -this.strafeDirection;
            }

            this.getMoveControl().strafe(0.05F, this.strafeDirection * 0.62F);
         }
      }
   }

   private BlockPos findCover(ServerLevel level, LivingEntity target) {
      BlockPos best = null;
      double bestScore = Double.POSITIVE_INFINITY;
      BlockPos origin = this.blockPosition();

      for (int attempt = 0; attempt < 32; attempt++) {
         int radius = 3 + this.getRandom().nextInt(8);
         int dx = this.getRandom().nextInt(radius * 2 + 1) - radius;
         int dz = this.getRandom().nextInt(radius * 2 + 1) - radius;
         BlockPos candidate = this.findStandPosition(level, origin.offset(dx, 0, dz));
         if (candidate != null && !this.hasClearBlockLine(level, this.eyeAt(candidate), target.getEyePosition())) {
            Path path = this.getNavigation().createPath(candidate, 0);
            if (path != null) {
               double targetDistance = Vec3.atCenterOf(candidate).distanceToSqr(target.position());
               if (!(targetDistance < 36.0) && !(targetDistance > 900.0)) {
                  double score = candidate.distSqr(origin) + Math.abs(Math.sqrt(targetDistance) - 15.0) * 2.5;
                  if (score < bestScore) {
                     bestScore = score;
                     best = candidate;
                  }
               }
            }
         }
      }

      return best;
   }

   private BlockPos findPeekPosition(ServerLevel level, LivingEntity target, BlockPos cover) {
      BlockPos best = null;
      double bestScore = Double.POSITIVE_INFINITY;

      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            if (dx != 0 || dz != 0) {
               BlockPos candidate = this.findStandPosition(level, cover.offset(dx, 0, dz));
               if (candidate != null && this.hasClearBlockLine(level, this.eyeAt(candidate), target.getEyePosition())) {
                  Path path = this.getNavigation().createPath(candidate, 0);
                  if (path != null) {
                     double score = candidate.distSqr(cover) + Vec3.atCenterOf(candidate).distanceToSqr(target.position()) * 0.01;
                     if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private BlockPos findStandPosition(ServerLevel level, BlockPos approximate) {
      for (int dy = 2; dy >= -2; dy--) {
         BlockPos position = approximate.offset(0, dy, 0);
         if (level.getBlockState(position.below()).blocksMotion()
            && level.getBlockState(position).getCollisionShape(level, position).isEmpty()
            && level.getBlockState(position.above()).getCollisionShape(level, position.above()).isEmpty()) {
            return position.immutable();
         }
      }

      return null;
   }

   private boolean hasClearBlockLine(ServerLevel level, Vec3 start, Vec3 end) {
      BlockHitResult hit = level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this));
      return hit.getType() == Type.MISS;
   }

   private Vec3 eyeAt(BlockPos position) {
      return Vec3.atBottomCenterOf(position).add(0.0, this.getEyeHeight(), 0.0);
   }

   private void moveTo(BlockPos position, double speed) {
      this.getNavigation().moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, speed);
   }

   private Vec3 chooseAimPoint(LivingEntity target) {
      double height = target.getBoundingBox().getYsize();

      float headChance = switch (this.combatantTier) {
         case TIER_1 -> 0.05F;
         case TIER_2 -> 0.13F;
         case TIER_3 -> 0.24F;
      };
      double fraction = this.getRandom().nextFloat() < headChance ? 0.88 : 0.6;
      return new Vec3(target.getX(), target.getBoundingBox().minY + height * fraction, target.getZ());
   }

   private void tickWithoutTarget(ServerLevel level, long now) {
      this.setAggressive(false);
      this.setShiftKeyDown(false);
      this.tacticalState = CombatantEntity.TacticalState.ENGAGE;
      this.coverPosition = null;
      this.peekPosition = null;
      this.shotPatternIndex = 0;
      this.burstShots = 0;
      if (this.combatantTeam == CombatantTeam.PLAYER) {
         Player player = level.players()
            .stream()
            .filter(candidatex -> candidatex.isAlive() && !candidatex.isSpectator())
            .filter(candidatex -> this.distanceToSqr(candidatex) <= 1600.0)
            .min(Comparator.comparingDouble(this::distanceToSqr))
            .orElse(null);
         if (player != null) {
            double distance = this.distanceToSqr(player);
            if (distance > 144.0) {
               this.getNavigation().moveTo(player, 1.1);
            } else if (distance < 9.0) {
               Vec3 away = this.position().subtract(player.position()).normalize().scale(3.0);
               this.getNavigation().moveTo(this.getX() + away.x, this.getY(), this.getZ() + away.z, 1.0);
            }

            return;
         }
      }

      if (now >= this.nextIdleMoveGameTime) {
         this.nextIdleMoveGameTime = now + 50L + this.getRandom().nextInt(70);
         BlockPos candidate = this.findStandPosition(level, this.blockPosition().offset(this.getRandom().nextInt(13) - 6, 0, this.getRandom().nextInt(13) - 6));
         if (candidate != null) {
            this.moveTo(candidate, 0.82);
         }
      }
   }

   private LivingEntity findTarget(ServerLevel level) {
      LivingEntity combatantTarget = level.getEntitiesOfClass(
            CombatantEntity.class, this.getBoundingBox().inflate(56.0), entity -> entity.isAlive() && this.isEnemy(entity)
         )
         .stream()
         .min(Comparator.comparingDouble(this::distanceToSqr))
         .orElse(null);
      if (this.combatantTeam == CombatantTeam.PLAYER) {
         return combatantTarget;
      } else {
         Player playerTarget = level.players()
            .stream()
            .filter(player -> this.isEnemy(player) && this.distanceToSqr(player) <= 3136.0)
            .min(Comparator.comparingDouble(this::distanceToSqr))
            .orElse(null);
         if (playerTarget == null) {
            return combatantTarget;
         } else {
            return (LivingEntity)(combatantTarget != null && !(this.distanceToSqr(playerTarget) < this.distanceToSqr(combatantTarget))
               ? combatantTarget
               : playerTarget);
         }
      }
   }

   private boolean isValidTarget(LivingEntity target) {
      return target != null && target.isAlive() && this.distanceToSqr(target) <= 4233.6 && this.isEnemy(target);
   }

   private boolean isEnemy(LivingEntity entity) {
      if (entity == null || entity == this || !entity.isAlive()) {
         return false;
      } else {
         return entity instanceof CombatantEntity combatant
            ? combatant.combatantTeam != this.combatantTeam
            : this.combatantTeam == CombatantTeam.NATURAL && entity instanceof Player player && !player.isCreative() && !player.isSpectator();
      }
   }

   private void onTargetAcquired(long now) {
      this.reactionUntilGameTime = now + switch (this.combatantTier) {
         case TIER_1 -> 12L;
         case TIER_2 -> 7L;
         case TIER_3 -> 4L;
      };
      this.tacticalState = CombatantEntity.TacticalState.REPOSITION;
      this.shotPatternIndex = 0;
      this.burstShots = 0;
   }

   private void equipFreshLoadout() {
      this.primaryStack = ItemStack.EMPTY;
      this.pistolStack = ItemStack.EMPTY;
      this.knifeStack = new ItemStack(ModItems.KNIFE);
      if (this.combatantTier == CombatantTier.TIER_1) {
         this.pistolStack = new ItemStack(ModItems.byId(this.combatantTeam == CombatantTeam.PLAYER ? "hkp2000" : "glock"));
         this.activeSlot = CombatantEntity.WeaponSlot.PISTOL;
      } else {
         this.primaryStack = new ItemStack(ModItems.byId(this.combatantTeam == CombatantTeam.PLAYER ? "m4a4" : "ak47"));
         if (this.combatantTier == CombatantTier.TIER_3) {
            this.pistolStack = new ItemStack(ModItems.byId(this.combatantTeam == CombatantTeam.PLAYER ? "hkp2000" : "glock"));
         }

         this.activeSlot = CombatantEntity.WeaponSlot.PRIMARY;
      }

      this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CS_HELMET));
      this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CS_KEVLAR));
      this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      this.setDropChance(EquipmentSlot.CHEST, 0.0F);
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
      this.setActiveSlot(this.activeSlot);
      this.setCustomName(
         this.getType().getDescription().copy().withStyle(this.combatantTeam == CombatantTeam.PLAYER ? ChatFormatting.AQUA : ChatFormatting.RED)
      );
      this.setCustomNameVisible(true);
   }

   private ItemStack activeStack() {
      return switch (this.activeSlot) {
         case PRIMARY -> this.primaryStack;
         case PISTOL -> this.pistolStack;
         case KNIFE -> this.knifeStack;
      };
   }

   private void setActiveSlot(CombatantEntity.WeaponSlot slot) {
      this.activeSlot = slot;
      this.setItemSlot(EquipmentSlot.MAINHAND, this.activeStack());
   }

   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("TacticalPrimary", ItemStack.OPTIONAL_CODEC, this.primaryStack);
      output.store("TacticalPistol", ItemStack.OPTIONAL_CODEC, this.pistolStack);
      output.store("TacticalKnife", ItemStack.OPTIONAL_CODEC, this.knifeStack);
      output.putString("TacticalActiveSlot", this.activeSlot.name());
   }

   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.primaryStack = input.read("TacticalPrimary", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
      this.pistolStack = input.read("TacticalPistol", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
      this.knifeStack = input.read("TacticalKnife", ItemStack.OPTIONAL_CODEC).orElseGet(() -> new ItemStack(ModItems.KNIFE));

      try {
         this.activeSlot = CombatantEntity.WeaponSlot.valueOf(input.getStringOr("TacticalActiveSlot", this.activeSlot.name()));
      } catch (IllegalArgumentException var3) {
         this.activeSlot = CombatantEntity.WeaponSlot.KNIFE;
      }

      this.setActiveSlot(this.activeSlot);
   }

   private static enum TacticalState {
      ENGAGE,
      SEEK_COVER,
      HOLD_COVER,
      PEEK,
      REPOSITION,
      KNIFE_RUSH;
   }

   private static enum WeaponSlot {
      PRIMARY,
      PISTOL,
      KNIFE;
   }
}
