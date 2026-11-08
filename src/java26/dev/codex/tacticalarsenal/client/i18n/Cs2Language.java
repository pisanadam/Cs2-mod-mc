package dev.codex.tacticalarsenal.client.i18n;

import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Cs2Language {
   private static final Map<String, String> ENGLISH = Map.ofEntries(
      Map.entry("screen.tactical_arsenal.cs2_settings.title", "CS2 Weapon & Hand Settings"),
      Map.entry("screen.tactical_arsenal.cs2_settings.description", "Changes client-side visuals only; weapon damage and gameplay are unaffected."),
      Map.entry("screen.tactical_arsenal.cs2_settings.reopen_hint", "You can reopen this screen later using the gold CS2 button in the pause menu."),
      Map.entry("screen.tactical_arsenal.cs2_settings.hold_mode", "Holding mode"),
      Map.entry("screen.tactical_arsenal.cs2_settings.hand_reduction", "Hand reduction: %s%%"),
      Map.entry("screen.tactical_arsenal.cs2_settings.hand_reduction.tooltip", "Shrinks the hands from 0% to 100%. Default: 40%."),
      Map.entry("screen.tactical_arsenal.cs2_settings.reset", "Reset to defaults"),
      Map.entry("screen.tactical_arsenal.cs2_settings.reset.tooltip", "Restores the 2.2.0-style hold and 40% hand reduction."),
      Map.entry("screen.tactical_arsenal.cs2_settings.done", "Save and close"),
      Map.entry("screen.tactical_arsenal.cs2_settings.first_setup", "First-time Setup"),
      Map.entry("screen.tactical_arsenal.cs2_settings.settings", "Settings"),
      Map.entry("screen.tactical_arsenal.cs2_settings.open", "Open CS2 weapon hand and holding settings"),
      Map.entry("options.tactical_arsenal.hand_mode.cs2_two_handed", "Mode 1: CS2 two-handed"),
      Map.entry("options.tactical_arsenal.hand_mode.cs2_two_handed.tooltip", "Uses the two-handed CS2 hold and hand animations from version 2.3.0."),
      Map.entry("options.tactical_arsenal.hand_mode.legacy_2_2", "Mode 2: 2.2.0 style"),
      Map.entry("options.tactical_arsenal.hand_mode.legacy_2_2.tooltip", "Usually shows one hand; Dual Berettas remain two-handed."),
      Map.entry("options.tactical_arsenal.hand_mode.hide_hands", "Mode 3: Hide hands"),
      Map.entry("options.tactical_arsenal.hand_mode.hide_hands.tooltip", "Hides player hands in first person while keeping guns, knives, and grenades visible.")
   );
   private static final Map<String, String> TURKISH = Map.ofEntries(
      Map.entry("screen.tactical_arsenal.cs2_settings.title", "CS2 Silah ve El Ayarları"),
      Map.entry("screen.tactical_arsenal.cs2_settings.description", "Yalnızca istemci görünümünü değiştirir; silah hasarları ve oynanış etkilenmez."),
      Map.entry("screen.tactical_arsenal.cs2_settings.reopen_hint", "Bu ekranı daha sonra duraklatma menüsündeki altın CS2 düğmesiyle yeniden açabilirsin."),
      Map.entry("screen.tactical_arsenal.cs2_settings.hold_mode", "Tutuş modu"),
      Map.entry("screen.tactical_arsenal.cs2_settings.hand_reduction", "El küçültme: %%%s"),
      Map.entry("screen.tactical_arsenal.cs2_settings.hand_reduction.tooltip", "Elleri %0 ile %100 arasında küçültür. Varsayılan: %40."),
      Map.entry("screen.tactical_arsenal.cs2_settings.reset", "Varsayılana dön"),
      Map.entry("screen.tactical_arsenal.cs2_settings.reset.tooltip", "2.2.0 tarzı tutuşu ve %40 el küçültmesini geri yükler."),
      Map.entry("screen.tactical_arsenal.cs2_settings.done", "Kaydet ve kapat"),
      Map.entry("screen.tactical_arsenal.cs2_settings.first_setup", "İlk Kurulum"),
      Map.entry("screen.tactical_arsenal.cs2_settings.settings", "Ayarları"),
      Map.entry("screen.tactical_arsenal.cs2_settings.open", "CS2 silah eli ve tutuş ayarlarını aç"),
      Map.entry("options.tactical_arsenal.hand_mode.cs2_two_handed", "Mod 1: CS2 tarzı iki elli"),
      Map.entry("options.tactical_arsenal.hand_mode.cs2_two_handed.tooltip", "2.3.0 sürümündeki iki elli CS2 tutuşunu ve el animasyonlarını kullanır."),
      Map.entry("options.tactical_arsenal.hand_mode.legacy_2_2", "Mod 2: 2.2.0 tarzı"),
      Map.entry("options.tactical_arsenal.hand_mode.legacy_2_2.tooltip", "Genellikle tek el gösterir; Dual Berettas iki elli kalır."),
      Map.entry("options.tactical_arsenal.hand_mode.hide_hands", "Mod 3: Elleri gizle"),
      Map.entry("options.tactical_arsenal.hand_mode.hide_hands.tooltip", "Birinci şahısta silah, bıçak ve bombalar görünür kalırken oyuncu ellerini gizler.")
   );

   private Cs2Language() {
   }

   public static Component text(String key, Object... arguments) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean turkish = minecraft != null && minecraft.options != null && "tr_tr".equalsIgnoreCase(minecraft.options.languageCode);
      String format = (turkish ? TURKISH : ENGLISH).getOrDefault(key, ENGLISH.getOrDefault(key, key));
      return arguments.length == 0 ? Component.literal(format) : Component.literal(String.format(Locale.ROOT, format, arguments));
   }
}
