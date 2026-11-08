package dev.codex.tacticalarsenal.client.animation;

import java.util.EnumMap;
import java.util.Map;

public final class AnimationClip {
   private final String name;
   private final float durationSeconds;
   private final Map<Bone, BoneTrack> tracks;

   private AnimationClip(String name, float durationSeconds, Map<Bone, BoneTrack> tracks) {
      this.name = name;
      this.durationSeconds = durationSeconds;
      this.tracks = Map.copyOf(tracks);
   }

   public String name() {
      return this.name;
   }

   public float durationSeconds() {
      return this.durationSeconds;
   }

   public Transform sample(Bone bone, float timeSeconds) {
      BoneTrack track = this.tracks.get(bone);
      return track == null ? Transform.IDENTITY : track.sample(Math.min(timeSeconds, this.durationSeconds));
   }

   public static AnimationClip.Builder builder(String name, float durationSeconds) {
      return new AnimationClip.Builder(name, durationSeconds);
   }

   public static final class Builder {
      private final String name;
      private final float durationSeconds;
      private final Map<Bone, BoneTrack> tracks = new EnumMap<>(Bone.class);

      private Builder(String name, float durationSeconds) {
         if (durationSeconds <= 0.0F) {
            throw new IllegalArgumentException("Animation duration must be positive");
         } else {
            this.name = name;
            this.durationSeconds = durationSeconds;
         }
      }

      public AnimationClip.Builder track(Bone bone, Keyframe... keyframes) {
         this.tracks.put(bone, new BoneTrack(keyframes));
         return this;
      }

      public AnimationClip build() {
         return new AnimationClip(this.name, this.durationSeconds, this.tracks);
      }
   }
}
