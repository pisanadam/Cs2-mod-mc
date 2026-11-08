package dev.codex.tacticalarsenal.gun;

public enum TracerStyle {
   PISTOL(0),
   SMG(1),
   ASSAULT_RIFLE(2),
   ASSAULT_RIFLE_AUG(3),
   SNIPER_RIFLE(4),
   SSG_SNIPER(5),
   SCAR_SNIPER(6),
   MACHINE_GUN(7),
   SHOTGUN(8);

   private static final TracerStyle[] BY_NETWORK_ID = values();
   private final int networkId;

   private TracerStyle(int networkId) {
      this.networkId = networkId;
   }

   public int networkId() {
      return this.networkId;
   }

   public static TracerStyle fromNetworkId(int id) {
      return id >= 0 && id < BY_NETWORK_ID.length ? BY_NETWORK_ID[id] : ASSAULT_RIFLE;
   }
}
