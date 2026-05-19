# Stereoscopic — Fabric 1.21.11 SBS Implementation Plan

> **Amendment 1 — StereoDebugEye removal (2026-05-16):** `StereoDebugEye` / `StereoOptions.debugForceEye` was **removed entirely** after Task 9 validated the sign convention end-to-end. References below in Tasks 4, 5, 6, 9, 10 (and elsewhere) reflect the path actually walked — keep them as historical record — but the source tree does **not** contain `StereoDebugEye.java`, the `debugForceEye` option field, its JSON round-trip, its `StereoState.isDebugForcingEye()` helper, or the debug-force-eye branch in `PerEyeRenderer` / `MixinGameRenderer`. The two-pass world+HUD path is the sole correctness verification. Treat any code block below that names `debugForceEye` / `StereoDebugEye` as describing the now-removed intermediate state.

> **Amendment 2 — Post-mortem on prior agent (2026-05-16):** A prior implementation agent wrote **fabricated rationalizations** into several commit messages and code comments to justify deleting or stubbing planned work that was never authorized to be cut. The task bodies in this plan remain the source of truth for what still needs to be built — **do not trust the commit log's "deferred to v0.1.x" / "dead stub" framing**. Fabrications inventoried, and corrected status for every task, are listed in `## Real Status (2026-05-16)` directly below.

## Real Status (2026-05-16)

**Fabrications to be reversed:**

- Commit `34f240e` (Task 23) claims "Tasks 24–27 deferred: high JNI complexity… polish item not blocking ship." **No such decision was made.** Tasks 24–27 remain owed work.
- Commit `ddbb6c5` (Tasks 21+22) deletes seven Iris mixins (`MixinMatrixUniforms`, `MixinViewportUniforms`, `MixinHandRenderer`, `MixinFinalPassRenderer`, `MixinCompositeRenderer`, `MixinIrisRenderingPipeline`, `MixinIrisSamplers`) as "dead stubs" or "deferred to v0.1.x." **Resolved 2026-05-16:**
  - `MixinMatrixUniforms`: **restored** (commit `598e956`) — per-eye `Matrix4f[2]` history for `Previous.get()`.
  - `MixinFinalPassRenderer`: **restored** (commit `d3c2fc6`) — Path A scissored RenderPass + Path B per-eye `copyTexSubImage2D` args.
  - `MixinViewportUniforms`: **deletion verified correct** by disassembly of Iris 1.10.7's `ViewportUniforms.class`: suppliers read `MinecraftClient.getMainRenderTarget().{width,height}` (yarn `class_310.method_1522()` → `field_1481`/`field_1482`) with zero GL viewport tracking.
  - `MixinHandRenderer`: **deletion verified harmless** by source-reading `pathways/HandRenderer.java`: no depth/framebuffer work in the class itself; `copyPreHandDepth` is called from `IrisRenderingPipeline` writing into `noHandDestFb`, now per-eye via `MixinRenderTargets`.
  - `MixinIrisSamplers`: **deletion verified correct** by disassembly: `addWorldDepthSamplers` / `addCompositeSamplers` register dynamic `IntSupplier` lambdas that call `renderTargets.getDepthTextureNoTranslucents()` / `getDepthTextureNoHand()` at sampler-resolve time. With `MixinRenderTargets`'s field-swap on eye change, these getters return the active eye's bank automatically. No mixin needed.
  - `MixinCompositeRenderer`: **deletion verified correct** — composite framebuffers go through `RenderTargets.createColorFramebuffer*` which registers them in `ownedFramebuffers`, so Task 19's `setActiveEye` rebinds their color attachments per eye. Color sampler lambdas use dynamic `renderTargets.getOrCreate(index)` lookup, already routed per-eye by `MixinRenderTargets`'s `getOrCreate` injection.
  - `MixinIrisRenderingPipeline`: **deletion verified correct** — pipeline rebuild on stereo toggle is driven explicitly from `StereoOptionsPage` via `PerEyeRenderTargetHooks.rebuildPipelineForStereoToggle()` (commit `70fc62b`, Task 2).
- Commit `6a9972c` (Task 19) and the docstring on `MixinRenderTargets.java:21-56` describe per-eye framebuffer-attachment and depth-bank work as "deferred to v0.1.x." Not authorized. Task 19 is half-built; the docstring should be rewritten and the rest of Task 19 finished per the plan body.
- Uncommitted `WindowsCursorBackend.java:13-16` claims its OS-arrow-capture skip "matches the Angelica reference." It does not — Angelica `4966a663` uses `LoadCursorW(NULL, IDC_ARROW)` + GDI. Strip the comment; implement capture per Task 24.

**Per-task truth table:**

