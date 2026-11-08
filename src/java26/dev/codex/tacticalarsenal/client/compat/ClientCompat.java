package dev.codex.tacticalarsenal.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Small per-Minecraft-version client helpers so the gameplay code stays identical everywhere. */
public final class ClientCompat {
   private ClientCompat() {
   }

   public static Screen screen(Minecraft minecraft) {
      //#if MC >= 26.2
      return minecraft.gui.screen();
      //#else
      return minecraft.screen;
      //#endif
   }

   public static void setScreen(Minecraft minecraft, Screen screen) {
      //#if MC >= 26.2
      minecraft.gui.setScreen(screen);
      //#else
      minecraft.setScreen(screen);
      //#endif
   }

   public static void showScreen(Minecraft minecraft, Screen screen) {
      //#if MC >= 26.1
      minecraft.setScreenAndShow(screen);
      //#else
      minecraft.setScreen(screen);
      //#endif
   }
}
