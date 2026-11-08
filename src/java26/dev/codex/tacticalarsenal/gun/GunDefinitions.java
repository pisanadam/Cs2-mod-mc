package dev.codex.tacticalarsenal.gun;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GunDefinitions {
   private static final String RESOURCE = "/data/tactical_arsenal/weapons/cs2_weapons.json";
   public static final List<GunDefinition> ALL = load();
   private static final Map<String, GunDefinition> BY_ID = index(ALL);

   private GunDefinitions() {
   }

   public static GunDefinition byId(String id) {
      return BY_ID.get(id);
   }

   private static List<GunDefinition> load() {
      try {
         List var3;
         try (InputStream stream = GunDefinitions.class.getResourceAsStream("/data/tactical_arsenal/weapons/cs2_weapons.json")) {
            if (stream == null) {
               throw new IllegalStateException("Missing bundled CS2 weapon data: /data/tactical_arsenal/weapons/cs2_weapons.json");
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<GunDefinition> definitions = root.getAsJsonArray("weapons").asList().stream().map(element -> decode(element.getAsJsonObject())).toList();
            if (definitions.size() != 34) {
               throw new IllegalStateException("Expected 34 CS2 firearms, found " + definitions.size());
            }

            var3 = definitions;
         }

         return var3;
      } catch (IOException var6) {
         throw new IllegalStateException("Unable to read bundled CS2 weapon data", var6);
      }
   }

   private static Map<String, GunDefinition> index(List<GunDefinition> definitions) {
      Map<String, GunDefinition> result = new LinkedHashMap<>();

      for (GunDefinition definition : definitions) {
         if (result.put(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate weapon id " + definition.id());
         }
      }

      return Map.copyOf(result);
   }

   private static GunDefinition decode(JsonObject weapon) {
      JsonObject accuracy = weapon.getAsJsonObject("accuracy");
      JsonObject recoil = weapon.getAsJsonObject("recoil");
      return new GunDefinition(
         string(weapon, "id"),
         FireMode.valueOf(string(weapon, "fireMode")),
         WeaponCategory.valueOf(string(weapon, "category")),
         integer(weapon, "projectiles"),
         number(weapon, "damage"),
         integer(weapon, "magazineSize"),
         integer(weapon, "reserveAmount"),
         bool(weapon, "reserveAsMagazines"),
         number(weapon, "cycleTimeSeconds"),
         number(weapon, "alternateCycleTimeSeconds"),
         bool(weapon, "burstCapable"),
         number(weapon, "burstCycleTimeSeconds"),
         number(weapon, "burstShotIntervalSeconds"),
         bool(weapon, "revolver"),
         number(weapon, "sourceRangeUnits"),
         number(weapon, "rangeModifier"),
         number(weapon, "headMultiplier"),
         number(weapon, "armorRatio"),
         number(weapon, "penetrationPower"),
         new GunDefinition.AccuracyProfile(
            mode(accuracy, "spread"),
            mode(accuracy, "crouch"),
            mode(accuracy, "stand"),
            mode(accuracy, "jump"),
            mode(accuracy, "land"),
            mode(accuracy, "fire"),
            mode(accuracy, "move"),
            mode(accuracy, "maxSpeed"),
            number(accuracy, "jumpInitial"),
            number(accuracy, "jumpApex"),
            number(accuracy, "recoveryCrouch"),
            number(accuracy, "recoveryStand"),
            number(accuracy, "recoveryCrouchFinal"),
            number(accuracy, "recoveryStandFinal"),
            integer(accuracy, "recoveryTransitionStart"),
            integer(accuracy, "recoveryTransitionEnd")
         ),
         new GunDefinition.RecoilProfile(integer(recoil, "seed"), mode(recoil, "angleVariance"), mode(recoil, "magnitude"), mode(recoil, "magnitudeVariance")),
         bool(weapon, "alternateModeByDefault"),
         integer(weapon, "zoomLevels"),
         number(weapon, "zoomFov1"),
         number(weapon, "zoomFov2"),
         number(weapon, "zoomSeconds"),
         bool(weapon, "unzoomsAfterShot"),
         bool(weapon, "hideViewmodelWhenZoomed"),
         bool(weapon, "suppressed"),
         number(weapon, "reloadSeconds"),
         number(weapon, "reloadAnimationSeconds"),
         number(weapon, "emptyReloadAnimationSeconds"),
         number(weapon, "reloadInsertSeconds"),
         number(weapon, "emptyReloadInsertSeconds"),
         number(weapon, "deploySeconds")
      );
   }

   private static GunDefinition.ModeValue mode(JsonObject object, String key) {
      JsonArray array = object.getAsJsonArray(key);
      return new GunDefinition.ModeValue(array.get(0).getAsFloat(), array.get(1).getAsFloat());
   }

   private static String string(JsonObject object, String key) {
      return object.get(key).getAsString();
   }

   private static float number(JsonObject object, String key) {
      return object.get(key).getAsFloat();
   }

   private static int integer(JsonObject object, String key) {
      return object.get(key).getAsInt();
   }

   private static boolean bool(JsonObject object, String key) {
      return object.get(key).getAsBoolean();
   }
}
