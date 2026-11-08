package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Minecraft 1.20.1 version of the reload packet (same id and empty body as 26.x). */
public record ReloadPayload() implements FabricPacket {
   public static final ReloadPayload INSTANCE = new ReloadPayload();
   public static final PacketType<ReloadPayload> TYPE = PacketType.create(TacticalArsenal.id("reload"), buffer -> INSTANCE);

   @Override
   public void write(FriendlyByteBuf buffer) {
   }

   @Override
   public PacketType<?> getType() {
      return TYPE;
   }
}
