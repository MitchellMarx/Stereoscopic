# Stereoscopic — Fabric 1.21.11 stereo SBS rendering

**Date:** 2026-05-13
**Status:** Draft (pre-implementation)
**Repository:** `C:\code\Stereoscopic`
**Reference implementation:** `C:\code\Angelica`, branch `stereo-sbs` (Forge 1.7.10)

## Overview

`Stereoscopic` is a client-side Fabric mod for Minecraft 1.21.11 that renders the game in side-by-side stereoscopic 3D for VR virtual-monitor viewers (Quest / Index / Vive via Virtual Desktop or Steam Link) and 3D-capable displays. It is a port of the stereo SBS implementation already shipping on the Angelica 1.7.10 branch, restructured for Fabric, the modern Minecraft rendering pipeline, and standalone distribution alongside Sodium and Iris.

Only the `SBS_HALF` layout is implemented in v1: each eye is rendered into half the screen width at the monoscopic aspect ratio, with the world horizontally compressed to fit. This is the layout supported by all major VR SBS-3D viewers.

## Goals

- **A.** Two-pass world rendering with a per-eye camera offset, hooked into the 1.21 `GameRenderer.render` path.
- **B.** Per-eye HUD and screen rendering with scissor remap so a single logical cursor position drives both eyes correctly.
- **D.** Iris shader-pack support via per-eye `RenderTargets`, per-eye camera/viewport uniforms, hand-renderer per-eye depth, and shadow-pass skip on the second eye.
- **E.** Sodium perf optimizations: skip chunk uploads on the second eye; skip Iris shadow pass on the second eye.
- **F.** In-game toggle inside Sodium's video options page; no restart required.
- **G.** Async OS-arrow cursor rendered into each eye half on a separate GL context (Windows only in v1; pluggable backend interface for later macOS/Linux backends).

## Non-Goals

- **H.** HUDCaching parity — the 1.7.10 HUDCaching feature is not being ported.
- Stereo layouts other than `SBS_HALF` — `SBS_FULL`, `OU_HALF`, `OU_FULL` are out of scope. The enum is open to extension if needed later, but no code paths are written for them.
- macOS / Linux cursor backend — interface exists, only Windows is implemented.
- Server-side anything — mod is strictly `environment: client`.
- Mod-compat work beyond Sodium + Iris.

## Architecture

### Mod identity

- Display name: `Stereoscopic`
- Mod ID: `stereoscopic`
- Root Java package: `com.mitchellmarx.stereoscopic`
- License: `LGPL-3.0-only` (matches Angelica)

### Dependency stance

- **Sodium 0.8.7** is a hard dependency. Stereo rendering is built on top of Sodium's world renderer; without Sodium present the mod fails to load.
- **Iris 1.10.7** is optional (`recommends` in `fabric.mod.json`). Iris-coupled mixins live in a separate mixin config with `"required": false`; missing Iris silently drops the shader-pack feature group (D) and leaves the rest of the mod functional.
- **Fabric Loader 0.19.0** minimum; **Minecraft 1.21.11**; **Java 21**.

### Source tree

