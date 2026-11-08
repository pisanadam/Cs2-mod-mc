package dev.codex.tacticalarsenal.client.animation;

public enum Easing {
   LINEAR {
      @Override
      public float apply(float value) {
         return value;
      }
   },
   SMOOTH_STEP {
      @Override
      public float apply(float value) {
         return value * value * (3.0F - 2.0F * value);
      }
   },
   EASE_OUT_CUBIC {
      @Override
      public float apply(float value) {
         float inverse = 1.0F - value;
         return 1.0F - inverse * inverse * inverse;
      }
   },
   EASE_IN_CUBIC {
      @Override
      public float apply(float value) {
         return value * value * value;
      }
   };

   public abstract float apply(float var1);
}