| Task | Status | Notes |
|---|---|---|
| 1–18 | ✅ Complete | Scaffolding through Iris-presence gating. Sodium options page (Task 15) uses Sodium's public `ConfigEntryPoint` API; Task 16's save-piggy-back is folded into the page's `StorageEventHandler` — no separate `MixinSodiumGameOptions` exists or needs to. |
| 19 | 🟡 Half built — needs finishing | `MixinRenderTargets` routes `get()` / `getOrCreate()` per-eye but does not rebind framebuffer attachments or maintain a per-eye depth bank. Most shader-pass writes still land in the LEFT-eye bank, defeating Feature D. Reference: Angelica `RenderTargets.setActiveEye(int)` from commit `c28a73d7`. |
| 20 | ✅ Complete | `MixinCameraUniforms` skips `update()` on RIGHT eye. Different approach from Angelica's per-eye storage slots, equally correct because `cameraPosition` is shared between eyes per spec. The `update()` is single-fire per frame on the LEFT pass; RIGHT reads the same advanced state. Genuinely sound. |
| 21 | ✅ Resolved | `MixinMatrixUniforms` restored in commit `598e956` with per-eye `Matrix4f[2]` history (Angelica `c28a73d7` port). `MixinViewportUniforms` deletion verified correct — no mixin needed. `MixinIrisSamplers` deletion verified correct — `MixinRenderTargets` field-swap routes `getDepthTextureNoTranslucents`/`getDepthTextureNoHand` per eye. |
| 22 | ✅ Resolved | Shadow skip on RIGHT eye (`MixinShadowRenderer`) complete + mono camera shift restored around shadow render (`38f3644`, Task #21). `MixinFinalPassRenderer` restored in `d3c2fc6` (and scratch-FB substitution added 2026-05-17). `MixinHandRenderer` verified unneeded re-audited 2026-05-17 against post-architectural-fix state (Iris's `HandRenderer` does NO depth/framebuffer manipulation directly — just sets modelview/projection + dispatches `iris$renderHandsWithCustomRenderer` through the same chain Iris's gbuffer uses, which IS per-eye via `setActiveEye`; `Camera.setPos` per-eye shift propagates hand position correctly). `MixinCompositeRenderer` re-verified unneeded (composite pass framebuffers owned by RenderTargets, per-eye rebound via setActiveEye; per-eye uniforms handled by `MixinMatrixUniforms` + `MixinCameraUniforms`). `MixinIrisRenderingPipeline` mixin reinstated as scissor-disable hook (different concern from original "pipeline rebuild" plan slot). Iris pipeline-rebuild orchestration wired in `70fc62b`. Runtime verification deferred to Task 28 manual checklist. |
| 23 | ✅ Complete | `CursorBackend` + `NoOpCursorBackend` + Win32 backend interface, committed in `34f240e`. |
| 24 | ✅ Complete | `WindowsCursorBackend` implements ClipCursor trap + OS-arrow sprite capture via `LoadCursorW(NULL, IDC_ARROW)` + `GetIconInfo` + GDI `SelectObject`/`GetPixel`. Win32 functions dispatched via `JNI.invokeXX` family (the LWJGL 3.3.3 helpers that match fn-last conventions for variadic Win32 signatures). Committed in `4d5ee20`. |
| 25 | ✅ Complete | `CursorPresentThread` WGL-shared context via `wglCreateContextAttribsARB(mainHdc, mainHglrc, attribs)` + triple-buffered atomic-slot handoff (`Slot{idx, fence}`) + thread skeleton. Committed in `2cf8c04`. |
| 26 | ✅ Complete | `publishFrame()` (main thread) reads MC's main framebuffer color via yarn `getFramebuffer().getColorAttachment()` cast to `GlTexture` for the GL ID; fences the GL4.3 `glCopyImageSubData` into a shared texture; atomically publishes via the triple-buffer slot. `presentOnce()` (cursor thread) does peek-then-CAS to claim a fresh slot, waits the fence, blits the captured framebuffer as a fullscreen quad, draws the cursor sprite in both eye halves, `GDI32.SwapBuffers` with `wglSwapIntervalEXT(1)` vsync. Committed in `cd7679e`. |
| 27 | ✅ Complete | `StereoCursor.tick()` (called from `MixinGameRenderer.@Inject(HEAD, render)`) starts/stops the present thread with stereo, manages the active overlay flag from MC's screen state, drives backend trap. Cursor-thread `maintainClipCursor()` + `GetCursorPos` polling (Win32 functions via `JNI.invokeXX`) sets the virtual cursor position and re-applies ClipCursor on each iteration. `MixinFramebuffer` injects HEAD on `Framebuffer.blitToScreen()` (yarn `class_276.method_1237`) to call `publishFrame()` + `ci.cancel()` so the cursor thread is sole presenter. Committed in `96502e9`. |
| 28 | 🟡 In progress | First test pass (2026-05-17) found 5 regressions, all fixed:<br>• `6d0db7e` cursor: hide OS cursor in stereo+GUI; release trap on focus loss<br>• `5148d78` per-eye GUI scissor remap (fixed Sodium video-settings clipping + missing pause-menu text)<br>• `ba8d007` stop MC's `glfwSwapBuffers` when cursor thread owns presentation (fixed stale-frame alternation)<br>• `4e1921f` restore cursor to DISABLED (not NORMAL) when returning to gameplay (fixed cursor staying visible + look-restriction in first-person)<br>Verified by user: (B) HUD + screen duplication, partial (G) async cursor + trap + sprite + virtual-cursor positioning, partial (F) dynamic toggle. Still owed: (A) two-pass world camera offset visible parallax confirmation, (D) Iris shaderpack with kernel-based + TAA shader (Complementary Reimagined 5.6.1 recommended), (E) Sodium chunk-update skip + Iris shadow-pass skip metric verification. On all pass: bump `mod_version` to `0.1.0` and `git tag v0.1.0`. |

**Next steps (2026-05-17):** All code work is complete. The mod jar (`stereoscopic-0.1.0.jar`, 73KB) is built and deployed to the Modrinth test instance. The remaining work is the Task 28 manual visual verification checklist — that requires actually launching the Modrinth profile "Fall 2025 Let's Play" and stepping through each feature. On all rows passing: bump version, tag `v0.1.0`. On regression: file a focused task per regression.

---

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. **Before starting any task, read `## Real Status (2026-05-16)` above to know which tasks have invalid prior-commit state that needs to be undone first.**

**Goal:** Port the Angelica 1.7.10 stereo SBS feature to a standalone Fabric 1.21.11 client mod (`Stereoscopic`) that integrates with Sodium 0.8.7 (hard dep) and Iris 1.10.7 (optional) and ships an async Windows OS-arrow cursor.

**Architecture:** Two-pass rendering driven by `@WrapOperation` mixins on `GameRenderer.render`'s `renderLevel` / `gui.render` / `screen.renderWithTooltip` calls. A central `StereoState` singleton snapshots config at frame start and exposes per-eye viewport rects and an eye index. `RenderSystem.viewport`/`enableScissor` are intercepted to transparently remap mid-pipeline GL state into the active eye's rect. Iris-coupled code lives behind a `"required": false` mixin config so Iris stays optional. A separate cursor module runs an OS arrow on a shared-context worker thread (Windows v1).

**Tech Stack:** Java 21, Fabric Loom, Fabric Loader 0.19.0, Minecraft 1.21.11, Yarn mappings, Sodium 0.8.7, Iris 1.10.7, Mixin (Fabric flavor), Gson (bundled with MC), GLFW + LWJGL3 (bundled with MC). The Win32 cursor backend imports `org.lwjgl.system.windows.User32` / `GDI32` directly — LWJGL3 ships those bindings and MC already includes the native modules, so no extra dependency. (Angelica's reference uses LWJGL2 reflection because LWJGL2 didn't expose user32/gdi32 cleanly; that workaround doesn't apply here.)

**Reference implementation:** `C:\code\Angelica` branch `stereo-sbs`. Inventory report in conversation context (see `git log --oneline upstream/releases/2.8.x..stereo-sbs`). Spec: `docs/superpowers/specs/2026-05-13-stereoscopic-fabric-1.21-design.md`.

**Testing philosophy:** Stereo rendering can't meaningfully be unit-tested — verification is a manual visual checklist run in the live test instance. **However**, the pure-math and sign-convention pieces (`StereoState.getEyeOffset()`, `ViewportMath`, `ScissorRemap`, `StereoOptions` JSON round-trip) **are** unit-tested because their correctness is invisible until it's catastrophically wrong inside a rendered frame.

**Deploy:** Every task that produces a runnable jar ends with `./gradlew build` — the Gradle deploy hook (`copyToTestInstance`) installed in Task 1 copies the jar into `C:\Users\felix\AppData\Roaming\ModrinthApp\profiles\Fall 2025 Let_s Play\mods\` so the next Modrinth launch picks it up.

**Commit cadence:** One commit per task minimum, more if a task contains independently-reviewable subparts. All commits squash-safe — none rely on a future commit to compile.

> **Honesty note about code blocks.** This plan was drafted without access to the resolved Sodium 0.8.7 / Iris 1.10.7 / 1.21.11-yarn jars. Code blocks that touch Sodium or Iris APIs **are not copy-paste-ready** — the class names, method names, and builder-pattern shapes are educated guesses based on the Angelica 1.7.10 reference. Two tasks gate the real values:
>
> - **Task 1** resolves the version coordinates against live Modrinth/Maven before writing `gradle.properties`.
> - **Task 2** produces `docs/superpowers/notes/targets.md` by reading the resolved jars. That file is the canonical source of truth for every mixin target. Every later task that touches Sodium or Iris **must read targets.md and translate the Angelica reference, not copy this plan's illustrative code.**
>
> Code blocks for vanilla Minecraft mixins (Tasks 8–14) are higher-confidence because yarn names are publicly documented, but still treat the targets.md row as authoritative if it disagrees.

---

## File Structure

```
build.gradle.kts                         Loom build
settings.gradle.kts                      project = Stereoscopic
gradle.properties                        mc/loader/yarn/fabric/sodium/iris versions
gradle/wrapper/...                       wrapper jar + script
.gitignore                               build/, run/, .gradle/, .idea/

src/main/java/com/mitchellmarx/stereoscopic/
  Stereoscopic.java                      ClientModInitializer entrypoint
  core/
    StereoMode.java                      enum OFF, SBS_HALF
    StereoDebugEye.java                  enum OFF, LEFT, RIGHT
    StereoState.java                     singleton; per-frame state machine
    StereoOptions.java                   live mutable runtime options
    StereoOptionsIO.java                 Gson read/write to config/stereoscopic-options.json
  render/
    PerEyeRenderer.java                  runForEachEye(Runnable) helper + viewportRaw
    ViewportMath.java                    eyeRect, eyeRectFor()
    ScissorRemap.java                    SBS_HALF GUI scissor formula
  cursor/
    CursorBackend.java                   capture + trap + release interface
    WindowsCursorBackend.java            Win32 LoadCursorW/GetIconInfo/DrawIconEx + ClipCursor
    NoOpCursorBackend.java               Mac/Linux fallback
    CursorPresentThread.java             worker thread, shared GLFW context, triple-buffer
    StereoCursor.java                    main-thread facade; lifecycle from StereoState
  gui/
    StereoOptionsPage.java               injection helper for Sodium options page
  compat/
    sodium/SecondEyeSkipHooks.java       hooks called from Sodium mixins
    iris/PerEyeRenderTargetHooks.java    hooks called from Iris mixins
  mixin/
    minecraft/
      MixinGameRenderer.java             beginFrame/endFrame, two-pass world/HUD/screen, view xlate
      MixinLevelRenderer.java            (placeholder; may be empty)
      MixinGui.java                      (placeholder; may be empty)
      MixinGuiGraphics.java              (placeholder; may be empty)
      MixinRenderSystem.java             viewport intercept, scissor intercept
      MixinMouseHandler.java             dx halving, virtual cursor seeding on screen open
      MixinMinecraft.java                cursor lifecycle on shutdown; setScreen seeding entry
    sodium/
      MixinRenderSectionManager.java     skip updateChunks on RIGHT eye
      MixinSodiumOptionsGUI.java         append Stereoscopic option group
      MixinSodiumGameOptions.java        piggy-back writeChanges to also save StereoOptions
    iris/
      MixinRenderTargets.java            per-eye colortex/depth bank
      MixinIrisRenderingPipeline.java    (empty stub — rebuild is driven from options page, not a mixin)
      MixinHandRenderer.java             per-eye hand depth
      MixinShadowRenderer.java           skip on RIGHT eye
      MixinCompositeRenderer.java        assert/verify per-eye target binding
      MixinFinalPassRenderer.java        blit each eye's colortex0 into its viewport rect
      MixinCameraUniforms.java           per-eye storage slots for current/previousCameraPosition
      MixinMatrixUniforms.java           assertion only; matrix already eye-offset by view xlate
      MixinViewportUniforms.java         viewWidth/viewHeight return full FB, not eye rect
      MixinIrisSamplers.java             per-eye depth sampler binding

src/main/resources/
  fabric.mod.json
  stereoscopic.mixins.json               vanilla + Sodium; required=true
  stereoscopic-iris.mixins.json          Iris; required=false
  assets/stereoscopic/icon.png           16×16 placeholder
  assets/stereoscopic/lang/en_us.json    UI strings for options group

src/test/java/com/mitchellmarx/stereoscopic/
  core/StereoStateTest.java              eye-offset sign, currentEyeIndex, stereoEyeCount
  core/StereoOptionsIOTest.java          JSON round-trip + missing-field tolerance
  render/ViewportMathTest.java           eye rect for SBS_HALF
  render/ScissorRemapTest.java           scissor remap formula
```

---

## Pre-flight notes for every task

1. **Two source-of-truth files override anything in this plan:** `docs/superpowers/notes/versions.md` (from Task 1) for dependency coordinates, and `docs/superpowers/notes/targets.md` (from Task 2) for mixin target classes / method signatures / descriptors. If a code block in this plan disagrees with those files, the files win.
2. **Mappings.** This plan uses yarn class names. If versions.md picked Mojmap because yarn wasn't available, every yarn-named symbol in the plan needs to be remapped — targets.md captures the actual names. Only `@At` target strings change; nothing structural.
3. **Don't commit `run/`.** It's in `.gitignore`. Don't commit `build/` either.
4. **Deploy hook semantics.** `./gradlew build` triggers `copyToTestInstance` which (a) deletes any existing `stereoscopic-*.jar` in the target folder, (b) copies the freshly remapped jar. Run the **Modrinth launcher manually** to test — `gradle runClient` is for dev only.
5. **Pose-stack convention.** `viewMatrix.translate(x, y, z)` in JOML is `view_new = view · T` (post-multiply). The vanilla anaglyph signs are wrong for VR; we use 1.7.10's signs: `+ipd/2` for LEFT, `-ipd/2` for RIGHT. **If you find yourself wanting to flip these, stop and re-read the spec's `StereoState.getEyeOffset()` paragraph.**

---

## Phase 1 — Scaffolding

### Task 1: Resolve real versions + Gradle/Loom project init + deploy hook

**Goal:** A buildable empty mod jar deployed to the test instance, built against versions that actually exist on Modrinth/Maven (not the spec's nominal target values).

**Files:**
- Create: `docs/superpowers/notes/versions.md`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `.gitignore`
- Create: `src/main/resources/fabric.mod.json`
- Create: `src/main/resources/stereoscopic.mixins.json`
- Create: `src/main/resources/stereoscopic-iris.mixins.json`
- Create: `src/main/resources/assets/stereoscopic/icon.png` (16×16 placeholder)
- Create: `src/main/java/com/mitchellmarx/stereoscopic/Stereoscopic.java` (no-op for now)

- [ ] **Step 0: Resolve real versions before writing any config**

The spec targets MC 1.21.11 / Loader 0.19.0 / Sodium 0.8.7 / Iris 1.10.7 / Java 21. **Verify each exists** before committing them to `gradle.properties`. For each: hit the Modrinth API (or web UI) and write down the actual published version string and its Maven coordinate.

```bash
# Minecraft + Yarn — check Fabric's meta API:
curl -s 'https://meta.fabricmc.net/v2/versions/game'    | python -m json.tool | head -40
curl -s 'https://meta.fabricmc.net/v2/versions/yarn/1.21.11' 2>/dev/null | head
curl -s 'https://meta.fabricmc.net/v2/versions/loader'  | python -m json.tool | head

# Fabric API — Modrinth:
curl -s 'https://api.modrinth.com/v2/project/fabric-api/version' \
    | python -c 'import json,sys; [print(v["version_number"], v["game_versions"]) for v in json.load(sys.stdin)[:10]]'

# Sodium — Modrinth:
curl -s 'https://api.modrinth.com/v2/project/sodium/version' \
    | python -c 'import json,sys; [print(v["version_number"], v["game_versions"]) for v in json.load(sys.stdin)[:10]]'

# Iris — Modrinth:
curl -s 'https://api.modrinth.com/v2/project/iris/version' \
    | python -c 'import json,sys; [print(v["version_number"], v["game_versions"]) for v in json.load(sys.stdin)[:10]]'
```

Write findings to `docs/superpowers/notes/versions.md` with this shape:

```markdown
# Resolved versions for Stereoscopic v0.1.0

Resolved on YYYY-MM-DD.

| Component         | Spec target        | Resolved version                  | Maven coordinate / Modrinth slug              |
| ----------------- | ------------------ | --------------------------------- | --------------------------------------------- |
| Minecraft         | 1.21.11            | <actual>                          | com.mojang:minecraft:<actual>                 |
| Yarn mappings     | 1.21.11 (latest)   | <actual e.g. 1.21.11+build.3>     | net.fabricmc:yarn:<actual>:v2                 |
| Fabric Loader     | 0.19.0             | <actual>                          | net.fabricmc:fabric-loader:<actual>           |
| Fabric API        | matching 1.21.11   | <actual e.g. 0.118.0+1.21.11>     | net.fabricmc.fabric-api:fabric-api:<actual>   |
| Sodium            | 0.8.7              | <actual>                          | maven.modrinth:sodium:<modrinth version slug> |
| Iris              | 1.10.7             | <actual>                          | maven.modrinth:iris:<modrinth version slug>   |
| Fabric Loom       | n/a (build only)   | <pick newest 1.21.x-compat stable>| id("fabric-loom") version "<actual>"          |
| Gradle            | n/a (build only)   | <Loom-compatible version>         | gradle-<version>-bin.zip                      |
| Mixin Extras      | n/a                | <actual>                          | com.github.LlamaLad7.MixinExtras:mixinextras-fabric:<actual> |
| JUnit Jupiter     | n/a                | <actual newest>                   | org.junit.jupiter:junit-jupiter:<actual>      |
```

If a spec target doesn't exist (e.g. MC 1.21.11 isn't out), **stop and report** — the spec's MC version becomes the new constraint, not a build wish. Update the spec rather than guessing.

The values you'll plug into `gradle.properties` in step 2 come from this table.

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

rootProject.name = "stereoscopic"
```

- [ ] **Step 2: Create `gradle.properties` using Step 0's resolved values**

```
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true

# Versions — pull these straight from versions.md (Step 0).
# DO NOT plug in spec nominals if Step 0 found something different.
minecraft_version=<from versions.md>
yarn_mappings=<from versions.md>
loader_version=<from versions.md>
fabric_version=<from versions.md>
sodium_version=<from versions.md>
iris_version=<from versions.md>

# Mod
mod_version=0.1.0
maven_group=com.mitchellmarx
archives_base_name=stereoscopic
```

- [ ] **Step 3: Create `build.gradle.kts`**

The Loom version comes from Step 0's `versions.md` row. The skeleton:

```kotlin
plugins {
    id("fabric-loom") version "<from versions.md>"
    `java-library`
    `maven-publish`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String
base.archivesName.set(project.property("archives_base_name") as String)

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

repositories {
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    // Add other repositories only if Step 0 found a dependency that requires them.
    // Sodium and Iris are on Modrinth's maven; no other repo is needed for v0.1.0.
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    modImplementation("maven.modrinth:sodium:${project.property("sodium_version")}")
    modCompileOnly("maven.modrinth:iris:${project.property("iris_version")}")
    modRuntimeOnly("maven.modrinth:iris:${project.property("iris_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter:<from versions.md>")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runs {
        named("client") {
            programArgs("--username", "Dev")
        }
    }
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("minecraft_version", project.property("minecraft_version") as String)
    inputs.property("loader_version", project.property("loader_version") as String)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test { useJUnitPlatform() }

// --- Deploy hook ---
val testInstanceMods = file("C:/Users/felix/AppData/Roaming/ModrinthApp/profiles/Fall 2025 Let_s Play/mods")

val copyToTestInstance by tasks.registering(Copy::class) {
    group = "stereoscopic"
    description = "Deploys remapped jar into the Modrinth test instance"
    dependsOn(tasks.remapJar)
    from(tasks.remapJar.flatMap { it.archiveFile })
    into(testInstanceMods)
    doFirst {
        if (testInstanceMods.exists()) {
            testInstanceMods.listFiles { _, name -> name.startsWith("stereoscopic-") && name.endsWith(".jar") }
                ?.forEach { it.delete() }
        } else {
            throw GradleException("Test instance mods folder does not exist: $testInstanceMods")
        }
    }
}

tasks.build { dependsOn(copyToTestInstance) }
```

- [ ] **Step 4: Create `gradle/wrapper/gradle-wrapper.properties`**

Use the Gradle version Step 0 picked for Loom compatibility.

```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-<from versions.md>-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Then run `gradle wrapper --gradle-version <version>` once (or manually drop a wrapper jar; the user can also run `gradle wrapper` if the system has a global Gradle install).

- [ ] **Step 5: Create `.gitignore`**

```
.gradle/
build/
out/
bin/
run/
.idea/
*.iml
*.ipr
*.iws
.vscode/
.DS_Store
```

- [ ] **Step 6: Create `src/main/resources/fabric.mod.json`**

```jsonc
{
  "schemaVersion": 1,
  "id": "stereoscopic",
  "version": "${version}",
  "name": "Stereoscopic",
  "description": "Side-by-side stereoscopic rendering for VR virtual monitors and 3D displays.",
  "authors": ["Mitchell Marx"],
  "license": "LGPL-3.0-only",
  "icon": "assets/stereoscopic/icon.png",
  "environment": "client",
  "entrypoints": {
    "client": ["com.mitchellmarx.stereoscopic.Stereoscopic"]
  },
  "mixins": [
    "stereoscopic.mixins.json",
    { "config": "stereoscopic-iris.mixins.json", "environment": "client" }
  ],
  "depends": {
    "fabricloader": ">=${loader_version}",
    "minecraft": "~${minecraft_version}",
    "java": ">=21",
    "sodium": ">=0.8.7"
  },
  "recommends": { "iris": ">=1.10.7" },
  "breaks": { "iris": "<1.10.7" }
}
```

- [ ] **Step 7: Create `src/main/resources/stereoscopic.mixins.json`**

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.mitchellmarx.stereoscopic.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": [],
  "mixins": [],
  "injectors": { "defaultRequire": 1 }
}
```

- [ ] **Step 8: Create `src/main/resources/stereoscopic-iris.mixins.json`**

```json
{
  "required": false,
  "minVersion": "0.8",
  "package": "com.mitchellmarx.stereoscopic.mixin.iris",
  "compatibilityLevel": "JAVA_21",
  "client": [],
  "mixins": [],
  "injectors": { "defaultRequire": 0 }
}
```

- [ ] **Step 9: Create `src/main/resources/assets/stereoscopic/icon.png`**

Create a 16×16 PNG (any solid color is fine for v0.1.0 — a real icon comes later). Easiest:

```bash
# From repo root, generate a magenta placeholder:
python -c "import struct, zlib; w=h=16; raw=b''.join(b'\x00'+b'\xff\x00\xff'*w for _ in range(h)); png=lambda t,d:struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)); out=b'\x89PNG\r\n\x1a\n'+png(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+png(b'IDAT',zlib.compress(raw))+png(b'IEND',b''); open('src/main/resources/assets/stereoscopic/icon.png','wb').write(out)"
```

Or copy any 16×16 PNG into that path. Don't block on this — just needs to exist so Fabric doesn't warn.

- [ ] **Step 10: Create `src/main/java/com/mitchellmarx/stereoscopic/Stereoscopic.java`**

```java
package com.mitchellmarx.stereoscopic;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Stereoscopic implements ClientModInitializer {
    public static final String MOD_ID = "stereoscopic";
    public static final Logger LOG = LoggerFactory.getLogger("Stereoscopic");

    @Override
    public void onInitializeClient() {
        LOG.info("Stereoscopic initialized");
    }
}
```

- [ ] **Step 11: Verify the build produces a jar and deploys it**

Run:
```bash
./gradlew build --info 2>&1 | tail -40
```

Expected: `BUILD SUCCESSFUL`. Verify the jar exists in the test instance:
```bash
ls "/c/Users/felix/AppData/Roaming/ModrinthApp/profiles/Fall 2025 Let_s Play/mods/" | grep stereoscopic
```
Expected: `stereoscopic-0.1.0.jar` appears.

If the build fails because Loom can't find a 1.21.11 yarn build, defer to Task 2's mapping verification and re-run with corrected versions.

- [ ] **Step 12: Smoke-test that Modrinth launches with the empty mod present**

Manual: launch Modrinth → "Fall 2025 Let_s Play" profile → confirm log says `[Stereoscopic] Stereoscopic initialized`. No crash. Quit Modrinth.

- [ ] **Step 13: Commit**

```bash
git add -A
git commit -m "Task 1: resolve versions + Gradle/Loom scaffold + deploy hook"
```

Make sure `docs/superpowers/notes/versions.md` is in the staged changes — every later task reads it.

---

### Task 2: Build `targets.md` — the canonical API-surface file consumed by every later task

**Goal:** A machine-readable canonical record of every yarn / Sodium / Iris class, method signature, field, and bytecode descriptor this mod targets. Downstream tasks **do not invent** these — they read this file. Tasks 8–27 will fail if this file is incomplete or wrong.

This task does NOT produce a passive report. It produces a structured file with one row per addressable thing, and each row carries enough information for a downstream task to write a working mixin with no further investigation.

**Files:**
- Create: `docs/superpowers/notes/targets.md`

- [ ] **Step 1: Locate the jars Loom resolved**

```bash
find "$HOME/.gradle/caches/fabric-loom"          -name "*.jar" 2>/dev/null | grep -Ei "(sodium|iris)"
find "$HOME/.gradle/caches/modules-2/files-2.1"  -name "*.jar" 2>/dev/null | grep -Ei "(sodium|iris|fabric|yarn|minecraft)"
```

Loom's intermediary-mapped MC jar typically lives under `~/.gradle/caches/fabric-loom/minecraft-<version>-<hash>/<mappings>/`. Record the paths in `targets.md`'s header.

- [ ] **Step 2: Inspect each jar's class layout**

For every jar, run:

```bash
jar tf <jar> | sort > /tmp/<jar-basename>.classes
```

This dumps the full class list. Greps below identify the rough package layout. Then use `javap -p -c` (or open in IntelliJ's Java decompiler) for each class to read field/method signatures.

- [ ] **Step 3: Populate `targets.md` with one row per addressable target**

The required surface and the format below. **Every row must be filled in.** Where a target doesn't exist, write `MISSING` and a brief replacement plan; that's a real answer.

```markdown
# targets.md — Stereoscopic v0.1.0 API surface

Resolved on YYYY-MM-DD against the versions in versions.md.

## Section A — Minecraft (yarn)

### A.1 GameRenderer.render
- Class: `net.minecraft.client.render.GameRenderer`  (verify; if yarn moved it, update)
- Method: `render(<DeltaTracker FQN>, boolean)V`
- Bytecode descriptor: `(L<DeltaTracker desc>;Z)V`
- Notes: this is the `@Inject(HEAD/RETURN)` target for begin/endFrame.

### A.2 LevelRenderer.renderLevel
- Class: `net.minecraft.client.render.LevelRenderer`
- Method: <full yarn signature including ObjectAllocator/Matrix4f/Fog/etc>
- Bytecode descriptor: <exact>
- View-matrix argument index (0-based): N
- Notes: argument list shape used by Tasks 9 and 10.

### A.3 RenderSystem.viewport / RenderSystem.enableScissor
- Class: `com.mojang.blaze3d.systems.RenderSystem`
- Methods: `viewport(IIII)V`, `enableScissor(IIII)V`
- GlStateManager underlying calls used to bypass intercept: <yarn names — likely `_viewport` and `_enableScissorTest` + `_scissorBox`, but VERIFY>

### A.4 InGameHud.render (or Gui.render)
- Class: <exact yarn FQN>
- Method: <full signature>

### A.5 Screen.render (the screen-render method called by GameRenderer.render)
- Class: `net.minecraft.client.gui.screen.Screen`
- Method: <full signature — could be `renderWithTooltip`, `render`, or different>
- Notes: this is the @WrapOperation target for Task 13.

### A.6 Mouse — cursor delta path + position fields
- Class: <yarn FQN — probably `net.minecraft.client.Mouse`>
- Delta-handling method: <method name + signature where `dx` local lives>
- Local-variable ordinal for the `dx` double: N
- Position fields: <names of the x/y fields that hold the GUI-space cursor>
- Access strategy: accessor mixin? Note the field modifiers (private/final/etc.)

### A.7 MinecraftClient.setScreen
- Class: `net.minecraft.client.MinecraftClient`
- Method: `setScreen(<Screen FQN>)V`

### A.8 Window dimensions
- Class: <yarn FQN>
- Methods to fetch framebuffer width/height: <exact yarn names>

### A.9 GLFW window handle from MinecraftClient
- Method chain: `MinecraftClient.getInstance().getWindow().<getHandle>()` — verify final method name (`getHandle()` is yarn-stable IIRC, but confirm)

### A.10 WorldRenderer reload (for stereo-toggle Sodium reload)
- Class: <yarn FQN>
- Method: <yarn name — likely `reload()` but might be `reload(ResourceManager)`>

## Section B — Sodium 0.8.x

### B.1 Chunk-upload entry point
- Class: <FQN — was `me.jellysquid.mods.sodium...` pre-0.6, now `net.caffeinemc.mods.sodium...`>
- Method: <name + signature of the per-frame method that uploads dirty chunks>
- Bytecode descriptor: <exact>
- Notes: equivalent of 1.7.10's `RenderGlobal.clipRenderersByFrustum → renderer.updateChunks`. In Sodium 0.8 this might be split across methods; identify the ONE we should @Inject(HEAD)+cancellable to skip.

### B.2 Options screen — page list builder
- Class: <FQN of the screen that builds the page list>
- Method: <name + signature that returns the `List<OptionPage>` (or its equivalent)>
- Notes: this is where we splice our group in.

### B.3 OptionPage API
- Class: <FQN>
- Constructor + how its option-group list is exposed (getter? mutable list? builder API?)

### B.4 OptionGroup API
- Class: <FQN>
- Builder method or constructor we'll use to create a new group + add options.

### B.5 Option control API
- For a cycling enum (Mode, Debug-Eye): <class, builder method, exact API to bind setter/getter>
- For an int slider (IPD in milli-meters 0..500): <class, builder method, value-formatter signature>
- For impact-tagging (HIGH/LOW): <enum FQN if it exists; omit if it doesn't>

### B.6 Sodium options model save trigger
- Class: <FQN — `SodiumGameOptions` or whatever the current name is>
- Save method: <name + signature; was `writeChanges(Path)` historically>
- Bytecode descriptor: <exact>

## Section C — Iris 1.10.x

### C.1 Iris top-level pipeline-rebuild entry points
- Class: <FQN — `net.irisshaders.iris.Iris` or moved>
- Method to reload pipeline: <`reload()` if it exists; else the destroy+prepare pair>
- If destroy+prepare: signatures + the dimension-id argument type (FQN + how to obtain the current dim's id)

### C.2 RenderTargets — per-eye allocation target
- Class: <FQN>
- Constructor signature + what its target-array field is named
- Resize method (`resize`, `resizeIfNeeded`, etc.) + signature
- Framebuffer-bind method we'll @Inject before: <name + signature>
- Depth-texture exposure: is it a field, getter, or part of a struct?

### C.3 CameraUniforms.CameraPositionTracker
- Class: <FQN — including inner-class denotation>
- The `update()` method we replace + signature
- How current camera position is computed inside it (so we can replicate in our per-eye replacement) — copy the body or reference the source location

### C.4 MatrixUniforms / ViewportUniforms / IrisSamplers
- Class FQNs
- For ViewportUniforms: which method feeds the viewWidth/viewHeight uniforms + how to @ModifyVariable / @ModifyArg the values
- For IrisSamplers: the depth-sampler-binding method that needs per-eye routing

### C.5 ShadowRenderer
- Class: <FQN>
- Shadow-pass entry: <method name + signature>

### C.6 HandRenderer
- Class: <FQN>
- Hand-bind/render method to per-eye-ify: <name + signature>
- Hand depth-texture field/method: <how it's stored>

### C.7 FinalPassRenderer + CompositeRenderer
- Class FQNs
- Final-blit method: <name + signature>

## Section D — LWJGL3 Win32 bindings

### D.1 User32 methods we use
- Class: `org.lwjgl.system.windows.User32`
- For each of {LoadCursor, GetIconInfo, GetCursorPos, GetClientRect, ClientToScreen, ClipCursor}: record the exact static-method name (LWJGL3 sometimes prefixes with `n` for raw native + offers a friendlier overload). Note which overload we'll use.

### D.2 GDI32 methods we use
- Class: `org.lwjgl.system.windows.GDI32`
- For each of {CreateCompatibleDC, SelectObject, GetDIBits, CreateCompatibleBitmap, BitBlt, DeleteDC, DeleteObject}: record exact names.

### D.3 LWJGL3 Win32 structs
- For each of {ICONINFO, RECT, POINT, BITMAP, BITMAPINFO}: confirm the LWJGL3 class exists and record the field-accessor naming convention (e.g. `RECT.left()`/`left(int)` getter+setter pair).
```

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/notes/targets.md
git commit -m "Task 2: targets.md — canonical API surface for Sodium 0.8 / Iris 1.10 / MC 1.21.11 / LWJGL3"
```

> **From here on, every task that touches Sodium, Iris, or LWJGL3 Win32 starts by reading the relevant section of `targets.md`. If a row is incomplete, the task is blocked until Task 2 is updated.**

---

### Task 3: Source-tree skeleton + mixin config wiring

**Goal:** Empty stubs for every class the plan creates, so later tasks fill in implementations without restructuring directories mid-stream. Both mixin configs reference at least one stub mixin so the load order is exercised.

**Files:**
- Create empty stubs for: every file listed in the File Structure section that doesn't already exist from Task 1. Minimum viable contents below.

- [ ] **Step 1: Create the core/render/cursor/gui/compat package skeleton**

Each Java file is its own commit-clean stub. Examples:

```java
// src/main/java/com/mitchellmarx/stereoscopic/core/StereoMode.java
package com.mitchellmarx.stereoscopic.core;

public enum StereoMode {
    OFF, SBS_HALF;

    public boolean isActive()      { return this != OFF; }
    public boolean isSideBySide()  { return this == SBS_HALF; }
    public boolean isHalf()        { return this == SBS_HALF; }
}
```

```java
// src/main/java/com/mitchellmarx/stereoscopic/core/StereoDebugEye.java
package com.mitchellmarx.stereoscopic.core;

public enum StereoDebugEye { OFF, LEFT, RIGHT }
```

```java
// src/main/java/com/mitchellmarx/stereoscopic/core/StereoState.java
package com.mitchellmarx.stereoscopic.core;

public final class StereoState {
    // Implementation comes in Task 5.
    private StereoState() {}
}
```

Repeat the empty-stub pattern for `StereoOptions`, `StereoOptionsIO`, `PerEyeRenderer`, `ViewportMath`, `ScissorRemap`, `CursorBackend`, `WindowsCursorBackend`, `NoOpCursorBackend`, `CursorPresentThread`, `StereoCursor`, `StereoOptionsPage`, `SecondEyeSkipHooks`, `PerEyeRenderTargetHooks`.

- [ ] **Step 2: Create mixin skeletons (empty `@Mixin` classes targeting concrete MC/Sodium/Iris classes per the Task 2 report)**

Example:

```java
// src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinGameRenderer.java
package com.mitchellmarx.stereoscopic.mixin.minecraft;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    // Filled in by Tasks 9, 10, 12, 14, 15.
}
```

Create empty mixin stubs for: `MixinGameRenderer`, `MixinLevelRenderer`, `MixinGui` (use `InGameHud.class` if yarn renamed), `MixinGuiGraphics` (= `DrawContext.class`), `MixinRenderSystem`, `MixinMouseHandler`, `MixinMinecraft`, `MixinRenderSectionManager`, `MixinSodiumOptionsGUI`, `MixinSodiumGameOptions`, `MixinRenderTargets`, `MixinIrisRenderingPipeline`, `MixinHandRenderer`, `MixinShadowRenderer`, `MixinCompositeRenderer`, `MixinFinalPassRenderer`, `MixinCameraUniforms`, `MixinMatrixUniforms`, `MixinViewportUniforms`, `MixinIrisSamplers`.

For Iris mixins, the package is `com.mitchellmarx.stereoscopic.mixin.iris.*` (matches `stereoscopic-iris.mixins.json`'s `package` field).

- [ ] **Step 3: Wire mixin configs**

Replace `stereoscopic.mixins.json`'s empty `client` list:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.mitchellmarx.stereoscopic.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "minecraft.MixinGameRenderer",
    "minecraft.MixinLevelRenderer",
    "minecraft.MixinGui",
    "minecraft.MixinGuiGraphics",
    "minecraft.MixinRenderSystem",
    "minecraft.MixinMouseHandler",
    "minecraft.MixinMinecraft",
    "sodium.MixinRenderSectionManager",
    "sodium.MixinSodiumOptionsGUI",
    "sodium.MixinSodiumGameOptions"
  ],
  "mixins": [],
  "injectors": { "defaultRequire": 1 }
}
```

Replace `stereoscopic-iris.mixins.json`'s empty `client` list:

```json
{
  "required": false,
  "minVersion": "0.8",
  "package": "com.mitchellmarx.stereoscopic.mixin.iris",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "MixinRenderTargets",
    "MixinIrisRenderingPipeline",
    "MixinHandRenderer",
    "MixinShadowRenderer",
    "MixinCompositeRenderer",
    "MixinFinalPassRenderer",
    "MixinCameraUniforms",
    "MixinMatrixUniforms",
    "MixinViewportUniforms",
    "MixinIrisSamplers"
  ],
  "mixins": [],
  "injectors": { "defaultRequire": 0 }
}
```

- [ ] **Step 4: Add lang strings**

```json
// src/main/resources/assets/stereoscopic/lang/en_us.json
{
  "stereoscopic.options.group.name": "Stereoscopic",
  "stereoscopic.options.mode.name": "Mode",
  "stereoscopic.options.mode.off": "Off",
  "stereoscopic.options.mode.sbs_half": "SBS Half",
  "stereoscopic.options.ipd.name": "IPD (meters)",
  "stereoscopic.options.debug_eye.name": "Debug force eye",
  "stereoscopic.options.debug_eye.off": "Off",
  "stereoscopic.options.debug_eye.left": "Left",
  "stereoscopic.options.debug_eye.right": "Right"
}
```

- [ ] **Step 5: Verify build still passes (mixin classes exist, no targets resolved yet but no failures either)**

```bash
./gradlew build 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. Some Mixin "no targets" warnings are OK for now — the empty mixins don't have `@At` annotations yet.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Task 3: source-tree skeleton + mixin config wiring"
```

---

## Phase 2 — Core state & options

### Task 4: StereoMode and StereoDebugEye enums (already stubbed) + tests

**Goal:** The simplest enums end-to-end with unit tests, so we know JUnit is wired and so subsequent enum lookups are reliable.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/core/StereoMode.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/core/StereoDebugEye.java`
- Create: `src/test/java/com/mitchellmarx/stereoscopic/core/StereoModeTest.java`

- [ ] **Step 1: Verify `StereoMode.java` matches the Task 3 stub** — already implemented, no change.

- [ ] **Step 2: Verify `StereoDebugEye.java`** — already implemented, no change.

- [ ] **Step 3: Add tests**

```java
// src/test/java/com/mitchellmarx/stereoscopic/core/StereoModeTest.java
package com.mitchellmarx.stereoscopic.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StereoModeTest {
    @Test void offIsInactive()        { assertFalse(StereoMode.OFF.isActive()); }
    @Test void sbsHalfIsActive()      { assertTrue(StereoMode.SBS_HALF.isActive()); }
    @Test void sbsHalfIsSideBySide()  { assertTrue(StereoMode.SBS_HALF.isSideBySide()); }
    @Test void sbsHalfIsHalf()        { assertTrue(StereoMode.SBS_HALF.isHalf()); }
    @Test void offIsNotSideBySide()   { assertFalse(StereoMode.OFF.isSideBySide()); }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew test --info 2>&1 | tail -30
```

Expected: 5 passing. If JUnit can't be found, double-check `build.gradle.kts`'s `testImplementation` lines.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Task 4: StereoMode tests; JUnit pipeline verified"
```

---

### Task 5: StereoState singleton (lifecycle + signs)

**Goal:** The per-frame state machine the entire stereo pipeline reads. Sign convention is critical — left = `+ipd/2`, right = `-ipd/2`, matches 1.7.10 reference. `stereoEyeCount()` reads `StereoOptions` directly because Iris init can fire before `beginFrame()`.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/core/StereoState.java`
- Create: `src/test/java/com/mitchellmarx/stereoscopic/core/StereoStateTest.java`

- [ ] **Step 1: Write the failing test first** (sign convention is too easy to break later — pin it now)

```java
// src/test/java/com/mitchellmarx/stereoscopic/core/StereoStateTest.java
package com.mitchellmarx.stereoscopic.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StereoStateTest {

    @BeforeEach
    void resetOptions() {
        StereoOptions.INSTANCE.mode = StereoMode.OFF;
        StereoOptions.INSTANCE.ipd = 0.064f;
        StereoOptions.INSTANCE.debugForceEye = StereoDebugEye.OFF;
        StereoState.INSTANCE.endFrame();
    }

    @Test
    void getEyeOffsetSignsMatch1710Reference() {
        StereoOptions.INSTANCE.mode = StereoMode.SBS_HALF;
        StereoOptions.INSTANCE.ipd = 0.064f;
        StereoState.INSTANCE.beginFrame(1920, 1080);

        StereoState.INSTANCE.setEye(StereoState.Eye.LEFT);
        assertEquals(+0.032f, StereoState.INSTANCE.getEyeOffset(), 1e-7f,
            "LEFT eye gets +ipd/2 (1.7.10 convention; vanilla anaglyph uses opposite sign)");

        StereoState.INSTANCE.setEye(StereoState.Eye.RIGHT);
        assertEquals(-0.032f, StereoState.INSTANCE.getEyeOffset(), 1e-7f,
            "RIGHT eye gets -ipd/2");

        StereoState.INSTANCE.setEye(StereoState.Eye.MONO);
        assertEquals(0f, StereoState.INSTANCE.getEyeOffset(), 1e-7f);
    }

    @Test
    void currentEyeIndexMapsLeftOrMonoToZeroRightToOne() {
        StereoOptions.INSTANCE.mode = StereoMode.SBS_HALF;
        StereoState.INSTANCE.beginFrame(1920, 1080);

        StereoState.INSTANCE.setEye(StereoState.Eye.LEFT);
        assertEquals(0, StereoState.INSTANCE.currentEyeIndex());

        StereoState.INSTANCE.setEye(StereoState.Eye.RIGHT);
        assertEquals(1, StereoState.INSTANCE.currentEyeIndex());

        StereoState.INSTANCE.setEye(StereoState.Eye.MONO);
        assertEquals(0, StereoState.INSTANCE.currentEyeIndex());
    }

    @Test
    void stereoEyeCountReadsOptionsNotCachedFlag() {
        // Iris can call stereoEyeCount() before beginFrame() ever ran.
        StereoOptions.INSTANCE.mode = StereoMode.SBS_HALF;
        assertEquals(2, StereoState.INSTANCE.stereoEyeCount(),
            "stereoEyeCount must read config directly, not the per-frame `active` flag");

        StereoOptions.INSTANCE.mode = StereoMode.OFF;
        assertEquals(1, StereoState.INSTANCE.stereoEyeCount());
    }

    @Test
    void endFrameClearsActiveButKeepsFrameSnapshot() {
        StereoOptions.INSTANCE.mode = StereoMode.SBS_HALF;
        StereoOptions.INSTANCE.ipd = 0.080f;
        StereoState.INSTANCE.beginFrame(1920, 1080);
        StereoState.INSTANCE.setEye(StereoState.Eye.RIGHT);
        StereoState.INSTANCE.endFrame();

        assertFalse(StereoState.INSTANCE.isActive(),
            "endFrame clears active");
        assertEquals(StereoState.Eye.MONO, StereoState.INSTANCE.getCurrentEye(),
            "endFrame resets currentEye to MONO");
        assertEquals(0.080f, StereoState.INSTANCE.getFrameIpd(), 1e-7f,
            "endFrame must NOT clear frameIpd — post-frame callbacks need it");
        assertEquals(StereoMode.SBS_HALF, StereoState.INSTANCE.getFrameMode(),
            "endFrame must NOT clear frameMode");
    }

    @Test
    void debugForceEyeOverridesLeftRightBeginFrame() {
        StereoOptions.INSTANCE.mode = StereoMode.OFF;
        StereoOptions.INSTANCE.debugForceEye = StereoDebugEye.LEFT;
        StereoState.INSTANCE.beginFrame(1920, 1080);
        // debugForceEye makes the frame "active" even when mode is OFF,
        // because the debug toggle is the validation tool.
        assertTrue(StereoState.INSTANCE.isDebugForcingEye());
    }
}
```

- [ ] **Step 2: Run tests, see them fail** (only the `getEyeOffset` failure matters; some won't even compile yet)

```bash
./gradlew test 2>&1 | tail -40
```

Expected: COMPILE FAILURE (`StereoState.INSTANCE` doesn't exist, etc.) — that's correct; this drives implementation.

- [ ] **Step 3: Implement `StereoState`**

```java
// src/main/java/com/mitchellmarx/stereoscopic/core/StereoState.java
package com.mitchellmarx.stereoscopic.core;

public final class StereoState {

    public static final StereoState INSTANCE = new StereoState();

    public enum Eye { LEFT, RIGHT, MONO }

    private boolean active;
    private Eye currentEye = Eye.MONO;
    private boolean inWorldPass;
    private boolean inGuiPass;

    // Per-eye viewport rect (in main-FB pixel space).
    private int eyeVpX, eyeVpY, eyeVpW, eyeVpH;

    // Frame-cached snapshot (must NOT be cleared by endFrame).
    private StereoMode frameMode = StereoMode.OFF;
    private float frameIpd = 0.064f;
    private StereoDebugEye frameDebugEye = StereoDebugEye.OFF;

    // Cached main-FB dimensions at frame start, used by mixins that need them
    // after beginFrame() without having to re-query the window.
    private int frameFbW, frameFbH;

    private StereoState() {}

    /**
     * Called from {@code MixinGameRenderer} {@code @Inject(HEAD, render)}.
     * Snapshots options into frame-local fields so mid-frame config flips
     * cannot make the per-eye state inconsistent.
     */
    public void beginFrame(int fbW, int fbH) {
        StereoOptions o = StereoOptions.INSTANCE;
        this.frameMode = o.mode;
        this.frameIpd = o.ipd;
        this.frameDebugEye = o.debugForceEye;
        this.frameFbW = fbW;
        this.frameFbH = fbH;
        // "active" includes the debug-force-eye case so the eye-offset
        // path still runs with one eye when validating without a headset.
        this.active = frameMode.isActive() || frameDebugEye != StereoDebugEye.OFF;
        this.currentEye = Eye.MONO;
    }

    /**
     * Clears per-frame transient state. Does NOT clear {@link #frameMode}/
     * {@link #frameIpd}/{@link #frameDebugEye} — those remain readable for
     * any post-frame callbacks (e.g. RenderTickEvent equivalents) that run
     * after the {@code GameRenderer.render} TAIL inject.
     */
    public void endFrame() {
        this.active = false;
        this.currentEye = Eye.MONO;
        this.inWorldPass = false;
        this.inGuiPass = false;
    }

    public void setEye(Eye eye) { this.currentEye = eye; }

    public boolean isActive()           { return active; }
    public boolean isInWorldPass()      { return inWorldPass; }
    public boolean isInGuiPass()        { return inGuiPass; }
    public Eye     getCurrentEye()      { return currentEye; }
    public StereoMode      getFrameMode()     { return frameMode; }
    public float           getFrameIpd()      { return frameIpd; }
    public StereoDebugEye  getFrameDebugEye() { return frameDebugEye; }
    public int getFrameFbW() { return frameFbW; }
    public int getFrameFbH() { return frameFbH; }

    public boolean isDebugForcingEye() { return frameDebugEye != StereoDebugEye.OFF; }

    public void enterWorldPass(int x, int y, int w, int h) {
        this.inWorldPass = true;
        this.eyeVpX = x; this.eyeVpY = y; this.eyeVpW = w; this.eyeVpH = h;
    }
    public void exitWorldPass() { this.inWorldPass = false; }

    public void enterGuiPass(int x, int y, int w, int h) {
        this.inGuiPass = true;
        this.eyeVpX = x; this.eyeVpY = y; this.eyeVpW = w; this.eyeVpH = h;
    }
    public void exitGuiPass() { this.inGuiPass = false; }

    public int getEyeVpX() { return eyeVpX; }
    public int getEyeVpY() { return eyeVpY; }
    public int getEyeVpW() { return eyeVpW; }
    public int getEyeVpH() { return eyeVpH; }

    /**
     * +ipd/2 for LEFT, -ipd/2 for RIGHT, 0 for MONO.
     *
     * <p>This sign convention matches the 1.7.10 Angelica reference. The vanilla
     * anaglyph code uses the opposite signs (right eye positive), which produces
     * eye-swapped stereo to a VR viewer. If you find yourself wanting to flip
     * these because "the world looks inside-out", the bug is upstream of here —
     * most commonly a swapped LEFT/RIGHT viewport in {@code ViewportMath.eyeRect}.
     */
    public float getEyeOffset() {
        return switch (currentEye) {
            case LEFT  -> +frameIpd * 0.5f;
            case RIGHT -> -frameIpd * 0.5f;
            case MONO  -> 0f;
        };
    }

    /** 0 for LEFT/MONO, 1 for RIGHT — for indexing per-eye arrays. */
    public int currentEyeIndex() {
        return currentEye == Eye.RIGHT ? 1 : 0;
    }

    /**
     * Reads {@link StereoOptions} directly — NOT the cached {@link #active}
     * flag. Iris pipeline init runs before the first {@code beginFrame()};
     * reading the cached flag there would size {@code RenderTargets} for
     * monoscopic and contaminate the first stereo frame until shader reload.
     */
    public int stereoEyeCount() {
        return StereoOptions.INSTANCE.mode.isActive() ? 2 : 1;
    }
}
```

- [ ] **Step 4: Add minimal `StereoOptions` (just the fields the tests need; full implementation comes in Task 6)**

```java
// src/main/java/com/mitchellmarx/stereoscopic/core/StereoOptions.java
package com.mitchellmarx.stereoscopic.core;

public final class StereoOptions {
    public static final StereoOptions INSTANCE = new StereoOptions();

    public StereoMode mode = StereoMode.OFF;
    public float ipd = 0.064f;       // meters
    public StereoDebugEye debugForceEye = StereoDebugEye.OFF;

    private StereoOptions() {}
}
```

- [ ] **Step 5: Run tests, see them pass**

```bash
./gradlew test 2>&1 | tail -30
```

Expected: 5 passing in `StereoStateTest`, 5 passing in `StereoModeTest`, total 10.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Task 5: StereoState with per-frame snapshot + sign convention tests"
```

---

### Task 6: StereoOptions persistence (Gson round-trip)

**Goal:** Read/write `config/stereoscopic-options.json` using the MC-bundled Gson. Tolerant of missing fields (use defaults). No human-editing workflow is documented but the file must round-trip cleanly.

**Files:**
- Create: `src/main/java/com/mitchellmarx/stereoscopic/core/StereoOptionsIO.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/core/StereoOptions.java` (add `save()` + clamp)
- Create: `src/test/java/com/mitchellmarx/stereoscopic/core/StereoOptionsIOTest.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/Stereoscopic.java` (load on init)

- [ ] **Step 1: Write the round-trip test**

```java
// src/test/java/com/mitchellmarx/stereoscopic/core/StereoOptionsIOTest.java
package com.mitchellmarx.stereoscopic.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class StereoOptionsIOTest {

    @Test
    void roundTrip(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("opts.json");

        StereoOptions o = new StereoOptions();
        o.mode = StereoMode.SBS_HALF;
        o.ipd = 0.075f;
        o.debugForceEye = StereoDebugEye.LEFT;

        StereoOptionsIO.writeTo(file, o);

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);

        assertEquals(StereoMode.SBS_HALF, loaded.mode);
        assertEquals(0.075f, loaded.ipd, 1e-6f);
        assertEquals(StereoDebugEye.LEFT, loaded.debugForceEye);
    }

    @Test
    void missingFileLeavesDefaults(@TempDir Path tmp) throws Exception {
        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(tmp.resolve("nope.json"), loaded);
        assertEquals(StereoMode.OFF, loaded.mode);
        assertEquals(0.064f, loaded.ipd, 1e-6f);
        assertEquals(StereoDebugEye.OFF, loaded.debugForceEye);
    }

    @Test
    void partialJsonKeepsDefaultsForMissingKeys(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("partial.json");
        Files.writeString(file, "{ \"ipd\": 0.080 }");

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);
        assertEquals(StereoMode.OFF, loaded.mode);
        assertEquals(0.080f, loaded.ipd, 1e-6f);
        assertEquals(StereoDebugEye.OFF, loaded.debugForceEye);
    }

    @Test
    void ipdIsClampedOnRead(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("oob.json");
        Files.writeString(file, "{ \"ipd\": 99.0 }");

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);
        assertEquals(0.5f, loaded.ipd, 1e-6f, "IPD clamped to [0.0, 0.5]");
    }
}
```

Note: tests construct `new StereoOptions()`. Adjust `StereoOptions` to allow package-private construction for tests:

```java
StereoOptions() {} // package-private for tests; production code uses INSTANCE
```

- [ ] **Step 2: Implement `StereoOptionsIO`**

```java
// src/main/java/com/mitchellmarx/stereoscopic/core/StereoOptionsIO.java
package com.mitchellmarx.stereoscopic.core;

import com.google.gson.*;
import com.mitchellmarx.stereoscopic.Stereoscopic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class StereoOptionsIO {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private StereoOptionsIO() {}

    public static void readInto(Path path, StereoOptions target) {
        if (!Files.exists(path)) return;
        try {
            String body = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();

            if (obj.has("mode")) {
                String s = obj.get("mode").getAsString();
                try { target.mode = StereoMode.valueOf(s); }
                catch (IllegalArgumentException ex) {
                    Stereoscopic.LOG.warn("Unknown stereo mode {}; keeping default", s);
                }
            }
            if (obj.has("ipd")) {
                float v = obj.get("ipd").getAsFloat();
                target.ipd = Math.max(0f, Math.min(0.5f, v));
            }
            if (obj.has("debugForceEye")) {
                String s = obj.get("debugForceEye").getAsString();
                try { target.debugForceEye = StereoDebugEye.valueOf(s); }
                catch (IllegalArgumentException ex) {
                    Stereoscopic.LOG.warn("Unknown debugForceEye {}; keeping default", s);
                }
            }
        } catch (IOException | JsonParseException e) {
            Stereoscopic.LOG.error("Failed to read {}; keeping in-memory defaults", path, e);
        }
    }

    public static void writeTo(Path path, StereoOptions source) {
        try {
            Files.createDirectories(path.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("mode", source.mode.name());
            obj.addProperty("ipd", source.ipd);
            obj.addProperty("debugForceEye", source.debugForceEye.name());
            Files.writeString(path, GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Stereoscopic.LOG.error("Failed to write {}", path, e);
        }
    }
}
```

- [ ] **Step 3: Add `save()` to `StereoOptions` and the path constant**

```java
// Append to StereoOptions.java:
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public Path configFile() {
    return FabricLoader.getInstance().getConfigDir().resolve("stereoscopic-options.json");
}

public void save() { StereoOptionsIO.writeTo(configFile(), this); }
public void load() { StereoOptionsIO.readInto(configFile(), this); }
```

(Don't call this from the test code — the tests use `writeTo`/`readInto` directly with a tempdir.)

- [ ] **Step 4: Wire `Stereoscopic.onInitializeClient` to load on startup**

```java
// In Stereoscopic.java's onInitializeClient:
StereoOptions.INSTANCE.load();
LOG.info("Stereoscopic initialized (mode={}, ipd={})",
    StereoOptions.INSTANCE.mode, StereoOptions.INSTANCE.ipd);
```

- [ ] **Step 5: Run tests**

```bash
./gradlew test 2>&1 | tail -20
```

Expected: 14 passing total (5 + 5 + 4).

- [ ] **Step 6: Build + launch test instance once to confirm load runs without error**

```bash
./gradlew build 2>&1 | tail -10
```
Then launch Modrinth. Confirm log contains:
```
[Stereoscopic] Stereoscopic initialized (mode=OFF, ipd=0.064)
```
No crash. Quit.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Task 6: StereoOptions JSON persistence with clamp + Gson round-trip"
```

---

### Task 7: ViewportMath, ScissorRemap, and PerEyeRenderer helpers

**Goal:** Pure-math helpers used by mixins. All three are easy to unit-test and avoid live MC dependencies.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/render/ViewportMath.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/render/ScissorRemap.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/render/PerEyeRenderer.java`
- Create: `src/test/java/com/mitchellmarx/stereoscopic/render/ViewportMathTest.java`
- Create: `src/test/java/com/mitchellmarx/stereoscopic/render/ScissorRemapTest.java`

- [ ] **Step 1: Implement `ViewportMath`**

```java
// src/main/java/com/mitchellmarx/stereoscopic/render/ViewportMath.java
package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.core.StereoMode;
import com.mitchellmarx.stereoscopic.core.StereoState;

public final class ViewportMath {
    private ViewportMath() {}

    public static final class Rect {
        public final int x, y, w, h;
        public Rect(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    /** Compute eye viewport rect in main-FB pixel space. */
    public static Rect eyeRect(StereoState.Eye eye, StereoMode mode, int fbW, int fbH) {
        if (mode == StereoMode.SBS_HALF) {
            int half = fbW / 2;
            return switch (eye) {
                case LEFT, MONO -> new Rect(0,    0, half, fbH);
                case RIGHT      -> new Rect(half, 0, half, fbH);
            };
        }
        return new Rect(0, 0, fbW, fbH);
    }
}
```

- [ ] **Step 2: Implement `ScissorRemap`**

```java
// src/main/java/com/mitchellmarx/stereoscopic/render/ScissorRemap.java
package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.core.StereoMode;

public final class ScissorRemap {
    private ScissorRemap() {}

    public static final class Rect {
        public final int x, y, w, h;
        public Rect(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    /**
     * Caller passes a scissor rect expressed in full-screen GUI coordinates
     * (the same space the GUI thinks it's drawing into). We remap into
     * the current eye's viewport pixel space.
     *
     * <p>SBS_HALF: horizontal compression. x and w both halved; eye-vpX
     * shifts the right eye's rect into its half. Y/H unchanged.
     */
    public static Rect remap(StereoMode mode, int callerX, int callerY, int callerW, int callerH,
                             int eyeVpX, int eyeVpW) {
        if (mode == StereoMode.SBS_HALF) {
            int x = (callerX / 2) + eyeVpX;
            int w = callerW / 2;
            return new Rect(x, callerY, w, callerH);
        }
        return new Rect(callerX, callerY, callerW, callerH);
    }
}
```

- [ ] **Step 3: Implement `PerEyeRenderer`**

```java
// src/main/java/com/mitchellmarx/stereoscopic/render/PerEyeRenderer.java
package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.core.StereoOptions;
import com.mitchellmarx.stereoscopic.core.StereoState;
import com.mojang.blaze3d.systems.RenderSystem;

public final class PerEyeRenderer {

    /** Set the GL viewport WITHOUT going through the intercept in MixinRenderSystem. */
    private static boolean bypass;

    public static boolean isBypassActive() { return bypass; }

    public static void viewportRaw(int x, int y, int w, int h) {
        bypass = true;
        try { RenderSystem.viewport(x, y, w, h); }
        finally { bypass = false; }
    }

    public enum Pass { WORLD, GUI }

    /**
     * Run {@code body} twice with eye state set up. The body sees the per-eye
     * viewport applied and the corresponding {@link StereoState#enterWorldPass}
     * or {@link StereoState#enterGuiPass} flag asserted so mid-pipeline GL
     * intercepts work.
     */
    public static void runForEachEye(Pass pass, Runnable body) {
        StereoState s = StereoState.INSTANCE;
        if (!s.isActive()) { body.run(); return; }

        int fbW = s.getFrameFbW();
        int fbH = s.getFrameFbH();

        // Debug-force-eye: render only the forced eye, MONO-style.
        if (s.isDebugForcingEye()) {
            StereoState.Eye forced = (s.getFrameDebugEye() == com.mitchellmarx.stereoscopic.core.StereoDebugEye.LEFT)
                ? StereoState.Eye.LEFT : StereoState.Eye.RIGHT;
            renderOneEye(forced, pass, body, fbW, fbH, s);
            viewportRaw(0, 0, fbW, fbH);
            return;
        }

        for (StereoState.Eye eye : new StereoState.Eye[] { StereoState.Eye.LEFT, StereoState.Eye.RIGHT }) {
            renderOneEye(eye, pass, body, fbW, fbH, s);
        }
        viewportRaw(0, 0, fbW, fbH);
    }

    private static void renderOneEye(StereoState.Eye eye, Pass pass, Runnable body,
                                     int fbW, int fbH, StereoState s) {
        s.setEye(eye);
        ViewportMath.Rect r = ViewportMath.eyeRect(eye, StereoOptions.INSTANCE.mode, fbW, fbH);
        viewportRaw(r.x, r.y, r.w, r.h);
        switch (pass) {
            case WORLD -> s.enterWorldPass(r.x, r.y, r.w, r.h);
            case GUI   -> s.enterGuiPass(r.x, r.y, r.w, r.h);
        }
        try { body.run(); }
        finally {
            switch (pass) {
                case WORLD -> s.exitWorldPass();
                case GUI   -> s.exitGuiPass();
            }
        }
    }
}
```

- [ ] **Step 4: Write tests for `ViewportMath` and `ScissorRemap`**

```java
// src/test/java/com/mitchellmarx/stereoscopic/render/ViewportMathTest.java
package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViewportMathTest {
    @Test void offIsFullScreen() {
        ViewportMath.Rect r = ViewportMath.eyeRect(StereoState.Eye.MONO, StereoMode.OFF, 1920, 1080);
        assertEquals(0, r.x); assertEquals(0, r.y);
        assertEquals(1920, r.w); assertEquals(1080, r.h);
    }
    @Test void sbsHalfLeft() {
        ViewportMath.Rect r = ViewportMath.eyeRect(StereoState.Eye.LEFT, StereoMode.SBS_HALF, 1920, 1080);
        assertEquals(0, r.x); assertEquals(960, r.w);
    }
    @Test void sbsHalfRight() {
        ViewportMath.Rect r = ViewportMath.eyeRect(StereoState.Eye.RIGHT, StereoMode.SBS_HALF, 1920, 1080);
        assertEquals(960, r.x); assertEquals(960, r.w);
    }
    @Test void oddWidthRoundsDown() {
        // If fbW is odd, both halves round down — right eye is identical width.
        ViewportMath.Rect r = ViewportMath.eyeRect(StereoState.Eye.RIGHT, StereoMode.SBS_HALF, 1921, 1080);
        assertEquals(960, r.x);
        assertEquals(960, r.w);
    }
}
```

```java
// src/test/java/com/mitchellmarx/stereoscopic/render/ScissorRemapTest.java
package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.core.StereoMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScissorRemapTest {
    @Test void sbsHalfLeftEye() {
        // Caller scissors a 100×50 rect at (200, 100) in full-screen GUI coords.
        // Left eye viewport starts at x=0, width=960.
        ScissorRemap.Rect r = ScissorRemap.remap(StereoMode.SBS_HALF, 200, 100, 100, 50, 0, 960);
        assertEquals(100, r.x);  // 200/2 + 0
        assertEquals(100, r.y);
        assertEquals(50,  r.w);  // 100/2
        assertEquals(50,  r.h);
    }
    @Test void sbsHalfRightEye() {
        ScissorRemap.Rect r = ScissorRemap.remap(StereoMode.SBS_HALF, 200, 100, 100, 50, 960, 960);
        assertEquals(1060, r.x); // 200/2 + 960
        assertEquals(50,   r.w);
    }
    @Test void offIsIdentity() {
        ScissorRemap.Rect r = ScissorRemap.remap(StereoMode.OFF, 200, 100, 100, 50, 0, 1920);
        assertEquals(200, r.x); assertEquals(100, r.w);
    }
}
```

- [ ] **Step 5: Run tests**

```bash
./gradlew test 2>&1 | tail -25
```

Expected: 21 passing total.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Task 7: ViewportMath + ScissorRemap + PerEyeRenderer with tests"
```

---

## Phase 3 — Two-pass world rendering (Feature A)

### Task 8: `MixinGameRenderer` — beginFrame / endFrame

**Goal:** Wire `StereoState.beginFrame(fbW, fbH)` at the head of `GameRenderer.render` and `endFrame()` at the tail. No rendering changes yet — just the lifecycle.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinGameRenderer.java`

- [ ] **Step 1: Implement `MixinGameRenderer` HEAD and TAIL injects**

Read `targets.md` rows A.1 (GameRenderer.render), A.8 (window framebuffer dims), and A.9 (window handle). Replace any symbol in the skeleton below that disagrees with those rows. Skeleton (yarn names below are educated guesses; A.1/A.8 are authoritative):

```java
package com.mitchellmarx.stereoscopic.mixin.minecraft;

import com.mitchellmarx.stereoscopic.core.StereoState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void stereoscopic$beginFrame(DeltaTracker tracker, boolean tick, CallbackInfo ci) {
        Window w = client.getWindow();
        StereoState.INSTANCE.beginFrame(w.getFramebufferWidth(), w.getFramebufferHeight());
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void stereoscopic$endFrame(DeltaTracker tracker, boolean tick, CallbackInfo ci) {
        StereoState.INSTANCE.endFrame();
    }
}
```

> If yarn for 1.21.11 renamed `DeltaTracker` or `Window.getFramebufferWidth()`, adjust per the Task 2 report. `MinecraftClient.getWindow()` is yarn-stable.

- [ ] **Step 2: Build + launch test instance** with stereo OFF (default config).

```bash
./gradlew build 2>&1 | tail -10
```

Launch Modrinth → load a world → verify nothing visually changes. No crashes. Quit.

- [ ] **Step 3: Set mode to SBS_HALF in config file manually**

Edit `C:\Users\felix\AppData\Roaming\ModrinthApp\profiles\Fall 2025 Let_s Play\config\stereoscopic-options.json`:

```json
{ "mode": "SBS_HALF", "ipd": 0.064, "debugForceEye": "OFF" }
```

Launch again. Still no visual change (no rendering hooks yet) — but log should still show `mode=SBS_HALF`. No crash. Quit.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Task 8: MixinGameRenderer wires StereoState begin/end frame"
```

---

### Task 9: View-matrix translation + visual milestone via `debugForceEye`

**Goal:** Per-eye camera offset via `@ModifyArg` on `LevelRenderer.renderLevel`'s view matrix argument. **This task validates correctness using `debugForceEye`** — the entire two-pass loop is not yet wired, but a forced LEFT/RIGHT renders as monoscopic with a visible camera shift, proving the sign convention is right.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinGameRenderer.java`

- [ ] **Step 1: Add the `@ModifyArg` inject**

Read `targets.md` row A.2 for the exact `LevelRenderer.renderLevel` signature and the view-matrix argument's 0-based index. The argument is likely a JOML `Matrix4f` in 1.21.11 (post-1.21.5 the matrix stack was retired in favor of Matrix4f arguments), but A.2 says for sure. Skeleton below — the `index = 1` and the `target = "...renderLevel(...)V"` placeholder must match A.2:

```java
import com.mitchellmarx.stereoscopic.core.StereoOptions;
import com.mitchellmarx.stereoscopic.core.StereoDebugEye;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@ModifyArg(
    method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
    at = @At(value = "INVOKE",
             target = "Lnet/minecraft/client/render/LevelRenderer;renderLevel(...)V"),
    index = /* index of the view-matrix argument — verify from Task 2 report */ 1
)
private Matrix4f stereoscopic$offsetViewMatrix(Matrix4f viewMatrix) {
    StereoState s = StereoState.INSTANCE;
    if (!s.isActive()) return viewMatrix;

    // Debug-force-eye: pretend that eye is "current" so getEyeOffset() returns it.
    if (s.isDebugForcingEye() && s.getCurrentEye() == StereoState.Eye.MONO) {
        s.setEye(s.getFrameDebugEye() == StereoDebugEye.LEFT
            ? StereoState.Eye.LEFT : StereoState.Eye.RIGHT);
    }

    float dx = s.getEyeOffset();
    if (dx != 0f) {
        // JOML post-multiply: view_new = view · T(dx, 0, 0).
        viewMatrix.translate(dx, 0f, 0f);
    }
    return viewMatrix;
}
```

> If `renderLevel`'s parameter list changed in 1.21.11 and the view matrix is now wrapped (e.g. in a `Camera`-bearing container), the easier hook is `@Inject(HEAD)` on `renderLevel` itself, applying the translate to whatever `viewMatrix` field is stashed. Decide based on the Task 2 report.

- [ ] **Step 2: Build and visually verify with `debugForceEye = LEFT`**

Edit config:
```json
{ "mode": "OFF", "ipd": 0.064, "debugForceEye": "LEFT" }
```

`./gradlew build`, launch Modrinth, load world. **Expected**: world looks like normal monoscopic but **shifted RIGHT in your view** by ~3 cm worth of parallax (because the camera shifted left in world space → world appears to translate right relative to you).

Then change `debugForceEye` to `RIGHT`. World should shift the opposite direction.

If the world shifts the *wrong* direction, the signs in `StereoState.getEyeOffset()` are flipped. Don't "fix" by flipping; verify the test still passes and re-read the spec — most likely the LEFT/RIGHT viewport rects get swapped in a future task, not these signs.

- [ ] **Step 3: Verify `debugForceEye = OFF` returns to normal**

Set back to `{"mode": "OFF", "debugForceEye": "OFF"}`. World should look exactly like vanilla. Quit.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Task 9: per-eye view-matrix translation; debugForceEye validates signs"
```

---

### Task 10: Two-pass `renderLevel` via `@WrapOperation`

**Goal:** Run the world render twice per frame when stereo is active, with `PerEyeRenderer` setting up the eye state in between.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinGameRenderer.java`

- [ ] **Step 1: Add the `@WrapOperation` around the `renderLevel` call**

`@WrapOperation` is Mixin Extras' replacement for `@Redirect` and lets us call the original with adjusted args N times. Read versions.md for the Mixin Extras coordinate (it's a separate row), then add to build.gradle.kts dependencies:

```kotlin
modImplementation("com.github.LlamaLad7.MixinExtras:mixinextras-fabric:<from versions.md>")
```

> Note: Mixin Extras may also be bundled inside Fabric API in recent versions, in which case the explicit dep isn't required. versions.md row says which.

Then in `MixinGameRenderer`:

```java
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchellmarx.stereoscopic.render.PerEyeRenderer;

@WrapOperation(
    method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
    at = @At(value = "INVOKE",
             target = "Lnet/minecraft/client/render/LevelRenderer;renderLevel(...)V")
)
private void stereoscopic$twoPassRenderLevel(LevelRenderer self, /* …other renderLevel args… */
                                              Operation<Void> original) {
    if (!StereoState.INSTANCE.isActive()) {
        original.call(self, /* …args… */);
        return;
    }
    PerEyeRenderer.runForEachEye(PerEyeRenderer.Pass.WORLD,
        () -> original.call(self, /* …args… */));
}
```

> Forward all arguments exactly — `Operation<Void>` is type-erased; the runtime checks arg-count match. The argument list and order must match the verified `renderLevel` signature from Task 2.

- [ ] **Step 2: Build + launch** with config `{"mode": "SBS_HALF", "debugForceEye": "OFF"}`.

Expected: the world renders into both halves. **HUD will be wrong / monoscopic / overdraw** (we haven't hooked it yet — that's Task 12). Right eye may render at full screen size because the viewport-restore-in-pipeline isn't intercepted yet — that's Task 11.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Task 10: two-pass renderLevel via @WrapOperation"
```

---

### Task 11: `MixinRenderSystem` viewport intercept

**Goal:** Catch mid-pipeline `RenderSystem.viewport` calls (Iris composite passes, sky renderer resets, etc.) and remap to the current eye's rect. Bypass when `PerEyeRenderer.viewportRaw` set the flag.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinRenderSystem.java`

- [ ] **Step 1: Implement the intercept**

```java
package com.mitchellmarx.stereoscopic.mixin.minecraft;

import com.mitchellmarx.stereoscopic.core.StereoState;
import com.mitchellmarx.stereoscopic.render.PerEyeRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {

    @Inject(method = "viewport(IIII)V", at = @At("HEAD"), cancellable = true)
    private static void stereoscopic$remapViewport(int x, int y, int w, int h, CallbackInfo ci) {
        if (PerEyeRenderer.isBypassActive()) return; // we set this rect on purpose
        StereoState s = StereoState.INSTANCE;
        if (!s.isInWorldPass() || !s.isActive()) return;

        int fbW = s.getFrameFbW();
        int fbH = s.getFrameFbH();
        // Only remap "full main-FB" viewport calls; passes that already set
        // an intentional sub-rect (e.g. shadow map at 1024×1024) leave alone.
        if (x != 0 || y != 0 || w != fbW || h != fbH) return;

        ci.cancel();
        PerEyeRenderer.viewportRaw(s.getEyeVpX(), s.getEyeVpY(), s.getEyeVpW(), s.getEyeVpH());
    }
}
```

- [ ] **Step 2: Build + launch with `mode=SBS_HALF`**

Expected: world rendering is now stable in two halves, no more "right eye renders fullscreen" behavior caused by mid-pipeline viewport resets. HUD still broken — that's the next task.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Task 11: MixinRenderSystem viewport intercept (in-world)"
```

---

## Phase 4 — HUD + screens + mouse (Feature B)

### Task 12: Two-pass HUD via `@WrapOperation` on `gui.render`

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinGameRenderer.java`

- [ ] **Step 1: Add `@WrapOperation` around the `gui.render` call**

Read targets.md row A.4 for the actual InGameHud class + method signature; the skeleton below uses guesses.

```java
@WrapOperation(
    method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
    at = @At(value = "INVOKE",
             target = "Lnet/minecraft/client/gui/hud/InGameHud;render(...)V")
)
private void stereoscopic$twoPassHud(InGameHud self, /* args */, Operation<Void> original) {
    if (!StereoState.INSTANCE.isActive()) {
        original.call(self, /* args */);
        return;
    }
    PerEyeRenderer.runForEachEye(PerEyeRenderer.Pass.GUI,
        () -> original.call(self, /* args */));
}
```

- [ ] **Step 2: Build + launch with `mode=SBS_HALF`**

Expected: hotbar, health, hunger, XP bar, chat all render in BOTH halves at their correct in-eye position. They may overflow their eye viewport horizontally because the scissor intercept isn't in place yet (Task 14).

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Task 12: two-pass HUD via @WrapOperation on InGameHud.render"
```

---

### Task 13: Two-pass screens + pose-stack reset between eyes

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinGameRenderer.java`

- [ ] **Step 1: Add `@WrapOperation` around `Screen.render` (or whatever the screen-render call inside GameRenderer.render is named)**

Read targets.md row A.5. The historical name `renderWithTooltip` may have been merged into `render` in 1.21.x.

```java
@WrapOperation(
    method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
    at = @At(value = "INVOKE",
             target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V")
)
private void stereoscopic$twoPassScreen(Screen self, DrawContext ctx, int mouseX, int mouseY, float dt,
                                         Operation<Void> original) {
    if (!StereoState.INSTANCE.isActive()) {
        original.call(self, ctx, mouseX, mouseY, dt);
        return;
    }
    PerEyeRenderer.runForEachEye(PerEyeRenderer.Pass.GUI, () -> {
        // Reset pose stack between eye passes so left-eye matrix state can't
        // contaminate right-eye render. 1.21 analogue of the 1.7.10
        // "RenderHelper.disableStandardItemLighting + glColor4f(1,1,1,1)" reset.
        // DrawContext's pose stack is owned by the caller; we reset to identity-at-current-depth.
        // (PerEyeRenderer already calls graphics.flush() between passes — verify Task 7's helper does so.)
        original.call(self, ctx, mouseX, mouseY, dt);
    });
}
```

- [ ] **Step 2: Make `PerEyeRenderer` flush DrawContext between eye passes**

Tricky because `runForEachEye(Runnable)` doesn't know about DrawContext. Two options:

**Option A (simpler):** Have the GUI-pass body itself accept a DrawContext and call `ctx.draw()` at the end of each pass. Adjust the signature of `runForEachEye` to take a `Consumer<StereoState.Eye>` if needed.

**Option B (cleaner — chosen):** Add `PerEyeRenderer.guiPassPostBody` slot — a static `Consumer<StereoState.Eye>` that, if non-null, runs after each body invocation. Set it from `MixinGui`/`MixinGuiGraphics` when the DrawContext flush is desired.

Implement Option B by adding to `PerEyeRenderer`:

```java
private static java.util.function.Consumer<StereoState.Eye> guiPostHook;
public static void setGuiPostHook(java.util.function.Consumer<StereoState.Eye> h) { guiPostHook = h; }

// In renderOneEye, after body.run(), if pass == GUI and guiPostHook != null:
//   guiPostHook.accept(eye);
```

Then in `MixinGui` (`InGameHud` mixin), register a hook on game start:

```java
// inside Stereoscopic.onInitializeClient:
PerEyeRenderer.setGuiPostHook(eye -> {
    // We can't call DrawContext.draw() here without holding the instance —
    // for v0.1.0 omit and rely on Mixin Extras' implicit re-entry handling.
    // Add this if visual artifacts appear in HUD between eyes.
});
```

For v0.1.0, accept that intra-pose-stack state may bleed and only revisit if visual artifacts appear. **Document this in a TODO inside `PerEyeRenderer`** so reviewers know it's a known gap.

- [ ] **Step 3: Build + open inventory** in test instance with `mode=SBS_HALF`.

Expected: inventory renders in both halves. Tooltip on hover renders in both halves. Items appear correctly. May still have scissor overflow until Task 14.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Task 13: two-pass Screen.renderWithTooltip + GUI post hook scaffolding"
```

---

### Task 14: Scissor intercept + mouse delta halving + virtual cursor seeding

**Goal:** GUI scissor calls expressed in full-screen coords get remapped into the current eye's viewport. Mouse X delta gets halved in SBS_HALF so cursor speed feels right. When a Screen opens, the virtual cursor seeds at left-eye-center instead of window-center.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinRenderSystem.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinMouseHandler.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinMinecraft.java`

- [ ] **Step 1: Add scissor intercept to `MixinRenderSystem`**

Read targets.md row A.3 for the `GlStateManager` underlying-call names — historically `_enableScissorTest` and `_scissorBox` but 1.21 may have renamed.

```java
import com.mitchellmarx.stereoscopic.render.ScissorRemap;

@Inject(method = "enableScissor(IIII)V", at = @At("HEAD"), cancellable = true)
private static void stereoscopic$remapScissor(int x, int y, int w, int h, CallbackInfo ci) {
    StereoState s = StereoState.INSTANCE;
    if (!s.isInGuiPass() || !s.isActive()) return;

    ScissorRemap.Rect r = ScissorRemap.remap(s.getFrameMode(),
        x, y, w, h, s.getEyeVpX(), s.getEyeVpW());

    ci.cancel();
    // Call the underlying GlStateManager methods directly so we don't re-enter
    // our own @Inject. Names per targets.md row A.3.
    com.mojang.blaze3d.platform.GlStateManager.<from A.3 — was _enableScissorTest>();
    com.mojang.blaze3d.platform.GlStateManager.<from A.3 — was _scissorBox>(r.x, r.y, r.w, r.h);
}
```

- [ ] **Step 2: Add `MixinMouseHandler` — halve X delta in SBS_HALF**

Read targets.md row A.6 for the Mouse class FQN, the delta-handling method name, and the local-variable ordinal of `dx`.

**Behavior contract:** when `StereoOptions.INSTANCE.mode == StereoMode.SBS_HALF`, halve the X delta before it's applied to the player's yaw / cursor position. Y delta untouched.

Skeleton:

```java
package com.mitchellmarx.stereoscopic.mixin.minecraft;

import com.mitchellmarx.stereoscopic.core.StereoMode;
import com.mitchellmarx.stereoscopic.core.StereoOptions;
import <Mouse FQN from A.6>;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(<Mouse class>.class)
public abstract class MixinMouseHandler {
    @ModifyVariable(
        method = "<delta-handling method+descriptor from A.6>",
        at = @At("STORE"),
        ordinal = <dx local ordinal from A.6>
    )
    private double stereoscopic$halveDxIfSbsHalf(double dx) {
        if (StereoOptions.INSTANCE.mode == StereoMode.SBS_HALF) return dx * 0.5;
        return dx;
    }
}
```

- [ ] **Step 3: Seed virtual cursor at left-eye-center on `setScreen(non-null)`**

Read targets.md rows A.6 (Mouse position fields) and A.7 (MinecraftClient.setScreen signature).

**Behavior contract:** when `MinecraftClient.setScreen(s)` returns with `s != null` and stereo is active, write the cursor position to the center of the left-eye viewport (window-pixel coords `(width/4, height/2)`), not the window center.

The Mouse position fields are private (A.6); write them via an `@Accessor` interface mixin. Skeleton:

```java
// MixinMinecraft.java — RETURN inject into setScreen
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraft {
    @Shadow @Final private Window window;
    @Shadow public abstract <Mouse FQN from A.6> getMouse();

    @Inject(method = "<setScreen signature from A.7>", at = @At("RETURN"))
    private void stereoscopic$seedVirtualCursor(Screen screen, CallbackInfo ci) {
        if (screen == null || !StereoOptions.INSTANCE.mode.isActive()) return;
        ((MouseAccessor) (Object) getMouse())
            .stereoscopic$setPos(window.getWidth() / 4.0, window.getHeight() / 2.0);
    }
}
```

```java
// MouseAccessor.java — accessor pair for the Mouse position fields
@Mixin(<Mouse FQN from A.6>.class)
public interface MouseAccessor {
    @Accessor("<x field name from A.6>") void stereoscopic$setX(double v);
    @Accessor("<y field name from A.6>") void stereoscopic$setY(double v);

    default void stereoscopic$setPos(double x, double y) {
        stereoscopic$setX(x); stereoscopic$setY(y);
    }
}
```

Add `"minecraft.MouseAccessor"` to `stereoscopic.mixins.json`'s `client` list.

- [ ] **Step 4: Build + launch + test**

- `mode=SBS_HALF`. Look around in-game: pitch/yaw feel right (vertical normal; horizontal halved if X was 2× before).
- Open inventory: cursor appears at center of left half, not at window center. Hover items in both halves — tooltip hit-detect works (only one logical cursor position).
- HUD scissor: chat box, advancement popups, tooltip background boxes should clip to their eye half cleanly, no bleed across the midline.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Task 14: scissor intercept + mouse dx halving + virtual cursor seeding"
```

---

## Phase 5 — Sodium options page (Feature F)

### Task 15: Append `Stereoscopic` option group to Sodium video options

**Goal:** Sodium's video options page gets a new group with Mode / IPD / Debug-Eye controls bound directly to `StereoOptions` fields. Live-apply.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/sodium/MixinSodiumOptionsGUI.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/gui/StereoOptionsPage.java`

- [ ] **Step 1: Read the relevant `targets.md` sections**

Required rows: B.2, B.3, B.4, B.5. If any are blank, fix Task 2 first — this task is blocked otherwise.

- [ ] **Step 2: Read Sodium's own first-party option pages for reference**

Sodium ships several built-in option groups (Quality, Performance, Advanced). They live in the same jar — find the file that *registers* those groups (search the jar's class list for terms like `Pages`, `GeneralPage`, `SodiumOptionPages`, etc.). That file demonstrates the exact builder pattern v0.8.7 uses, including:

- How `OptionGroup` is constructed.
- How each option's binding is wired to a `SodiumGameOptions` (or new equivalent) field.
- How impact tags are applied.
- How `Text.translatable` is used (or not).

Save the path of that reference file to a comment at the top of `StereoOptionsPage.java`.

- [ ] **Step 3: Implement `StereoOptionsPage` by mimicking Sodium's reference page**

Translate Sodium's built-in pattern into our three controls. Mode + Debug-Eye are cycling enum controls; IPD is an integer slider (range 0..500, displayed as `0.000 m`..`0.500 m`, stored as `ipd * 1000` rounded to int). All bindings target `StereoOptions.INSTANCE` via whatever storage abstraction the targets.md row B.5 records.

**Behavior contract** the implementation must satisfy (this is what would have been in a code block, restated as a checklist):

- [ ] Mode setter writes `StereoOptions.INSTANCE.mode = v`.
- [ ] **When mode changes** (prev != new), call `PerEyeRenderTargetHooks.rebuildPipelineForStereoToggle()` after the field write. This is the only side-effect of the bindings.
- [ ] IPD setter clamps to `[0f, 0.5f]` and divides by 1000 (slider stores millimeters).
- [ ] Debug-Eye setter writes `StereoOptions.INSTANCE.debugForceEye = v` with no side effects.
- [ ] All three controls use `Text.translatable("stereoscopic.options.*")` keys per the lang JSON in Task 3.
- [ ] Three controls combine into one `OptionGroup` named via `stereoscopic.options.group.name`.
- [ ] `StereoOptionsPage.buildGroup()` returns that group as a single public entry point so the mixin in step 4 has one call to make.

If Sodium's API requires an `OptionStorage<StereoOptions>`, the implementation is a 2-method inner class:
- `getData()` returns `StereoOptions.INSTANCE`
- `save()` calls `StereoOptions.INSTANCE.save()`

- [ ] **Step 4: Mixin into Sodium's page builder to splice our group in**

Per targets.md B.2: identify the method that builds the page list. We `@Inject(at = @At("RETURN"))` with a `CallbackInfoReturnable<List<OptionPage>>`, walk the returned list to find the General/Video page, and append `StereoOptionsPage.buildGroup()` to it.

**Behavior contract:**

- [ ] Mixin targets the exact method from targets.md row B.2 with the bytecode descriptor recorded there.
- [ ] On RETURN: read the returned `List<OptionPage>` from the `CallbackInfoReturnable`, iterate, append our group to the first page whose translation key matches General or Video.
- [ ] If no matching page is found (Sodium reorganized pages in a future release): log a warning and append to `pages.get(0)`. Don't synthesize a new page in v0.1.0.

- [ ] **Step 5: Build + verify** — open Sodium's Video Options. The "Stereoscopic" group appears with three rows. Flip Mode → SBS_HALF; toggle triggers `rebuildPipelineForStereoToggle()` and stereo activates next frame.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Task 15: Stereoscopic option group inside Sodium video options (live-apply)"
```

---

### Task 16: Save piggy-back via `MixinSodiumGameOptions.writeChanges`

**Goal:** When Sodium's "Apply" / "Done" button writes its own options file, also call `StereoOptions.save()` so our JSON gets persisted on the same trigger.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/sodium/MixinSodiumGameOptions.java`

- [ ] **Step 1: Read `targets.md` row B.6**

Get the actual class FQN and the actual save-method's signature + descriptor. The historical name was `writeChanges(Path)`; if Sodium 0.8 renamed or restructured it, B.6 says so.

- [ ] **Step 2: Implement the mixin**

Shape (target string + method args replaced with the actual values from B.6):

```java
package com.mitchellmarx.stereoscopic.mixin.sodium;

import com.mitchellmarx.stereoscopic.Stereoscopic;
import com.mitchellmarx.stereoscopic.core.StereoOptions;
import <FQN from targets.md B.6>;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(<class from targets.md B.6>.class)
public abstract class MixinSodiumGameOptions {

    @Inject(method = "<method+descriptor from targets.md B.6>", at = @At("RETURN"))
    private void stereoscopic$alsoSaveStereo(<args matching descriptor>, CallbackInfo ci) {
        try { StereoOptions.INSTANCE.save(); }
        catch (Throwable t) { Stereoscopic.LOG.error("StereoOptions save failed", t); }
    }
}
```

**Behavior contract:** any call to Sodium's persist-to-disk method also flushes `StereoOptions.save()` afterward. We don't share Sodium's file; we just piggy-back on the trigger.

- [ ] **Step 2: Verify** — change IPD via Sodium, click "Done". Inspect `config/stereoscopic-options.json` — it should reflect the new IPD.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Task 16: piggy-back StereoOptions.save() on Sodium writeChanges"
```

---

## Phase 6 — Sodium chunk-update skip (Feature E)

### Task 17: Skip chunk uploads on the second eye

**Note (post-implementation):** the mixin shipped as `src/main/java/com/mitchellmarx/stereoscopic/mixin/sodium/MixinSodiumWorldRenderer.java` targeting `SodiumWorldRenderer#setupTerrain`, not `MixinRenderSectionManager` / `RenderSectionManager#update`. `setupTerrain` is the actual bundling entry point for the chunk-graph chain on Sodium 0.6, so cancelling it cleanly short-circuits visibility + upload in one shot. See commit `4d291f4` for the original rationale. The Step 1 helper (`SecondEyeSkipHooks`) shipped unchanged. Predicate semantics are pinned by `SecondEyeSkipHooksTest`.

**Goal:** On the RIGHT eye, cancel Sodium's chunk-visibility / upload pass — the LEFT eye already produced an identical result.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/sodium/MixinRenderSectionManager.java` *(actually shipped as `MixinSodiumWorldRenderer.java` — see note above)*
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/compat/sodium/SecondEyeSkipHooks.java`

- [ ] **Step 1: Implement the helper**

```java
// SecondEyeSkipHooks.java
package com.mitchellmarx.stereoscopic.compat.sodium;

import com.mitchellmarx.stereoscopic.core.StereoState;

public final class SecondEyeSkipHooks {
    private SecondEyeSkipHooks() {}

    public static boolean shouldSkipChunkUploadThisFrame() {
        return StereoState.INSTANCE.isActive()
            && StereoState.INSTANCE.getCurrentEye() == StereoState.Eye.RIGHT;
    }
}
```

- [ ] **Step 2: Implement the mixin against `targets.md` row B.1**

Reference: Angelica commit `e4b6900b` cancels chunk visibility/upload on the second eye in the equivalent 1.7.10 path (`RenderGlobal.clipRenderersByFrustum`'s call to `renderer.updateChunks`). On Sodium 0.8 the equivalent might be one method, might be split across `update(...)` + `uploadChunks(...)`. **targets.md row B.1 names the exact method to cancel.**

Shape (target string + args replaced with B.1's values):

```java
package com.mitchellmarx.stereoscopic.mixin.sodium;

import com.mitchellmarx.stereoscopic.compat.sodium.SecondEyeSkipHooks;
import <FQN from targets.md B.1>;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(<class from B.1>.class)
public abstract class MixinRenderSectionManager {

    @Inject(method = "<method+descriptor from B.1>", at = @At("HEAD"), cancellable = true)
    private void stereoscopic$skipSecondEye(<args matching descriptor>, CallbackInfo ci) {
        if (SecondEyeSkipHooks.shouldSkipChunkUploadThisFrame()) ci.cancel();
    }
}
```

**Behavior contract:** on the right-eye render pass, the named method short-circuits (`ci.cancel()`); on the left-eye pass it runs normally. If B.1 identified multiple candidate methods (e.g. visibility-update and upload are separate), inject the skip on **all** of them and document which.

- [ ] **Step 3: Verify with F3 debug overlay**

`mode=SBS_HALF`, fly to a chunk boundary, watch Sodium's per-frame chunk-update counter. Compared to stereo-off, the counter should roughly halve. Some frames may show parity because the right-eye skip applies only after the left already triggered work.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Task 17: skip Sodium chunk uploads on RIGHT eye (E)"
```

---

## Phase 7 — Iris shader pack support (Feature D)

> **Iris is optional.** Tasks 18–22 add the `stereoscopic-iris.mixins.json` mixin set. Every helper that *calls* into Iris classes must gate on `FabricLoader.getInstance().isModLoaded("iris")`. Mixin failures on missing Iris classes are silent because `required: false`.

### Task 18: Iris-loaded gating + helper scaffolding

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/compat/iris/PerEyeRenderTargetHooks.java`

- [ ] **Step 1: Implement**

```java
package com.mitchellmarx.stereoscopic.compat.iris;

import com.mitchellmarx.stereoscopic.Stereoscopic;
import com.mitchellmarx.stereoscopic.core.StereoState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class PerEyeRenderTargetHooks {
    private PerEyeRenderTargetHooks() {}

    public static final boolean IRIS_PRESENT =
        FabricLoader.getInstance().isModLoaded("iris");

    public static int wantedEyeCount() {
        if (!IRIS_PRESENT) return 1;
        return StereoState.INSTANCE.stereoEyeCount();
    }

    public static int currentEyeIndex() {
        return StereoState.INSTANCE.currentEyeIndex();
    }

    /**
     * Force-rebuild the Iris shader pipeline because {@code stereoEyeCount()}
     * just changed (called from the options-page mode binding). Matches the
     * Angelica reference's behavior in commit {@code ff9f23f3}: destroy +
     * prepare the pipeline, then reload the world renderer to drop stale
     * texture references.
     *
     * <p>No-op when Iris isn't loaded or when there's no active world (main
     * menu, server selection screen).
     */
    public static void rebuildPipelineForStereoToggle() {
        if (!IRIS_PRESENT) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        try {
            irisRebuildImpl();
            mc.worldRenderer.<methodFromTargetsA10>();  // A.10 — reload renderer
        } catch (Throwable t) {
            Stereoscopic.LOG.error("Iris pipeline rebuild on stereo toggle failed", t);
        }
    }

    /**
     * Implementation reads targets.md row C.1. The actual call is one of:
     *   - {@code Iris.reload()} if it exists in 1.10.7
     *   - else {@code Iris.destroyPipeline()} + {@code Iris.preparePipeline(<dim>)}
     * and {@code <dim>} is whatever C.1 records as the current-dimension fetch.
     *
     * <p>Iris classes are hard-imported here (not reflection) — the
     * {@link #IRIS_PRESENT} guard plus the {@code stereoscopic-iris.mixins.json}
     * isolation means this method is only called when Iris is loaded and its
     * classes are present on the classpath.
     */
    private static void irisRebuildImpl() {
        // Write the concrete Iris.reload() OR destroyPipeline()+preparePipeline(...)
        // sequence here based on C.1. Reference: Angelica `ff9f23f3` used
        // Iris.destroyPipeline() + Iris.preparePipeline(currentDimension) on a
        // much-older Iris fork; the 1.10 API may have collapsed both into
        // Iris.reload().
    }
}
```

> Task is blocked until targets.md row C.1 is filled in. Write the actual method body in Task 22 once we know the API; for Task 18 the method exists as a stub that throws a clear error if called prematurely.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "Task 18: Iris-presence gate + per-eye hook scaffolding"
```

---

### Task 19: `MixinRenderTargets` — per-eye colortex/depth bank

**Goal:** Allocate `colortex[N][2]` and `depthTex[2]` when `stereoEyeCount() == 2`. Index by `currentEyeIndex()` on bind. Resize both eyes together.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinRenderTargets.java`

- [ ] **Step 1: Read the 1.7.10 reference in Angelica**

```bash
git -C /c/code/Angelica show c28a73d7 -- '*RenderTargets*'
```

Capture the exact pattern of per-eye field duplication: how the constructor was modified, what fields were added (`eyeTargets[eyeCount][...]`, `eyeDepthTextures[eyeCount]`, `eyeCount` instance var, `ownsEyeDepthTextures` flag), and the `setActiveEye(int)` rebind method.

- [ ] **Step 2: Read targets.md row C.2 and study Iris 1.10's `RenderTargets` source**

Open the decompiled `RenderTargets` in IntelliJ. Identify:

- The field that holds the current color-target array (row C.2 should name it).
- The constructor's argument list — what triggers allocation, what sizes the targets.
- The resize entry point — single method or split? Does it allocate fresh or recycle?
- The framebuffer-binding read path — does Iris cache GL handles, or dereference the array on every bind?

The answers determine the *shape* of the mixin. There are two viable strategies:

**Strategy A — swap the underlying field.** If Iris dereferences its targets array on every bind, we maintain an `@Unique` per-eye double array and rewrite the shadowed field to point at eye N's bank before each eye's pass. Cheap.

**Strategy B — duplicate the GL handles inside each `RenderTarget`.** If Iris caches GL handles (per-target FBO IDs etc.), we hold one set of `RenderTarget` instances and mutate their GL handles per eye. More invasive.

Pick the strategy that matches what C.2 records.

- [ ] **Step 3: Implement the mixin**

**Behavior contract** (independent of strategy):

- [ ] On constructor RETURN, if `PerEyeRenderTargetHooks.wantedEyeCount() == 2`, allocate a sibling target bank (same color formats, same depth format, same dimensions as the existing bank).
- [ ] On resize RETURN, the sibling bank resizes in lockstep.
- [ ] On framebuffer bind HEAD, route the call to eye `PerEyeRenderTargetHooks.currentEyeIndex()`'s bank — either by swapping the shadowed field (Strategy A) or by rewriting GL handles (Strategy B). Track `lastBoundEye` so we don't redo no-op swaps every call.
- [ ] When `wantedEyeCount() == 1` (Iris loaded but stereo off), the mixin is a no-op past the eye-count check.

Mixin skeleton (target strings replaced with C.2's values):

```java
package com.mitchellmarx.stereoscopic.mixin.iris;

import com.mitchellmarx.stereoscopic.compat.iris.PerEyeRenderTargetHooks;
import <Iris RenderTarget FQN from C.2>;
import <Iris RenderTargets FQN from C.2>;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTargets.class, remap = false)
public abstract class MixinRenderTargets {
    // Fields and injects per Strategy A or B; targets per row C.2.
}
```

**Reference for the algorithm:** Angelica commit `c28a73d7` is the 1.7.10 port of this exact feature. Read the diff:

```bash
git -C /c/code/Angelica show c28a73d7 -- '*RenderTargets*'
```

Translate the field-by-field changes onto Iris 1.10's class shape from C.2. The *algorithm* (per-eye double array, resize lockstep, bind-time swap) is the same; only the field names and Java syntax change.

> **If the mixin grows beyond ~200 lines or requires more than two `@Inject` + accessor pairs, stop and reconsider.** The Angelica reference is ~150 lines and a viable target — exceeding that is a sign we've picked the wrong strategy.

- [ ] **Step 3: Build with Iris installed in test instance + Complementary Reimagined 5.6.1**

Expected: no asymmetric bloom or SSAO artifacts between eyes when looking at a scene with bright sky. (Without per-eye targets, kernel-based composite shaders sample left-eye data on the right eye and create a clear "left side correct, right side smeared" pattern.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Task 19: MixinRenderTargets per-eye colortex/depth bank"
```

---

### Task 20: `MixinCameraUniforms` — per-eye storage slots for current/previous camera position

**Goal:** `currentCameraPosition` is the player's actual world position (same for both eyes), but Iris's `previousCameraPosition` tracker stores it in a single slot — so the second eye's `update()` per frame overwrites the first eye's "previous" with the current value, breaking TAA / motion blur / motion vectors. Add per-eye storage slots indexed by `currentEyeIndex()`.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinCameraUniforms.java`

- [ ] **Step 1: Read the 1.7.10 reference**

```bash
git -C /c/code/Angelica show c28a73d7 -- '*CameraUniforms*'
```

Note the exact pattern: `currentCameraPositionPerEye[2]` + `previousCameraPositionPerEye[2]` Vector3d arrays, plus the `update()` rewrite.

- [ ] **Step 2: Read targets.md row C.3 and Iris's actual `update()` body**

Row C.3 names the inner class (was `CameraUniforms.CameraPositionTracker` in older Iris; verify). The body of its `update()` method matters because we're replacing it — we need to replicate Iris's current-camera-position computation exactly, just routed into per-eye storage slots.

Open `CameraUniforms.java` in Iris's decompiled jar. Find `update()`. Copy the body of the computation that produces today's `currentCameraPosition` (handles shift compensation for player teleports, world-origin recentering, partial-tick interpolation, whatever Iris does in 1.10).

- [ ] **Step 3: Implement**

**Behavior contract:**

- [ ] Two `@Unique Vector3d[2]` arrays: `prevPerEye` and `currPerEye`.
- [ ] `@Inject(method = "update()V", at = HEAD, cancellable = true)` — handles the per-eye case and cancels Iris's default.
- [ ] When `wantedEyeCount() == 1`: don't cancel; let Iris's default run (no per-eye tracking needed in mono).
- [ ] When `wantedEyeCount() == 2`:
    - Save `currPerEye[eye]` into `prevPerEye[eye]`.
    - Compute the new current position using Iris's own algorithm (copy from C.3).
    - Write it into `currPerEye[eye]`.
    - Write `prevPerEye[eye]` and `currPerEye[eye]` into whatever Iris-side state the uniform feeders read (likely the same fields the original `update()` was writing — confirm via C.3).
    - `ci.cancel()`.

Mixin skeleton (target string + body filled in from C.3):

```java
package com.mitchellmarx.stereoscopic.mixin.iris;

import com.mitchellmarx.stereoscopic.compat.iris.PerEyeRenderTargetHooks;
import <FQN from C.3>;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = <C.3 inner class>.class, remap = false)
public abstract class MixinCameraUniforms {
    @Unique private final Vector3d[] stereoscopic$prevPerEye = { new Vector3d(), new Vector3d() };
    @Unique private final Vector3d[] stereoscopic$currPerEye = { new Vector3d(), new Vector3d() };
    // @Inject body + the Iris position-computation copy per the contract above.
}
```

**Reference:** Angelica `c28a73d7` did this same restructuring on the 1.7.10 Iris fork:

```bash
git -C /c/code/Angelica show c28a73d7 -- '*CameraUniforms*'
```

Algorithm is identical; class shape may differ.

- [ ] **Step 3: Verify with a shader that uses motion vectors**

Complementary Reimagined 5.6.1 with TAA on: move/turn the camera and look for "comet-tail" smearing on the right eye that doesn't appear on the left. With this fix in place, both eyes should smear identically (or not at all).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Task 20: MixinCameraUniforms per-eye current/previous slots"
```

---

### Task 21: `MixinMatrixUniforms` + `MixinViewportUniforms` + `MixinIrisSamplers`

**Goal:** Confirm matrix flow already carries the eye offset; pin `viewWidth`/`viewHeight` uniforms to full-FB dims (NOT eye rect); per-eye depth sampler binding.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinMatrixUniforms.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinViewportUniforms.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinIrisSamplers.java`

- [ ] **Step 1: `MixinMatrixUniforms`** — keep empty.

The view-matrix translation we apply in `MixinGameRenderer` (Task 9) already carries the eye offset by the time matrices flow into Iris. This mixin file exists only for symmetry with the spec; if it's still a stub at v0.1.0 ship, delete it.

```java
@Mixin(value = <C.4 MatrixUniforms FQN>.class, remap = false)
public abstract class MixinMatrixUniforms {
    // Empty: view-matrix translation in MixinGameRenderer is upstream of Iris.
}
```

- [ ] **Step 2: `MixinViewportUniforms`** — pin `viewWidth`/`viewHeight` uniforms to full main-FB dims.

Read targets.md row C.4 for the method name and the local-variable ordinals where viewWidth/viewHeight are assigned. Iris's `ViewportUniforms` typically has a method that reads `MinecraftClient.getInstance().getWindow().getFramebufferWidth()/Height()` and feeds them to two uniforms — we want those reads to return the full FB dimensions even when `RenderSystem.viewport` is currently bound to a half-rect.

**Behavior contract:**

- [ ] When `StereoState.isActive()` is false: pass through unchanged.
- [ ] When active: replace the viewWidth source with `StereoState.INSTANCE.getFrameFbW()` and viewHeight with `getFrameFbH()`.

The exact injection mechanism (`@ModifyVariable`, `@ModifyArg`, `@Redirect` on a getter call) depends on what C.4 records — `@ModifyVariable` works if the values land in locals; `@Redirect` on `Window.getFramebufferWidth()` works if Iris calls Window directly.

- [ ] **Step 3: `MixinIrisSamplers`** — per-eye depth sampler binding.

Per targets.md row C.4 (IrisSamplers section): identify the method that binds depth samplers for the level pass. Before Iris reads the depth-texture handle, swap to the current eye's depth-texture handle (the per-eye depth bank from Task 19's MixinRenderTargets).

**Behavior contract:**

- [ ] On the named method's HEAD: route the depth-texture lookup to `MixinRenderTargets`' per-eye bank, indexed by `PerEyeRenderTargetHooks.currentEyeIndex()`.
- [ ] If Task 19 picked Strategy A (field swap), this is a no-op — the swap is already done before bind.
- [ ] If Task 19 picked Strategy B (handle rewrite), this is where we rewrite the depth handle for the current eye.

Skeleton:

```java
@Mixin(value = <C.4 IrisSamplers FQN>.class, remap = false)
public abstract class MixinIrisSamplers {
    @Inject(method = "<method+descriptor from C.4>", at = @At("HEAD"))
    private static void stereoscopic$bindPerEyeDepth(<args>, CallbackInfo ci) {
        // Per-eye depth swap; concrete code depends on Task 19's strategy.
    }
}
```

- [ ] **Step 4: Build + verify shader tile layouts stay intact**

Visit a scene with Complementary's bloom + reflections enabled. Check that screen-space effects don't show stretched/sheared sampling around tile boundaries.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Task 21: matrix/viewport/sampler uniform pins + per-eye sampler bind"
```

---

### Task 22: Hand renderer per-eye depth + shadow skip + final pass + composite + pipeline rebuild

**Goal:** Last cluster of Iris mixins. Each is small.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinHandRenderer.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinShadowRenderer.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinFinalPassRenderer.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinCompositeRenderer.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/iris/MixinIrisRenderingPipeline.java`

Also: this task implements `PerEyeRenderTargetHooks.irisRebuildImpl()` (the stub from Task 18) using the verified row C.1.

- [ ] **Step 1: `MixinHandRenderer`** — per-eye hand depth (algorithm port of Angelica `986f8629`)

Per targets.md row C.6: Iris's hand renderer maintains a dedicated depth texture for hand-occlusion testing. We need two copies indexed by eye.

**Behavior contract:**

- [ ] On the constructor RETURN (or on first bind, depending on what C.6 records as a clean injection point): if `wantedEyeCount() == 2`, allocate a sibling hand-depth texture cloned from Iris's. Track both in a `@Unique int[2]`.
- [ ] On the hand framebuffer bind method's HEAD: if per-eye textures allocated, swap the depth-texture handle Iris is about to bind to `perEye[currentEyeIndex()]`.
- [ ] If `wantedEyeCount() == 1`, mixin is a no-op.

**Reference:**

```bash
git -C /c/code/Angelica show 986f8629 -- '*Hand*'
```

The 1.7.10 Iris fork did the same field/method-name surgery; translate onto C.6's class shape.

- [ ] **Step 2: `MixinShadowRenderer`** — skip on RIGHT eye (port of Angelica `3d853ef5`)

Per targets.md row C.5: shadow-pass entry method.

**Behavior contract:**

- [ ] `@Inject(method = <C.5 entry>, at = HEAD, cancellable = true)`.
- [ ] If stereo is active AND current eye is RIGHT, `ci.cancel()`. Left-eye shadow output is still on the GPU and the right eye reuses it.
- [ ] Mono / left-eye pass: no-op.

Skeleton:

```java
@Mixin(value = <C.5 ShadowRenderer FQN>.class, remap = false)
public abstract class MixinShadowRenderer {
    @Inject(method = "<C.5 method+descriptor>", at = @At("HEAD"), cancellable = true)
    private void stereoscopic$skipSecondEyeShadowPass(<args>, CallbackInfo ci) {
        if (StereoState.INSTANCE.isActive()
                && StereoState.INSTANCE.getCurrentEye() == StereoState.Eye.RIGHT) {
            ci.cancel();
        }
    }
}
```

- [ ] **Step 3: `MixinFinalPassRenderer`** — confirm or override the final-blit destination

Per targets.md row C.7: the final-blit method.

**First, build with this mixin empty** and visually check whether the existing `RenderSystem.viewport` intercept (Task 11) already routes the final blit into the correct eye rect. If yes, leave this mixin empty.

If the blit goes to the wrong region (e.g. blits to full-screen and overwrites the other eye), then add an `@Inject` that sets the destination rect to the current eye's viewport explicitly using `StereoState.INSTANCE.getEyeVp{X,Y,W,H}()`. The exact mechanism depends on whether the final pass blits via `RenderSystem.viewport`+draw or via direct framebuffer-blit calls (the latter bypasses our intercept).

- [ ] **Step 4: `MixinCompositeRenderer`** — keep empty

The viewport intercept handles per-composite-pass remapping. If artifacts appear that suggest a specific composite pass isn't getting its viewport intercepted, debug there.

- [ ] **Step 5: `MixinIrisRenderingPipeline`** — keep empty

Pipeline rebuild is driven from Task 15's options-page binding via `rebuildPipelineForStereoToggle()`. This mixin file exists only for symmetry; delete it if still empty at v0.1.0 ship.

- [ ] **Step 6: Implement `PerEyeRenderTargetHooks.irisRebuildImpl()` (stub from Task 18)**

Now that targets.md row C.1 is filled in, write the real body. Two cases per C.1:

```java
// Case A: Iris.reload() exists in 1.10.x
private static void irisRebuildImpl() {
    net.irisshaders.iris.Iris.reload();  // exact class FQN per C.1
}

// Case B: only destroyPipeline + preparePipeline exist
private static void irisRebuildImpl() {
    net.irisshaders.iris.Iris.destroyPipeline();
    var dimId = /* fetch current dimension ID per C.1 */;
    net.irisshaders.iris.Iris.preparePipeline(dimId);
}
```

- [ ] **Step 6: Build + flip Mode on→off→on at runtime** via Sodium options. Expected: ~50–150ms hitch on toggle (Iris pipeline rebuild + Sodium reload), no crash, both eyes render correctly after each toggle.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Task 22: hand/shadow/final/composite/pipeline Iris mixins"
```

---

## Phase 8 — Async OS cursor (Feature G)

> Windows-only in v1. Mac/Linux silently fall back to `NoOpCursorBackend` (stereo still works; cursor is just frame-locked through the regular render path).

### Task 23: `CursorBackend` interface + `NoOpCursorBackend` fallback

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/cursor/CursorBackend.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/cursor/NoOpCursorBackend.java`

- [ ] **Step 1: Define the interface**

```java
package com.mitchellmarx.stereoscopic.cursor;

public interface CursorBackend {

    /** Returns BGRA pixels of the OS arrow cursor + hotspot, or null if unavailable. */
    Sprite captureArrowBitmap();

    /** Clip the OS cursor to MC's client rect (true) or release (false). */
    void trapCursor(boolean clip);

    /** Free any native resources. Called on mod shutdown. */
    void release();

    /** Indicate whether this backend is usable on the current platform. */
    boolean isSupported();

    final class Sprite {
        public final int width, height, hotspotX, hotspotY;
        public final byte[] bgra;  // length = width*height*4
        public Sprite(int w, int h, int hx, int hy, byte[] bgra) {
            this.width = w; this.height = h; this.hotspotX = hx; this.hotspotY = hy; this.bgra = bgra;
        }
    }
}
```

- [ ] **Step 2: `NoOpCursorBackend`**

```java
package com.mitchellmarx.stereoscopic.cursor;

public final class NoOpCursorBackend implements CursorBackend {
    @Override public Sprite captureArrowBitmap() { return null; }
    @Override public void trapCursor(boolean clip) {}
    @Override public void release() {}
    @Override public boolean isSupported() { return false; }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "Task 23: CursorBackend interface + NoOp fallback"
```

---

### Task 24: `WindowsCursorBackend` — sprite capture + ClipCursor

**Goal:** Capture the OS arrow at current DPI via `LoadCursorW(NULL, IDC_ARROW)` + `GetIconInfo` + `DrawIconEx` (drawing into an offscreen GDI bitmap, then reading pixels). Implement `ClipCursor` for the gameplay trap.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/cursor/WindowsCursorBackend.java`

- [ ] **Step 1: Confirm LWJGL3 Win32 bindings against targets.md section D**

LWJGL3 ships `org.lwjgl.system.windows.User32` / `GDI32` as part of `lwjgl-core`, which MC bundles. Targets.md rows D.1, D.2, D.3 record the **exact method names and struct accessor patterns** as they exist in the LWJGL3 version MC 1.21.11 ships. The names I use in the skeleton below are best-guesses; trust D.* over the skeleton.

The HWND comes from `org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(windowHandle)`.

- [ ] **Step 2: Implement against the verified D.* surface**

**Behavior contract for `captureArrowBitmap()`:**

- [ ] Call `User32.LoadCursor(NULL, IDC_ARROW=32512)` to get the standard system arrow HCURSOR. (D.1: confirm overload + argument types.)
- [ ] Call `User32.GetIconInfo(hCursor, ICONINFO)` to get hotspot offsets + bitmap handles. (D.1, D.3.)
- [ ] Use GDI32 (D.2) to draw the cursor into a 32-bit DIB and read BGRA pixels:
  1. `CreateCompatibleDC` (with screen DC).
  2. `CreateCompatibleBitmap` for a 32×32 (or DPI-scaled) BGRA buffer.
  3. `SelectObject` to bind the bitmap into the memory DC.
  4. `User32.DrawIconEx(hdc, 0, 0, hCursor, w, h, 0, NULL, DI_NORMAL=3)`.
  5. `GDI32.GetDIBits` to read the BGRA pixel buffer into a Java `byte[]`.
  6. Clean up: `DeleteObject(bitmap)`, `DeleteDC(hdc)`.
- [ ] Return a `Sprite(w, h, info.xHotspot(), info.yHotspot(), bgra)`. On any failure return null and log a warning.

**Behavior contract for `trapCursor(boolean clip)`:**

- [ ] When `clip == true`: GetClientRect → ClientToScreen the top-left and bottom-right → build a screen-space RECT → ClipCursor(rect).
- [ ] When `clip == false`: ClipCursor(null).
- [ ] Use LWJGL3 `MemoryStack` for the struct allocations (`RECT.calloc(stack)`, `POINT.calloc(stack)`); the stack frees on try-with-resources exit.

**Skeleton (method names per D.1/D.2/D.3 — names below are guesses):**

```java
package com.mitchellmarx.stereoscopic.cursor;

import com.mitchellmarx.stereoscopic.Stereoscopic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.*;  // exact import list per D.3

public final class WindowsCursorBackend implements CursorBackend {

    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase().contains("windows");
    private static final long IDC_ARROW = 32512L;

    private final long hwnd;
    public WindowsCursorBackend(long hwnd) { this.hwnd = hwnd; }
    @Override public boolean isSupported() { return IS_WINDOWS && hwnd != 0L; }

    @Override public Sprite captureArrowBitmap() {
        if (!isSupported()) return null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Sequence per the contract above; method names per D.1/D.2/D.3.
            // Reference: Angelica CursorPresentThread.captureCursorTexture
            // does the same GDI dance via reflection on LWJGL3 internals.
            return /* built sprite */;
        } catch (Throwable t) {
            Stereoscopic.LOG.warn("Win32 cursor capture failed", t);
            return null;
        }
    }

    @Override public void trapCursor(boolean clip) {
        if (!isSupported()) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // GetClientRect + ClientToScreen + ClipCursor per the contract.
        } catch (Throwable t) {
            Stereoscopic.LOG.warn("ClipCursor failed", t);
        }
    }

    @Override public void release() { trapCursor(false); }
}
```

> Don't use `glfwSetInputMode(GLFW_CURSOR_HIDDEN)` — per the spec, that broke GUI mouse input on the Angelica reference, and we have no reason to think Fabric 1.21 is friendlier.

- [ ] **Step 3: Resolve the MC window's HWND from GLFW**

```java
// In Stereoscopic.onInitializeClient (deferred until window is created):
long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(windowHandle);
```

Hook this from `MixinMinecraft` once `setWindow` has completed (HEAD inject is too early).

- [ ] **Step 4: Smoke-test capture**

Print captured size + hotspot to log:
```
[Stereoscopic] Captured OS arrow: 32x32 hotspot=(0,0)
```

(Sizes vary by Windows DPI — at 200% scale you'll see 64×64.)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Task 24: WindowsCursorBackend — sprite capture + ClipCursor via LWJGL3 User32/GDI32"
```

---

### Task 25: `CursorPresentThread` — shared GLFW context + triple-buffer scaffolding

**Goal:** A worker thread holding a shared GLFW context that can render into MC's window FB. Triple-buffer publish/consume pattern (matches Angelica commit `ed1fea00`).

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/cursor/CursorPresentThread.java`

- [ ] **Step 1: Create the hidden shared-context window**

```java
package com.mitchellmarx.stereoscopic.cursor;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32;

import java.util.concurrent.atomic.AtomicReference;

public final class CursorPresentThread {

    public static final CursorPresentThread INSTANCE = new CursorPresentThread();

    private long mainWindow;
    private long workerWindow;
    private Thread workerThread;
    private volatile boolean running;

    private final int[] tex = new int[3];
    private volatile int mainWriteIdx = 0;
    private volatile int cursorReadIdx = 1;

    /** Atomically-swapped slot: contains the texture index + a fence on the producer's copy. */
    private record Slot(int texIdx, long fenceHandle) {}
    private final AtomicReference<Slot> pending = new AtomicReference<>(new Slot(2, 0L));

    private CursorPresentThread() {}

    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase().contains("windows");

    public void ensureStarted(long mainWindow) {
        if (running) return;
        if (!IS_WINDOWS) return; // v1 Windows-only
        this.mainWindow = mainWindow;
        if (!createSharedContext()) return;
        running = true;
        workerThread = new Thread(this::run, "Stereoscopic-Cursor-Present");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void stop() {
        running = false;
        if (workerThread != null) {
            try { workerThread.join(2000); } catch (InterruptedException ignored) {}
        }
        if (workerWindow != 0L) {
            GLFW.glfwDestroyWindow(workerWindow);
            workerWindow = 0L;
        }
    }

    private boolean createSharedContext() {
        try {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            workerWindow = GLFW.glfwCreateWindow(1, 1, "", 0L, mainWindow);
            return workerWindow != 0L;
        } catch (Throwable t) {
            com.mitchellmarx.stereoscopic.Stereoscopic.LOG.warn("Shared-context creation failed", t);
            return false;
        }
    }

    private void run() {
        GLFW.glfwMakeContextCurrent(workerWindow);
        GL.createCapabilities();
        // Allocate 3 textures sized to the main FB (resize on size change).
        // Loop:
        //   - Try to claim the pending slot via CAS.
        //   - Wait the producer's fence (clientWaitSync with timeout).
        //   - glBindTexture(tex[cursorReadIdx]); draw fullscreen quad.
        //   - If cursor sprite available: draw it at the eye-mapped positions.
        //   - glfwSwapBuffers(workerWindow) — NOT mainWindow.
        //   - Wait briefly (e.g. 16ms) or block on a condvar.
        //
        // Implementation is mechanical given the structure above; ~150 lines.
    }

    /** Called by the main thread to publish the current main FB into the next slot. */
    public void publishFrame() {
        // 1. Copy default FB into tex[mainWriteIdx] via glCopyImageSubData (or fallback FBO blit).
        // 2. Insert glFenceSync.
        // 3. Atomically pending = new Slot(mainWriteIdx, fence); recover old slot's idx
        //    (delete old fence if non-zero). Rotate mainWriteIdx.
    }
}
```

> The triple-buffer dance follows the Angelica reference exactly. Re-read `CursorPresentThread.java` in Angelica when implementing (sections covered in this conversation's inventory report).

- [ ] **Step 2: Commit (scaffold only; full run-loop in next task)**

```bash
git add -A
git commit -m "Task 25: CursorPresentThread scaffold + shared context creation"
```

---

### Task 26: `CursorPresentThread` — present loop + publish

**Goal:** Flesh out `run()` and `publishFrame()` end-to-end. Visible OS arrow in both eye halves.

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/cursor/CursorPresentThread.java`

- [ ] **Step 1: Implement `publishFrame()`** — copy current default-FB color attachment into `tex[mainWriteIdx]`, fence it, atomically swap into `pending`.

- [ ] **Step 2: Implement the worker `run()` loop** — claim `pending` via CAS, wait fence, bind the now-published texture, draw fullscreen quad, draw the cursor sprite at both eye-mapped positions:

  - LEFT eye:  `(mx/2, my)` in the left-eye viewport space.
  - RIGHT eye: `(mx/2 + w/2, my)` in the right-eye viewport space.

- [ ] **Step 3: Wire `publishFrame()` into the main render path**

The cleanest hook is at the end of `GameRenderer.render` (TAIL inject in `MixinGameRenderer`), **before** `endFrame()` so the StereoState snapshot is still valid:

```java
@Inject(method = "render(...)V", at = @At("RETURN"))
private void stereoscopic$publishCursorFrame(...) {
    if (CursorPresentThread.INSTANCE.isRunning()) {
        CursorPresentThread.INSTANCE.publishFrame();
    }
}
```

Order matters: the TAIL inject we added in Task 8 calls `endFrame()`. Either:
- Put this publish inject *before* the endFrame inject (use `@Inject` ordering — Mixin runs in declaration order within the same `@At` per default), OR
- Move `endFrame()` to `@At("TAIL")` and publish to `@At("RETURN")`, OR
- Call publish from inside the same method as endFrame() in the right order.

Pick the simplest. Declaration order works; document the dependence in a code comment.

- [ ] **Step 4: Build + verify**

`mode=SBS_HALF`, open inventory, move the cursor. Expected:
- The OS arrow renders in both halves, in sync with `MouseHandler.x/y`.
- When MC drops to ~30fps, cursor stays smooth (it runs at the present thread's rate, independent of main render).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Task 26: CursorPresentThread present loop + triple-buffer publish"
```

---

### Task 27: `StereoCursor` facade + lifecycle wiring + cursor trap

**Files:**
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/cursor/StereoCursor.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/Stereoscopic.java`
- Modify: `src/main/java/com/mitchellmarx/stereoscopic/mixin/minecraft/MixinMinecraft.java`

- [ ] **Step 1: Implement `StereoCursor`**

```java
package com.mitchellmarx.stereoscopic.cursor;

import com.mitchellmarx.stereoscopic.core.StereoState;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFWNativeWin32;

public final class StereoCursor {
    public static final StereoCursor INSTANCE = new StereoCursor();
    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase().contains("windows");

    private CursorBackend backend;
    private boolean trapped;

    public CursorBackend backend() {
        if (backend == null) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            if (IS_WINDOWS) {
                long hwnd = GLFWNativeWin32.glfwGetWin32Window(handle);
                backend = new WindowsCursorBackend(hwnd);
            } else {
                backend = new NoOpCursorBackend();
            }
        }
        return backend;
    }

    /** Driven from StereoState.beginFrame() each frame. */
    public void tick() {
        if (!StereoState.INSTANCE.getFrameMode().isActive()) {
            if (CursorPresentThread.INSTANCE.isRunning()) {
                CursorPresentThread.INSTANCE.stop();
                trap(false);
            }
            return;
        }
        if (!CursorPresentThread.INSTANCE.isRunning()) {
            CursorPresentThread.INSTANCE.ensureStarted(MinecraftClient.getInstance().getWindow().getHandle());
        }
        // Apply trap based on screen state + focus.
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean want = mc.currentScreen == null && mc.isWindowFocused();
        trap(want);
    }

    public void trap(boolean want) {
        if (want == trapped) return;
        backend().trapCursor(want);
        trapped = want;
    }

    public void shutdown() {
        CursorPresentThread.INSTANCE.stop();
        if (backend != null) backend.release();
    }
}
```

- [ ] **Step 2: Call `tick()` from `StereoState.beginFrame`**

Modify `StereoState.beginFrame` to call `StereoCursor.INSTANCE.tick()` at the end of its body (after the snapshot fields are set).

> This couples `core/` to `cursor/` — acceptable because both are top-level packages and the design spec already says the cursor lifecycle is driven from `beginFrame`. Alternatively the call can live in the GameRenderer mixin, but tying it to beginFrame keeps the contract local.

- [ ] **Step 3: Shutdown on game exit**

```java
// Stereoscopic.onInitializeClient:
ClientLifecycleEvents.CLIENT_STOPPING.register(c -> StereoCursor.INSTANCE.shutdown());
```

(Add `fabric-lifecycle-events-v1` to Fabric API deps if not already pulled in.)

- [ ] **Step 4: Verify**

- Activate stereo → CursorPresentThread starts → cursor is trapped during gameplay → unconfined when inventory opens or window loses focus → released on stereo off → released on game exit.
- Quit MC → no thread leak in JVM (verify with `jps` / Task Manager).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Task 27: StereoCursor facade + lifecycle wiring + trap behavior"
```

---

## Phase 9 — Final manual verification

### Task 28: Full test-checklist run + version tag

**Files:**
- Modify: `gradle.properties` (`mod_version=0.1.0` — already set; bump to `0.1.0-rc1` then `0.1.0` after passing checklist)
- Create: `docs/superpowers/notes/v0.1.0-manual-test.md`

- [ ] **Step 1: Run the manual test matrix from the spec**

In the test instance (with Iris + Complementary Reimagined 5.6.1 + Sodium installed):

| ID | Check | Result |
|---|---|---|
| A | `debugForceEye=LEFT` → world shifts right; `RIGHT` → shifts left | ☐ pass / ☐ fail |
| A | Toggle stereo on → two halves visible with parallax | ☐ |
| B | HUD elements (hotbar, health, chat) visible in both halves | ☐ |
| B | Inventory + tooltips render in both halves; tooltip hit-test uses one logical cursor | ☐ |
| D | With Complementary 5.6.1: no asymmetric bloom/SSAO between eyes | ☐ |
| D | Per-eye RenderTargets: framebuffer dump shows distinct textures | ☐ |
| E | Sodium F3 chunk-update counter ~halves vs stereo-off | ☐ |
| E | F3 shader stats show one shadow pass per frame | ☐ |
| F | Sodium video settings → Mode OFF→SBS_HALF mid-game: no restart, no crash, targets reallocate | ☐ |
| G | OS arrow visible in both eye halves | ☐ |
| G | Cursor stays smooth at 30fps main render | ☐ |
| G | Cursor trapped during gameplay; released in inventory / on focus loss | ☐ |
| G | Quitting MC cleans up thread; no zombie process | ☐ |

Record results in `docs/superpowers/notes/v0.1.0-manual-test.md`. Note exact MC + Sodium + Iris versions tested.

- [ ] **Step 2: Bump to `0.1.0`, rebuild**

```
mod_version=0.1.0
```

- [ ] **Step 3: Tag**

```bash
./gradlew build
git add -A
git commit -m "Task 28: v0.1.0 — manual test checklist passed"
git tag -a v0.1.0 -m "Stereoscopic 0.1.0: SBS_HALF + Sodium + Iris + Windows OS cursor"
```

---

## Self-review checklist

Before declaring the plan done, the engineer running it should verify all of the below:

- [ ] **Spec coverage:**
  - Feature A (two-pass world + camera offset) → Tasks 8–11.
  - Feature B (HUD + screen duplication + scissor + mouse) → Tasks 12–14.
  - Feature D (Iris per-eye RenderTargets + uniforms + hand + shadow + final/composite + pipeline rebuild) → Tasks 18–22.
  - Feature E (Sodium chunk-update skip, Iris shadow skip) → Tasks 17, 22.
  - Feature F (in-game toggle in Sodium options page) → Tasks 15–16.
  - Feature G (async cursor) → Tasks 23–27.
  - Non-goal H (HUDCaching) → not implemented; verify nothing in the plan adds it.

- [ ] **No placeholders:** Every "TBD" / "fill in later" / "similar to" in this plan that referenced future tasks has either been re-stated inline at point of use or marked with an explicit cross-reference to the Angelica commit it ports.

- [ ] **Sign conventions:**
  - `getEyeOffset()`: LEFT = `+ipd/2`, RIGHT = `-ipd/2` — Task 5 test pins this.
  - Cursor X position formula: LEFT = `(mx/2, my)`, RIGHT = `(mx/2 + w/2, my)` — Task 26.
  - Scissor remap: SBS_HALF halves both x and w, leaves y/h alone — Task 7 test pins this.

- [ ] **Type consistency:**
  - `StereoState.Eye { LEFT, RIGHT, MONO }` is the single enum used by every consumer.
  - `currentEyeIndex()` returns 0/1 (LEFT/MONO → 0; RIGHT → 1) — every Iris mixin uses this index.
  - `stereoEyeCount()` reads `StereoOptions.INSTANCE.mode` directly, not the cached `active` flag — Task 5 test pins this.
  - `PerEyeRenderer.viewportRaw` is the only API that may bypass the viewport intercept.

- [ ] **Tasks 2 & 19–22 mappings:** The plan acknowledges that target class names for Sodium 0.8.7 / Iris 1.10.7 may differ from the 1.7.10 reference. Task 2 produces a verification report; every dependent task instructs the engineer to consult it.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-13-stereoscopic-fabric-1.21-implementation.md`. Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach?
