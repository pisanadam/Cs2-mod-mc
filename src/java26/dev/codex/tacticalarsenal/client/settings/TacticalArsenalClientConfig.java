package dev.codex.tacticalarsenal.client.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.codex.tacticalarsenal.TacticalArsenal;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

public final class TacticalArsenalClientConfig {
   public static final HandRenderMode DEFAULT_HAND_RENDER_MODE = HandRenderMode.LEGACY_2_2;
   public static final int DEFAULT_HAND_REDUCTION_PERCENT = 40;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("tactical_arsenal-2.2-cs2-hands.json");
   private static HandRenderMode handRenderMode = DEFAULT_HAND_RENDER_MODE;
   private static int handReductionPercent = 40;
   private static boolean setupComplete;
   private static boolean initialized;

   private TacticalArsenalClientConfig() {
   }

   public static synchronized void initialize() {
      if (!initialized) {
         initialized = true;
         load();
      }
   }

   public static HandRenderMode handRenderMode() {
      return handRenderMode;
   }

   public static boolean hideHands() {
      return handRenderMode == HandRenderMode.HIDE_HANDS;
   }

   public static int handReductionPercent() {
      return handReductionPercent;
   }

   public static float handScale() {
      return 1.0F - handReductionPercent / 100.0F;
   }

   public static boolean needsFirstRunSetup() {
      return !setupComplete;
   }

   public static synchronized void apply(HandRenderMode mode, int reductionPercent, boolean completeFirstRunSetup) {
      handRenderMode = mode == null ? DEFAULT_HAND_RENDER_MODE : mode;
      handReductionPercent = Mth.clamp(reductionPercent, 0, 100);
      if (completeFirstRunSetup) {
         setupComplete = true;
      }

      save();
   }

   private static void load() {
      handRenderMode = DEFAULT_HAND_RENDER_MODE;
      handReductionPercent = 40;
      setupComplete = false;
      if (!Files.isRegularFile(CONFIG_PATH)) {
         save();
      } else {
         try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
               throw new IOException("root JSON value is not an object");
            }

            JsonObject root = parsed.getAsJsonObject();
            if (root.has("handMode") && root.get("handMode").isJsonPrimitive()) {
               try {
                  handRenderMode = HandRenderMode.valueOf(root.get("handMode").getAsString());
               } catch (IllegalArgumentException var5) {
                  TacticalArsenal.LOGGER
                     .warn("Unknown Tactical Arsenal hand mode '{}'; using {}", root.get("handMode").getAsString(), DEFAULT_HAND_RENDER_MODE);
               }
            }

            if (root.has("handReductionPercent") && root.get("handReductionPercent").isJsonPrimitive()) {
               handReductionPercent = Mth.clamp(root.get("handReductionPercent").getAsInt(), 0, 100);
            }

            if (root.has("setupComplete") && root.get("setupComplete").isJsonPrimitive()) {
               setupComplete = root.get("setupComplete").getAsBoolean();
            }
         } catch (Exception var7) {
            TacticalArsenal.LOGGER.warn("Could not read {}; using safe 2.2.0 client defaults", CONFIG_PATH, var7);
         }
      }
   }

   private static void save() {
      JsonObject root = new JsonObject();
      root.addProperty("handMode", handRenderMode.name());
      root.addProperty("handReductionPercent", handReductionPercent);
      root.addProperty("setupComplete", setupComplete);
      Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");

      try {
         Files.createDirectories(CONFIG_PATH.getParent());

         try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
         }

         try {
            Files.move(temporary, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException var6) {
            Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException var8) {
         TacticalArsenal.LOGGER.warn("Could not save Tactical Arsenal client settings to {}", CONFIG_PATH, var8);
      }
   }
}
