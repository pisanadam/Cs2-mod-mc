package dev.codex.tacticalarsenal.client.compat;

/** Minecraft 1.20.1 has no DeltaTracker; this carries the frame's partial tick the same way. */
public final class DeltaTracker {
   private final float partialTick;

   private DeltaTracker(float partialTick) {
      this.partialTick = partialTick;
   }

   public static DeltaTracker of(float partialTick) {
      return new DeltaTracker(partialTick);
   }

   public float getGameTimeDeltaPartialTick(boolean runsNormally) {
      return this.partialTick;
   }
}
