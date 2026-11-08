package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import dev.codex.tacticalarsenal.npc.CombatantTeam;
import dev.codex.tacticalarsenal.npc.CombatantTier;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item.Properties;

/** Combatant entity types and spawn eggs for Minecraft 1.21.1 / 1.20.1 (same ids, sizes and tracking as 26.x). */
public final class ModCombatants {
   /** Colours of the villager spawn egg, whose look the 26.x spawn eggs use. */
   private static final int EGG_BACKGROUND = 0x563C33;
   private static final int EGG_HIGHLIGHT = 0xBD8B72;
   private static final List<EntityType<CombatantEntity>> TYPES = new ArrayList<>();
   private static final List<Item> SPAWN_EGGS = new ArrayList<>();
   public static final EntityType<CombatantEntity> TEAM_PLAYER_TIER_1 = registerType("team_player_tier_1", CombatantTeam.PLAYER, CombatantTier.TIER_1);
   public static final EntityType<CombatantEntity> TEAM_PLAYER_TIER_2 = registerType("team_player_tier_2", CombatantTeam.PLAYER, CombatantTier.TIER_2);
   public static final EntityType<CombatantEntity> TEAM_PLAYER_TIER_3 = registerType("team_player_tier_3", CombatantTeam.PLAYER, CombatantTier.TIER_3);
   public static final EntityType<CombatantEntity> TEAM_NATURAL_TIER_1 = registerType("team_natural_tier_1", CombatantTeam.NATURAL, CombatantTier.TIER_1);
   public static final EntityType<CombatantEntity> TEAM_NATURAL_TIER_2 = registerType("team_natural_tier_2", CombatantTeam.NATURAL, CombatantTier.TIER_2);
   public static final EntityType<CombatantEntity> TEAM_NATURAL_TIER_3 = registerType("team_natural_tier_3", CombatantTeam.NATURAL, CombatantTier.TIER_3);
   public static final Item TEAM_PLAYER_TIER_1_SPAWN_EGG = registerSpawnEgg("team_player_tier_1_spawn_egg", TEAM_PLAYER_TIER_1);
   public static final Item TEAM_PLAYER_TIER_2_SPAWN_EGG = registerSpawnEgg("team_player_tier_2_spawn_egg", TEAM_PLAYER_TIER_2);
   public static final Item TEAM_PLAYER_TIER_3_SPAWN_EGG = registerSpawnEgg("team_player_tier_3_spawn_egg", TEAM_PLAYER_TIER_3);
   public static final Item TEAM_NATURAL_TIER_1_SPAWN_EGG = registerSpawnEgg("team_natural_tier_1_spawn_egg", TEAM_NATURAL_TIER_1);
   public static final Item TEAM_NATURAL_TIER_2_SPAWN_EGG = registerSpawnEgg("team_natural_tier_2_spawn_egg", TEAM_NATURAL_TIER_2);
   public static final Item TEAM_NATURAL_TIER_3_SPAWN_EGG = registerSpawnEgg("team_natural_tier_3_spawn_egg", TEAM_NATURAL_TIER_3);
   private static final ResourceKey<CreativeModeTab> ARSENAL_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TacticalArsenal.id("arsenal"));

   private ModCombatants() {
   }

   public static void initialize() {
      for (EntityType<CombatantEntity> type : TYPES) {
         FabricDefaultAttributeRegistry.register(type, CombatantEntity.createAttributes());
      }

      ItemGroupEvents.modifyEntriesEvent(ARSENAL_TAB).register(entries -> {
         for (Item egg : SPAWN_EGGS) {
            entries.accept(new ItemStack(egg));
         }
      });
   }

   public static List<EntityType<CombatantEntity>> playerTypes() {
      return List.of(TEAM_PLAYER_TIER_1, TEAM_PLAYER_TIER_2, TEAM_PLAYER_TIER_3);
   }

   public static List<EntityType<CombatantEntity>> naturalTypes() {
      return List.of(TEAM_NATURAL_TIER_1, TEAM_NATURAL_TIER_2, TEAM_NATURAL_TIER_3);
   }

   private static EntityType<CombatantEntity> registerType(String id, CombatantTeam team, CombatantTier tier) {
      Builder<CombatantEntity> builder = Builder.<CombatantEntity>of((entityType, level) -> new CombatantEntity(entityType, level, team, tier), MobCategory.MISC)
         .sized(0.6F, 1.95F)
         //#if MC >= 1.20.5
         .eyeHeight(1.74F)
         //#endif
         .clientTrackingRange(10)
         .updateInterval(2);
      EntityType<CombatantEntity> type = builder.build(TacticalArsenal.id(id).toString());
      Registry.register(BuiltInRegistries.ENTITY_TYPE, TacticalArsenal.id(id), type);
      TYPES.add(type);
      return type;
   }

   private static Item registerSpawnEgg(String id, EntityType<CombatantEntity> type) {
      Item item = new SpawnEggItem(type, EGG_BACKGROUND, EGG_HIGHLIGHT, new Properties());
      Registry.register(BuiltInRegistries.ITEM, TacticalArsenal.id(id), item);
      SPAWN_EGGS.add(item);
      return item;
   }
}
