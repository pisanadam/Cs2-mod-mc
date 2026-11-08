package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.server.GunServerController;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
   private ModNetworking() {
   }

   public static void initialize() {
      PayloadTypeRegistry.clientboundPlay().register(TracerPayload.TYPE, TracerPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(FirePayload.TYPE, FirePayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(ReloadPayload.TYPE, ReloadPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(
         FirePayload.TYPE, (payload, context) -> GunServerController.handleFire(context.player(), payload.aiming(), payload.burstMode(), payload.triggerHeld())
      );
      ServerPlayNetworking.registerGlobalReceiver(ReloadPayload.TYPE, (payload, context) -> GunServerController.handleReload(context.player()));
   }
}
