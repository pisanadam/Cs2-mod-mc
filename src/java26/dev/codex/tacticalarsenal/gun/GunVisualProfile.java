package dev.codex.tacticalarsenal.gun;

public record GunVisualProfile(TracerStyle tracerStyle, float muzzleForward, float muzzleSide, float muzzleUp) {
   public static GunVisualProfile forShot(GunDefinition gun, int patternIndex) {
      return gun.id().equals("elite") && (patternIndex & 1) != 0 ? source(TracerStyle.PISTOL, 23.496F, 3.105F, -1.799F) : forWeapon(gun);
   }

   public static GunVisualProfile forWeapon(GunDefinition gun) {
      String var1 = gun.id();

      return switch (var1) {
         case "desert_eagle" -> source(TracerStyle.PISTOL, 24.317F, -3.83F, -1.22F);
         case "revolver" -> source(TracerStyle.PISTOL, 24.558F, -3.634F, -1.894F);
         case "elite" -> source(TracerStyle.PISTOL, 23.752F, -3.066F, -1.754F);
         case "fiveseven" -> source(TracerStyle.PISTOL, 22.214F, -2.825F, -1.692F);
         case "cz75a" -> source(TracerStyle.PISTOL, 23.526F, -2.805F, -1.753F);
         case "glock" -> source(TracerStyle.PISTOL, 21.775F, -2.805F, -1.975F);
         case "tec9" -> source(TracerStyle.PISTOL, 27.931F, -5.126F, -3.697F);
         case "hkp2000" -> source(TracerStyle.PISTOL, 21.32F, -2.801F, -1.704F);
         case "usp_silencer" -> source(TracerStyle.PISTOL, 30.599F, -2.8F, -2.361F);
         case "p250" -> source(TracerStyle.PISTOL, 21.729F, -2.82F, -1.623F);
         case "mac10" -> source(TracerStyle.SMG, 28.289F, -6.171F, -3.444F);
         case "p90" -> source(TracerStyle.SMG, 24.058F, -5.247F, -3.181F);
         case "ump45" -> source(TracerStyle.SMG, 34.674F, -5.162F, -4.139F);
         case "bizon" -> source(TracerStyle.SMG, 33.442F, -5.107F, -3.014F);
         case "mp7" -> source(TracerStyle.SMG, 27.101F, -5.278F, -3.958F);
         case "mp5sd" -> source(TracerStyle.SMG, 38.905F, -5.166F, -4.33F);
         case "mp9" -> source(TracerStyle.SMG, 22.783F, -4.759F, -2.531F);
         case "ak47" -> source(TracerStyle.ASSAULT_RIFLE, 37.422F, -4.938F, -3.394F);
         case "aug" -> source(TracerStyle.ASSAULT_RIFLE_AUG, 29.957F, -3.597F, -2.666F);
         case "awp" -> source(TracerStyle.SNIPER_RIFLE, 54.128F, -5.022F, -3.124F);
         case "famas" -> source(TracerStyle.ASSAULT_RIFLE, 36.464F, -6.028F, -4.783F);
         case "g3sg1" -> source(TracerStyle.SSG_SNIPER, 45.492F, -5.287F, -4.119F);
         case "galilar" -> source(TracerStyle.ASSAULT_RIFLE, 34.673F, -5.319F, -3.601F);
         case "m4a4" -> source(TracerStyle.ASSAULT_RIFLE, 35.62F, -4.975F, -3.707F);
         case "m4a1_silencer" -> source(TracerStyle.ASSAULT_RIFLE, 46.463F, -4.83F, -3.374F);
         case "scar20" -> source(TracerStyle.SCAR_SNIPER, 44.474F, -5.69F, -4.37F);
         case "sg556" -> source(TracerStyle.ASSAULT_RIFLE_AUG, 40.466F, -5.134F, -3.871F);
         case "ssg08" -> source(TracerStyle.SSG_SNIPER, 48.901F, -5.688F, -3.681F);
         case "m249" -> source(TracerStyle.MACHINE_GUN, 52.925F, -7.43F, -3.928F);
         case "negev" -> source(TracerStyle.MACHINE_GUN, 45.371F, -7.503F, -3.87F);
         case "xm1014" -> source(TracerStyle.SHOTGUN, 39.73F, -4.274F, -4.177F);
         case "mag7" -> source(TracerStyle.SHOTGUN, 39.795F, -6.293F, -5.04F);
         case "sawedoff" -> source(TracerStyle.SHOTGUN, 35.318F, -4.309F, -2.752F);
         case "nova" -> source(TracerStyle.SHOTGUN, 42.23F, -4.268F, -3.652F);
         default -> fallback(gun.category());
      };
   }

   private static GunVisualProfile source(TracerStyle style, float x, float y, float z) {
      return new GunVisualProfile(style, x / 52.493F, -y / 52.493F, z / 52.493F);
   }

   private static GunVisualProfile fallback(WeaponCategory category) {
      return switch (category) {
         case PISTOL -> new GunVisualProfile(TracerStyle.PISTOL, 0.46F, 0.06F, -0.04F);
         case SMG -> new GunVisualProfile(TracerStyle.SMG, 0.62F, 0.1F, -0.07F);
         case RIFLE -> new GunVisualProfile(TracerStyle.ASSAULT_RIFLE, 0.72F, 0.1F, -0.07F);
         case SNIPER_RIFLE -> new GunVisualProfile(TracerStyle.SNIPER_RIFLE, 0.94F, 0.1F, -0.07F);
         case SHOTGUN -> new GunVisualProfile(TracerStyle.SHOTGUN, 0.76F, 0.1F, -0.08F);
         case MACHINE_GUN -> new GunVisualProfile(TracerStyle.MACHINE_GUN, 0.94F, 0.14F, -0.08F);
      };
   }
}
