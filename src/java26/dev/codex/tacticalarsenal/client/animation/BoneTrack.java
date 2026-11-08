package dev.codex.tacticalarsenal.client.animation;

import java.util.Arrays;

public final class BoneTrack {
   private final Keyframe[] keyframes;

   public BoneTrack(Keyframe... keyframes) {
      if (keyframes.length == 0) {
         throw new IllegalArgumentException("A bone track needs at least one keyframe");
      } else {
         this.keyframes = (Keyframe[])keyframes.clone();
         Arrays.sort(this.keyframes, (left, right) -> Float.compare(left.timeSeconds(), right.timeSeconds()));
      }
   }

   public Transform sample(float timeSeconds) {
      if (timeSeconds <= this.keyframes[0].timeSeconds()) {
         return this.keyframes[0].transform();
      } else {
         for (int index = 0; index < this.keyframes.length - 1; index++) {
            Keyframe from = this.keyframes[index];
            Keyframe to = this.keyframes[index + 1];
            if (timeSeconds <= to.timeSeconds()) {
               float span = Math.max(1.0E-4F, to.timeSeconds() - from.timeSeconds());
               float alpha = from.easingToNext().apply((timeSeconds - from.timeSeconds()) / span);
               return Transform.blend(from.transform(), to.transform(), alpha);
            }
         }

         return this.keyframes[this.keyframes.length - 1].transform();
      }
   }
}
