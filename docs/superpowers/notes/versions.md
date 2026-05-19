# Resolved versions for Stereoscopic v0.1.0

Resolved on 2026-05-16 against live Modrinth / Fabric meta / Maven Central APIs.

| Component         | Spec target        | Resolved version                  | Maven coordinate / Modrinth slug                              |
| ----------------- | ------------------ | --------------------------------- | ------------------------------------------------------------- |
| Minecraft         | 1.21.11            | 1.21.11                           | com.mojang:minecraft:1.21.11                                  |
| Yarn mappings     | 1.21.11 (latest)   | 1.21.11+build.5                   | net.fabricmc:yarn:1.21.11+build.5:v2                          |
| Fabric Loader     | 0.19.0             | 0.19.2                            | net.fabricmc:fabric-loader:0.19.2                             |
| Fabric API        | matching 1.21.11   | 0.141.4+1.21.11                   | net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11            |
| Sodium            | 0.8.7              | 0.8.11                            | maven.modrinth:sodium:mc1.21.11-0.8.11-fabric                 |
| Iris              | 1.10.7             | 1.10.7                            | maven.modrinth:iris:1.10.7+1.21.11-fabric                     |
| Fabric Loom       | n/a (build only)   | 1.16.2                            | id("fabric-loom") version "1.16.2"                            |
| Gradle            | n/a (build only)   | 9.5.1                             | gradle-9.5.1-bin.zip                                          |
| Mixin Extras      | n/a                | 0.5.4                             | io.github.llamalad7:mixinextras-fabric:0.5.4                  |
| JUnit Jupiter     | n/a                | 6.0.3                             | org.junit.jupiter:junit-jupiter:6.0.3                         |

## Notes / deviations from spec

- **Fabric Loader 0.19.0 was not marked stable** in the Fabric meta API. The
  spec target was 0.19.0; the nearest stable is **0.19.2**, which we use.
  Loom 1.16's release notes also call out "Fabric Loader 0.19.0+" for the
  ClassTweaker enum-extensions feature, so 0.19.2 is well within the
  supported range.
- **Sodium 0.8.7** exists for 1.21.11 as a release, but **0.8.11** is the
  newest stable release for 1.21.11 (0.8.12 is beta-only). We use **0.8.11**.
  This means `fabric.mod.json` should declare `sodium >= 0.8.11` rather than
  the spec's `>= 0.8.7`. (`>= 0.8.7` would still satisfy 0.8.11 at runtime,
  but the depends string should reflect the actually-resolved version.)
- **Iris 1.10.7** is the latest 1.10.x release for 1.21.11 (matches spec).
- **Loom 1.16** requires Gradle 9.4 or newer; we use **Gradle 9.5.1** (the
  current Gradle stable as reported by services.gradle.org/versions/current).
- **JUnit Jupiter**: the latest stable on Maven Central is **6.0.3**
  (5.13.0-M3 is a milestone build). We use 6.0.3.
- **MixinExtras**: published on Maven Central as
  `io.github.llamalad7:mixinextras-fabric` (the spec's `com.github.LlamaLad7.MixinExtras:mixinextras-fabric`
  Jitpack-style coordinate is no longer the canonical path; the io.github
  group is the Maven Central artifact). Latest is **0.5.4**.

## API endpoints used

- `https://meta.fabricmc.net/v2/versions/game`
- `https://meta.fabricmc.net/v2/versions/yarn/1.21.11`
- `https://meta.fabricmc.net/v2/versions/loader`
- `https://api.modrinth.com/v2/project/fabric-api/version`
- `https://api.modrinth.com/v2/project/sodium/version`
- `https://api.modrinth.com/v2/project/iris/version`
- `https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml`
- `https://services.gradle.org/versions/current`
- `https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/`
- `https://repo1.maven.org/maven2/io/github/llamalad7/mixinextras-fabric/`
