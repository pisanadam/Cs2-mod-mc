package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.server.GunServerController;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Minecraft 1.20.1 packet registration (packet types register themselves on creation). */
public final class ModNetworking {
   private ModNetworking() {
   }

   public static void initialize() {
      ServerPlayNetworking.registerGlobalReceiver(
         FirePayload.TYPE, (payload, player, responseSender) -> GunServerController.handleFire(player, payload.aiming(), payload.burstMode(), payload.triggerHeld())
      );
      ServerPlayNetworking.registerGlobalReceiver(ReloadPayload.TYPE, (payload, player, responseSender) -> GunServerController.handleReload(player));
   }
}
