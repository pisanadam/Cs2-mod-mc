package dev.codex.tacticalarsenal.client.render;

import dev.codex.tacticalarsenal.TacticalArsenal;
import dev.codex.tacticalarsenal.client.GunClientController;
import dev.codex.tacticalarsenal.client.animation.GunAnimationEngine;
import dev.codex.tacticalarsenal.gun.GunItem;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class GunHud {
   private static final Identifier SCOPE_CIRCLE = TacticalArsenal.id("textures/gui/scope/circle.png");
   private static final Identifier CS2_SHIELD = TacticalArsenal.id("textures/gui/hud/cs2_shield.png");

   private GunHud() {
   }

   public static void initialize() {
      HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> crosshair(original));
      HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, original -> (graphics, deltaTracker) -> {});
      HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> GunHud::drawCsStatus);
      HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, TacticalArsenal.id("ammo_hud"), GunHud::drawAmmo);
   }

   private static HudElement crosshair(HudElement original) {
      return (graphics, deltaTracker) -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof GunItem gun) {
            drawCrosshair(graphics, deltaTracker, gun);
         } else {
            original.extractRenderState(graphics, deltaTracker);
         }
      };
   }

   private static void drawCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, GunItem gun) {
      float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
      if (gun.definition().canScope()) {
         if (GunClientController.isScopeOverlayActive(partialTick)) {
            boolean inaccurate = GunClientController.scopedShotConeExceedsOneCentimetre();
            if (gun.definition().hideViewmodelWhenZoomed()) {
               drawSniperScope(graphics, inaccurate);
            } else {
               drawAssaultScope(graphics, inaccurate);
            }

            return;
         }

         if (gun.definition().hideViewmodelWhenZoomed()) {
            return;
         }
      }

      int centerX = graphics.guiWidth() / 2;
      int centerY = graphics.guiHeight() / 2;
      Minecraft minecraft = Minecraft.getInstance();
      float movement = minecraft.player == null ? 0.0F : (float)minecraft.player.getDeltaMovement().horizontalDistance();
      int gap = 4 + Math.min(8, Math.round(movement * 24.0F));
      if (minecraft.player != null && !minecraft.player.onGround()) {
         gap += 7;
      }

      int length = 4;
      int color = -387386113;
      graphics.fill(centerX - gap - length, centerY, centerX - gap, centerY + 1, color);
      graphics.fill(centerX + gap + 1, centerY, centerX + gap + length + 1, centerY + 1, color);
      graphics.fill(centerX, centerY - gap - length, centerX + 1, centerY - gap, color);
      graphics.fill(centerX, centerY + gap + 1, centerX + 1, centerY + gap + length + 1, color);
   }

   private static void drawSniperScope(GuiGraphicsExtractor graphics, boolean inaccurate) {
      GunHud.ScopeBounds scope = drawSniperScopeTextures(graphics);
      int centerX = scope.centerX;
      int centerY = scope.centerY;
      int radius = scope.side / 2;
      graphics.fill(centerX - radius, centerY, centerX + radius, centerY + 1, -436207616);
      graphics.fill(centerX, centerY - radius, centerX + 1, centerY + radius, -436207616);
      if (inaccurate) {
         drawAimDot(graphics, centerX, centerY, true);
      }
   }

   private static void drawAssaultScope(GuiGraphicsExtractor graphics, boolean inaccurate) {
      int centerX = graphics.guiWidth() / 2;
      int centerY = graphics.guiHeight() / 2;
      drawAimDot(graphics, centerX, centerY, inaccurate);
   }

   private static void drawAimDot(GuiGraphicsExtractor graphics, int centerX, int centerY, boolean inaccurate) {
      int glow = inaccurate ? -1191235536 : -1205207184;
      int core = inaccurate ? -24416 : -1769488;
      graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, glow);
      graphics.fill(centerX, centerY, centerX + 1, centerY + 1, core);
   }

   private static GunHud.ScopeBounds drawSniperScopeTextures(GuiGraphicsExtractor graphics) {
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      int side = Math.max(32, Math.min(width, height));
      return drawScopeTextures(graphics, side, true);
   }

   private static GunHud.ScopeBounds drawScopeTextures(GuiGraphicsExtractor graphics, int side, boolean blackOutSides) {
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      int left = (width - side) / 2;
      int top = (height - side) / 2;
      if (blackOutSides) {
         graphics.fill(0, 0, Math.max(0, left), height, -16777216);
         graphics.fill(Math.min(width, left + side), 0, width, height, -16777216);
         graphics.fill(0, 0, width, Math.max(0, top), -16777216);
         graphics.fill(0, Math.min(height, top + side), width, height, -16777216);
      }

      graphics.blit(RenderPipelines.GUI_TEXTURED, SCOPE_CIRCLE, left, top, 0.0F, 0.0F, side, side, 512, 512, 512, 512);
      return new GunHud.ScopeBounds(left, top, side, left + side / 2, top + side / 2);
   }

   private static void drawCsStatus(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         int health = Math.max(0, Mth.ceil(minecraft.player.getHealth()));
         int armor = Math.max(0, minecraft.player.getArmorValue());
         int absorption = Math.max(0, Mth.ceil(minecraft.player.getAbsorptionAmount()));
         int healthBoost = Math.max(0, Mth.ceil(minecraft.player.getMaxHealth() - 20.0F));
         String healthText = Integer.toString(health);
         String armorText = Integer.toString(armor);
         String absorptionText = absorption > 0 ? "+" + absorption : "";
         String boostText = healthBoost > 0 ? "MAX +" + healthBoost : "";
         int left = 12;
         int top = graphics.guiHeight() - (graphics.guiWidth() < 500 ? 72 : 42);
         int healthTextX = left + 25;
         int dividerX = healthTextX + minecraft.font.width(healthText) + 8;
         int shieldX = dividerX + 7;
         int armorTextX = shieldX + 16;
         int cursorX = armorTextX + minecraft.font.width(armorText);
         int width = cursorX - left + 10;
         if (!absorptionText.isEmpty()) {
            width += 8 + minecraft.font.width(absorptionText);
         }

         if (!boostText.isEmpty()) {
            width += 8 + minecraft.font.width(boostText);
         }

         width = Math.max(94, width);
         graphics.fill(left, top, left + width, top + 28, -1340991456);
         graphics.fill(left, top, left + 3, top + 28, -2689894);
         int crossX = left + 12;
         int crossY = top + 14;
         graphics.fill(crossX - 2, crossY - 7, crossX + 3, crossY + 8, -2689894);
         graphics.fill(crossX - 7, crossY - 2, crossX + 8, crossY + 3, -2689894);
         graphics.text(minecraft.font, healthText, healthTextX, top + 10, -1, true);
         graphics.fill(dividerX, top + 6, dividerX + 1, top + 22, 1618839444);
         graphics.blit(RenderPipelines.GUI_TEXTURED, CS2_SHIELD, shieldX, top + 6, 0.0F, 0.0F, 12, 16, 128, 144, 128, 144);
         graphics.text(minecraft.font, armorText, armorTextX, top + 10, -1, true);
         cursorX += 8;
         if (!absorptionText.isEmpty()) {
            graphics.text(minecraft.font, absorptionText, cursorX, top + 10, -14246, true);
            cursorX += minecraft.font.width(absorptionText) + 8;
         }

         if (!boostText.isEmpty()) {
            graphics.text(minecraft.font, boostText, cursorX, top + 10, -9971457, true);
         }
      }
   }

   private static void drawAmmo(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         ItemStack stack = minecraft.player.getMainHandItem();
         if (stack.getItem() instanceof GunItem gun) {
            String reserveUnit = gun.definition().reserveAsMagazines() ? "MAG" : "RND";
            String ammo = gun.ammo(stack) + "  |  " + gun.reserveAmmo(stack) + " " + reserveUnit;
            int width = Math.max(78, minecraft.font.width(ammo) + 18);
            int left = graphics.guiWidth() - width - 12;
            int top = graphics.guiHeight() - 42;
            graphics.fill(left, top, left + width, top + 28, -1340991456);
            graphics.fill(left, top, left + 3, top + 28, -1926597);
            Component weaponLabel = (Component)(GunClientController.isBurstMode()
               ? gun.getName(stack).copy().append(Component.translatable("hud.tactical_arsenal.burst_suffix"))
               : gun.getName(stack));
            graphics.text(minecraft.font, weaponLabel, left + 9, top + 5, -2564376, true);
            graphics.text(minecraft.font, ammo, left + 9, top + 16, -1, true);
            if (GunAnimationEngine.INSTANCE.isReloading()) {
               float progress = GunAnimationEngine.INSTANCE.reloadProgress(deltaTracker.getGameTimeDeltaPartialTick(false));
               int barWidth = Math.round((width - 9) * progress);
               graphics.fill(left + 6, top + 25, left + 6 + barWidth, top + 27, -1926597);
               graphics.text(minecraft.font, Component.translatable("hud.tactical_arsenal.reloading"), left + 9, top - 10, -1926597, true);
            }
         }
      }
   }

   private record ScopeBounds(int left, int top, int side, int centerX, int centerY) {
   }
}
