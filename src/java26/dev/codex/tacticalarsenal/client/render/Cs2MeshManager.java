package dev.codex.tacticalarsenal.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.codex.tacticalarsenal.TacticalArsenal;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class Cs2MeshManager implements SimpleSynchronousResourceReloadListener {
   private static final Cs2MeshManager INSTANCE = new Cs2MeshManager();
   private static final String WEAPON_DIRECTORY = "cs2_meshes";
   private static final String NPC_DIRECTORY = "cs2_npc_meshes";
   private static final String WEAPON_PREFIX = "cs2_meshes/";
   private static final String NPC_PREFIX = "cs2_npc_meshes/";
   private static final String SUFFIX = ".tam";
   private static final int TAM1_MAGIC = 1413565745;
   private static final int TAM2_MAGIC = 1413565746;
   private static final int FLOATS_PER_VERTEX = 8;
   private static final int MAX_VERTICES_PER_MESH = 5000000;
   private static final float MIN_MUZZLE_CAP_DEPTH = 0.008F;
   private static final float MUZZLE_CAP_LENGTH_FACTOR = 0.0125F;
   private static final float MUZZLE_FORWARD_CLEARANCE = 0.002F;
   private static final float INVENTORY_YAW_RADIANS = (float)Math.toRadians(68.0);
   private static final float INVENTORY_TILT_RADIANS = (float)Math.toRadians(25.0);
   private static final float INVENTORY_MAX_WIDTH = 0.88F;
   private static final float INVENTORY_MAX_HEIGHT = 0.78F;
   private static final float GROUND_MAX_SIZE = 0.48F;
   private static final float THIRD_PERSON_MESH_YAW_RADIANS = (float)Math.toRadians(90.0);
   private static final float THIRD_PERSON_MESH_SCALE = 1.25F;
   private static final float THIRD_PERSON_GRIP_TARGET_X = 0.33125F;
   private static final float THIRD_PERSON_GRIP_TARGET_Y = 0.33125F;
   private static final float THIRD_PERSON_GRIP_TARGET_Z = 0.5F;
   private static final float ELITE_HALF_GRIP_SEPARATION = 0.508F;
   private static final Vector3fc[] NO_EXTENTS = new Vector3fc[0];
   private static final Map<String, Vector3fc> THIRD_PERSON_GRIP_ANCHORS = Map.ofEntries(
      Map.entry("ak47", new Vector3f(0.0F, -0.03875F, -0.011236F)),
      Map.entry("aug", new Vector3f(-5.08E-4F, -9.44E-4F, 0.003479F)),
      Map.entry("awp", new Vector3f(0.0F, 0.113873F, 0.047741F)),
      Map.entry("bizon", new Vector3f(-1.0E-6F, -0.03621F, -0.015046F)),
      Map.entry("cz75a", new Vector3f(-1.0E-6F, 0.041289F, 0.02111F)),
      Map.entry("desert_eagle", new Vector3f(0.0F, -0.004828F, -0.025899F)),
      Map.entry("elite", new Vector3f(0.0F, 0.003623F, 0.03476F)),
      Map.entry("famas", new Vector3f(0.0F, -0.0254F, 0.106378F)),
      Map.entry("fiveseven", new Vector3f(0.0F, 0.087519F, -0.02537F)),
      Map.entry("g3sg1", new Vector3f(4.82E-4F, 0.030841F, 0.369469F)),
      Map.entry("galilar", new Vector3f(0.0F, -0.012229F, 0.062398F)),
      Map.entry("glock", new Vector3f(0.0F, -0.021349F, -0.02537F)),
      Map.entry("hkp2000", new Vector3f(0.0F, 8.26E-4F, 0.052599F)),
      Map.entry("m249", new Vector3f(4.9E-5F, -3.9E-5F, 2.7E-5F)),
      Map.entry("m4a1_silencer", new Vector3f(0.004903F, 0.005482F, 0.001481F)),
      Map.entry("m4a4", new Vector3f(0.0F, 0.200561F, 0.37652F)),
      Map.entry("mac10", new Vector3f(0.0F, -0.00762F, 0.119056F)),
      Map.entry("mag7", new Vector3f(0.0F, 0.09454F, -0.002984F)),
      Map.entry("mp5sd", new Vector3f(0.0F, -0.0254F, 0.00762F)),
      Map.entry("mp7", new Vector3f(0.0F, -0.03286F, -0.060605F)),
      Map.entry("mp9", new Vector3f(0.0F, 0.048337F, 0.108574F)),
      Map.entry("negev", new Vector3f(0.00144F, 0.007945F, -0.011851F)),
      Map.entry("nova", new Vector3f(-1.0E-6F, 0.01739F, 0.030471F)),
      Map.entry("p250", new Vector3f(0.0F, 0.040446F, 0.023181F)),
      Map.entry("p90", new Vector3f(0.0F, 0.031926F, 0.182975F)),
      Map.entry("revolver", new Vector3f(0.0F, -0.010661F, 0.004376F)),
      Map.entry("sawedoff", new Vector3f(0.0F, -0.059195F, 8.3E-4F)),
      Map.entry("scar20", new Vector3f(0.0F, 0.20056F, 0.37398F)),
      Map.entry("sg556", new Vector3f(0.0F, -0.02593F, 0.087524F)),
      Map.entry("ssg08", new Vector3f(-0.001993F, -0.008102F, 0.005579F)),
      Map.entry("tec9", new Vector3f(0.0F, 0.034497F, 0.106467F)),
      Map.entry("ump45", new Vector3f(0.0F, -0.01689F, 0.073451F)),
      Map.entry("usp_silencer", new Vector3f(0.0F, 0.026227F, 0.083079F)),
      Map.entry("xm1014", new Vector3f(-1.0E-6F, -0.001437F, -0.060364F)),
      Map.entry("knife", new Vector3f(0.0F, 0.0127F, 0.0F)),
      Map.entry("karambit", new Vector3f(0.0F, 0.0254F, 0.0F)),
      Map.entry("butterfly", new Vector3f(0.0F, 0.0254F, 0.0F))
   );
   private volatile Map<String, Cs2MeshManager.Mesh> meshes = Map.of();

   private Cs2MeshManager() {
   }

   public static void initialize() {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
   }

   public static boolean hasMesh(String weaponId) {
      return INSTANCE.meshes.containsKey(weaponId);
   }

   static Object renderIdentity(String weaponId) {
      return INSTANCE.meshes.get(weaponId);
   }

   static Matrix4fc inventoryTransform(String weaponId) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      return (Matrix4fc)(mesh == null ? new Matrix4f() : mesh.inventoryTransform());
   }

   static Matrix4fc groundTransform(String weaponId) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      return (Matrix4fc)(mesh == null ? new Matrix4f() : mesh.groundTransform());
   }

   static Matrix4fc thirdPersonTransform(String weaponId) {
      return thirdPersonTransform(weaponId, false);
   }

   static Matrix4fc thirdPersonTransform(String weaponId, boolean leftHand) {
      Vector3fc gripAnchor = THIRD_PERSON_GRIP_ANCHORS.get(weaponId);
      return (Matrix4fc)(gripAnchor == null ? new Matrix4f() : thirdPersonTransform(gripAnchor, leftHand));
   }

   static Matrix4fc thirdPersonHalfTransform(String weaponId, boolean positiveX) {
      Vector3fc gripAnchor = THIRD_PERSON_GRIP_ANCHORS.get(weaponId);
      if (gripAnchor == null) {
         return new Matrix4f();
      } else {
         if (weaponId.equals("elite") && positiveX) {
            gripAnchor = new Vector3f(gripAnchor.x() + 0.508F, gripAnchor.y(), gripAnchor.z());
         }

         return thirdPersonTransform(gripAnchor, positiveX);
      }
   }

   private static Matrix4fc thirdPersonTransform(Vector3fc gripAnchor, boolean leftHand) {
      return new Matrix4f()
         .translate(0.33125F, 0.33125F, 0.5F)
         .rotateY(THIRD_PERSON_MESH_YAW_RADIANS)
         .scale(1.25F)
         .rotateY(leftHand ? (float) Math.PI : 0.0F)
         .translate(-gripAnchor.x(), -gripAnchor.y(), -gripAnchor.z());
   }

   static Vector3fc gripAnchor(String itemId) {
      return THIRD_PERSON_GRIP_ANCHORS.get(itemId);
   }

   static Vector3fc gripAnchor(String itemId, boolean positiveX) {
      Vector3fc anchor = THIRD_PERSON_GRIP_ANCHORS.get(itemId);
      return (Vector3fc)(anchor != null && itemId.equals("elite") && positiveX ? new Vector3f(anchor.x() + 0.508F, anchor.y(), anchor.z()) : anchor);
   }

   static void visitExtents(String weaponId, Consumer<Vector3fc> visitor) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      if (mesh != null) {
         for (Vector3fc extent : mesh.extents()) {
            visitor.accept(extent);
         }
      }
   }

   static Vector3fc[] inventoryExtents(String weaponId) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      return mesh == null ? NO_EXTENTS : mesh.extents();
   }

   static Vector3fc[] halfExtents(String weaponId, boolean positiveX) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      if (mesh == null) {
         return NO_EXTENTS;
      } else {
         return positiveX ? mesh.positiveXExtents() : mesh.negativeXExtents();
      }
   }

   static Vector3fc muzzleAnchor(String weaponId) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      return mesh == null ? null : mesh.muzzleAnchor();
   }

   static Vector3fc muzzleAnchor(String weaponId, boolean positiveX) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      if (mesh == null) {
         return null;
      } else {
         return positiveX ? mesh.positiveXMuzzleAnchor() : mesh.negativeXMuzzleAnchor();
      }
   }

   public static boolean submit(String weaponId, PoseStack poses, SubmitNodeCollector collector, int light) {
      return submitPart(weaponId, Cs2MeshManager.MeshPart.ALL, poses, collector, light);
   }

   public static boolean submitPart(String weaponId, Cs2MeshManager.MeshPart part, PoseStack poses, SubmitNodeCollector collector, int light) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      if (mesh != null && mesh.vertexCount(part) != 0) {
         submitGeometry(weaponId, mesh, part, null, poses, collector, light);
         return true;
      } else {
         return false;
      }
   }

   public static boolean submitPartHalf(
      String weaponId, Cs2MeshManager.MeshPart part, boolean positiveX, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      Cs2MeshManager.Mesh mesh = INSTANCE.meshes.get(weaponId);
      if (mesh != null && mesh.vertexCount(part) != 0 && mesh.halfExtents(positiveX).length != 0) {
         submitGeometry(weaponId, mesh, part, positiveX, poses, collector, light);
         return true;
      } else {
         return false;
      }
   }

   private static void submitGeometry(
      String weaponId, Cs2MeshManager.Mesh mesh, Cs2MeshManager.MeshPart part, Boolean positiveX, PoseStack poses, SubmitNodeCollector collector, int light
   ) {
      Identifier texture = textureLocation(weaponId);
      collector.submitCustomGeometry(poses, RenderTypes.entitySolid(texture), (pose, buffer) -> mesh.write(pose, buffer, light, part, positiveX));
   }

   public Identifier getFabricId() {
      return TacticalArsenal.id("cs2_meshes");
   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      Map<Identifier, Resource> resources = new LinkedHashMap<>();
      resources.putAll(resourceManager.listResources("cs2_meshes", id -> id.getNamespace().equals("tactical_arsenal") && id.getPath().endsWith(".tam")));
      resources.putAll(resourceManager.listResources("cs2_npc_meshes", id -> id.getNamespace().equals("tactical_arsenal") && id.getPath().endsWith(".tam")));
      Map<String, Cs2MeshManager.Mesh> loaded = new LinkedHashMap<>();
      long triangles = 0L;

      for (Entry<Identifier, Resource> entry : resources.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
         Identifier location = entry.getKey();
         String weaponId = weaponId(location);
         if (weaponId == null) {
            TacticalArsenal.LOGGER.warn("Ignoring CS2 mesh at unexpected path {}", location);
         } else {
            Identifier texture = textureLocation(weaponId);
            if (resourceManager.getResource(texture).isEmpty()) {
               TacticalArsenal.LOGGER.warn("Ignoring CS2 mesh {} because {} is missing", location, texture);
            } else {
               try (InputStream input = entry.getValue().open()) {
                  Cs2MeshManager.Mesh mesh = read(location, input);
                  loaded.put(weaponId, mesh);
                  triangles += mesh.vertexCount() / 3L;
               } catch (RuntimeException | IOException var16) {
                  TacticalArsenal.LOGGER.warn("Could not load CS2 mesh {}; the fallback model will be used", location, var16);
               }
            }
         }
      }

      this.meshes = Map.copyOf(loaded);
      TacticalArsenal.LOGGER.info("Loaded {} local CS2 meshes ({} triangles)", loaded.size(), triangles);
   }

   private static Identifier textureLocation(String meshId) {
      if (meshId.startsWith("ct_")) {
         return TacticalArsenal.id("textures/cs2_npc/ct_atlas.png");
      } else if (meshId.startsWith("t_")) {
         return TacticalArsenal.id("textures/cs2_npc/t_atlas.png");
      } else {
         String textureId = meshId.startsWith("butterfly_") ? "butterfly" : meshId;
         return TacticalArsenal.id("textures/cs2/" + textureId + ".png");
      }
   }

   private static String weaponId(Identifier location) {
      String path = location.getPath();
      if (!path.endsWith(".tam")) {
         return null;
      } else {
         int prefixLength;
         if (path.startsWith("cs2_meshes/")) {
            prefixLength = "cs2_meshes/".length();
         } else {
            if (!path.startsWith("cs2_npc_meshes/")) {
               return null;
            }

            prefixLength = "cs2_npc_meshes/".length();
         }

         String id = path.substring(prefixLength, path.length() - ".tam".length());
         return !id.isEmpty() && id.indexOf(47) < 0 ? id : null;
      }
   }

   private static Cs2MeshManager.Mesh read(Identifier location, InputStream rawInput) throws IOException {
      DataInputStream input = new DataInputStream(new BufferedInputStream(rawInput));
      int magic = input.readInt();
      int bodyVertexCount;
      int magazineVertexCount;
      int actionVertexCount;
      if (magic == 1413565745) {
         bodyVertexCount = input.readInt();
         magazineVertexCount = 0;
         actionVertexCount = 0;
      } else {
         if (magic != 1413565746) {
            throw new IOException("Invalid TAM1/TAM2 magic in " + location);
         }

         bodyVertexCount = input.readInt();
         magazineVertexCount = input.readInt();
         actionVertexCount = input.readInt();
      }

      if (bodyVertexCount > 0
         && bodyVertexCount % 3 == 0
         && magazineVertexCount >= 0
         && magazineVertexCount % 3 == 0
         && actionVertexCount >= 0
         && actionVertexCount % 3 == 0) {
         long totalVertexCount = (long)bodyVertexCount + magazineVertexCount + actionVertexCount;
         if (totalVertexCount > 5000000L) {
            throw new IOException("CS2 mesh has too many vertices (" + totalVertexCount + ") in " + location);
         } else {
            int vertexCount = (int)totalVertexCount;

            float[] vertices;
            try {
               vertices = new float[Math.multiplyExact(vertexCount, 8)];
            } catch (OutOfMemoryError | ArithmeticException var26) {
               throw new IOException("CS2 mesh is too large: " + location, var26);
            }

            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;

            for (int index = 0; index < vertices.length; index++) {
               float value = input.readFloat();
               if (!Float.isFinite(value)) {
                  throw new IOException("Non-finite vertex value in " + location);
               }

               vertices[index] = value;
               int component = index % 8;
               if (component == 0) {
                  minX = Math.min(minX, value);
                  maxX = Math.max(maxX, value);
               } else if (component == 1) {
                  minY = Math.min(minY, value);
                  maxY = Math.max(maxY, value);
               } else if (component == 2) {
                  minZ = Math.min(minZ, value);
                  maxZ = Math.max(maxZ, value);
               }
            }

            if (input.read() != -1) {
               throw new IOException("Trailing data in " + location);
            } else {
               Vector3fc[] extents = boundsCorners(minX, minY, minZ, maxX, maxY, maxZ);
               float splitX = (minX + maxX) * 0.5F;
               Vector3fc[] negativeXExtents = halfBounds(vertices, vertexCount, splitX, false);
               Vector3fc[] positiveXExtents = halfBounds(vertices, vertexCount, splitX, true);
               Vector3fc muzzleAnchor = muzzleAnchor(vertices, bodyVertexCount, splitX, null);
               Vector3fc negativeXMuzzleAnchor = muzzleAnchor(vertices, bodyVertexCount, splitX, false);
               Vector3fc positiveXMuzzleAnchor = muzzleAnchor(vertices, bodyVertexCount, splitX, true);
               Matrix4fc inventoryTransform = createInventoryTransform(minX, minY, minZ, maxX, maxY, maxZ);
               Matrix4fc groundTransform = createGroundTransform(minX, minY, minZ, maxX, maxY, maxZ);
               return new Cs2MeshManager.Mesh(
                  vertexCount,
                  bodyVertexCount,
                  magazineVertexCount,
                  actionVertexCount,
                  vertices,
                  extents,
                  negativeXExtents,
                  positiveXExtents,
                  splitX,
                  muzzleAnchor,
                  negativeXMuzzleAnchor,
                  positiveXMuzzleAnchor,
                  inventoryTransform,
                  groundTransform
               );
            }
         }
      } else {
         throw new IOException("Invalid TAM section counts " + bodyVertexCount + "/" + magazineVertexCount + "/" + actionVertexCount + " in " + location);
      }
   }

   private static Vector3fc muzzleAnchor(float[] vertices, int bodyVertexCount, float splitX, Boolean positiveX) {
      float minZ = Float.POSITIVE_INFINITY;
      float maxZ = Float.NEGATIVE_INFINITY;

      for (int triangle = 0; triangle < bodyVertexCount; triangle += 3) {
         if (positiveX == null || trianglePositiveX(vertices, triangle, splitX) == positiveX) {
            for (int corner = 0; corner < 3; corner++) {
               float z = vertices[(triangle + corner) * 8 + 2];
               minZ = Math.min(minZ, z);
               maxZ = Math.max(maxZ, z);
            }
         }
      }

      if (Float.isFinite(minZ) && Float.isFinite(maxZ)) {
         float capDepth = Math.max(0.008F, (maxZ - minZ) * 0.0125F);
         float capStart = maxZ - capDepth;
         float minX = Float.POSITIVE_INFINITY;
         float minY = Float.POSITIVE_INFINITY;
         float maxX = Float.NEGATIVE_INFINITY;
         float maxY = Float.NEGATIVE_INFINITY;

         for (int trianglex = 0; trianglex < bodyVertexCount; trianglex += 3) {
            if (positiveX == null || trianglePositiveX(vertices, trianglex, splitX) == positiveX) {
               for (int corner = 0; corner < 3; corner++) {
                  int offset = (trianglex + corner) * 8;
                  if (!(vertices[offset + 2] < capStart)) {
                     minX = Math.min(minX, vertices[offset]);
                     minY = Math.min(minY, vertices[offset + 1]);
                     maxX = Math.max(maxX, vertices[offset]);
                     maxY = Math.max(maxY, vertices[offset + 1]);
                  }
               }
            }
         }

         return Float.isFinite(minX) && Float.isFinite(minY) ? new Vector3f((minX + maxX) * 0.5F, (minY + maxY) * 0.5F, maxZ + 0.002F) : null;
      } else {
         return null;
      }
   }

   private static boolean trianglePositiveX(float[] vertices, int triangle, float splitX) {
      float x0 = vertices[triangle * 8];
      float x1 = vertices[(triangle + 1) * 8];
      float x2 = vertices[(triangle + 2) * 8];
      return (x0 + x1 + x2) / 3.0F >= splitX;
   }

   private static Vector3fc[] halfBounds(float[] vertices, int vertexCount, float splitX, boolean positiveX) {
      float minX = Float.POSITIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float minZ = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;
      float maxZ = Float.NEGATIVE_INFINITY;
      boolean found = false;

      for (int triangle = 0; triangle < vertexCount; triangle += 3) {
         float centroidX = 0.0F;

         for (int corner = 0; corner < 3; corner++) {
            centroidX += vertices[(triangle + corner) * 8];
         }

         boolean trianglePositive = centroidX / 3.0F >= splitX;
         if (trianglePositive == positiveX) {
            found = true;

            for (int corner = 0; corner < 3; corner++) {
               int offset = (triangle + corner) * 8;
               float x = vertices[offset];
               float y = vertices[offset + 1];
               float z = vertices[offset + 2];
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               minZ = Math.min(minZ, z);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
               maxZ = Math.max(maxZ, z);
            }
         }
      }

      return found ? boundsCorners(minX, minY, minZ, maxX, maxY, maxZ) : NO_EXTENTS;
   }

   private static Vector3fc[] boundsCorners(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      return new Vector3fc[]{
         new Vector3f(minX, minY, minZ),
         new Vector3f(minX, minY, maxZ),
         new Vector3f(minX, maxY, minZ),
         new Vector3f(minX, maxY, maxZ),
         new Vector3f(maxX, minY, minZ),
         new Vector3f(maxX, minY, maxZ),
         new Vector3f(maxX, maxY, minZ),
         new Vector3f(maxX, maxY, maxZ)
      };
   }

   private static Matrix4fc createInventoryTransform(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      Matrix4f rotation = new Matrix4f().rotateZ(INVENTORY_TILT_RADIANS).rotateY(INVENTORY_YAW_RADIANS);
      Vector3fc[] corners = boundsCorners(minX, minY, minZ, maxX, maxY, maxZ);
      Vector3f transformed = new Vector3f();
      float projectedMinX = Float.POSITIVE_INFINITY;
      float projectedMinY = Float.POSITIVE_INFINITY;
      float projectedMinZ = Float.POSITIVE_INFINITY;
      float projectedMaxX = Float.NEGATIVE_INFINITY;
      float projectedMaxY = Float.NEGATIVE_INFINITY;
      float projectedMaxZ = Float.NEGATIVE_INFINITY;

      for (Vector3fc corner : corners) {
         rotation.transformPosition(corner, transformed);
         projectedMinX = Math.min(projectedMinX, transformed.x);
         projectedMinY = Math.min(projectedMinY, transformed.y);
         projectedMinZ = Math.min(projectedMinZ, transformed.z);
         projectedMaxX = Math.max(projectedMaxX, transformed.x);
         projectedMaxY = Math.max(projectedMaxY, transformed.y);
         projectedMaxZ = Math.max(projectedMaxZ, transformed.z);
      }

      float projectedWidth = Math.max(1.0E-4F, projectedMaxX - projectedMinX);
      float projectedHeight = Math.max(1.0E-4F, projectedMaxY - projectedMinY);
      float scale = Math.min(0.88F / projectedWidth, 0.78F / projectedHeight);
      float centerX = (projectedMinX + projectedMaxX) * 0.5F;
      float centerY = (projectedMinY + projectedMaxY) * 0.5F;
      float centerZ = (projectedMinZ + projectedMaxZ) * 0.5F;
      return new Matrix4f()
         .translate(0.5F - centerX * scale, 0.5F - centerY * scale, 0.5F - centerZ * scale)
         .scale(scale)
         .rotateZ(INVENTORY_TILT_RADIANS)
         .rotateY(INVENTORY_YAW_RADIANS);
   }

   private static Matrix4fc createGroundTransform(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      float largestDimension = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
      float scale = 0.48F / Math.max(1.0E-4F, largestDimension);
      float centerX = (minX + maxX) * 0.5F;
      float centerY = (minY + maxY) * 0.5F;
      float centerZ = (minZ + maxZ) * 0.5F;
      return new Matrix4f().translate(0.5F, 0.5F, 0.5F).scale(scale).translate(-centerX, -centerY, -centerZ);
   }

   private record Mesh(
      int vertexCount,
      int bodyVertexCount,
      int magazineVertexCount,
      int actionVertexCount,
      float[] vertices,
      Vector3fc[] extents,
      Vector3fc[] negativeXExtents,
      Vector3fc[] positiveXExtents,
      float splitX,
      Vector3fc muzzleAnchor,
      Vector3fc negativeXMuzzleAnchor,
      Vector3fc positiveXMuzzleAnchor,
      Matrix4fc inventoryTransform,
      Matrix4fc groundTransform
   ) {
      private int vertexCount(Cs2MeshManager.MeshPart part) {
         return switch (part) {
            case ALL -> this.vertexCount;
            case BODY -> this.bodyVertexCount;
            case MAGAZINE -> this.magazineVertexCount;
            case ACTION -> this.actionVertexCount;
         };
      }

      private int firstVertex(Cs2MeshManager.MeshPart part) {
         return switch (part) {
            case ALL, BODY -> 0;
            case MAGAZINE -> this.bodyVertexCount;
            case ACTION -> this.bodyVertexCount + this.magazineVertexCount;
         };
      }

      private Vector3fc[] halfExtents(boolean positiveX) {
         return positiveX ? this.positiveXExtents : this.negativeXExtents;
      }

      private void write(Pose pose, VertexConsumer buffer, int light, Cs2MeshManager.MeshPart part, Boolean positiveX) {
         int firstVertex = this.firstVertex(part);
         int endVertex = firstVertex + this.vertexCount(part);

         for (int triangle = firstVertex; triangle < endVertex; triangle += 3) {
            if (positiveX == null || this.isPositiveX(triangle) == positiveX) {
               this.writeVertex(pose, buffer, triangle, light);
               this.writeVertex(pose, buffer, triangle + 1, light);
               this.writeVertex(pose, buffer, triangle + 2, light);
               this.writeVertex(pose, buffer, triangle + 2, light);
            }
         }
      }

      private boolean isPositiveX(int triangle) {
         return Cs2MeshManager.trianglePositiveX(this.vertices, triangle, this.splitX);
      }

      private void writeVertex(Pose pose, VertexConsumer buffer, int vertex, int light) {
         int offset = vertex * 8;
         buffer.addVertex(pose, this.vertices[offset], this.vertices[offset + 1], this.vertices[offset + 2])
            .setColor(-1)
            .setUv(this.vertices[offset + 3], this.vertices[offset + 4])
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, this.vertices[offset + 5], this.vertices[offset + 6], this.vertices[offset + 7]);
      }
   }

   public static enum MeshPart {
      ALL,
      BODY,
      MAGAZINE,
      ACTION;
   }
}
