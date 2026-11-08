# Tactical Arsenal (CS2 guns mod) – multi-version build

Multi-version Fabric build of **Tactical Arsenal 2.2.0** ("counter craft (cs guns mod)" by pisankusgaming
on Modrinth). The features are the same as the original 26.2 release; only the Minecraft-version glue
differs.

| Minecraft | Java | Fabric API used to build |
|-----------|------|--------------------------|
| 1.20.1    | 17   | 0.92.12+1.20.1           |
| 1.21.1    | 21   | 0.116.17+1.21.1          |
| 1.21.11   | 21   | 0.141.6+1.21.11          |
| 26.1      | 25   | 0.145.1+26.1             |
| 26.1.1    | 25   | 0.145.4+26.1.1           |
| 26.1.2    | 25   | 0.155.3+26.1.2           |
| 26.2      | 25   | 0.161.0+26.2             |
| 26.3      | 25   | 0.165.2+26.3             |

## Layout

* `src/java26` – the mod sources (decompiled from the 2.2.0 jar), shared by every version.
* `src/compat-pre26` – helpers for versions before 26.1 (camera state for tracers, ADS FOV hook).
* `src/legacy` – 1.21.1 / 1.20.1 replacements for code built on the newer render-state / item-model APIs.
* `src/resources26`, `src/resources-legacy`, `shared/resources` – resources (meshes, textures, sounds, models).
* `versions/<mc>` – one Gradle subproject per Minecraft version (`gradle.properties` holds its settings,
  `src/main/java` holds files used only by that version).
* `gradle/remap/*.txt` – rename tables (class/method names that changed between versions).
* Sources may contain `//#if MC >= x.y` … `//#else` … `//#endif` blocks; `gradle/common.gradle` resolves them.
* `src/testmod` – a dev-only scripted scene used to screenshot every version (never in the released jars).

## Building

Needs Java 25 to run Gradle (Fabric Loom 1.19) and Gradle 9.8+.

```
gradle build            # all versions
gradle :1.20.1:build    # one version
```

Jars end up in `versions/<mc>/build/libs/Tactical-Arsenal-2.2.0+mc<mc>.jar`.

## Notes on older versions

* Older versions have no "stab" swing type; the 26.x knife stab (8-tick swing, stab pose and motion) is
  reproduced by mixins.
* 1.20.1/1.21.1 have no `block.iron.hit` sound; one reload tick uses `block.chain.hit` instead.
* Spawn eggs use the classic two-colour egg (villager colours) since per-egg textures do not exist there.
