package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.gun.GunDefinition;
import dev.codex.tacticalarsenal.gun.GunDefinitions;
import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.knife.KnifeItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
//#if MC >= 1.20.5
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
//#endif

/**
 * Item registration for Minecraft 1.21.1 / 1.20.1. Same items, ids, stack sizes, durability,
 * attributes and creative tab as the 26.x version; only the registration API differs.
 */
public final class ModItems {
   private static final Map<String, GunItem> GUNS = new LinkedHashMap<>();
   private static final Map<String, KnifeItem> KNIVES = new LinkedHashMap<>();
   public static final Item CS_HELMET = registerArmor("cs_helmet", ArmorItem.Type.HELMET);
   public static final Item CS_KEVLAR = registerArmor("cs_kevlar", ArmorItem.Type.CHESTPLATE);
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
         FabricItemGroup.builder()
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
      Properties properties = new Properties().stacksTo(1).durability(definition.magazineSize() + 1);
      //#if MC >= 1.20.5
      properties = properties.component(
         DataComponents.ATTRIBUTE_MODIFIERS,
         ItemAttributeModifiers.builder()
            .add(
               Attributes.MOVEMENT_SPEED,
               new AttributeModifier(TacticalArsenal.id("held_weapon_speed"), definition.maximumMoveSpeed(false) / 250.0F - 1.0F, Operation.ADD_MULTIPLIED_TOTAL),
               EquipmentSlotGroup.MAINHAND
            )
            .build()
            .withTooltip(false)
      );
      //#endif
      GunItem item = new GunItem(properties, definition);
      Registry.register(BuiltInRegistries.ITEM, TacticalArsenal.id(definition.id()), item);
      GUNS.put(definition.id(), item);
      return item;
   }

   private static KnifeItem registerKnife(String id) {
      Properties properties = new Properties().stacksTo(1);
      //#if MC >= 1.20.5
      properties = properties.attributes(
         ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 29.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.5, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build()
      );
      //#endif
      KnifeItem item = new KnifeItem(properties, id);
      Registry.register(BuiltInRegistries.ITEM, TacticalArsenal.id(id), item);
      KNIVES.put(id, item);
      return item;
   }

   private static Item registerArmor(String id, ArmorItem.Type armorType) {
      //#if MC >= 1.20.5
      Item item = new ArmorItem(
         ModArmorMaterials.CS2_EQUIPMENT, armorType, new Properties().durability(armorType.getDurability(ModArmorMaterials.DURABILITY_MULTIPLIER))
      );
      //#else
      Item item = new ArmorItem(ModArmorMaterials.CS2_EQUIPMENT, armorType, new Properties());
      //#endif
      return (Item)Registry.register(BuiltInRegistries.ITEM, TacticalArsenal.id(id), item);
   }

   static {
      GunDefinitions.ALL.forEach(ModItems::registerGun);
   }
}
