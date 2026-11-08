package dev.codex.tacticalarsenal.client;

import dev.codex.tacticalarsenal.client.compat.ClientCompat;
import dev.codex.tacticalarsenal.client.animation.GunReloadAnimationLibrary;
import dev.codex.tacticalarsenal.client.render.Cs2ArmorRenderer;
import dev.codex.tacticalarsenal.client.render.Cs2InventoryItemModels;
import dev.codex.tacticalarsenal.client.render.Cs2MeshManager;
import dev.codex.tacticalarsenal.client.render.GunHud;
import dev.codex.tacticalarsenal.client.render.TracerRenderer;
import dev.codex.tacticalarsenal.client.screen.Cs2SettingsScreen;
import dev.codex.tacticalarsenal.client.settings.TacticalArsenalClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;

public final class TacticalArsenalClient implements ClientModInitializer {
   private static boolean firstRunScreenShownThisSession;

   public void onInitializeClient() {
      TacticalArsenalClientConfig.initialize();
      GunReloadAnimationLibrary.initialize();
      Cs2InventoryItemModels.initialize();
      Cs2MeshManager.initialize();
      Cs2ArmorRenderer.initialize();
      ModCombatantsClient.initialize();
      GunClientController.initialize();
      GunHud.initialize();
      TracerRenderer.initialize();
      ClientTickEvents.END_CLIENT_TICK
         .register(
            (EndTick)client -> {
               if (!firstRunScreenShownThisSession
                  && TacticalArsenalClientConfig.needsFirstRunSetup()
                  && client.player != null
                  && client.level != null
                  && ClientCompat.screen(client) == null) {
                  firstRunScreenShownThisSession = true;
                  ClientCompat.showScreen(client, new Cs2SettingsScreen(null, true));
               }
            }
         );
   }
}
