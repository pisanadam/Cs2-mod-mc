package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record FirePayload(boolean aiming, boolean burstMode, boolean triggerHeld) implements CustomPacketPayload {
   public static final Type<FirePayload> TYPE = new Type(TacticalArsenal.id("fire"));
   public static final StreamCodec<RegistryFriendlyByteBuf, FirePayload> CODEC = CustomPacketPayload.codec((payload, buffer) -> {
      buffer.writeBoolean(payload.aiming);
      buffer.writeBoolean(payload.burstMode);
      buffer.writeBoolean(payload.triggerHeld);
   }, buffer -> new FirePayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
