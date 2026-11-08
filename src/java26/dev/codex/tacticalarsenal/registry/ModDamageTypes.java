package dev.codex.tacticalarsenal.registry;

import dev.codex.tacticalarsenal.TacticalArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
   public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, TacticalArsenal.id("bullet"));

   private ModDamageTypes() {
   }

   public static DamageSource bullet(Level level, Entity shooter) {
      return level.damageSources().source(BULLET, shooter);
   }
}
