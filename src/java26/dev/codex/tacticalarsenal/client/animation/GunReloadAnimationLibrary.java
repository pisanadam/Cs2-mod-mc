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
import org.joml.Vector3f;

public final class GunReloadAnimationLibrary implements SimpleSynchronousResourceReloadListener {
   private static final GunReloadAnimationLibrary INSTANCE = new GunReloadAnimationLibrary();
   private static final Identifier RESOURCE = TacticalArsenal.id("cs2_animations/gun_reload_animations.tgr");
   private static final int TGR1_MAGIC = 1413960241;
   private static final int FLAG_EMPTY = 1;
   private static final int FLAG_SHELL_SINGLE_CYCLE = 2;
   private static final int ALL_FLAGS = 3;
   private static final int REQUIRED_TRACK_BITS = GunReloadAnimationLibrary.Track.ROOT.bit
      | GunReloadAnimationLibrary.Track.LEFT_HAND.bit
      | GunReloadAnimationLibrary.Track.RIGHT_HAND.bit;
   private static final int MAX_CLIPS = 128;
   private static final int MAX_FRAMES_PER_CLIP = 4096;
   private static final int MAX_TOTAL_TRACK_KEYFRAMES = 1000000;
   private static final int MAX_SHELL_REPEATS = 256;
   private static final int MAX_GENERATED_TRACK_KEYFRAMES = 100000;
   private static final float MAX_DURATION_SECONDS = 60.0F;
   private static final float MAX_ABS_TRANSLATION = 64.0F;
   private static final float IDENTITY_EPSILON = 1.0E-4F;
   private static final float MIN_QUATERNION_LENGTH_SQUARED = 0.98F;
   private static final float MAX_QUATERNION_LENGTH_SQUARED = 1.02F;
   private volatile Map<String, GunReloadAnimationLibrary.LoadedClip> clips = Map.of();

   private GunReloadAnimationLibrary() {
   }

   public static void initialize() {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
   }

   public static AnimationClip clip(String weaponId, boolean empty) {
      if (!validWeaponId(weaponId)) {
         return null;
      } else {
         Map<String, GunReloadAnimationLibrary.LoadedClip> loaded = INSTANCE.clips;
         GunReloadAnimationLibrary.LoadedClip selected = loaded.get(weaponId + (empty ? "/reload_empty" : "/reload"));
         if (selected != null && !selected.isShellCycle() && selected.isEmptyReload() == empty) {
            return selected.animation;
         } else {
            if (empty) {
               GunReloadAnimationLibrary.LoadedClip normal = loaded.get(weaponId + "/reload");
               if (normal != null && !normal.isEmptyReload() && !normal.isShellCycle()) {
                  return normal.animation;
               }
            }

            return null;
         }
      }
   }

   public static boolean hasClip(String weaponId) {
      if (!validWeaponId(weaponId)) {
         return false;
      } else {
         Map<String, GunReloadAnimationLibrary.LoadedClip> loaded = INSTANCE.clips;
         return loaded.containsKey(weaponId + "/reload") || loaded.containsKey(weaponId + "/reload_empty") || loaded.containsKey(weaponId + "/reload_shell");
      }
   }

   public static AnimationClip shellReload(String weaponId, int requestedShells) {
      if (validWeaponId(weaponId) && requestedShells > 0 && requestedShells <= 256) {
         GunReloadAnimationLibrary.LoadedClip cycle = INSTANCE.clips.get(weaponId + "/reload_shell");
         if (cycle == null || !cycle.isShellCycle() || cycle.isEmptyReload()) {
            return null;
         } else {
            return requestedShells == 1 ? cycle.animation : repeatShellCycle(cycle, requestedShells);
         }
      } else {
         return null;
      }
   }

