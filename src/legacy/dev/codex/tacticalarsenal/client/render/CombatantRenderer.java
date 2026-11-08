package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Minecraft 1.21.1 / 1.20.1 version of the combatant renderer (same textures, layers and order as 26.x). */
public final class CombatantRenderer extends HumanoidMobRenderer<CombatantEntity, HumanoidModel<CombatantEntity>> {
   private static final Identifier PLAYER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
   private static final Identifier NATURAL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
   private final Identifier texture;
   private final String meshPrefix;

   public static CombatantRenderer player(Context context) {
      return new CombatantRenderer(context, PLAYER_TEXTURE, "ct");
   }

   public static CombatantRenderer natural(Context context) {
      return new CombatantRenderer(context, NATURAL_TEXTURE, "t");
   }

   private CombatantRenderer(Context context, Identifier texture, String meshPrefix) {
      super(context, new CombatantHumanoidModel(context.bakeLayer(ModelLayers.ZOMBIE), meshPrefix), 0.5F);
      this.texture = texture;
      this.meshPrefix = meshPrefix;
      // 26.x clears the main-hand item state for dual pistols; here the held-item layer skips it.
      for (int i = 0; i < this.layers.size(); i++) {
         if (this.layers.get(i) instanceof ItemInHandLayer) {
            this.layers.set(i, new CombatantItemInHandLayer(this, context));
         }
      }

      this.layers.add(0, new Cs2CombatantSkinLayer(this, meshPrefix));
      this.layers.add(1, new CombatantDualPistolLayer(this));
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
            context.getModelManager()
         )
      );
   }

   @Override
   public Identifier getTextureLocation(CombatantEntity entity) {
      return this.texture;
   }

   @Override
   protected RenderType getRenderType(CombatantEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      return Cs2CombatantSkinLayer.isReady(this.meshPrefix) ? null : super.getRenderType(entity, bodyVisible, translucent, glowing);
   }

   private static final class CombatantItemInHandLayer extends ItemInHandLayer<CombatantEntity, HumanoidModel<CombatantEntity>> {
      private CombatantItemInHandLayer(RenderLayerParent<CombatantEntity, HumanoidModel<CombatantEntity>> parent, Context context) {
         super(parent, context.getItemInHandRenderer());
      }

      @Override
      protected void renderArmWithItem(
         LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light
      ) {
         if (arm == entity.getMainArm() && CombatantDualPistolLayer.isDualPistol(stack) && CombatantDualPistolLayer.isReady()) {
            return;
         }

         super.renderArmWithItem(entity, stack, context, arm, poses, buffers, light);
      }
   }
}
