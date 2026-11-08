package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/** Minecraft 1.20.1 version of the fire packet (same id and wire format as 26.x). */
public record FirePayload(boolean aiming, boolean burstMode, boolean triggerHeld) implements FabricPacket {
   public static final PacketType<FirePayload> TYPE = PacketType.create(
      TacticalArsenal.id("fire"), buffer -> new FirePayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean())
   );

   @Override
   public void write(FriendlyByteBuf buffer) {
      buffer.writeBoolean(this.aiming);
      buffer.writeBoolean(this.burstMode);
      buffer.writeBoolean(this.triggerHeld);
   }

   @Override
   public PacketType<?> getType() {
      return TYPE;
   }
}