   public Identifier getFabricId() {
      return TacticalArsenal.id("gun_reload_animations");
   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      Resource resource = (Resource)resourceManager.getResource(RESOURCE).orElse(null);
      if (resource == null) {
         this.clips = Map.of();
         TacticalArsenal.LOGGER.warn("CS2 gun reload animation resource {} is missing; procedural reloads will be used", RESOURCE);
      } else {
         try (InputStream input = resource.open()) {
            Map<String, GunReloadAnimationLibrary.LoadedClip> loaded = read(input);
            this.clips = Map.copyOf(loaded);
            TacticalArsenal.LOGGER.info("Loaded {} exact CS2 gun reload animation clips", loaded.size());
         } catch (RuntimeException | IOException var8) {
            this.clips = Map.of();
            TacticalArsenal.LOGGER.warn("Could not load CS2 gun reload animation resource {}; procedural reloads will be used", RESOURCE, var8);
         }
      }
   }

   private static Map<String, GunReloadAnimationLibrary.LoadedClip> read(InputStream rawInput) throws IOException {
      DataInputStream input = new DataInputStream(new BufferedInputStream(rawInput));
      if (input.readInt() != 1413960241) {
         throw new IOException("Invalid TGR1 magic");
      } else {
         int clipCount = input.readInt();
         if (clipCount > 0 && clipCount <= 128) {
            Map<String, GunReloadAnimationLibrary.LoadedClip> loaded = new LinkedHashMap<>();
            long totalTrackKeyframes = 0L;

            for (int clipIndex = 0; clipIndex < clipCount; clipIndex++) {
               String clipId = readClipId(input);
               float durationSeconds = input.readFloat();
               int frameCount = input.readInt();
               int trackMask = input.readUnsignedByte();
               int flags = input.readUnsignedByte();
               validateHeader(clipId, durationSeconds, frameCount, trackMask, flags);
               long clipTrackKeyframes = (long)frameCount * Integer.bitCount(trackMask);
               totalTrackKeyframes += clipTrackKeyframes;
               if (totalTrackKeyframes > 1000000L) {
                  throw new IOException("TGR1 animation data exceeds the keyframe limit");
               }

               EnumMap<GunReloadAnimationLibrary.Track, Keyframe[]> tracks = new EnumMap<>(GunReloadAnimationLibrary.Track.class);

               for (GunReloadAnimationLibrary.Track track : GunReloadAnimationLibrary.Track.values()) {
                  if ((trackMask & track.bit) != 0) {
                     tracks.put(track, new Keyframe[frameCount]);
                  }
               }

               float frameDenominator = frameCount - 1.0F;

               for (int frame = 0; frame < frameCount; frame++) {
                  float timeSeconds = durationSeconds * frame / frameDenominator;

                  for (GunReloadAnimationLibrary.Track trackx : GunReloadAnimationLibrary.Track.values()) {
                     Keyframe[] keyframes = tracks.get(trackx);
                     if (keyframes != null) {
                        Transform transform = readTransform(input, clipId, trackx, frame);
                        if (frame == 0 || frame == frameCount - 1) {
                           validateBoundaryIdentity(clipId, trackx, frame, transform);
                        }

                        keyframes[frame] = new Keyframe(timeSeconds, transform, Easing.LINEAR);
                     }
                  }
               }

               AnimationClip animation = buildAnimation(clipId, durationSeconds, tracks);
               GunReloadAnimationLibrary.LoadedClip previous = loaded.put(
                  clipId, new GunReloadAnimationLibrary.LoadedClip(clipId, durationSeconds, frameCount, flags, Map.copyOf(tracks), animation)
               );
               if (previous != null) {
                  throw new IOException("Duplicate TGR1 clip " + clipId);
               }
            }

            if (input.read() != -1) {
               throw new IOException("Trailing TGR1 data");
            } else {
               return loaded;
            }
         } else {
            throw new IOException("Invalid TGR1 clip count " + clipCount);
         }
      }
   }

