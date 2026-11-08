package dev.codex.tacticalarsenal.client.animation;

import net.minecraft.util.Mth;

public final class CriticalSpring {
   private final float angularFrequency;
   private float previousPosition;
   private float position;
   private float velocity;

   public CriticalSpring(float angularFrequency) {
      this.angularFrequency = angularFrequency;
   }

   public void impulse(float amount) {
      this.velocity += amount;
   }

   public void tick(float target, float deltaSeconds) {
      this.previousPosition = this.position;
      float displacement = this.position - target;
      float coefficient = this.velocity + this.angularFrequency * displacement;
      float decay = (float)Math.exp(-this.angularFrequency * deltaSeconds);
      this.position = target + (displacement + coefficient * deltaSeconds) * decay;
      this.velocity = (this.velocity - this.angularFrequency * coefficient * deltaSeconds) * decay;
   }

   public float sample(float partialTick) {
      return Mth.lerp(Math.max(0.0F, Math.min(1.0F, partialTick)), this.previousPosition, this.position);
   }

   public void reset() {
      this.previousPosition = 0.0F;
      this.position = 0.0F;
      this.velocity = 0.0F;
   }
}
