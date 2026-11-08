package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.compat.ClientCompat;
import dev.codex.tacticalarsenal.client.i18n.Cs2Language;
import dev.codex.tacticalarsenal.client.screen.Cs2SettingsScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PauseScreen.class})
public abstract class PauseScreenMixin extends Screen {
   protected PauseScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void tacticalArsenal$addCs2SettingsButton(CallbackInfo callback) {
      int buttonWidth = Math.min(84, Math.max(1, this.width - 8));
      int x = Math.max(0, this.width - buttonWidth - 4);
      int y = Math.min(4, Math.max(0, this.height - 20));
      this.addRenderableWidget(
         Button.builder(
               Component.literal("CS2").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}),
               button -> ClientCompat.showScreen(this.minecraft, new Cs2SettingsScreen(this))
            )
            .bounds(x, y, buttonWidth, Math.min(20, Math.max(1, this.height)))
            .tooltip(Tooltip.create(Cs2Language.text("screen.tactical_arsenal.cs2_settings.open")))
            .build()
      );
   }
}