```
src/main/
  java/com/mitchellmarx/stereoscopic/
    Stereoscopic.java                      (ClientModInitializer entrypoint)
    core/
      StereoMode.java                      (OFF, SBS_HALF)
      StereoDebugEye.java                  (OFF, LEFT, RIGHT)
      StereoState.java                     (per-frame singleton)
      StereoOptions.java                   (live runtime options + persistence)
    render/
      PerEyeRenderer.java                  (eye-iteration helper)
      ViewportMath.java                    (eye rect math)
      ScissorRemap.java                    (full-screen → eye remap)
    cursor/
      CursorBackend.java                   (interface)
      WindowsCursorBackend.java            (only impl in v1)
      NoOpCursorBackend.java               (Mac/Linux fallback)
      CursorPresentThread.java             (async present worker)
      StereoCursor.java                    (main-thread facade)
    gui/
      StereoOptionsPage.java               (Sodium options page binding)
    compat/
      sodium/SecondEyeSkipHooks.java
      iris/PerEyeRenderTargetHooks.java
    mixin/
      minecraft/                           MixinGameRenderer, MixinLevelRenderer,
                                           MixinGui, MixinGuiGraphics,
                                           MixinRenderSystem, MixinMouseHandler,
                                           MixinMinecraft
      sodium/                              MixinRenderSectionManager,
                                           MixinSodiumOptionsGUI,
                                           MixinSodiumGameOptions
      iris/                                MixinRenderTargets,
                                           MixinIrisRenderingPipeline,
                                           MixinHandRenderer,
                                           MixinShadowRenderer,
                                           MixinCompositeRenderer,
                                           MixinFinalPassRenderer,
                                           MixinCameraUniforms,
                                           MixinMatrixUniforms,
                                           MixinViewportUniforms,
                                           MixinIrisSamplers
  resources/
    fabric.mod.json
    stereoscopic.mixins.json               (vanilla + Sodium; required=true)
    stereoscopic-iris.mixins.json          (Iris; required=false)
    assets/stereoscopic/...
```

### Mixin configs

Two mixin config files isolate optional Iris hooks from the rest:

- `stereoscopic.mixins.json` — vanilla MC mixins + Sodium mixins. `injectors.defaultRequire = 1`. Failures here are fatal.
- `stereoscopic-iris.mixins.json` — Iris mixins only. `"required": false`. If Iris is missing or its internal class layout has changed, these mixins skip silently and feature D becomes a no-op.

### Per-frame runtime flow (stereo active, `SBS_HALF`)

```
GameRenderer.render() [hooked]
  └─ StereoState.beginFrame()                  // snapshot config
     setEye(LEFT)
       viewport = (0, 0, w/2, h)               // left half of main FB
       view matrix · translation(+ipd/2, 0, 0) // push eye offset
       LevelRenderer.renderLevel(...)          // full world pass; Iris/Sodium run
     setEye(RIGHT)
       viewport = (w/2, 0, w/2, h)
       view matrix · translation(-ipd/2, 0, 0)
       LevelRenderer.renderLevel(...)
     HUD pass (always duplicated)
       setEye(LEFT)   → viewport left,  gui.render(...)
       setEye(RIGHT)  → viewport right, gui.render(...)
     Screen pass (if a Screen is open)
       setEye(LEFT)   → viewport left,  screen.renderWithTooltip(...)
       setEye(RIGHT)  → viewport right, screen.renderWithTooltip(...)
     StereoState.endFrame()
  └─ window.swapBuffers()

Async cursor thread (separate context):
  shared GLFW context bound to main window
  loops at native input rate:
    read latest (x,y) from triple-buffer
    draw captured OS arrow sprite in both eye halves
    glFlush; coordinate with main thread for present
```

When `mode == OFF` all hooks short-circuit and rendering is monoscopic.

## Core state and options

### Enums

- `StereoMode { OFF, SBS_HALF }`. `isActive()`, `isSideBySide()`, `isHalf()` helpers. Open to extension without changing call sites.
- `StereoDebugEye { OFF, LEFT, RIGHT }`. Forces a one-eye monoscopic render so per-eye camera offset and per-eye RenderTargets can be validated without a headset.
- (No `StereoHudMode` — HUD is always duplicated when stereo is active.)

### `StereoState` (singleton)

Same shape as the 1.7.10 reference. Fields and methods:

