package dev.codex.tacticalarsenal.client.animation;

public record Keyframe(float timeSeconds, Transform transform, Easing easingToNext) {
   public Keyframe(float timeSeconds, Transform transform, Easing easingToNext) {
      if (timeSeconds < 0.0F) {
         throw new IllegalArgumentException("Keyframe time cannot be negative");
      } else {
         this.timeSeconds = timeSeconds;
         this.transform = transform;
         this.easingToNext = easingToNext;
      }
   }
}
