package dev.codex.tacticalarsenal.client.render;

import dev.codex.tacticalarsenal.gun.GunItem;
import dev.codex.tacticalarsenal.npc.CombatantEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public final class CombatantRenderer extends HumanoidMobRenderer<CombatantEntity, CombatantRenderState, HumanoidModel<CombatantRenderState>> {
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
      this.layers.add(0, new Cs2CombatantSkinLayer(this, meshPrefix));
      this.layers.add(1, new CombatantDualPistolLayer(this));
      ArmorModelSet<HumanoidModel<CombatantRenderState>> armorModels = ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new);
      this.addLayer(new HumanoidArmorLayer(this, armorModels, context.getEquipmentRenderer()));
   }

   public CombatantRenderState createRenderState() {
      return new CombatantRenderState();
   }

   public Identifier getTextureLocation(CombatantRenderState state) {
      return this.texture;
   }

   protected RenderType getRenderType(CombatantRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
      return Cs2CombatantSkinLayer.isReady(this.meshPrefix) ? null : super.getRenderType(state, bodyVisible, translucent, glowing);
   }

   public void extractRenderState(CombatantEntity entity, CombatantRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      state.entityId = entity.getId();
      if (CombatantDualPistolLayer.isDualPistol(state.getMainHandItemStack()) && CombatantDualPistolLayer.isReady()) {
         state.getMainHandItemState().clear();
      }
   }

   protected ArmPose getArmPose(CombatantEntity entity, HumanoidArm arm) {
      if (arm != entity.getMainArm()) {
         return ArmPose.EMPTY;
      } else {
         ItemStack stack = entity.getItemHeldByArm(arm);
         return stack.getItem() instanceof GunItem ? ArmPose.CROSSBOW_HOLD : super.getArmPose(entity, arm);
      }
   }
}
