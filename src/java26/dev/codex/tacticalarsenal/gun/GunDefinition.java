package dev.codex.tacticalarsenal.gun;

import net.minecraft.util.Mth;

public record GunDefinition(
   String id,
   FireMode fireMode,
   WeaponCategory category,
   int projectiles,
   float damage,
   int magazineSize,
   int reserveAmount,
   boolean reserveAsMagazines,
   float cycleTimeSeconds,
   float alternateCycleTimeSeconds,
   boolean burstCapable,
   float burstCycleTimeSeconds,
   float burstShotIntervalSeconds,
   boolean revolver,
   float sourceRangeUnits,
   float rangeModifier,
   float headMultiplier,
   float armorRatio,
   float penetrationPower,
   GunDefinition.AccuracyProfile accuracy,
   GunDefinition.RecoilProfile recoil,
   boolean alternateModeByDefault,
   int zoomLevels,
   float zoomFov1,
   float zoomFov2,
   float zoomSeconds,
   boolean unzoomsAfterShot,
   boolean hideViewmodelWhenZoomed,
   boolean suppressed,
   float reloadSeconds,
   float reloadAnimationSeconds,
   float emptyReloadAnimationSeconds,
   float reloadInsertSeconds,
   float emptyReloadInsertSeconds,
   float deploySeconds
) {
   public static final float SOURCE_UNITS_PER_BLOCK = 52.493F;
   public static final float HEALTH_SCALE = 0.2F;

   public float roundsPerMinute() {
      return 60.0F / this.cycleTimeSeconds;
   }

   public long shotIntervalNanos() {
      return (long)(this.cycleTimeSeconds * 1.0E9);
   }

   public long revolverFireDelayNanos() {
      return this.revolver ? 200000000L : 0L;
   }

   public float maximumRange() {
      return this.sourceRangeUnits / 52.493F;
   }

   public float damageAtDistance(double blocks) {
      double sourceUnits = Math.max(0.0, blocks) * 52.493F;
      double falloff = Math.pow(this.rangeModifier, sourceUnits / 500.0);
      return (float)(this.damage * 0.2F * falloff);
   }

   public float armoredDamageFactor() {
      return this.armorRatio * 0.5F;
   }

   public int maximumReserveRounds() {
      return this.reserveAsMagazines ? this.reserveAmount * this.magazineSize : this.reserveAmount;
   }

   public boolean canScope() {
      return this.zoomLevels > 0;
   }

   public float zoomFov(int scopeLevel) {
      return scopeLevel >= 2 && this.zoomLevels >= 2 ? this.zoomFov2 : this.zoomFov1;
   }

   public boolean usesAlternateMode(boolean scoped) {
      return this.usesAlternateMode(scoped, false);
   }

   public boolean usesAlternateMode(boolean scoped, boolean alternateFireMode) {
      return this.alternateModeByDefault || scoped || alternateFireMode;
   }

   public int tracerFrequency(boolean scoped, boolean alternateFireMode) {
      return 1;
   }

   public float spread(boolean scoped) {
      return this.spread(scoped, false);
   }

   public float spread(boolean scoped, boolean alternateFireMode) {
      return this.accuracy.spread.select(this.usesAlternateMode(scoped, alternateFireMode));
   }

   public float standingInaccuracy(boolean scoped) {
      return this.standingInaccuracy(scoped, false);
   }

   public float standingInaccuracy(boolean scoped, boolean alternateFireMode) {
      return this.accuracy.stand.select(this.usesAlternateMode(scoped, alternateFireMode));
   }

   public float crouchingInaccuracy(boolean scoped) {
      return this.crouchingInaccuracy(scoped, false);
   }

   public float crouchingInaccuracy(boolean scoped, boolean alternateFireMode) {
      return this.accuracy.crouch.select(this.usesAlternateMode(scoped, alternateFireMode));
   }

   public float movementInaccuracy(boolean scoped) {
      return this.movementInaccuracy(scoped, false);
   }

   public float movementInaccuracy(boolean scoped, boolean alternateFireMode) {
      return this.accuracy.move.select(this.usesAlternateMode(scoped, alternateFireMode));
   }

   public float jumpInaccuracy(boolean scoped) {
      return this.jumpInaccuracy(scoped, false);
   }

   public float jumpInaccuracy(boolean scoped, boolean alternateFireMode) {
      return this.accuracy.jump.select(this.usesAlternateMode(scoped, alternateFireMode));
   }

   public float fireInaccuracy(boolean scoped) {
      return this.fireInaccuracy(scoped, false);
   }

   public float fireInaccuracy(boolean scoped, boolean alternateFireMode) {
      return this.accuracy.fire.select(this.usesAlternateMode(scoped, alternateFireMode));
   }

   public float maximumMoveSpeed(boolean scoped) {
      return this.accuracy.maxSpeed.select(this.usesAlternateMode(scoped));
   }

   public float recoverySeconds(boolean crouching, int shotIndex) {
      float start = crouching ? this.accuracy.recoveryCrouch : this.accuracy.recoveryStand;
      float end = crouching ? this.accuracy.recoveryCrouchFinal : this.accuracy.recoveryStandFinal;
      float progress = Mth.clamp(
         (float)(shotIndex - this.accuracy.recoveryTransitionStart) / Math.max(1, this.accuracy.recoveryTransitionEnd - this.accuracy.recoveryTransitionStart),
         0.0F,
         1.0F
      );
      return Mth.lerp(progress, start, end);
   }

   public float recoilPitchDegrees(boolean scoped, int shotIndex) {
      boolean alternate = this.usesAlternateMode(scoped);
      float magnitude = this.recoil.magnitude.select(alternate);
      float variance = this.recoil.magnitudeVariance.select(alternate);
      float varied = magnitude + noise(this.recoil.seed, shotIndex, 17) * variance;
      float automaticGrowth = this.fireMode == FireMode.AUTOMATIC ? 1.0F + Math.min(shotIndex, 12) * 0.022F : 1.0F;
      return Math.max(0.15F, varied / 30.0F) * automaticGrowth;
   }

   public float recoilYawDegrees(boolean scoped, int shotIndex) {
      boolean alternate = this.usesAlternateMode(scoped);
      float angleVariance = this.recoil.angleVariance.select(alternate);
      float magnitude = this.recoil.magnitude.select(alternate) / 30.0F;
      float[] spray = new float[]{
         0.02F,
         -0.05F,
         -0.1F,
         -0.18F,
         -0.27F,
         -0.34F,
         -0.31F,
         -0.21F,
         -0.06F,
         0.12F,
         0.28F,
         0.39F,
         0.44F,
         0.36F,
         0.22F,
         0.04F,
         -0.15F,
         -0.31F,
         -0.39F,
         -0.28F,
         -0.09F,
         0.14F,
         0.31F,
         0.4F
      };
      float seededJitter = noise(this.recoil.seed, shotIndex, 43) * 0.07F;
      float pattern = this.fireMode == FireMode.AUTOMATIC ? spray[Math.floorMod(shotIndex, spray.length)] : noise(this.recoil.seed, shotIndex, 71) * 0.34F;
      return (pattern + seededJitter) * magnitude * Math.max(0.2F, angleVariance / 70.0F);
   }

   private static float noise(int seed, int index, int salt) {
      long value = Integer.toUnsignedLong(seed);
      value ^= Integer.toUnsignedLong(index * -1640531527);
      value ^= Integer.toUnsignedLong(salt * -2048144789);
      value ^= value >>> 16;
      value *= 2146121005L;
      value ^= value >>> 15;
      value *= 2221713035L;
      value ^= value >>> 16;
      return (float)(value & 16777215L) / 8388607.5F - 1.0F;
   }

   public record AccuracyProfile(
      GunDefinition.ModeValue spread,
      GunDefinition.ModeValue crouch,
      GunDefinition.ModeValue stand,
      GunDefinition.ModeValue jump,
      GunDefinition.ModeValue land,
      GunDefinition.ModeValue fire,
      GunDefinition.ModeValue move,
      GunDefinition.ModeValue maxSpeed,
      float jumpInitial,
      float jumpApex,
      float recoveryCrouch,
      float recoveryStand,
      float recoveryCrouchFinal,
      float recoveryStandFinal,
      int recoveryTransitionStart,
      int recoveryTransitionEnd
   ) {
   }

   public record ModeValue(float primary, float alternate) {
      public static GunDefinition.ModeValue same(float value) {
         return new GunDefinition.ModeValue(value, value);
      }

      public float select(boolean useAlternate) {
         return useAlternate ? this.alternate : this.primary;
      }
   }

   public record RecoilProfile(int seed, GunDefinition.ModeValue angleVariance, GunDefinition.ModeValue magnitude, GunDefinition.ModeValue magnitudeVariance) {
   }
}
