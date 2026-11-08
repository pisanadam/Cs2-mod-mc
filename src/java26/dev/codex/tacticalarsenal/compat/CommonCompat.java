package dev.codex.tacticalarsenal.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small per-Minecraft-version helpers shared by client and server code. */
public final class CommonCompat {
   private CommonCompat() {
   }

   /** Same rule as BlockState#blocksMotion in Minecraft 26.2. */
   public static boolean blocksMotion(BlockState state) {
      //#if MC >= 26.3
      Block block = state.getBlock();
      return block != Blocks.COBWEB && block != Blocks.BAMBOO_SAPLING && state.isSolid();
      //#else
      return state.blocksMotion();
      //#endif
   }
}
