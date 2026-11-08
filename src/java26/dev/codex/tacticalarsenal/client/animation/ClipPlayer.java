package dev.codex.tacticalarsenal.client.animation;

public final class ClipPlayer {
   private AnimationClip clip;
   private float elapsedSeconds;
   private boolean playing;

   public void play(AnimationClip clip) {
      this.clip = clip;
      this.elapsedSeconds = 0.0F;
      this.playing = true;
   }

   public void stop() {
      this.clip = null;
      this.elapsedSeconds = 0.0F;
      this.playing = false;
   }

   public void tick(float deltaSeconds) {
      if (this.playing && this.clip != null) {
         this.elapsedSeconds += deltaSeconds;
         if (this.elapsedSeconds >= this.clip.durationSeconds()) {
            this.elapsedSeconds = this.clip.durationSeconds();
            this.playing = false;
         }
      }
   }

   public Transform sample(Bone bone, float partialTick) {
      if (this.clip == null) {
         return Transform.IDENTITY;
      } else {
         float renderTime = Math.min(this.clip.durationSeconds(), this.elapsedSeconds + Math.max(0.0F, partialTick) / 20.0F);
         return this.clip.sample(bone, renderTime);
      }
   }

   public boolean isPlaying() {
      return this.playing;
   }

   public float progress(float partialTick) {
      return this.clip == null ? 0.0F : Math.min(1.0F, (this.elapsedSeconds + partialTick / 20.0F) / this.clip.durationSeconds());
   }
}
