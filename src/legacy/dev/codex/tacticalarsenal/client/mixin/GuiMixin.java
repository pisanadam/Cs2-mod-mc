package dev.codex.tacticalarsenal.client.mixin;

import dev.codex.tacticalarsenal.client.render.GunHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.21.1 / 1.20.1 version of the 26.x HUD element replacements: gun crosshair, no vanilla
 * armor bar and the CS2 health/armor panel in place of the hearts.
 */
@Mixin({Gui.class})
public abstract class GuiMixin {
   //#if MC >= 1.21
   @Inject(method = {"renderCrosshair"}, at = {@At("HEAD")}, cancellable = true)
   private void tacticalArsenal$gunCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
      if (GunHud.renderCrosshair(graphics, deltaTracker)) {
         callback.cancel();
      }
   }

   @Redirect(
      method = {"renderPlayerHealth"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V")
   )
   private void tacticalArsenal$hideArmorBar(GuiGraphics graphics, Player player, int a, int b, int c, int d) {
   }
   //#else
   @Inject(method = {"renderCrosshair"}, at = {@At("HEAD")}, cancellable = true)
   private void tacticalArsenal$gunCrosshair(GuiGraphics graphics, CallbackInfo callback) {
      if (GunHud.renderCrosshair(graphics, DeltaTracker.of(Minecraft.getInstance().getFrameTime()))) {
         callback.cancel();
      }
   }

   @Redirect(
      method = {"renderPlayerHealth"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getArmorValue()I")
   )
   private int tacticalArsenal$hideArmorBar(Player player) {
      return 0;
   }
   //#endif

   @Redirect(
      method = {"renderPlayerHealth"},
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V")
   )
   private void tacticalArsenal$csStatus(Gui gui, GuiGraphics graphics, Player player, int a, int b, int c, int d, float e, int f, int g, int h, boolean i) {
      //#if MC >= 1.21
      GunHud.renderHealth(graphics, Minecraft.getInstance().getTimer());
      //#else
      GunHud.renderHealth(graphics, DeltaTracker.of(Minecraft.getInstance().getFrameTime()));
      //#endif
   }
}
