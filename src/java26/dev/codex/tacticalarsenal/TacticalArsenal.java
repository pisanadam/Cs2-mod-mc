package dev.codex.tacticalarsenal;

import dev.codex.tacticalarsenal.gun.GunDefinitions;
import dev.codex.tacticalarsenal.knife.KnifeCombatController;
import dev.codex.tacticalarsenal.network.ModNetworking;
import dev.codex.tacticalarsenal.registry.ModCombatSounds;
import dev.codex.tacticalarsenal.registry.ModCombatants;
import dev.codex.tacticalarsenal.registry.ModItems;
import dev.codex.tacticalarsenal.registry.ModSounds;
import dev.codex.tacticalarsenal.server.GunServerController;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TacticalArsenal implements ModInitializer {
   public static final String MOD_ID = "tactical_arsenal";
   public static final Logger LOGGER = LoggerFactory.getLogger("tactical_arsenal");

   public void onInitialize() {
      ModItems.initialize();
      ModSounds.initialize();
      ModCombatSounds.initialize();
      ModCombatants.initialize();
      ModNetworking.initialize();
      GunServerController.initialize();
      KnifeCombatController.initialize();
      LOGGER.info("Tactical Arsenal loaded with {} CS2-profile weapons, 3 knives and 6 tactical combatants", GunDefinitions.ALL.size());
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("tactical_arsenal", path);
   }
}
