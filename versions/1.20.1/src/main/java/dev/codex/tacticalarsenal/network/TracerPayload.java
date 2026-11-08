package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.gun.TracerStyle;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** Minecraft 1.20.1 version of the tracer packet (same id and wire format as 26.x). */
public record TracerPayload(
   int shooterEntityId,
   String weaponId,
   int patternIndex,
   double startX,
   double startY,
   double startZ,
   float offsetX,
   float offsetY,
   float offsetZ,
   int styleId
) implements FabricPacket {
   public static final PacketType<TracerPayload> TYPE = PacketType.create(
      TacticalArsenal.id("tracer"),
      buffer -> new TracerPayload(
         buffer.readVarInt(),
         buffer.readUtf(32),
         buffer.readVarInt(),
         buffer.readDouble(),
         buffer.readDouble(),
         buffer.readDouble(),
         buffer.readFloat(),
         buffer.readFloat(),
         buffer.readFloat(),
         buffer.readUnsignedByte()
      )
   );

   public static TracerPayload between(int shooterEntityId, String weaponId, int patternIndex, Vec3 start, Vec3 end, TracerStyle style) {
      Vec3 offset = end.subtract(start);
      return new TracerPayload(
         shooterEntityId, weaponId, patternIndex, start.x, start.y, start.z, (float)offset.x, (float)offset.y, (float)offset.z, style.networkId()
      );
   }

   @Override
   public void write(FriendlyByteBuf buffer) {
      buffer.writeVarInt(this.shooterEntityId);
      buffer.writeUtf(this.weaponId, 32);
      buffer.writeVarInt(this.patternIndex);
      buffer.writeDouble(this.startX);
      buffer.writeDouble(this.startY);
      buffer.writeDouble(this.startZ);
      buffer.writeFloat(this.offsetX);
      buffer.writeFloat(this.offsetY);
      buffer.writeFloat(this.offsetZ);
      buffer.writeByte(this.styleId);
   }

   public TracerStyle style() {
      return TracerStyle.fromNetworkId(this.styleId);
   }

   public Vec3 start() {
      return new Vec3(this.startX, this.startY, this.startZ);
   }

   public Vec3 end() {
      return new Vec3(this.startX + this.offsetX, this.startY + this.offsetY, this.startZ + this.offsetZ);
   }

   @Override
   public PacketType<?> getType() {
      return TYPE;
   }
}