   private static String readClipId(DataInputStream input) throws IOException {
      int idLength = input.readUnsignedByte();
      if (idLength == 0) {
         throw new IOException("Empty TGR1 clip id");
      } else {
         byte[] encodedId = input.readNBytes(idLength);
         if (encodedId.length != idLength) {
            throw new IOException("Truncated TGR1 clip id");
         } else {
            for (byte value : encodedId) {
               int unsigned = Byte.toUnsignedInt(value);
               if (unsigned < 33 || unsigned > 126) {
                  throw new IOException("TGR1 clip id is not printable ASCII");
               }
            }

            return new String(encodedId, StandardCharsets.US_ASCII);
         }
      }
   }

   private static void validateHeader(String clipId, float durationSeconds, int frameCount, int trackMask, int flags) throws IOException {
      int expectedFlags = expectedFlags(clipId);
      if (expectedFlags < 0 || flags != expectedFlags || (flags & -4) != 0) {
         throw new IOException("Invalid TGR1 id/flags combination for " + clipId);
      } else if (!Float.isFinite(durationSeconds) || durationSeconds <= 0.0F || durationSeconds > 60.0F) {
         throw new IOException("Invalid duration for " + clipId);
      } else if (frameCount >= 2 && frameCount <= 4096) {
         if ((trackMask & ~allTrackBits()) != 0 || (trackMask & REQUIRED_TRACK_BITS) != REQUIRED_TRACK_BITS) {
            throw new IOException("Invalid or incomplete track mask for " + clipId);
         }
      } else {
         throw new IOException("Invalid frame count for " + clipId);
      }
   }

   private static int expectedFlags(String clipId) {
      String suffix;
      int flags;
      if (clipId.endsWith("/reload_empty")) {
         suffix = "/reload_empty";
         flags = 1;
      } else if (clipId.endsWith("/reload_shell")) {
         suffix = "/reload_shell";
         flags = 2;
      } else {
         if (!clipId.endsWith("/reload")) {
            return -1;
         }

         suffix = "/reload";
         flags = 0;
      }

      String weaponId = clipId.substring(0, clipId.length() - suffix.length());
      return validWeaponId(weaponId) ? flags : -1;
   }

