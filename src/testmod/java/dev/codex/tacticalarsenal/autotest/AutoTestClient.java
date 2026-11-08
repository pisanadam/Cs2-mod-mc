package dev.codex.tacticalarsenal.autotest;

import dev.codex.tacticalarsenal.client.compat.ClientCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dev-only scripted scene used to take screenshots of the mod in every Minecraft version. */
public final class AutoTestClient implements ClientModInitializer {
   private static final Logger LOGGER = LoggerFactory.getLogger("autotest");
   private final List<Step> steps = new ArrayList<>();
   private int tick;
   private int stepIndex;
   private int nextAt = -1;

   private record Step(int delay, Consumer<Minecraft> action) {
   }

   private void step(int delay, Consumer<Minecraft> action) {
      this.steps.add(new Step(delay, action));
   }

   private static void cmd(Minecraft mc, String command) {
      mc.player.connection.sendCommand(command);
   }

   private static void shot(String name) {
      LOGGER.info("AUTOTEST_SHOT {}", name);
   }

   @Override
   public void onInitializeClient() {
      if (!Boolean.getBoolean("tactical_arsenal.autotest")) {
         return;
      }
      step(60, mc -> {
         ClientCompat.setScreen(mc, null);
         cmd(mc, "gamemode creative");
         cmd(mc, "time set noon");
         cmd(mc, "weather clear");
         cmd(mc, "tp @s ~ ~ ~ 0 0");
         cmd(mc, "item replace entity @s weapon.mainhand with tactical_arsenal:ak47");
         cmd(mc, "item replace entity @s hotbar.1 with tactical_arsenal:awp");
         cmd(mc, "item replace entity @s hotbar.2 with tactical_arsenal:knife");
         cmd(mc, "item replace entity @s hotbar.3 with tactical_arsenal:elite");
         cmd(mc, "item replace entity @s hotbar.4 with tactical_arsenal:cs_kevlar");
         cmd(mc, "item replace entity @s hotbar.5 with tactical_arsenal:team_player_tier_1_spawn_egg");
      });
      step(60, mc -> shot("fp_ak47"));
      step(40, mc -> mc.options.keyAttack.setDown(true));
      step(8, mc -> shot("fp_ak47_firing"));
      step(20, mc -> mc.options.keyAttack.setDown(false));
      step(40, mc -> ClientCompat.setScreen(mc, new InventoryScreen(mc.player)));
      step(30, mc -> shot("inventory"));
      step(40, mc -> {
         ClientCompat.setScreen(mc, null);
         mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      });
      step(30, mc -> shot("tp_front_ak47"));
      step(40, mc -> {
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         cmd(mc, "item replace entity @s weapon.mainhand with tactical_arsenal:knife");
      });
      step(30, mc -> shot("fp_knife"));
      step(40, mc -> cmd(mc, "item replace entity @s weapon.mainhand with tactical_arsenal:elite"));
      step(30, mc -> shot("fp_elite"));
      step(40, mc -> cmd(mc, "item replace entity @s weapon.mainhand with tactical_arsenal:awp"));
      step(60, mc -> mc.options.keyUse.setDown(true));
      step(6, mc -> mc.options.keyUse.setDown(false));
      step(40, mc -> shot("fp_awp_scoped"));
      step(40, mc -> {
         mc.options.keyUse.setDown(false);
         cmd(mc, "summon tactical_arsenal:team_player_tier_1 ~ ~ ~4 {NoAI:1b,Rotation:[180f,0f]}");
         cmd(mc, "summon tactical_arsenal:team_natural_tier_3 ~2 ~ ~5 {NoAI:1b,Rotation:[180f,0f]}");
         cmd(mc, "item replace entity @s armor.chest with tactical_arsenal:cs_kevlar");
         cmd(mc, "item replace entity @s armor.head with tactical_arsenal:cs_helmet");
         cmd(mc, "item replace entity @s weapon.mainhand with tactical_arsenal:m4a4");
      });
      step(60, mc -> shot("npcs"));
      step(40, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
      step(30, mc -> shot("tp_front_armor_m4a4"));
      step(40, mc -> {
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         cmd(mc, "item replace entity @s weapon.mainhand with tactical_arsenal:ak47");
         mc.options.keyAttack.setDown(true);
      });
      step(30, mc -> shot("fp_shooting_npcs"));
      step(30, mc -> {
         mc.options.keyAttack.setDown(false);
         shot("after_shooting");
      });
      step(20, mc -> LOGGER.info("AUTOTEST_DONE"));
      ClientTickEvents.END_CLIENT_TICK.register(mc -> {
         if (mc.player == null || mc.level == null || this.stepIndex >= this.steps.size()) {
            return;
         }
         this.tick++;
         if (this.nextAt < 0) {
            this.nextAt = this.tick + this.steps.get(0).delay;
         }
         if (this.tick >= this.nextAt) {
            this.steps.get(this.stepIndex++).action.accept(mc);
            if (this.stepIndex < this.steps.size()) {
               this.nextAt = this.tick + this.steps.get(this.stepIndex).delay;
            }
         }
      });
   }
}
