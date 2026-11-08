package dev.codex.tacticalarsenal.client;

import dev.codex.tacticalarsenal.client.render.CombatantRenderer;
import dev.codex.tacticalarsenal.registry.ModCombatants;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ModCombatantsClient {
   private ModCombatantsClient() {
   }

   public static void initialize() {
      ModCombatants.playerTypes().forEach(type -> EntityRendererRegistry.register(type, CombatantRenderer::player));
      ModCombatants.naturalTypes().forEach(type -> EntityRendererRegistry.register(type, CombatantRenderer::natural));
   }
}
