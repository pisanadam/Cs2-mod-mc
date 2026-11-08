package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ReloadPayload() implements CustomPacketPayload {
   public static final ReloadPayload INSTANCE = new ReloadPayload();
   public static final Type<ReloadPayload> TYPE = new Type(TacticalArsenal.id("reload"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ReloadPayload> CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
