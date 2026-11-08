package dev.codex.tacticalarsenal.client.compat;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Snapshot of the 26.x AvatarRenderState fields the mod reads, for Minecraft 1.21.1 / 1.20.1. */
public final class AvatarRenderState {
   public int id = -1;
   public HumanoidArm mainArm = HumanoidArm.RIGHT;
   private ItemStack mainHand = ItemStack.EMPTY;

   public static AvatarRenderState of(LivingEntity entity) {
      AvatarRenderState state = new AvatarRenderState();
      state.id = entity.getId();
      state.mainArm = entity.getMainArm();
      state.mainHand = entity.getMainHandItem();
      return state;
   }

   public ItemStack getMainHandItemStack() {
      return this.mainHand;
   }
}
