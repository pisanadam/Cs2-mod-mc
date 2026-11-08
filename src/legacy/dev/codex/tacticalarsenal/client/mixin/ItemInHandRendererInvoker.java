package dev.codex.tacticalarsenal.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ItemInHandRenderer.class})
public interface ItemInHandRendererInvoker {
   @Invoker("applyItemArmAttackTransform")
   void tacticalArsenal$applyItemArmAttackTransform(PoseStack poses, HumanoidArm arm, float attack);
}
