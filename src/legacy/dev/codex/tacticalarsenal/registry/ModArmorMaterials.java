package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
//#if MC >= 1.20.5
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//#endif

/**
 * The CS2 helmet/kevlar material: durability multiplier 15, 10 armor on helmet and chestplate,
 * enchantability 9, iron equip sound, no toughness or knockback resistance, repaired with iron
 * (same values as the Minecraft 26.x version of the mod).
 */
public final class ModArmorMaterials {
   public static final int DURABILITY_MULTIPLIER = 15;
   //#if MC >= 1.20.5
   public static final Holder<ArmorMaterial> CS2_EQUIPMENT = Registry.registerForHolder(
      BuiltInRegistries.ARMOR_MATERIAL,
      TacticalArsenal.id("cs2_equipment"),
      new ArmorMaterial(
         defense(),
         9,
         SoundEvents.ARMOR_EQUIP_IRON,
         () -> Ingredient.of(Items.IRON_INGOT),
         List.of(new ArmorMaterial.Layer(TacticalArsenal.id("cs2_equipment"))),
         0.0F,
         0.0F
      )
   );

   private static EnumMap<ArmorItem.Type, Integer> defense() {
      EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
      map.put(ArmorItem.Type.BOOTS, 0);
      map.put(ArmorItem.Type.LEGGINGS, 0);
      map.put(ArmorItem.Type.CHESTPLATE, 10);
      map.put(ArmorItem.Type.HELMET, 10);
      map.put(ArmorItem.Type.BODY, 0);
      return map;
   }
   //#else
   public static final ArmorMaterial CS2_EQUIPMENT = new ArmorMaterial() {
      @Override
      public int getDurabilityForType(ArmorItem.Type type) {
         int base = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
         };
         return base * DURABILITY_MULTIPLIER;
      }

      @Override
      public int getDefenseForType(ArmorItem.Type type) {
         return type == ArmorItem.Type.HELMET || type == ArmorItem.Type.CHESTPLATE ? 10 : 0;
      }

      @Override
      public int getEnchantmentValue() {
         return 9;
      }

      @Override
      public SoundEvent getEquipSound() {
         return SoundEvents.ARMOR_EQUIP_IRON;
      }

      @Override
      public Ingredient getRepairIngredient() {
         return Ingredient.of(Items.IRON_INGOT);
      }

      @Override
      public String getName() {
         return "tactical_arsenal:cs2_equipment";
      }

      @Override
      public float getToughness() {
         return 0.0F;
      }

      @Override
      public float getKnockbackResistance() {
         return 0.0F;
      }
   };
   //#endif

   private ModArmorMaterials() {
   }

   static void initialize() {
   }
}
