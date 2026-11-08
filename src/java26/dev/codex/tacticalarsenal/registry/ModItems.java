package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunDefinitions;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.ItemAttributeModifiers.Display;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;

public final class ModItems {
   private static final Map<String, GunItem> GUNS = new LinkedHashMap<>();
   private static final Map<String, KnifeItem> KNIVES = new LinkedHashMap<>();
   public static final Item CS_HELMET = registerArmor("cs_helmet", ArmorType.HELMET);
   public static final Item CS_KEVLAR = registerArmor("cs_kevlar", ArmorType.CHESTPLATE);
   public static final KnifeItem KNIFE = registerKnife("knife");
   public static final KnifeItem KARAMBIT = registerKnife("karambit");
   public static final KnifeItem BUTTERFLY = registerKnife("butterfly");
   private static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TacticalArsenal.id("arsenal"));

   private ModItems() {
   }

   public static void initialize() {
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tactical_arsenal.arsenal"))
            .icon(() -> new ItemStack((ItemLike)GUNS.get("ak47")))
            .displayItems((context, output) -> {
               GUNS.values().forEach(output::accept);
               KNIVES.values().forEach(output::accept);
               output.accept(CS_HELMET);
               output.accept(CS_KEVLAR);
            })
            .build()
      );
   }

   public static GunItem byId(String id) {
      return GUNS.get(id);
   }

   public static KnifeItem knifeById(String id) {
      return KNIVES.get(id);
   }

   private static GunItem registerGun(GunDefinition definition) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TacticalArsenal.id(definition.id()));
      Properties properties = new Properties()
         .setId(key)
         .stacksTo(1)
         .durability(definition.magazineSize() + 1)
         .component(
            DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.builder()
               .add(
                  Attributes.MOVEMENT_SPEED,
                  new AttributeModifier(
                     TacticalArsenal.id("held_weapon_speed"), definition.maximumMoveSpeed(false) / 250.0F - 1.0F, Operation.ADD_MULTIPLIED_TOTAL
                  ),
                  EquipmentSlotGroup.MAINHAND,
                  Display.hidden()
               )
               .build()
         );
      GunItem item = new GunItem(properties, definition);
      Registry.register(BuiltInRegistries.ITEM, key, item);
      GUNS.put(definition.id(), item);
      return item;
   }

   private static KnifeItem registerKnife(String id) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TacticalArsenal.id(id));
      Properties properties = new Properties()
         .setId(key)
         .stacksTo(1)
         .component(DataComponents.WEAPON, new Weapon(0))
         .component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, 8))
         .attributes(
            ItemAttributeModifiers.builder()
               .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 29.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
               .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.5, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
               .build()
         );
      KnifeItem item = new KnifeItem(properties, id);
      Registry.register(BuiltInRegistries.ITEM, key, item);
      KNIVES.put(id, item);
      return item;
   }

   private static Item registerArmor(String id, ArmorType armorType) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TacticalArsenal.id(id));
      Item item = new Item(new Properties().setId(key).humanoidArmor(ModArmorMaterials.CS2_EQUIPMENT, armorType));
      return (Item)Registry.register(BuiltInRegistries.ITEM, key, item);
   }

   static {
      GunDefinitions.ALL.forEach(ModItems::registerGun);
   }
}
