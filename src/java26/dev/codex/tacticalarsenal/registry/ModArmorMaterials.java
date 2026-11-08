package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
   public static final ResourceKey<EquipmentAsset> CS2_EQUIPMENT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, TacticalArsenal.id("cs2_equipment"));
   public static final ArmorMaterial CS2_EQUIPMENT = new ArmorMaterial(
      15, ArmorMaterials.makeDefense(0, 0, 10, 10, 0), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, ItemTags.REPAIRS_IRON_ARMOR, CS2_EQUIPMENT_ASSET
   );

   private ModArmorMaterials() {
   }
}
