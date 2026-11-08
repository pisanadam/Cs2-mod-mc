package dev.codex.tacticalarsenal.gun;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class GunItem extends Item {
   private static final String RESERVE_AMMO_KEY = "tactical_arsenal_reserve_ammo";
   private final GunDefinition definition;

   public GunItem(Properties properties, GunDefinition definition) {
      super(properties);
      this.definition = definition;
   }

   public GunDefinition definition() {
      return this.definition;
   }

   public int ammo(ItemStack stack) {
      return Mth.clamp(this.definition.magazineSize() - stack.getDamageValue(), 0, this.definition.magazineSize());
   }

   public boolean consumeRound(ItemStack stack) {
      int ammo = this.ammo(stack);
      if (ammo <= 0) {
         return false;
      } else {
         stack.setDamageValue(this.definition.magazineSize() - ammo + 1);
         return true;
      }
   }

   public int reserveAmmo(ItemStack stack) {
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
         .copyTag()
         .getInt("tactical_arsenal_reserve_ammo")
         .map(value -> Mth.clamp(value, 0, this.definition.reserveAmount()))
         .orElse(this.definition.reserveAmount());
   }

   public int plannedReloadUnits(ItemStack stack) {
      int missing = this.definition.magazineSize() - this.ammo(stack);
      if (missing <= 0) {
         return 0;
      } else {
         return this.definition.reserveAsMagazines() ? Math.min(1, this.reserveAmmo(stack)) : Math.min(missing, this.reserveAmmo(stack));
      }
   }

   public boolean reloadOneUnit(ItemStack stack) {
      int reserve = this.reserveAmmo(stack);
      if (reserve > 0 && this.ammo(stack) < this.definition.magazineSize()) {
         if (this.definition.reserveAsMagazines()) {
            stack.setDamageValue(0);
         } else {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
         }

         CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("tactical_arsenal_reserve_ammo", reserve - 1));
         return true;
      } else {
         return false;
      }
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      return InteractionResult.CONSUME;
   }

   public boolean isBarVisible(ItemStack stack) {
      return this.ammo(stack) < this.definition.magazineSize();
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * this.ammo(stack) / this.definition.magazineSize());
   }

   public int getBarColor(ItemStack stack) {
      float ratio = (float)this.ammo(stack) / this.definition.magazineSize();
      return Mth.hsvToRgb(ratio * 0.33F, 0.95F, 1.0F);
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
      builder.accept(
         Component.translatable(
            "tooltip.tactical_arsenal.ammo",
            new Object[]{
               this.ammo(stack),
               this.definition.magazineSize(),
               this.reserveAmmo(stack),
               Component.translatable(
                  this.definition.reserveAsMagazines() ? "tooltip.tactical_arsenal.reserve_magazines" : "tooltip.tactical_arsenal.reserve_rounds"
               )
            }
         )
      );
      builder.accept(Component.translatable("tooltip.tactical_arsenal.damage", new Object[]{Math.round(this.definition.damage())}));
      builder.accept(
         Component.translatable("tooltip.tactical_arsenal.armor_penetration", new Object[]{Math.round(this.definition.armoredDamageFactor() * 100.0F)})
      );
      builder.accept(Component.translatable("tooltip.tactical_arsenal.rpm", new Object[]{Math.round(this.definition.roundsPerMinute())}));
      String controls = this.definition.canScope()
         ? "tooltip.tactical_arsenal.controls_scoped"
         : (this.definition.burstCapable() ? "tooltip.tactical_arsenal.controls_burst" : "tooltip.tactical_arsenal.controls");
      builder.accept(Component.translatable(controls));
   }
}
