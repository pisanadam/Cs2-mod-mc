package dev.codex.tacticalarsenal.knife;

import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;

public final class KnifeItem extends Item {
   public static final float ATTACK_DAMAGE = 30.0F;
   public static final float ATTACKS_PER_SECOND = 2.5F;
   public static final int MISS_COOLDOWN_TICKS = 8;
   public static final int HIT_COOLDOWN_TICKS = 10;
   public static final float ATTACK_CLIP_SECONDS = 1.1667F;
   private static final Set<String> IDS = Set.of("knife", "karambit", "butterfly");
   private final String id;

   public KnifeItem(Properties properties, String id) {
      super(properties);
      if (!IDS.contains(id)) {
         throw new IllegalArgumentException("Unsupported CS2 knife id: " + id);
      } else {
         this.id = id;
      }
   }

   public String id() {
      return this.id;
   }

   public static boolean isKnifeId(String id) {
      return IDS.contains(id);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
      builder.accept(Component.translatable("tooltip.tactical_arsenal.knife_damage", new Object[]{30}));
      builder.accept(Component.translatable("tooltip.tactical_arsenal.knife_controls"));
   }
}