- `Eye { LEFT, RIGHT, MONO }` inner enum.
- `beginFrame()` / `endFrame()` / `setEye(Eye)`.
- `inWorldPass` and `inGuiPass` boolean flags, plus `eyeVpX, eyeVpY, eyeVpW, eyeVpH` — the current eye's viewport rect, consulted by the viewport and scissor intercept mixins.
- Frame-cached `frameMode` and `frameIpd` snapshot at `beginFrame()` so mid-frame config flips can't cause inconsistency.
- `getEyeOffset()` returns `+ipd/2` for LEFT and `-ipd/2` for RIGHT. **Signs match 1.7.10.** The vanilla anaglyph code uses the opposite signs, which produces eye-swapped stereo to a VR viewer. See `StereoState.getEyeOffset()` JavaDoc in the 1.7.10 reference for the full derivation.
- `currentEyeIndex()` returns 0 for LEFT/MONO, 1 for RIGHT — used for indexing per-eye arrays (Iris RenderTargets, hand depth).
- `stereoEyeCount()` returns 1 or 2. **Reads `StereoOptions` directly, not the cached `active` flag**, because Iris pipeline init can fire before the first `beginFrame()`; reading the cached flag at that point would allocate mono targets that contaminate the first stereo frame until a manual shader reload.
- `endFrame()` clears `active` and resets `currentEye = MONO`, but **does not clear** `frameMode` / `frameIpd` — they must remain readable for any post-frame callbacks. `beginFrame()` overwrites them next frame.

### `StereoOptions` (live runtime options)

In-memory singleton with three fields:

- `mode: StereoMode = OFF`
- `ipd: float = 0.064` (clamped to 0.0..0.5)
- `debugForceEye: StereoDebugEye = OFF`

Read by `StereoState.beginFrame()`. Written by the Sodium options page (Section "Sodium integration"). Live-apply — changing a value affects the next frame's render with no Apply button gating from our side.

### Persistence

- Serialized to `config/stereoscopic-options.json` using Minecraft's bundled Gson. No extra dependency.
- **Save trigger piggy-backs on Sodium**: `MixinSodiumGameOptions` injects into Sodium's `writeChanges()` (or 0.8 equivalent) to also flush `StereoOptions.save()`. The file is ours, not Sodium's — we never mutate Sodium's schema.
- Load happens once on `ClientModInitializer.onInitializeClient`.
- The JSON file is implementation detail. The only intended UI is the Sodium options page; no human-editable config-file workflow is documented.

## Render pipeline integration

### Hook surface

- `MixinGameRenderer`:
  - `@Inject(HEAD)` `render(DeltaTracker, boolean)` → `StereoState.beginFrame()`.
  - `@Inject(TAIL)` `render(...)` → `StereoState.endFrame()`.
  - `@WrapOperation` around the `renderLevel(...)` call → `PerEyeRenderer.runForEachEye(...)`.
  - `@WrapOperation` around the `gui.render(...)` call → `PerEyeRenderer.runForEachEye(...)` (always duplicated; no mode branching).
  - `@WrapOperation` around the `screen.renderWithTooltip(...)` call → `PerEyeRenderer.runForEachEye(...)`.
  - `@ModifyArg` on the view matrix passed into `LevelRenderer.renderLevel` → call `viewMatrix.translate(getEyeOffset(), 0, 0)` (JOML post-multiply: `view_new = view · T`). This is the matrix-stack equivalent of the 1.7.10 `glTranslatef(dx, 0, 0)` applied after the camera matrix was loaded. We use **parallel-axis** stereo (modelview-only offset, no projection offset) — same convention as the 1.7.10 reference, avoids the asymmetric-frustum gap between eyes.

### `PerEyeRenderer.runForEachEye(Runnable body)`

Shared helper used by all three `@WrapOperation` hooks:

```
if (!StereoState.active) { body.run(); return; }
for (Eye eye in [LEFT, RIGHT]):
  StereoState.setEye(eye)
  (x, y, w, h) = ViewportMath.eyeRect(eye, mainFb.width, mainFb.height)
  PerEyeRenderer.viewportRaw(x, y, w, h)        // bypasses intercept
  StereoState.enterWorldPass(x, y, w, h)        // or enterGuiPass — caller picks
  body.run()
  StereoState.exitWorldPass()                    // / exitGuiPass
PerEyeRenderer.viewportRaw(0, 0, mainFb.width, mainFb.height)
```

