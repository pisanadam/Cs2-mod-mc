package dev.codex.tacticalarsenal.gun;

import java.util.function.Consumer;
//#if MC >= 1.20.5
import net.minecraft.core.component.DataComponents;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
//#if MC >= 1.20.5
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
//#endif
//#if MC >= 1.21.5
import net.minecraft.world.item.component.TooltipDisplay;
//#endif
import net.minecraft.world.level.Level;

public final class GunItem extends Item {
   private static final String RESERVE_AMMO_KEY = "tactical_arsenal_reserve_ammo";
   private final GunDefinition definition;

   public GunItem(Properties properties, GunDefinition definition) {
      super(properties);
      this.definition = definition;
   }

   //#if MC < 1.20.5
   private static final java.util.UUID HELD_WEAPON_SPEED_UUID = java.util.UUID.nameUUIDFromBytes(
      "tactical_arsenal:held_weapon_speed".getBytes(java.nio.charset.StandardCharsets.UTF_8)
   );
   private com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> mainHandModifiers;

   /** Before 1.20.5 there are no item attribute components: same held movement-speed modifier as 26.x. */
   @Override
   public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(
      net.minecraft.world.entity.EquipmentSlot slot
   ) {
      if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
         return super.getDefaultAttributeModifiers(slot);
      }

      if (this.mainHandModifiers == null) {
         this.mainHandModifiers = com.google.common.collect.ImmutableMultimap.of(
            net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(
               HELD_WEAPON_SPEED_UUID,
               "tactical_arsenal:held_weapon_speed",
               this.definition.maximumMoveSpeed(false) / 250.0F - 1.0F,
               net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL
            )
         );
      }

      return this.mainHandModifiers;
   }
   //#endif

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
      //#if MC >= 1.21.5
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY))
         .copyTag()
         .getInt("tactical_arsenal_reserve_ammo")
         .map(value -> Mth.clamp(value, 0, this.definition.reserveAmount()))
         .orElse(this.definition.reserveAmount());
      //#else
      //#if MC >= 1.20.5
      net.minecraft.nbt.CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      //#else
      net.minecraft.nbt.CompoundTag tag = stack.getTag() == null ? new net.minecraft.nbt.CompoundTag() : stack.getTag();
      //#endif
      return tag.contains("tactical_arsenal_reserve_ammo", net.minecraft.nbt.Tag.TAG_INT)
         ? Mth.clamp(tag.getInt("tactical_arsenal_reserve_ammo"), 0, this.definition.reserveAmount())
         : this.definition.reserveAmount();
      //#endif
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

         //#if MC >= 1.20.5
         CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("tactical_arsenal_reserve_ammo", reserve - 1));
         //#else
         stack.getOrCreateTag().putInt("tactical_arsenal_reserve_ammo", reserve - 1);
         //#endif
         return true;
      } else {
         return false;
      }
   }

   //#if MC >= 1.21.2
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      return InteractionResult.CONSUME;
   }
   //#else
   public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
   }
   //#endif

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

   //#if MC >= 1.21.5
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
   //#elif MC >= 1.20.5
   public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
      Consumer<Component> builder = lines::add;
   //#else
   public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flag) {
      Consumer<Component> builder = lines::add;
   //#endif
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
