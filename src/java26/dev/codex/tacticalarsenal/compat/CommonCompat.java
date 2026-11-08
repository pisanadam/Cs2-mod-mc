package dev.codex.tacticalarsenal.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small per-Minecraft-version helpers shared by client and server code. */
public final class CommonCompat {
   private CommonCompat() {
   }

   /** Server-side damage, routed to the method each Minecraft version uses. */
   public static boolean hurt(net.minecraft.world.entity.Entity target, net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
      //#if MC >= 1.21.2
      return target.hurtServer(level, source, amount);
      //#else
      return target.hurt(source, amount);
      //#endif
   }

   //#if MC < 1.21.6
   public static net.minecraft.nbt.Tag saveStack(net.minecraft.world.item.ItemStack stack, net.minecraft.core.RegistryAccess registries) {
      //#if MC >= 1.20.5
      return stack.saveOptional(registries);
      //#else
      return stack.save(new net.minecraft.nbt.CompoundTag());
      //#endif
   }

   public static net.minecraft.world.item.ItemStack loadStack(net.minecraft.nbt.CompoundTag tag, String key, net.minecraft.core.RegistryAccess registries) {
      //#if MC >= 1.20.5
      return net.minecraft.world.item.ItemStack.parseOptional(registries, tag.getCompound(key));
      //#else
      return net.minecraft.world.item.ItemStack.of(tag.getCompound(key));
      //#endif
   }
   //#endif

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