### Camera offset

Applied as a view-matrix translation. The translation goes on the **view matrix**, not the projection — same convention as 1.7.10. The camera *position* itself is not modified, only the matrix, so culling, fog, and frustum stay stable across eyes.

### Viewport intercept (`MixinRenderSystem.viewport`)

When `StereoState.inWorldPass` is true and the caller is setting the viewport to the full main framebuffer size, remap to the current eye rect. This catches Iris's mid-pipeline viewport changes (composite passes, deferred passes, `Pass.use()`) automatically. `PerEyeRenderer.viewportRaw(...)` bypasses the intercept when we intentionally set the per-eye viewport.

### Scissor intercept (`MixinRenderSystem.enableScissor`)

When `StereoState.inGuiPass` is true, remap caller scissor coords from full-screen GUI space to eye viewport coords:
- SBS_HALF horizontal compression: `x_remap = x/2 + eyeVpX`, `w_remap = w/2`. Y and H unchanged.

Direct port of the 1.7.10 `glScissor` intercept.

## HUD, screens, mouse

### HUD render

`gui.render(graphics, deltaTracker)` is always run twice when stereo is active — once per eye, inside `PerEyeRenderer.runForEachEye`. No mode branching; the HUD is always duplicated.

### Screens

`screen.renderWithTooltip(graphics, mouseX, mouseY, partialTick)` runs twice. Mouse coords passed unchanged — the screen draws as if it occupies the full screen, the scissor intercept clips it to the eye viewport. Tooltip hit-testing uses one logical cursor position across both eyes.

### GuiGraphics state reset between eye passes

After each eye's `gui.render` / `screen.render` call, `PerEyeRenderer` calls `graphics.flush()` and resets the pose stack so left-eye state doesn't contaminate right-eye render. This is the 1.21 analogue of the 1.7.10 "reset GUI lighting state between eye drawScreen calls" — different mechanism (no fixed-function lighting in Core profile), same problem class.

### Mouse delta halving

`MixinMouseHandler` on the player-turn / cursor-move path:
- When `mode == SBS_HALF`: horizontal delta `dx *= 0.5` before vanilla applies it. Vertical `dy` untouched.
- Reason: each eye is horizontally compressed 2×, so OS-reported mouse pixels travel "twice as fast" relative to what the user sees. Halving X restores the felt sensitivity.

### Virtual cursor seeding on GUI open

When `Minecraft.setScreen(non-null)` fires while stereo is active, seed `MouseHandler.xpos / ypos` to the center of the left-eye viewport rect, not the absolute window center. Without this, the cursor lands offscreen on first open. Port of 1.7.10 commit `344ff560`.

### Fabric event duplication — implicit

Fabric API hooks its render events (`HudRenderCallback`, `WorldRenderEvents.*`) by mixin into the same `Gui.render` / `LevelRenderer.renderLevel` methods we wrap. Running each method twice fires the callback twice. Overlay mods get per-eye rendering for free; no additional code on our side.

## Iris integration (optional)

All Iris-coupled mixins live in `stereoscopic-iris.mixins.json` with `"required": false`. The `compat/iris/*` helpers gate all calls behind `FabricLoader.isModLoaded("iris")` so they become no-ops when Iris is absent. Missing Iris = feature D off, other features unaffected.

### Per-eye `RenderTargets`

`MixinRenderTargets`:
- On allocation: if `StereoState.stereoEyeCount() == 2`, allocate `colortex[N][2]` and `depthTex[2]` instead of `colortex[N]` and `depthTex`.
- On bind: index by `StereoState.currentEyeIndex()`.
- On resize: resize both eye banks.

