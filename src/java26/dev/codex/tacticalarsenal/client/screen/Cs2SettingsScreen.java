package dev.codex.tacticalarsenal.client.screen;

import dev.codex.tacticalarsenal.client.compat.ClientCompat;
import dev.codex.tacticalarsenal.client.i18n.Cs2Language;
import dev.codex.tacticalarsenal.client.settings.HandRenderMode;
import dev.codex.tacticalarsenal.client.settings.TacticalArsenalClientConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class Cs2SettingsScreen extends Screen {
   private final Screen parent;
   private final boolean firstRun;
   private HandRenderMode selectedMode;
   private int selectedReductionPercent;
   private CycleButton<HandRenderMode> modeButton;
   private Cs2SettingsScreen.HandReductionSlider reductionSlider;
   private int controlsTop;

   public Cs2SettingsScreen(Screen parent) {
      this(parent, false);
   }

   public Cs2SettingsScreen(Screen parent, boolean firstRun) {
      super(Cs2Language.text("screen.tactical_arsenal.cs2_settings.title"));
      this.parent = parent;
      this.firstRun = firstRun;
      this.selectedMode = TacticalArsenalClientConfig.handRenderMode();
      this.selectedReductionPercent = TacticalArsenalClientConfig.handReductionPercent();
   }

   protected void init() {
      int horizontalMargin = Math.min(8, Math.max(0, this.width / 10));
      int contentWidth = Math.min(330, Math.max(1, this.width - horizontalMargin * 2));
      int left = Math.max(0, (this.width - contentWidth) / 2);
      int gap = this.height >= 110 ? 6 : 2;
      int availableHeight = Math.max(30, this.height - 24);
      int rowHeight = Math.min(20, Math.max(10, (availableHeight - gap * 2) / 3));
      int controlsHeight = rowHeight * 3 + gap * 2;
      int preferredTop = this.firstRun ? 70 : 48;
      this.controlsTop = Math.max(2, Math.min(preferredTop, Math.max(2, this.height - controlsHeight - 4)));
      //#if MC >= 1.21.9
      this.modeButton = CycleButton.builder(HandRenderMode::displayName, this.selectedMode)
         .withValues(List.of(HandRenderMode.values()))
      //#else
      this.modeButton = CycleButton.builder(HandRenderMode::displayName)
         .withValues(List.of(HandRenderMode.values()))
         .withInitialValue(this.selectedMode)
      //#endif
         .withTooltip(mode -> Tooltip.create(mode.tooltip()))
         .create(
            left,
            this.controlsTop,
            contentWidth,
            rowHeight,
            Cs2Language.text("screen.tactical_arsenal.cs2_settings.hold_mode"),
            (button, mode) -> this.selectedMode = mode
         );
      this.addRenderableWidget(this.modeButton);
      this.reductionSlider = new Cs2SettingsScreen.HandReductionSlider(
         left, this.controlsTop + rowHeight + gap, contentWidth, rowHeight, this.selectedReductionPercent, value -> this.selectedReductionPercent = value
      );
      this.reductionSlider.setTooltip(Tooltip.create(Cs2Language.text("screen.tactical_arsenal.cs2_settings.hand_reduction.tooltip")));
      this.addRenderableWidget(this.reductionSlider);
      int footerY = this.controlsTop + (rowHeight + gap) * 2;
      int footerGap = Math.min(6, Math.max(2, contentWidth / 20));
      int firstWidth = Math.max(1, (contentWidth - footerGap) / 2);
      int secondWidth = Math.max(1, contentWidth - footerGap - firstWidth);
      this.addRenderableWidget(
         Button.builder(Cs2Language.text("screen.tactical_arsenal.cs2_settings.reset"), button -> this.resetToDefaults())
            .bounds(left, footerY, firstWidth, rowHeight)
            .tooltip(Tooltip.create(Cs2Language.text("screen.tactical_arsenal.cs2_settings.reset.tooltip")))
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Cs2Language.text("screen.tactical_arsenal.cs2_settings.done"), button -> this.commitAndClose())
            .bounds(left + firstWidth + footerGap, footerY, secondWidth, rowHeight)
            .build()
      );
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      Component logoTitle = Component.literal("CS2 ")
         .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})
         .append(Cs2Language.text(this.firstRun ? "screen.tactical_arsenal.cs2_settings.first_setup" : "screen.tactical_arsenal.cs2_settings.settings"));
      if (this.controlsTop >= 20) {
         graphics.centeredText(this.font, logoTitle, this.width / 2, Math.max(4, this.controlsTop - (this.firstRun ? 38 : 18)), -1);
      }

      Component description = Cs2Language.text("screen.tactical_arsenal.cs2_settings.description");
      if (this.controlsTop >= 36 && this.font.width(description) <= Math.max(1, this.width - 8)) {
         graphics.centeredText(this.font, description, this.width / 2, this.controlsTop - (this.firstRun ? 25 : 8), -4671304);
      }

      Component reopenHint = Cs2Language.text("screen.tactical_arsenal.cs2_settings.reopen_hint");
      if (this.firstRun && this.controlsTop >= 52 && this.font.width(reopenHint) <= Math.max(1, this.width - 8)) {
         graphics.centeredText(this.font, reopenHint, this.width / 2, this.controlsTop - 11, -11172);
      }
   }

   public void onClose() {
      ClientCompat.showScreen(this.minecraft, this.parent);
   }

   private void resetToDefaults() {
      this.selectedMode = TacticalArsenalClientConfig.DEFAULT_HAND_RENDER_MODE;
      this.selectedReductionPercent = 40;
      this.modeButton.setValue(this.selectedMode);
      this.reductionSlider.setReductionPercent(this.selectedReductionPercent);
   }

   private void commitAndClose() {
      TacticalArsenalClientConfig.apply(this.selectedMode, this.selectedReductionPercent, this.firstRun || !TacticalArsenalClientConfig.needsFirstRunSetup());
      ClientCompat.showScreen(this.minecraft, this.parent);
   }

   private static final class HandReductionSlider extends AbstractSliderButton {
      private final Cs2SettingsScreen.IntValueConsumer onChange;

      private HandReductionSlider(int x, int y, int width, int height, int reductionPercent, Cs2SettingsScreen.IntValueConsumer onChange) {
         super(x, y, width, height, Component.empty(), Mth.clamp(reductionPercent, 0, 100) / 100.0);
         this.onChange = onChange;
         this.updateMessage();
      }

      private void setReductionPercent(int reductionPercent) {
         this.value = Mth.clamp(reductionPercent, 0, 100) / 100.0;
         this.updateMessage();
         this.applyValue();
      }

      protected void updateMessage() {
         this.setMessage(Cs2Language.text("screen.tactical_arsenal.cs2_settings.hand_reduction", Math.round(this.value * 100.0)));
      }

      protected void applyValue() {
         this.onChange.accept(Mth.clamp((int)Math.round(this.value * 100.0), 0, 100));
      }
   }

   @FunctionalInterface
   private interface IntValueConsumer {
      void accept(int var1);
   }
}
