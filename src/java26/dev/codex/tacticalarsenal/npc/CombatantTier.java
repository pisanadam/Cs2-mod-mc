package dev.codex.tacticalarsenal.npc;

public enum CombatantTier {
   TIER_1(1),
   TIER_2(2),
   TIER_3(3);

   private final int level;

   private CombatantTier(int level) {
      this.level = level;
   }

   public int level() {
      return this.level;
   }
}
