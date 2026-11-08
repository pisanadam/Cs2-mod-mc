package dev.codex.tacticalarsenal.client.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

final class MuzzlePositionTracker {
   private static final long SAMPLE_TTL_NANOS = 250000000L;
   private static final int MAX_ENTRIES = 256;
   private static final Map<Integer, MuzzlePositionTracker.HeldMuzzles> THIRD_PERSON = new HashMap<>();

   private MuzzlePositionTracker() {
   }

   static synchronized void capture(int entityId, String weaponId, HumanoidArm mainArm, Vec3 negativeX, Vec3 positiveX) {
      long now = System.nanoTime();
      if (THIRD_PERSON.size() >= 256 && !THIRD_PERSON.containsKey(entityId)) {
         prune(now);
      }

      THIRD_PERSON.put(entityId, new MuzzlePositionTracker.HeldMuzzles(weaponId, mainArm == null ? HumanoidArm.RIGHT : mainArm, negativeX, positiveX, now));
   }

   static synchronized Vec3 resolve(int entityId, String weaponId, int patternIndex) {
      long now = System.nanoTime();
      MuzzlePositionTracker.HeldMuzzles sample = THIRD_PERSON.get(entityId);
      if (sample != null && now - sample.capturedNanos <= 250000000L && sample.weaponId.equals(weaponId)) {
         boolean mainHandShot = (patternIndex & 1) == 0;
         boolean positiveX = sample.mainArm == HumanoidArm.LEFT ? mainHandShot : !mainHandShot;
         return positiveX ? sample.positiveX : sample.negativeX;
      } else {
         if (sample != null) {
            THIRD_PERSON.remove(entityId);
         }

         return null;
      }
   }

   static synchronized void clear() {
      THIRD_PERSON.clear();
   }

   private static void prune(long now) {
      Iterator<MuzzlePositionTracker.HeldMuzzles> iterator = THIRD_PERSON.values().iterator();

      while (iterator.hasNext()) {
         if (now - iterator.next().capturedNanos > 250000000L) {
            iterator.remove();
         }
      }

      if (THIRD_PERSON.size() >= 256) {
         THIRD_PERSON.clear();
      }
   }

   private record HeldMuzzles(String weaponId, HumanoidArm mainArm, Vec3 negativeX, Vec3 positiveX, long capturedNanos) {
   }
}