Without this duplication, a kernel-based composite shader (bloom, SSAO, temporal effects) running on the right eye samples pixels from the left eye and produces the asymmetric per-eye artifact pattern documented in `project_stereo_temporal_shaders`.

### Per-eye uniforms

- `MixinCameraUniforms` — `cameraPosition` value itself is **not** offset by `getEyeOffset()` (the view matrix already carries the offset; cameraPosition stays at the player's actual world position, same for both eyes). What we do add is **per-eye storage slots** for `currentCameraPosition` and `previousCameraPosition`, indexed by `currentEyeIndex()`. Without per-eye slots, the right eye's `update()` would overwrite `previousCameraPosition` with the left eye's same-frame current position — shaders would then see zero motion between previous and current on the right eye, desyncing every temporal effect (TAA, motion blur, motion vectors). Port of the 1.7.10 `CameraUniforms` diff.
- `MixinMatrixUniforms` — `modelViewMatrix` and `gbufferModelView` already include the eye offset from our view-matrix translation in `MixinGameRenderer`; no additional math, but an `@Inject` assertion confirms the matrix flowing through Iris matches what we set.
- `MixinViewportUniforms` — `viewWidth` / `viewHeight` return the full main-FB dimensions, **not** the eye rect. This preserves the monoscopic aspect ratio inside Iris's tile-layout math (Complementary's `colortexN` tile layout uses `max(vec2(viewWidth, viewHeight) / vec2(1920, 1080), 1.0)` — a non-square ratio causes tile reads to clamp past the texture edge and produce dark patchwork artifacts).

### Hand renderer per-eye depth

`MixinHandRenderer`: hand depth gets its own pair of textures, indexed by `currentEyeIndex()`. Port of 1.7.10 commit `986f8629`.

### Shadow pass skip on second eye

`MixinShadowRenderer`: short-circuit when `StereoState.getCurrentEye() == RIGHT`. Shadow map is view-independent — the left-eye pass already produced it.

### Final pass + composite

- `MixinFinalPassRenderer`: when drawing the final result back to the main framebuffer, blit each eye's `colortex0` into its eye-viewport rect.
- `MixinCompositeRenderer`: viewport intercept already handles per-composite-pass viewport remap. No additional work beyond confirming the per-eye RenderTargets are bound when each pass runs.

### Pipeline rebuild on stereo toggle

When `mode` flips `OFF ↔ SBS_HALF`, `stereoEyeCount()` changes. Next time Iris's `RenderTargets` queries it, sized targets will mismatch. We trip Iris's internal resize-on-mismatch path to force reallocation. No full pipeline rebuild needed.

## Sodium integration

Sodium 0.8.7 is a hard dependency. Sodium mixins live in `stereoscopic.mixins.json` with default `required = true`.

### Skip chunk uploads on the second eye

`MixinRenderSectionManager` (or 0.8 equivalent — exact method name verified during scaffolding):
- `@Inject(HEAD)` on `updateChunks(...)` → `if (StereoState.currentEye() == RIGHT) ci.cancel()`.
- Chunks that become visible only on the right eye won't upload until next frame's left eye. Negligible — IPD is ~0.064 m, sub-block.

### Add stereo controls to Sodium's options page

`MixinSodiumOptionsGUI` (or the 0.8 `VideoOptionsScreen` / `OptionGroup` API equivalent): append a "Stereoscopic" option group with three controls:

- **Mode** — cycle: OFF / SBS_HALF
- **IPD** — slider: 0.0..0.5, step 0.001, default 0.064
- **Debug force eye** — cycle: OFF / LEFT / RIGHT

Controls bind directly to `StereoOptions` fields. Live-apply: changes affect next frame.

`MixinSodiumGameOptions` injects into `writeChanges()` to also call `StereoOptions.save()` alongside Sodium's save.

## Async OS cursor

A real OS arrow cursor is rendered into both eye halves at the native mouse-polling rate (~1 kHz), independent of the main render rate. Direct functional port of the 1.7.10 cursor system, restructured behind a backend interface.

