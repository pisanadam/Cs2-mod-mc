package dev.codex.tacticalarsenal.client.animation;

import dev.codex.tacticalarsenal.TacticalArsenal;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class KnifeAnimationLibrary implements SimpleSynchronousResourceReloadListener {
   private static final KnifeAnimationLibrary INSTANCE = new KnifeAnimationLibrary();
   private static final Identifier RESOURCE = TacticalArsenal.id("cs2_animations/knife_animations.tka");
   private static final int TKA2_MAGIC = 1414218034;
   private static final int VALUES_PER_TRANSFORM = 7;
   private static final int MAX_CLIPS = 64;
   private static final int MAX_FRAMES = 2000;
   private static final int ALL_TRACK_BITS = 63;
   private static final KnifeAnimationLibrary.Transform IDENTITY_TRANSFORM = new KnifeAnimationLibrary.Transform(new Vector3f(), new Quaternionf());
   private static final KnifeAnimationLibrary.Pose IDENTITY_POSE = new KnifeAnimationLibrary.Pose(
      IDENTITY_TRANSFORM, IDENTITY_TRANSFORM, IDENTITY_TRANSFORM, IDENTITY_TRANSFORM, IDENTITY_TRANSFORM, IDENTITY_TRANSFORM
   );
   private static final KnifeAnimationLibrary.Bindings IDENTITY_BINDINGS = new KnifeAnimationLibrary.Bindings(IDENTITY_TRANSFORM, IDENTITY_TRANSFORM);
   private volatile Map<String, KnifeAnimationLibrary.Clip> clips = Map.of();

   private KnifeAnimationLibrary() {
   }

   public static void initialize() {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
   }

   public static boolean hasClip(String clipId) {
      return INSTANCE.clips.containsKey(clipId);
   }

   public static float durationSeconds(String clipId, float fallback) {
      KnifeAnimationLibrary.Clip clip = INSTANCE.clips.get(clipId);
      return clip == null ? fallback : clip.durationSeconds();
   }

   public static KnifeAnimationLibrary.Pose sample(String clipId, float elapsedSeconds) {
      KnifeAnimationLibrary.Clip clip = INSTANCE.clips.get(clipId);
      return clip == null ? IDENTITY_POSE : clip.sample(elapsedSeconds);
   }

   public static KnifeAnimationLibrary.Bindings bindings(String clipId) {
      KnifeAnimationLibrary.Clip clip = INSTANCE.clips.get(clipId);
      return clip == null ? IDENTITY_BINDINGS : clip.bindings();
   }

   public Identifier getFabricId() {
      return TacticalArsenal.id("knife_animations");
   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      Resource resource = (Resource)resourceManager.getResource(RESOURCE).orElse(null);
      if (resource == null) {
         this.clips = Map.of();
         TacticalArsenal.LOGGER.warn("CS2 knife animation resource {} is missing", RESOURCE);
      } else {
         try (InputStream input = resource.open()) {
            Map<String, KnifeAnimationLibrary.Clip> loaded = read(input);
            this.clips = Map.copyOf(loaded);
            TacticalArsenal.LOGGER.info("Loaded {} exact CS2 knife animation clips", loaded.size());
         } catch (RuntimeException | IOException var8) {
            this.clips = Map.of();
            TacticalArsenal.LOGGER.warn("Could not load CS2 knife animation resource {}; static poses will be used", RESOURCE, var8);
         }
      }
   }

   private static Map<String, KnifeAnimationLibrary.Clip> read(InputStream rawInput) throws IOException {
      DataInputStream input = new DataInputStream(new BufferedInputStream(rawInput));
      if (input.readInt() != 1414218034) {
         throw new IOException("Invalid TKA2 magic");
      } else {
         int clipCount = input.readInt();
         if (clipCount > 0 && clipCount <= 64) {
            Map<String, KnifeAnimationLibrary.Clip> loaded = new LinkedHashMap<>();
            int clipIndex = 0;

            while (clipIndex < clipCount) {
               int idLength = input.readUnsignedByte();
               if (idLength == 0) {
                  throw new IOException("Empty TKA2 clip id");
               }

               byte[] encodedId = input.readNBytes(idLength);
               if (encodedId.length != idLength) {
                  throw new IOException("Truncated TKA2 clip id");
               }

               String clipId = new String(encodedId, StandardCharsets.US_ASCII);
               float durationSeconds = input.readFloat();
               int frameCount = input.readInt();
               int trackMask = input.readUnsignedByte();
               if (Float.isFinite(durationSeconds) && !(durationSeconds <= 0.0F) && !(durationSeconds > 60.0F)) {
                  if (frameCount > 0 && frameCount <= 2000) {
                     if (trackMask != 0 && (trackMask & -64) == 0) {
                        KnifeAnimationLibrary.Bindings bindings = new KnifeAnimationLibrary.Bindings(
                           readTransform(input, clipId, "hand_l binding"), readTransform(input, clipId, "hand_r binding")
                        );
                        EnumMap<KnifeAnimationLibrary.Track, float[]> tracks = new EnumMap<>(KnifeAnimationLibrary.Track.class);

                        for (KnifeAnimationLibrary.Track track : KnifeAnimationLibrary.Track.values()) {
                           if ((trackMask & track.bit) != 0) {
                              tracks.put(track, new float[Math.multiplyExact(frameCount, 7)]);
                           }
                        }

                        for (int frame = 0; frame < frameCount; frame++) {
                           for (KnifeAnimationLibrary.Track trackx : KnifeAnimationLibrary.Track.values()) {
                              float[] values = tracks.get(trackx);
                              if (values != null) {
                                 int offset = frame * 7;

                                 for (int component = 0; component < 7; component++) {
                                    float value = input.readFloat();
                                    if (!Float.isFinite(value)) {
                                       throw new IOException("Non-finite transform in " + clipId);
                                    }

                                    values[offset + component] = value;
                                 }

                                 float lengthSquared = values[offset + 3] * values[offset + 3]
                                    + values[offset + 4] * values[offset + 4]
                                    + values[offset + 5] * values[offset + 5]
                                    + values[offset + 6] * values[offset + 6];
                                 if (lengthSquared < 0.98F || lengthSquared > 1.02F) {
                                    throw new IOException("Non-unit quaternion in " + clipId);
                                 }
                              }
                           }
                        }

                        for (Entry<KnifeAnimationLibrary.Track, float[]> entry : tracks.entrySet()) {
                           validateIdentityFrame(clipId, entry.getKey(), entry.getValue());
                        }

                        KnifeAnimationLibrary.Clip previous = loaded.put(
                           clipId, new KnifeAnimationLibrary.Clip(durationSeconds, frameCount, Map.copyOf(tracks), bindings)
                        );
                        if (previous != null) {
                           throw new IOException("Duplicate TKA2 clip " + clipId);
                        }

                        clipIndex++;
                        continue;
                     }

                     throw new IOException("Invalid track mask for " + clipId);
                  }

                  throw new IOException("Invalid frame count for " + clipId);
               }

               throw new IOException("Invalid duration for " + clipId);
            }

            if (input.read() != -1) {
               throw new IOException("Trailing TKA2 data");
            } else {
               return loaded;
            }
         } else {
            throw new IOException("Invalid TKA2 clip count " + clipCount);
         }
      }
   }

   private static KnifeAnimationLibrary.Transform readTransform(DataInputStream input, String clipId, String label) throws IOException {
      float[] values = new float[7];

      for (int component = 0; component < 7; component++) {
         values[component] = input.readFloat();
         if (!Float.isFinite(values[component])) {
            throw new IOException("Non-finite " + label + " in " + clipId);
         }
      }

      float lengthSquared = values[3] * values[3] + values[4] * values[4] + values[5] * values[5] + values[6] * values[6];
      if (!(lengthSquared < 0.98F) && !(lengthSquared > 1.02F)) {
         return new KnifeAnimationLibrary.Transform(
            new Vector3f(values[0], values[1], values[2]), new Quaternionf(values[3], values[4], values[5], values[6]).normalize()
         );
      } else {
         throw new IOException("Non-unit " + label + " quaternion in " + clipId);
      }
   }

   private static void validateIdentityFrame(String clipId, KnifeAnimationLibrary.Track track, float[] values) throws IOException {
      float[] identity = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};

      for (int component = 0; component < 7; component++) {
         if (Math.abs(values[component] - identity[component]) > 1.0E-4F) {
            throw new IOException(clipId + "/" + track + " does not start at identity");
         }
      }
   }

   private static KnifeAnimationLibrary.Transform blendTransform(KnifeAnimationLibrary.Transform transform, float amount) {
      return new KnifeAnimationLibrary.Transform(
         new Vector3f(transform.translation()).mul(1.0F - amount), new Quaternionf(transform.rotation()).slerp(new Quaternionf(), amount).normalize()
      );
   }

   private static float clamp(float value, float minimum, float maximum) {
      return Math.max(minimum, Math.min(maximum, value));
   }

   private static float lerp(float amount, float first, float second) {
      return first + (second - first) * amount;
   }

   public record Bindings(KnifeAnimationLibrary.Transform handL, KnifeAnimationLibrary.Transform handR) {
   }

   private record Clip(float durationSeconds, int frameCount, Map<KnifeAnimationLibrary.Track, float[]> tracks, KnifeAnimationLibrary.Bindings bindings) {
      private KnifeAnimationLibrary.Pose sample(float elapsedSeconds) {
         float clampedTime = KnifeAnimationLibrary.clamp(elapsedSeconds, 0.0F, this.durationSeconds);
         float framePosition = clampedTime * this.frameCount / this.durationSeconds;
         int lowerFrame = Math.min((int)Math.floor(framePosition), this.frameCount - 1);
         int upperFrame = Math.min(lowerFrame + 1, this.frameCount - 1);
         float amount = KnifeAnimationLibrary.clamp(framePosition - lowerFrame, 0.0F, 1.0F);
         return new KnifeAnimationLibrary.Pose(
            this.sampleTrack(KnifeAnimationLibrary.Track.BODY, lowerFrame, upperFrame, amount),
            this.sampleTrack(KnifeAnimationLibrary.Track.BLADE, lowerFrame, upperFrame, amount),
            this.sampleTrack(KnifeAnimationLibrary.Track.REAR, lowerFrame, upperFrame, amount),
            this.sampleTrack(KnifeAnimationLibrary.Track.LOCK, lowerFrame, upperFrame, amount),
            this.sampleTrack(KnifeAnimationLibrary.Track.HAND_L, lowerFrame, upperFrame, amount),
            this.sampleTrack(KnifeAnimationLibrary.Track.HAND_R, lowerFrame, upperFrame, amount)
         );
      }

      private KnifeAnimationLibrary.Transform sampleTrack(KnifeAnimationLibrary.Track track, int lowerFrame, int upperFrame, float amount) {
         float[] values = this.tracks.get(track);
         if (values == null) {
            return KnifeAnimationLibrary.IDENTITY_TRANSFORM;
         } else {
            int lower = lowerFrame * 7;
            int upper = upperFrame * 7;
            Vector3f translation = new Vector3f(
               KnifeAnimationLibrary.lerp(amount, values[lower], values[upper]),
               KnifeAnimationLibrary.lerp(amount, values[lower + 1], values[upper + 1]),
               KnifeAnimationLibrary.lerp(amount, values[lower + 2], values[upper + 2])
            );
            Quaternionf rotation = new Quaternionf(values[lower + 3], values[lower + 4], values[lower + 5], values[lower + 6])
               .slerp(new Quaternionf(values[upper + 3], values[upper + 4], values[upper + 5], values[upper + 6]), amount)
               .normalize();
            return new KnifeAnimationLibrary.Transform(translation, rotation);
         }
      }
   }

   public record Pose(
      KnifeAnimationLibrary.Transform body,
      KnifeAnimationLibrary.Transform blade,
      KnifeAnimationLibrary.Transform rear,
      KnifeAnimationLibrary.Transform lock,
      KnifeAnimationLibrary.Transform handL,
      KnifeAnimationLibrary.Transform handR
   ) {
      public KnifeAnimationLibrary.Transform track(KnifeAnimationLibrary.Track track) {
         return switch (track) {
            case BODY -> this.body;
            case BLADE -> this.blade;
            case REAR -> this.rear;
            case LOCK -> this.lock;
            case HAND_L -> this.handL;
            case HAND_R -> this.handR;
         };
      }

      public KnifeAnimationLibrary.Pose blendToIdentity(float rawAmount) {
         float amount = KnifeAnimationLibrary.clamp(rawAmount, 0.0F, 1.0F);
         if (amount <= 0.0F) {
            return this;
         } else {
            return amount >= 1.0F
               ? KnifeAnimationLibrary.IDENTITY_POSE
               : new KnifeAnimationLibrary.Pose(
                  KnifeAnimationLibrary.blendTransform(this.body, amount),
                  KnifeAnimationLibrary.blendTransform(this.blade, amount),
                  KnifeAnimationLibrary.blendTransform(this.rear, amount),
                  KnifeAnimationLibrary.blendTransform(this.lock, amount),
                  KnifeAnimationLibrary.blendTransform(this.handL, amount),
                  KnifeAnimationLibrary.blendTransform(this.handR, amount)
               );
         }
      }
   }

   public static enum Track {
      BODY(1),
      BLADE(2),
      REAR(4),
      LOCK(8),
      HAND_L(16),
      HAND_R(32);

      private final int bit;

      private Track(int bit) {
         this.bit = bit;
      }
   }

   public record Transform(Vector3fc translation, Quaternionfc rotation) {
   }
}
