package dev.codex.tacticalarsenal.client.render;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Vector3f;

final class ThirdPersonWeaponTransforms {
   private static final ItemTransform DUAL_PISTOL_RIGHT = new ItemTransform(
      new Vector3f(-115.947F, -79.9764F, 55.0F), new Vector3f(-0.00577F, -0.2371F, -0.2466F), new Vector3f(0.8F, 0.8F, 0.8F)
   );
   private static final ItemTransform DUAL_PISTOL_LEFT = new ItemTransform(
      new Vector3f(127.7081F, -78.59F, -60.4417F), new Vector3f(0.010056F, -0.277458F, 0.023295F), new Vector3f(0.8F, 0.8F, 0.8F)
   );

   private ThirdPersonWeaponTransforms() {
   }

   static ItemTransform dualPistol(boolean leftHand) {
      return leftHand ? DUAL_PISTOL_LEFT : DUAL_PISTOL_RIGHT;
   }
}