### Module layout (`cursor/`)

- `CursorBackend` — interface. Methods: `captureArrowBitmap()` returning BGRA pixels of the OS arrow at current DPI, `trapCursor(boolean clip)`, `release()`.
- `WindowsCursorBackend` — only impl in v1. Uses `LoadCursorW(NULL, IDC_ARROW)` + `GetIconInfo` + `DrawIconEx` for sprite capture, `ClipCursor()` for the trap. Per the `project_mc_cursor_static` memory note, `LoadCursorW(IDC_ARROW)` is the correct call (not `GetCursor()`, which returns whatever cursor MC has set mid-frame).
- `NoOpCursorBackend` — fallback for non-Windows hosts. Cursor feature silently disabled; stereo still works.
- `CursorPresentThread` — the worker. Owns a hidden GLFW window with a shared GL context bound to MC's main window (created via `glfwCreateWindow(1, 1, "", NULL, mainWindowHandle)`). Loops: read latest `(x, y)` from a triple-buffered slot, draw the cursor sprite at the eye-mapped positions, `glFlush`, hand to main thread for present. Triple-buffered handoff matches 1.7.10 commit `ed1fea00`.
- `StereoCursor` — main-thread facade. `start()` / `stop()` driven by `StereoState.beginFrame()`. `poll(x, y)` for the main thread to publish the user's logical cursor position. `trap(enabled)` for gameplay vs GUI mode.

### Sprite position math

Mouse logical position `(mx, my)` is in full-screen GUI coordinates. The cursor sprite is drawn at:
- Left eye: `(mx/2, my)` in left-eye viewport space.
- Right eye: `(mx/2 + w/2, my)` in right-eye viewport space.

Same horizontal compression rule as scissor remap, per the `project_stereo_sbs_x_compression` memory note: sprites drawn at full-screen coords need half X width and half X anchor because SBS only compresses horizontally.

### Cursor trap during gameplay

When stereo is active AND no `Screen` is open AND the window has focus: `WindowsCursorBackend.trapCursor(true)` → `ClipCursor(clientRect)`. The cursor cannot escape the game window. Port of 1.7.10 commit `894fa1a5`.

When a screen opens, the window loses focus, or stereo deactivates: `trapCursor(false)`.

**We do not use `glfwSetInputMode(GLFW_CURSOR_DISABLED)`** — that hides the OS cursor and breaks raw input, per the `project_mc_cursor_static` memory note.

### Lifecycle

- `StereoState.beginFrame()` with `mode != OFF` → `CursorPresentThread.ensureStarted()` (cheap if already running).
- `StereoState.beginFrame()` with `mode == OFF` → `CursorPresentThread.stop()`.
- Mod shutdown → `CursorPresentThread.stop()` + `WindowsCursorBackend.release()`.

## Build, distribution, testing

### Gradle / Loom setup

- Loom targeting Fabric 1.21.11 (Loom version pinned after scaffolding verifies compat).
- `gradle.properties`:
  - `minecraft_version=1.21.11`
  - `loader_version=0.19.0`
  - `yarn_mappings=…` (latest yarn for 1.21.11)
  - `fabric_version=…` (Fabric API version for 1.21.11)
  - `sodium_version=0.8.7`
  - `iris_version=1.10.7`
- Source set: single `src/main`. No `client`-only split (whole mod is client-side, declared via `environment: client` in `fabric.mod.json`).

### `fabric.mod.json` essentials

