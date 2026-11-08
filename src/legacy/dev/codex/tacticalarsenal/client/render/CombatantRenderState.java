package dev.codex.tacticalarsenal.client.render;

import dev.codex.tacticalarsenal.npc.CombatantEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 1.21.1 / 1.20.1 has no entity render states; this snapshot carries the same fields the
 * 26.x CombatantRenderState gave the combatant renderers.
 */
final class CombatantRenderState {
   int entityId = -1;
   HumanoidArm mainArm = HumanoidArm.RIGHT;
   boolean isCrouching;
   boolean isInvisible;
   boolean isInvisibleToPlayer;
   private ItemStack mainHand = ItemStack.EMPTY;

   static CombatantRenderState of(CombatantEntity entity) {
      CombatantRenderState state = new CombatantRenderState();
      state.entityId = entity.getId();
      state.mainArm = entity.getMainArm();
      state.isCrouching = entity.isCrouching();
      state.isInvisible = entity.isInvisible();
      state.isInvisibleToPlayer = Minecraft.getInstance().player != null && entity.isInvisibleTo(Minecraft.getInstance().player);
      state.mainHand = entity.getMainHandItem();
      return state;
   }

   ItemStack getMainHandItemStack() {
      return this.mainHand;
   }
}
