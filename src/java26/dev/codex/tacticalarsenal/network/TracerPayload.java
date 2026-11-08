package dev.codex.tacticalarsenal.network;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.gun.TracerStyle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;

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
) implements CustomPacketPayload {
   public static final Type<TracerPayload> TYPE = new Type(TacticalArsenal.id("tracer"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TracerPayload> CODEC = CustomPacketPayload.codec(
      (payload, buffer) -> {
         buffer.writeVarInt(payload.shooterEntityId);
         buffer.writeUtf(payload.weaponId, 32);
         buffer.writeVarInt(payload.patternIndex);
         buffer.writeDouble(payload.startX);
         buffer.writeDouble(payload.startY);
         buffer.writeDouble(payload.startZ);
         buffer.writeFloat(payload.offsetX);
         buffer.writeFloat(payload.offsetY);
         buffer.writeFloat(payload.offsetZ);
         buffer.writeByte(payload.styleId);
      },
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

   public TracerStyle style() {
      return TracerStyle.fromNetworkId(this.styleId);
   }

   public Vec3 start() {
      return new Vec3(this.startX, this.startY, this.startZ);
   }

   public Vec3 end() {
      return new Vec3(this.startX + this.offsetX, this.startY + this.offsetY, this.startZ + this.offsetZ);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
