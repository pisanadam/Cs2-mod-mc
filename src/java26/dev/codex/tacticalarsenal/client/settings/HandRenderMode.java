package dev.codex.tacticalarsenal.client.settings;

import dev.codex.tacticalarsenal.client.i18n.Cs2Language;
import net.minecraft.network.chat.Component;

public enum HandRenderMode {
   CS2_TWO_HANDED("options.tactical_arsenal.hand_mode.cs2_two_handed", "options.tactical_arsenal.hand_mode.cs2_two_handed.tooltip"),
   LEGACY_2_2("options.tactical_arsenal.hand_mode.legacy_2_2", "options.tactical_arsenal.hand_mode.legacy_2_2.tooltip"),
   HIDE_HANDS("options.tactical_arsenal.hand_mode.hide_hands", "options.tactical_arsenal.hand_mode.hide_hands.tooltip");

   private final String labelKey;
   private final String tooltipKey;

   private HandRenderMode(String labelKey, String tooltipKey) {
      this.labelKey = labelKey;
      this.tooltipKey = tooltipKey;
   }

   public Component displayName() {
      return Cs2Language.text(this.labelKey);
   }

   public Component tooltip() {
      return Cs2Language.text(this.tooltipKey);
   }
}
