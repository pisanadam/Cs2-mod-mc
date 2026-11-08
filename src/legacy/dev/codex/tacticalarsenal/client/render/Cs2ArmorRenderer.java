package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.client.compat.SubmitNodeCollector;
import dev.codex.tacticalarsenal.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** Minecraft 1.21.1 / 1.20.1 version of the CS2 helmet/kevlar renderer (same offsets and scales as 26.x). */
public final class Cs2ArmorRenderer implements ArmorRenderer {
   private static final Cs2ArmorRenderer INSTANCE = new Cs2ArmorRenderer();

   private Cs2ArmorRenderer() {
   }

   public static void initialize() {
      ArmorRenderer.register(INSTANCE, ModItems.CS_HELMET, ModItems.CS_KEVLAR);
   }

   @Override
   public void render(
      PoseStack poses, MultiBufferSource buffers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> parentModel
   ) {
      if (entity instanceof dev.codex.tacticalarsenal.npc.CombatantEntity) {
         // In 26.x the combatants' loadout helmet/kevlar never reach the client render state, so
         // they are not drawn on combatants; keep that look.
         return;
      }

      SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
      if (slot == EquipmentSlot.HEAD && stack.getItem() == ModItems.CS_HELMET) {
         renderHelmet(poses, collector, light, parentModel);
      } else if (slot == EquipmentSlot.CHEST && stack.getItem() == ModItems.CS_KEVLAR) {
         renderKevlar(poses, collector, light, parentModel);
      }
   }

   private static void renderHelmet(PoseStack poses, SubmitNodeCollector collector, int light, HumanoidModel<?> parentModel) {
      poses.pushPose();
      parentModel.head.translateAndRotate(poses);
      poses.translate(-0.0077F, -0.2603F, 0.0052F);
      orientSource2Equipment(poses);
      poses.scale(2.45F, 2.45F, 2.45F);
      Cs2MeshManager.submit("cs_helmet", poses, collector, light);
      poses.popPose();
   }

   private static void renderKevlar(PoseStack poses, SubmitNodeCollector collector, int light, HumanoidModel<?> parentModel) {
      poses.pushPose();
      parentModel.body.translateAndRotate(poses);
      poses.translate(-0.0045F, 0.3627F, 0.0031F);
      orientSource2Equipment(poses);
      poses.scale(1.44F, 1.44F, 1.44F);
      Cs2MeshManager.submit("cs_kevlar", poses, collector, light);
      poses.popPose();
   }

   private static void orientSource2Equipment(PoseStack poses) {
      poses.mulPose(new Quaternionf().rotationZ((float) Math.PI));
      poses.mulPose(new Quaternionf().rotationY((float) (-Math.PI / 2)));
   }
}
