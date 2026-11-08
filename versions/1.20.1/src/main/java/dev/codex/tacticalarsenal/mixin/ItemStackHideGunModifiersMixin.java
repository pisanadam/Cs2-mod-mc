package dev.codex.tacticalarsenal.mixin;

import dev.codex.tacticalarsenal.gun.GunItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.x hides the guns' held-speed modifier in tooltips; 1.20.1 does that through the hide flags. */
@Mixin({ItemStack.class})
public abstract class ItemStackHideGunModifiersMixin {
   @Inject(method = {"getHideFlags"}, at = {@At("RETURN")}, cancellable = true)
   private void tacticalArsenal$hideGunModifiers(CallbackInfoReturnable<Integer> callback) {
      if (((ItemStack)(Object)this).getItem() instanceof GunItem) {
         callback.setReturnValue(callback.getReturnValueI() | ItemStack.TooltipPart.MODIFIERS.getMask());
      }
   }
}