```jsonc
{
  "schemaVersion": 1,
  "id": "stereoscopic",
  "version": "${version}",
  "name": "Stereoscopic",
  "description": "Side-by-side stereoscopic rendering for VR virtual monitors and 3D displays.",
  "license": "LGPL-3.0-only",
  "environment": "client",
  "entrypoints": { "client": ["com.mitchellmarx.stereoscopic.Stereoscopic"] },
  "mixins": [
    "stereoscopic.mixins.json",
    { "config": "stereoscopic-iris.mixins.json", "environment": "client" }
  ],
  "depends": {
    "fabricloader": ">=0.19.0",
    "minecraft": "~1.21.11",
    "java": ">=21",
    "sodium": "0.8.7"
  },
  "recommends": { "iris": "1.10.7" },
  "breaks": { "iris": "<1.10.7 || >=1.11.0" }
}
```

### Deploy-to-test-instance hook

Custom Gradle task `copyToTestInstance` depends on `remapJar`:

- Copies the built jar to `C:\Users\felix\AppData\Roaming\ModrinthApp\profiles\Fall 2025 Let_s Play\mods\`.
- Wired so `./gradlew build` triggers it.
- Deletes any pre-existing `stereoscopic-*.jar` from the target folder first to avoid running two builds simultaneously.

### Testing strategy — manual visual verification

Stereo rendering can't really be unit-tested. The plan is a checklist run by hand in the test instance:

| Feature | Verification |
|---|---|
| **A** Two-pass world + camera offset | `debugForceEye = LEFT` → world appears shifted right; `RIGHT` → shifted left. Toggle stereo on → see two halves with appropriate parallax. |
| **B** HUD + screen duplication | HUD elements (hotbar, health, chat) visible in both halves. Open inventory → screen renders correctly in both halves; tooltip hit-test uses one logical cursor. |
| **D** Iris shaderpack | With Complementary Reimagined 5.6.1: no asymmetric bloom/SSAO artifacts; per-eye RenderTargets confirmed by checking framebuffer dump. |
| **E** Sodium chunk-update skip | Stereo on vs stereo off in identical scene: chunk-upload counter (Sodium F3 debug overlay) shows ~half the rate per frame. |
| **E** Iris shadow-pass skip | F3 shader stats show one shadow pass per frame regardless of mode. |
| **F** Dynamic toggle | Open Sodium video settings during gameplay, flip Mode OFF→SBS_HALF: stereo activates on next frame, no restart, no crash, RenderTargets reallocate. |
| **G** Async cursor | OS arrow visible in both eye halves; cursor stays smooth when main render drops to 30fps; cursor trapped to window during gameplay. |

The `debugForceEye` knob is the primary correctness tool — lets us validate camera math and per-eye target allocation without putting on a headset.

### What's not tested in v1

- macOS / Linux runtime — backend stubs only.
- Servers — mod is `environment: client`, server runs unaffected.
- Mod compat beyond Sodium + Iris.

### Versioning

Start at `0.1.0`. Mark `experimental` in description until cross-feature stability is confirmed in the test instance.

## Open questions and risks

- **Sodium 0.8.7 / Iris 1.10.7 internal class layout.** This spec uses 1.7.10 fork class names (`RenderTargets`, `SodiumGameOptions`, `RenderSectionManager`, `IrisRenderingPipeline`, etc.). Sodium 0.6+ and Iris 1.8+ may have renamed or restructured these. Exact target-class verification happens during scaffolding (Plan step 1).
- **1.21 `RenderPipeline` interaction.** 1.21.5+ introduced declarative `RenderPipeline` objects. Vanilla world rendering still funnels through `LevelRenderer.renderLevel`, so the two-pass approach holds, but mid-pipeline GL state assumptions need verifying against the new pipeline.
- **Cursor thread + GLFW swap-buffer race.** The 1.7.10 implementation manages cursor-thread / main-thread swap ordering on a per-OS basis. On 1.21 we use GLFW everywhere, which simplifies but doesn't eliminate the coordination problem. The implementation plan should call out the swap-synchronization choice explicitly.
- **Yarn vs Mojmap mappings.** This spec assumes yarn class names because that's standard for Fabric mods. If we switch to Mojmap (intermediary-stable), mixin `@At` targets need updating but no other architectural change.