   private static boolean validWeaponId(String weaponId) {
      if (weaponId != null && !weaponId.isEmpty() && weaponId.length() <= 96) {
         for (int index = 0; index < weaponId.length(); index++) {
            char value = weaponId.charAt(index);
            if ((value < 'a' || value > 'z') && (value < '0' || value > '9') && value != '_') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static int allTrackBits() {
      int bits = 0;

      for (GunReloadAnimationLibrary.Track track : GunReloadAnimationLibrary.Track.values()) {
         bits |= track.bit;
      }

      return bits;
   }

   private static Transform readTransform(DataInputStream input, String clipId, GunReloadAnimationLibrary.Track track, int frame) throws IOException {
      float x = readFinite(input, clipId, track, frame);
      float y = readFinite(input, clipId, track, frame);
      float z = readFinite(input, clipId, track, frame);
      if (!(Math.abs(x) > 64.0F) && !(Math.abs(y) > 64.0F) && !(Math.abs(z) > 64.0F)) {
         float qx = readFinite(input, clipId, track, frame);
         float qy = readFinite(input, clipId, track, frame);
         float qz = readFinite(input, clipId, track, frame);
         float qw = readFinite(input, clipId, track, frame);
         float lengthSquared = qx * qx + qy * qy + qz * qz + qw * qw;
         if (!(lengthSquared < 0.98F) && !(lengthSquared > 1.02F)) {
            return new Transform(new Vector3f(x, y, z), new Quaternionf(qx, qy, qz, qw).normalize(), new Vector3f(1.0F));
         } else {
            throw new IOException("Non-unit quaternion in " + clipId + "/" + track);
         }
      } else {
         throw new IOException("Out-of-range translation in " + clipId + "/" + track);
      }
   }

   private static float readFinite(DataInputStream input, String clipId, GunReloadAnimationLibrary.Track track, int frame) throws IOException {
      float value = input.readFloat();
      if (!Float.isFinite(value)) {
         throw new IOException("Non-finite transform in " + clipId + "/" + track + " frame " + frame);
      } else {
         return value;
      }
   }

   private static void validateBoundaryIdentity(String clipId, GunReloadAnimationLibrary.Track track, int frame, Transform transform) throws IOException {
      if (Math.abs(transform.translation().x()) > 1.0E-4F
         || Math.abs(transform.translation().y()) > 1.0E-4F
         || Math.abs(transform.translation().z()) > 1.0E-4F
         || Math.abs(transform.rotation().x()) > 1.0E-4F
         || Math.abs(transform.rotation().y()) > 1.0E-4F
         || Math.abs(transform.rotation().z()) > 1.0E-4F
         || Math.abs(Math.abs(transform.rotation().w()) - 1.0F) > 1.0E-4F) {
         throw new IOException(clipId + "/" + track + " boundary frame " + frame + " is not identity");
      }
   }

   private static AnimationClip buildAnimation(String name, float durationSeconds, Map<GunReloadAnimationLibrary.Track, Keyframe[]> tracks) {
      AnimationClip.Builder builder = AnimationClip.builder(name, durationSeconds);

      for (GunReloadAnimationLibrary.Track track : GunReloadAnimationLibrary.Track.values()) {
         Keyframe[] keyframes = tracks.get(track);
         if (keyframes != null) {
            builder.track(track.bone, keyframes);
         }
      }

      return builder.build();
   }

   private static AnimationClip repeatShellCycle(GunReloadAnimationLibrary.LoadedClip cycle, int repeats) {
      long repeatedFrameCount = 1L + repeats * (cycle.frameCount - 1L);
      if (repeatedFrameCount > 100000L) {
         return null;
      } else {
         float repeatedDuration = cycle.durationSeconds * repeats;
         if (!Float.isFinite(repeatedDuration)) {
            return null;
         } else {
            EnumMap<GunReloadAnimationLibrary.Track, Keyframe[]> repeatedTracks = new EnumMap<>(GunReloadAnimationLibrary.Track.class);

            for (Entry<GunReloadAnimationLibrary.Track, Keyframe[]> entry : cycle.tracks.entrySet()) {
               Keyframe[] source = entry.getValue();
               Keyframe[] destination = new Keyframe[(int)repeatedFrameCount];
               int destinationIndex = 0;

               for (int cycleIndex = 0; cycleIndex < repeats; cycleIndex++) {
                  int firstFrame = cycleIndex == 0 ? 0 : 1;
                  float timeOffset = cycleIndex * cycle.durationSeconds;

                  for (int frame = firstFrame; frame < source.length; frame++) {
                     Keyframe keyframe = source[frame];
                     destination[destinationIndex++] = new Keyframe(timeOffset + keyframe.timeSeconds(), keyframe.transform(), Easing.LINEAR);
                  }
               }

               repeatedTracks.put(entry.getKey(), destination);
            }

            return buildAnimation(
               cycle.id.substring(0, cycle.id.length() - "/reload_shell".length()) + "/reload_shells_" + repeats, repeatedDuration, repeatedTracks
            );
         }
      }
   }

   private record LoadedClip(
      String id, float durationSeconds, int frameCount, int flags, Map<GunReloadAnimationLibrary.Track, Keyframe[]> tracks, AnimationClip animation
   ) {
      private boolean isEmptyReload() {
         return (this.flags & 1) != 0;
      }

      private boolean isShellCycle() {
         return (this.flags & 2) != 0;
      }
   }

   private static enum Track {
      ROOT(1, Bone.ROOT),
      MAGAZINE(2, Bone.MAGAZINE),
      ACTION(4, Bone.ACTION),
      LEFT_HAND(8, Bone.LEFT_HAND),
      RIGHT_HAND(16, Bone.RIGHT_HAND);

      private final int bit;
      private final Bone bone;

      private Track(int bit, Bone bone) {
         this.bit = bit;
         this.bone = bone;
      }
   }
}
