# targets.md — Stereoscopic v0.1.0 API surface

Resolved on 2026-05-16 against the versions in `versions.md`.

This file is the single source of truth for every yarn / Sodium / Iris / LWJGL
class, method, field, and bytecode descriptor that the Stereoscopic mixins
target. Tasks 8–27 read this file and **do not invent** signatures; if a row
says `MISSING — replacement plan: …`, the task should adopt the plan rather
than guess.

## How rows were verified

Two parallel inputs were used:

1. **`javap -p -s -v`** dumps of the yarn-remapped Loom artifact (for MC and the
   built-in `com.mojang.blaze3d.*` / `org.lwjgl.system.windows.*` classes).
2. **Upstream Java source** via `curl https://raw.githubusercontent.com/…` for
   Sodium 0.8.11, Iris 1.10.x (1.21.11 branch), and LWJGL 3.3.3.

When the two disagreed (e.g. Iris's branch sources use Mojang names like
`Minecraft` / `LevelRenderer` while runtime targets the yarn names
`MinecraftClient` / `WorldRenderer`), the **yarn name from the jar** is
authoritative for mixin `@At` targets; the Iris source body is documented for
behavioural reference.

## Caveats baked into this file

- **Iris 1.10.7 has no git tag.** Iris's release pipeline cuts binaries from
  branches named after MC versions, not from per-patch tags. The 1.21.11 branch
  currently declares `MOD_VERSION = "1.10.6"` in `build.gradle.kts` (HEAD as of
  resolution date). The 1.10.7 Modrinth jar was built from a not-explicitly-
  identified commit on this same branch a small number of commits ahead. The
  public API surface (`Iris.reload`, `PipelineManager.destroyPipeline`,
  `RenderTargets`, `CameraUniforms`, etc.) is stable across the 1.10.x patch
  range; any rows below sourced from the branch HEAD therefore apply equally to
  1.10.7. Where a method-body verbatim quote is given (C.3), it is the body
  on the branch at resolution time.
- **MC yarn-remapped jar paths use Loom's `minecraftMaven` cache.** Loom 1.16
  does not produce a single classical "named jar" under
  `caches/fabric-loom/<version>/` — the canonical remapped jar lives under
  `caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<ver>-<mappings>/`
  and that is what's recorded here.
- **LWJGL 3.3.3 is the version Loom resolved** (transitively, via MC's library
  list). Records below reflect that exact build.

## Resolved jar paths

| Jar | Path on disk |
| --- | --- |
| MC yarn-remapped (merged client+server, 1.21.11) | `C:\Users\felix\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.5-v2\minecraft-merged-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.5-v2.jar` |
| Yarn mappings (1.21.11+build.5) | `C:\Users\felix\.gradle\caches\fabric-loom\1.21.11\net.fabricmc.yarn.1_21_11.1.21.11+build.5-v2\mappings.jar` |
| Fabric Loader 0.19.2 | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\net.fabricmc\fabric-loader\0.19.2\cc647a5b22dbc49b9a3267cebcee25fc4882b108\fabric-loader-0.19.2.jar` |
| Fabric API 0.141.4+1.21.11 | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-api\0.141.4+1.21.11\13d3885dfec40313e2b3bf9ee639353272b9b48a\fabric-api-0.141.4+1.21.11.jar` |
| Sodium 0.8.11 (Fabric, 1.21.11) | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\maven.modrinth\sodium\mc1.21.11-0.8.11-fabric\3735e0249b11b4df88aa79e08311c475025c0c13\sodium-mc1.21.11-0.8.11-fabric.jar` |
| Iris 1.10.7 (Fabric, 1.21.11) | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\maven.modrinth\iris\1.10.7+1.21.11-fabric\aae8567bd9ea397d50aff1d0b680a82ffe67040c\iris-1.10.7+1.21.11-fabric.jar` |
| LWJGL 3.3.3 core | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\org.lwjgl\lwjgl\3.3.3\29589b5f87ed335a6c7e7ee6a5775f81f97ecb84\lwjgl-3.3.3.jar` |
| LWJGL 3.3.3 windows-x64 natives | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\org.lwjgl\lwjgl\3.3.3\a5ed18a2b82fc91b81f40d717cb1f64c9dcb0540\lwjgl-3.3.3-natives-windows.jar` |
| MixinExtras 0.5.4 (Fabric) | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\io.github.llamalad7\mixinextras-fabric\0.5.4\5e167154bcb9942111313a5e186e16cd7265f858\mixinextras-fabric-0.5.4.jar` |
| Sponge Mixin 0.17.2+mixin.0.8.7 | `C:\Users\felix\.gradle\caches\modules-2\files-2.1\net.fabricmc\sponge-mixin\0.17.2+mixin.0.8.7\edf98d1d98229e46e36c61774ae2b54dcd852981\sponge-mixin-0.17.2+mixin.0.8.7.jar` |

## Source references used for non-MC rows

- Sodium 0.8.11 at tag `mc1.21.11-0.8.11`:
  `https://github.com/CaffeineMC/sodium/tree/mc1.21.11-0.8.11`
- Iris 1.10.x at branch `1.21.11` (HEAD ~ 1.10.6; 1.10.7 jar was built from a
  later commit on this branch):
  `https://github.com/IrisShaders/Iris/tree/1.21.11`
- LWJGL 3.3.3 at tag `3.3.3`:
  `https://github.com/LWJGL/lwjgl3/tree/3.3.3/modules/lwjgl/core/src/generated/java/org/lwjgl/system/windows`

---

## Section A — Minecraft (yarn)

### A.1 GameRenderer.render (frame entry)

- **Class:** `net.minecraft.client.render.GameRenderer`
- **Method:** `public void render(net.minecraft.client.render.RenderTickCounter tickCounter, boolean tick)`
- **Bytecode descriptor:** `(Lnet/minecraft/client/render/RenderTickCounter;Z)V`
- **Notes:**
  - This is the `@Inject(HEAD)` / `@Inject(RETURN)` target for begin/endFrame.
  - There is also a sibling `public void renderWorld(net.minecraft.client.render.RenderTickCounter)` desc `(Lnet/minecraft/client/render/RenderTickCounter;)V` — in 1.21 the GameRenderer dispatches into `renderWorld` for the world pass, which in turn calls `WorldRenderer.render` (see A.2). Either method is a valid HEAD/RETURN injection point for "before/after the world is drawn", depending on whether the mod wants to wrap the world-only sub-pass or the whole frame.
  - **Cross-confirmation:** Iris's `MixinGameRenderer.iris$startFrame` targets `render(DeltaTracker, boolean)` (Mojang names) — same method, same descriptor under yarn (`RenderTickCounter` = yarn for `DeltaTracker`).

### A.2 WorldRenderer.render (world pass; the two-pass stereo target)

- **Class:** `net.minecraft.client.render.WorldRenderer`
  - **Note:** In 1.21 yarn this class is `WorldRenderer`, not `LevelRenderer` (Mojang). Iris mixins-as-Mojang name it `LevelRenderer`; the runtime mixin will compile against the yarn name `WorldRenderer`.
- **Method:** `public void render(net.minecraft.client.util.ObjectAllocator allocator, net.minecraft.client.render.RenderTickCounter tickCounter, boolean renderBlockOutline, net.minecraft.client.render.Camera camera, org.joml.Matrix4f modelView, org.joml.Matrix4f projection, org.joml.Matrix4f matrix4f3, com.mojang.blaze3d.buffers.GpuBufferSlice fog, org.joml.Vector4f fogColor, boolean renderHand)`
- **Bytecode descriptor:** `(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V`
- **Argument list (0-based, Java args; bytecode local slot for non-static method = arg+1):**
  - arg 0 (slot 1): `ObjectAllocator allocator`
  - arg 1 (slot 2): `RenderTickCounter tickCounter`
  - arg 2 (slot 3): `boolean renderBlockOutline`
  - arg 3 (slot 4): `Camera camera`
  - arg 4 (slot 5): **`Matrix4f modelView`** ← view-matrix arg for Task 9 `@ModifyArg`
  - arg 5 (slot 6): `Matrix4f projection`
  - arg 6 (slot 7): `Matrix4f matrix4f3` (third matrix; identified by Iris source only as `matrix4f3`, role not externally named — likely a per-frame position/local matrix used in `setupFrustum`)
  - arg 7 (slot 8 — `category=2`, so 2 slots): `GpuBufferSlice fog`
  - arg 8: `Vector4f fogColor`
  - arg 9: `boolean renderHand`
- **View-matrix argument index:** **4** (the first `Matrix4f`).
- **Notes:**
  - `@WrapOperation` target for Task 10's two-pass world render.
  - `@ModifyArg(method = "render(L…)V", at = @At(value="INVOKE", target="…"), index = 4)` for Task 9's view-matrix translation, with the wrapped INVOKE being the GameRenderer→WorldRenderer dispatch.
  - **Cross-confirmation:** Iris `MixinLevelRenderer.iris$setupPipeline` parameter list reads `(GraphicsResourceAllocator, DeltaTracker, boolean, Camera, Matrix4f modelView, Matrix4f projection, Matrix4f matrix4f3, GpuBufferSlice, Vector4f, boolean)` — the Iris developers explicitly named arg 4 `modelView` and arg 5 `projection`. Yarn arg names will follow the same semantics.
  - Inside the method body the private helper `setupFrustum(Matrix4f, Matrix4f, Vec3d) -> Frustum` is invoked with bytecode locals 5 and 7 (i.e. `modelView` and `matrix4f3`, NOT `projection`). Confirmed by `javap -v` of the merged jar.

### A.3 RenderSystem.viewport / RenderSystem.enableScissor

- **`RenderSystem.viewport(IIII)V` does NOT exist in 1.21.11.** Confirmed by `javap -p -s` of `com/mojang/blaze3d/systems/RenderSystem.class` in the merged jar — there is no `viewport` method. The 1.21 RenderSystem only exposes `enableScissorForRenderTypeDraws(IIII)` and `disableScissorForRenderTypeDraws()`. All viewport state changes go through `GlStateManager` directly.
- **`RenderSystem.enableScissorForRenderTypeDraws`:**
  - Class: `com.mojang.blaze3d.systems.RenderSystem`
  - Signature: `public static void enableScissorForRenderTypeDraws(int x, int y, int width, int height)`
  - Descriptor: `(IIII)V`
- **`RenderSystem.disableScissorForRenderTypeDraws`:**
  - Signature: `public static void disableScissorForRenderTypeDraws()`
  - Descriptor: `()V`
- **GlStateManager underlying calls (for direct GL state when bypassing the RenderSystem wrappers):**
  - Class: `com.mojang.blaze3d.opengl.GlStateManager` (note: yarn moved this from the older `com.mojang.blaze3d.platform` package in earlier MC versions).
  - `public static void _viewport(int x, int y, int width, int height)` desc `(IIII)V`
  - `public static void _scissorBox(int x, int y, int width, int height)` desc `(IIII)V`
  - `public static void _enableScissorTest()` desc `()V`
  - `public static void _disableScissorTest()` desc `()V`
- **Notes:** Per-eye viewport setting in Task 11 will call `GlStateManager._viewport(...)` directly (no `RenderSystem.viewport` to mixin in front of). For HUD/screen-pass clipping the higher-level `RenderSystem.enableScissorForRenderTypeDraws(...)` is the right wrapper.

### A.4 InGameHud.render

- **Class:** `net.minecraft.client.gui.hud.InGameHud`
- **Method:** `public void render(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter)`
- **Bytecode descriptor:** `(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V`
- **Notes:** `@WrapOperation` target for Task 12 HUD duplication. The single-arg variant has been removed in 1.21.

### A.5 Screen render method invoked from GameRenderer.render

- **Class:** `net.minecraft.client.gui.screen.Screen`
- **Method:** `public void renderWithTooltip(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float deltaTicks)`
- **Bytecode descriptor:** `(Lnet/minecraft/client/gui/DrawContext;IIF)V`
- **Notes:**
  - This is the method `GameRenderer.render` calls (verified in the constant pool of `GameRenderer.class`: entry `#1157 = Methodref … Screen.renderWithTooltip:(Lnet/minecraft/client/gui/DrawContext;IIF)V`, invoked at bytecode offset `627` inside `GameRenderer.render`).
  - This is NOT the method Screens themselves override — overriding subclasses generally override `Screen.render(DrawContext, int, int, float)`. `renderWithTooltip` calls `Screen.render(...)` internally and then draws tooltips.
  - `@WrapOperation` target for Task 13 (screen duplication for SBS).

### A.6 Mouse — cursor delta path + position fields

- **Class:** `net.minecraft.client.Mouse`

- **Cursor-callback method (the one GLFW calls):**
  - `private void onCursorPos(long window, double x, double y)` desc `(JDD)V`
  - Body computes `cursorDeltaX += x - this.x; cursorDeltaY += y - this.y; this.x = x; this.y = y;` (when window focused).
  - LocalVariableTable: slot 1 = `window` (long, 2 slots → next free slot is 3), slot 3 = `x` (double, 2 slots → 5), slot 5 = `y` (double).
  - **NOTE: this method has no intermediate dx local — the cursor delta is computed and stored as a field, not held in a local.** Sensitivity multiplication happens later in `updateMouse`.

- **Delta-handling / sensitivity-application method (called from `tick()` once per game tick):**
  - `private void updateMouse(double timeDelta)` desc `(D)V`
  - Body reads `cursorDeltaX` / `cursorDeltaY` fields, multiplies by smoothed sensitivity, stores final `dx` in slot 3 (`double i`) and final `dy` in slot 5 (`double j`), then passes both to `ClientPlayerEntity.changeLookDirection(dx, dy)` desc `(DD)V`. The applied X-rotation is `±i` and Y-rotation is `±j` depending on invertMouseX/invertMouseY options.
  - LocalVariableTable (verified via `javap -v`):
    - slot 0: `this`
    - slot 1: `timeDelta` (double)
    - slot 3: `i` (double) — **the final dx after sensitivity & smoothing**, just before being fed to `changeLookDirection`
    - slot 5: `j` (double) — the final dy, same point
    - slot 7: `d` (double, intermediate sensitivity factor)
    - slot 9: `e` (double, intermediate)
    - slot 11: `f` (double, intermediate)
    - slot 13: `g` (double) and slot 15: `h` (double) — scratch when smooth-camera path is taken

- **Local-variable ordinal for the `dx` double:** the local in `updateMouse` is at slot **3** (`@ModifyVariable(method = "updateMouse(D)V", at = @At(...), ordinal = 0)` with `index = 3` — or use named MixinExtras `@Local(ordinal = 0)` on a double parameter; ordinal-0 double maps to slot 3 since slot 1 is the already-counted `timeDelta` param).
- **NOTE for Task 16 (if mouse-input mirroring needs raw OS dx):** there is no raw-dx-as-local — the OS dx is *only* visible as the difference `x - this.x` inside `onCursorPos`. To inject before sensitivity application but after raw OS delta you must either `@Redirect`/`@WrapMethod` the field-store of `cursorDeltaX` in `onCursorPos` or wrap the `ClientPlayerEntity.changeLookDirection` INVOKE in `updateMouse`.

- **Position fields (GUI-space cursor):**
  - `private double x;` (the live GUI-space cursor x, kept in sync with GLFW)
  - `private double y;` (likewise)
  - Both `private` (non-final). Use `@Accessor` to read/write.

- **Delta fields:**
  - `private double cursorDeltaX;`
  - `private double cursorDeltaY;`

- **Access strategy:** `@Accessor` mixin interface on `Mouse` for `x`/`y`/`cursorDeltaX`/`cursorDeltaY` (all private, non-final → need both `@Accessor` getter and `@Mutable @Accessor` setter if Task 16 writes them).

### A.7 MinecraftClient.setScreen

- **Class:** `net.minecraft.client.MinecraftClient`
- **Method:** `public void setScreen(net.minecraft.client.gui.screen.Screen screen)`
- **Bytecode descriptor:** `(Lnet/minecraft/client/gui/screen/Screen;)V`

### A.8 Window framebuffer dimensions

- **Class:** `net.minecraft.client.util.Window`
- **Methods:**
  - `public int getFramebufferWidth()` desc `()I`
  - `public int getFramebufferHeight()` desc `()I`
- **Also useful:**
  - `public int getWidth()` desc `()I` (logical/window width)
  - `public int getHeight()` desc `()I`
  - `public int getScaledWidth()` desc `()I` (GUI-space, post-scaleFactor)
  - `public int getScaledHeight()` desc `()I`
  - `public int getScaleFactor()` desc `()I`

### A.9 GLFW window handle from MinecraftClient

- **Method chain:** `MinecraftClient.getInstance().getWindow().getHandle()` — all three confirmed by javap on the respective classes:
  - `MinecraftClient.getInstance()` desc `()Lnet/minecraft/client/MinecraftClient;` (static)
  - `MinecraftClient.getWindow()` desc `()Lnet/minecraft/client/util/Window;`
  - `Window.getHandle()` desc `()J` (returns the GLFW window pointer as a Java `long`)

### A.10 WorldRenderer reload (Sodium-mediated chunk-graph wipe)

- **Class:** `net.minecraft.client.render.WorldRenderer`
- **Method (no-arg):** `public void reload()` desc `()V` — the 1.21 equivalent of 1.7.10's `RenderGlobal.loadRenderers()`. Vanilla calls this when render-distance, dimension, or graphics-mode changes.
- **Method (with ResourceManager):** `public void reload(net.minecraft.resource.ResourceManager resourceManager)` desc `(Lnet/minecraft/resource/ResourceManager;)V` — called from resource-reload flows.
- **Sodium-aware path:** Sodium intercepts `WorldRenderer.reload()` via its own mixin and routes the actual chunk-graph rebuild through `SodiumWorldRenderer.reload()` (see Section B). For `PerEyeRenderTargetHooks.rebuildPipelineForStereoToggle`, calling the no-arg `reload()` on the world-renderer instance is the correct entry point; with Sodium present it transparently invokes Sodium's rebuild.

---

## Section B — Sodium 0.8.x

Sodium 0.8 has a **fully redesigned public configuration API** that obsoletes
the 0.5.x / 0.6.x technique of mixin-injecting into private list builders. We
should use the public API; the implementation classes below are recorded only
for completeness / fallback.

### B.1 Chunk-upload entry point

- **Public class:** `net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager`
- **Per-frame upload method:** `public void uploadChunks()` desc `()V`
  (file: `common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager.java`, line 368)
- **Per-frame "scan + decide what to build" method:**
  `public void update(net.minecraft.client.render.Camera camera, net.caffeinemc.mods.sodium.client.render.viewport.Viewport viewport, net.caffeinemc.mods.sodium.client.util.FogParameters fogParameters, boolean spectator)`
- **Per-frame mesh-build dispatch:** `public void updateChunks(boolean updateImmediately)` desc `(Z)V`
- **Call-site that drives all of the above (the actual @Inject target for skipping a second-eye chunk pass):**
  - Class: `net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer`
  - Method: `public void setupTerrain(net.minecraft.client.render.Camera camera, net.caffeinemc.mods.sodium.client.render.viewport.Viewport viewport, net.caffeinemc.mods.sodium.client.util.FogParameters fogParameters, boolean spectator, boolean updateChunksImmediately, net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices matrices)`
  - This method internally calls (in order) `processChunkEvents()` → `renderSectionManager.prepareFrame` → `renderSectionManager.update` → `renderSectionManager.cleanupAndFlip` → `renderSectionManager.updateChunks` → `renderSectionManager.uploadChunks` → `renderSectionManager.finalizeRenderLists` → `renderSectionManager.tickVisibleRenders`.
  - **Recommended Task target:** `@Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true)` on `SodiumWorldRenderer`, cancel when the current pass is the second eye.
- **Drawing entry per layer (separate from upload, called during the actual draw):**
  - `public void drawChunkLayer(net.minecraft.client.render.chunk.ChunkSectionLayerGroup group, net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices matrices, double x, double y, double z, com.mojang.blaze3d.textures.GpuSampler terrainSampler)` on `SodiumWorldRenderer`.
- **Helper record:** `public record ChunkRenderMatrices(org.joml.Matrix4fc projection, org.joml.Matrix4fc modelView) {}` — Sodium passes BOTH matrices around as a single value, which means stereo-view-matrix injection has to construct a new `ChunkRenderMatrices` per eye rather than mutate one component.

### B.2–B.5 Options pages — **use the public API**

Sodium 0.8 exposes a stable public API under `net.caffeinemc.mods.sodium.api.config.*`. Stereoscopic should integrate via this API, not by mixing into Sodium's internal `OptionPage`/`OptionGroup` classes. The API entry point is registered via a Fabric mod-loader entrypoint:

- **fabric.mod.json entry to add:**
  ```json
  "entrypoints": {
    "sodium:config_api_user": [
      "com.stereoscopic.config.SodiumOptionsHook"
    ]
  }
  ```

- **Entry-point interface:**
  - Class: `net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint`
  - Methods to implement:
    - `default void registerConfigEarly(net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder builder) {}`
    - `void registerConfigLate(net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder builder)` ← we implement this one

- **B.2 / B.3 Page list builder (ConfigBuilder):**
  - Class: `net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder`
  - Key methods we'll use:
    - `net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder registerOwnModOptions()` — registers a header in the page list for *our* mod (id resolved from the entry-point's owning mod).
    - `net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder createOptionPage()`
    - `net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder createOptionGroup()`
    - `net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder createBooleanOption(net.minecraft.util.Identifier id)`
    - `net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder createIntegerOption(net.minecraft.util.Identifier id)`
    - `<E extends Enum<E>> net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder<E> createEnumOption(net.minecraft.util.Identifier id, Class<E> enumClass)`
  - `ModOptionsBuilder` chain: `addPage(OptionPageBuilder)`, `setIcon(Identifier)`, `setColorTheme(...)`.

- **B.3 OptionPageBuilder:**
  - Class: `net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder` (interface)
  - Methods:
    - `OptionPageBuilder setName(net.minecraft.network.chat.Component name)` — **NB:** the API uses Mojang-mapped `Component`, not yarn `Text`. At runtime under yarn these are the same class via the access-widener pattern; in source we'll see Mojang names.
    - `OptionPageBuilder addOptionGroup(OptionGroupBuilder group)`
    - `OptionPageBuilder addOption(OptionBuilder option)` (adds to an implicit unnamed group at bottom of page)

- **B.4 OptionGroupBuilder:**
  - Class: `net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder` (interface)
  - Methods:
    - `OptionGroupBuilder setName(Component name)` — optional; only used if `setName` is called
    - `OptionGroupBuilder addOption(OptionBuilder option)`

- **B.5 Option control APIs:**
  - **Cycling enum (Mode, Debug-Eye):**
    - Class: `net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder<E extends Enum<E>>`
    - Required: `setName(Component)`, `setBinding(Consumer<E> save, Supplier<E> load)` OR `setBinding(OptionBinding<E>)`, `setDefaultValue(E)`, **`setElementNameProvider(Function<E, Component>)`** (required for display labels), `setStorageHandler(StorageEventHandler)` (called after a change to flush to disk).
    - Optional: `setAllowedValues(Set<E>)`, `setTooltip(Component)` or `setTooltip(Function<E, Component>)`, `setFlags(OptionFlag...)` or `setFlags(Identifier...)`, `setImpact(OptionImpact)`, `setEnabled(boolean)`, `setEnabledProvider(Function<ConfigState, Boolean>, Identifier...)`.
    - Helper for label arrays (matches enum.ordinal()): `static <E extends Enum<E>> Function<E, Component> nameProviderFrom(Component... names)`.
  - **Int slider (IPD in millimeters 0..500):**
    - Class: `net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder`
    - Required: `setName`, `setBinding(Consumer<Integer>, Supplier<Integer>)`, `setDefaultValue(Integer)`, **`setRange(int min, int max, int step)`** (or `setRange(Range)`), **`setValueFormatter(ControlValueFormatter)`** (formats int → Component for the slider readout). `ControlValueFormatter` lives in `net.caffeinemc.mods.sodium.api.config.option`.
    - Optional: `setValidator(SteppedValidator)`, `setRangeProvider(...)`, `setImpact`, `setFlags`, `setTooltip(Component)` or `setTooltip(Function<Integer, Component>)`.
  - **Boolean option (any tickbox):** `net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder` with `setName`, `setBinding`, `setDefaultValue(Boolean)`, optional flags/impact/tooltip.
  - **Impact-tagging enum:**
    - Class: `net.caffeinemc.mods.sodium.api.config.option.OptionImpact`
    - Values: `LOW` (green), `MEDIUM` (yellow), `HIGH` (gold), `VARIES` (white).
    - Set via `OptionBuilder.setImpact(OptionImpact)`.
  - **Flags (post-change action triggers):**
    - Class: `net.caffeinemc.mods.sodium.api.config.option.OptionFlag` (enum)
    - Values relevant to us: `REQUIRES_RENDERER_RELOAD` (rebuild all meshes; the right flag for the stereo-toggle option to trigger Sodium's chunk-graph wipe), `REQUIRES_RENDERER_UPDATE`, `REQUIRES_ASSET_RELOAD`, `REQUIRES_VIDEOMODE_RELOAD`, `REQUIRES_GAME_RESTART`.
  - **Binding contract:** `OptionBuilder.setBinding(Consumer<T> save, Supplier<T> load)` OR `OptionBuilder.setBinding(OptionBinding<T>)` (interface).
  - **Storage flush:** `OptionBuilder.setStorageHandler(StorageEventHandler)` — Sodium calls this `StorageEventHandler.flush()` once after a batch of option changes is applied (i.e. we get one flush per "Apply" click, not per option change).

### B.6 Sodium options model save trigger

- **Class:** `net.caffeinemc.mods.sodium.client.gui.SodiumOptions`
- **Save method:** `public static void writeToDisk(SodiumOptions config) throws java.io.IOException` desc `(Lnet/caffeinemc/mods/sodium/client/gui/SodiumOptions;)V`
  - **Note:** This is **Sodium's own** options-save (writes `sodium-options.json` via GSON). Stereoscopic does NOT save into this file — we use our own `StorageEventHandler` (B.5) plumbed to our own JSON file. This row is recorded only so that anyone hunting for "Sodium's save method" knows it has been renamed in 0.8.x from `writeChanges(Path)` (the historical 0.5.x name).
- **Load method (for reference):** `public static SodiumOptions loadFromDisk()` desc `()Lnet/caffeinemc/mods/sodium/client/gui/SodiumOptions;`.

---

## Section C — Iris 1.10.x

Sources read at `https://github.com/IrisShaders/Iris` branch `1.21.11`. See
"Caveats" at top of file for the 1.10.6 vs 1.10.7 branch-versus-binary gap; the
API surface listed here is stable across 1.10.x patches.

### C.1 Iris top-level pipeline-rebuild entry points

- **Top-level class:** `net.irisshaders.iris.Iris`
- **`reload()` method:**
  - `public static void reload() throws java.io.IOException`
  - Body: re-initialises `IrisConfig`, resets texture-reload counter, calls `destroyEverything()` (which calls `getPipelineManager().destroyPipeline()`), loads the shaderpack again, then `getPipelineManager().preparePipeline(getCurrentDimension())` if a level is loaded.
- **PipelineManager-level methods:**
  - Class: `net.irisshaders.iris.pipeline.PipelineManager`
  - Obtain via: `Iris.getPipelineManager()` (static) → returns the singleton `PipelineManager`.
  - `public net.irisshaders.iris.pipeline.WorldRenderingPipeline preparePipeline(net.irisshaders.iris.shaderpack.materialmap.NamespacedId currentDimension)` — creates or returns the pipeline for the given dimension; first-call side-effect is full pipeline construction (RenderTargets, shaders, etc.).
  - `public void destroyPipeline()` — destroys ALL per-dimension pipelines, sets `pipeline = null`, increments a version counter that triggers Sodium shader reload. Documented in source as "EXTREMELY DANGEROUS! … You must make sure that you *immediately* re-prepare the pipeline after destroying it to prevent the program from falling into an inconsistent state."
- **Dimension ID type:** `net.irisshaders.iris.shaderpack.materialmap.NamespacedId` (Iris's own wrapper; obtain current one via `Iris.getCurrentDimension()` which inspects `Minecraft.getInstance().level.dimension()` and applies the shaderpack's overworld/end/skybox heuristics).
- **For Task 23 (rebuildPipelineForStereoToggle):**
  - Single Iris-only entry: call `Iris.reload()` — this wipes and rebuilds everything cleanly, including respecting the (newly-toggled) per-eye render-target size we publish.
  - Lower-level alternative: `Iris.getPipelineManager().destroyPipeline(); Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());`. **Avoid** unless we need to skip shaderpack reload.

### C.2 RenderTargets — per-eye allocation target

- **Class:** `net.irisshaders.iris.targets.RenderTargets`
- **Constructor:**
  ```java
  public RenderTargets(
      int width,
      int height,
      com.mojang.blaze3d.textures.GpuTexture depthTexture,
      int depthBufferVersion,
      net.irisshaders.iris.gl.texture.DepthBufferFormat depthFormat,
      java.util.Map<Integer, net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives.RenderTargetSettings> renderTargets,
      net.irisshaders.iris.shaderpack.properties.PackDirectives packDirectives)
  ```
- **Color-target array field:**
  - Name: `targets`
  - Type: `private final net.irisshaders.iris.targets.RenderTarget[] targets;`
  - Access: per-index via `public RenderTarget get(int index)` / `public RenderTarget getOrCreate(int index)`. Iteration count via `public int getRenderTargetCount()`.
- **Resize method:**
  - `public boolean resizeIfNeeded(int newDepthBufferVersion, com.mojang.blaze3d.textures.GpuTexture newDepthTextureId, int newWidth, int newHeight, net.irisshaders.iris.gl.texture.DepthBufferFormat newDepthFormat, net.irisshaders.iris.shaderpack.properties.PackDirectives packDirectives)`
  - Returns true if size changed.
- **Framebuffer-bind methods (to @Inject before for per-eye target swap):**
  - `public net.irisshaders.iris.gl.framebuffer.GlFramebuffer createFramebufferWritingToMain(int[] drawBuffers)`
  - `public net.irisshaders.iris.gl.framebuffer.GlFramebuffer createFramebufferWritingToAlt(int[] drawBuffers)`
  - `public net.irisshaders.iris.gl.framebuffer.GlFramebuffer createColorFramebufferWithDepth(com.google.common.collect.ImmutableSet<Integer> stageWritesToMain, int[] drawBuffers)`
  - `public net.irisshaders.iris.gl.framebuffer.GlFramebuffer createColorFramebuffer(com.google.common.collect.ImmutableSet<Integer> stageWritesToMain, int[] drawBuffers)`
  - `public net.irisshaders.iris.gl.framebuffer.GlFramebuffer createGbufferFramebuffer(com.google.common.collect.ImmutableSet<Integer> stageWritesToAlt, int[] drawBuffers)`
- **Depth-texture access:**
  - Primary depth: `public com.mojang.blaze3d.textures.GpuTexture getDepthTexture()` returns the per-frame depth GpuTexture (field name `currentDepthTexture`).
  - No-translucents copy: `public com.mojang.blaze3d.textures.GpuTexture getDepthTextureNoTranslucents()` (field `noTranslucents`).
  - No-hand copy: `public com.mojang.blaze3d.textures.GpuTexture getDepthTextureNoHand()` (field `noHand`).
  - These are GpuTexture objects with a `iris$getGlId()` accessor injected by Iris's `MixinGpuTexture`.
- **Pre-depth-copy hooks (for stereo, we'll need to per-eye these too):**
  - `public void copyPreTranslucentDepth()`
  - `public void copyPreHandDepth()`
- **Dimensions accessors:** `public int getCurrentWidth()`, `public int getCurrentHeight()`.
- **Lifecycle:** `public void destroy()`.

### C.3 CameraUniforms.CameraPositionTracker

- **Outer class:** `net.irisshaders.iris.uniforms.CameraUniforms`
- **Inner class:** `net.irisshaders.iris.uniforms.CameraUniforms$CameraPositionTracker` (package-private, named `CameraPositionTracker` inside `CameraUniforms.java`)
- **`update()` method signature:** `private void update()` desc `()V` — wired as a per-frame listener via `notifier.addListener(this::update)` in the constructor.
- **Fields (private):**
  - `private static final double WALK_RANGE = 30000;`
  - `private static final double TP_RANGE = 1000;`
  - `private final org.joml.Vector3d shift = new Vector3d();`
  - `private org.joml.Vector3d previousCameraPosition = new Vector3d();`
  - `private org.joml.Vector3d currentCameraPosition = new Vector3d();`
  - `private org.joml.Vector3d previousCameraPositionUnshifted = new Vector3d();`
  - `private org.joml.Vector3d currentCameraPositionUnshifted = new Vector3d();`
- **`CameraUniforms.getUnshiftedCameraPosition()` (static, public):** returns `org.joml.Vector3d` from `Minecraft.getInstance().gameRenderer.getMainCamera().position()` (Mojang) ≡ yarn `MinecraftClient.getInstance().gameRenderer.getCamera().getPos()`. The yarn equivalent uses `net.minecraft.util.math.Vec3d` instead of `org.joml.Vector3d`; conversion is via Iris's `net.irisshaders.iris.helpers.JomlConversions.fromVec3`.

- **Verbatim `update()` body (for Task 20 replication — copied from `https://raw.githubusercontent.com/IrisShaders/Iris/1.21.11/common/src/main/java/net/irisshaders/iris/uniforms/CameraUniforms.java`):**

  ```java
  private void update() {
      previousCameraPosition = currentCameraPosition;
      previousCameraPositionUnshifted = currentCameraPositionUnshifted;
      currentCameraPosition = getUnshiftedCameraPosition().add(shift);
      currentCameraPositionUnshifted = getUnshiftedCameraPosition();

      updateShift();
  }
  ```

- **Supporting `updateShift()` body (also needed by Task 20):**

  ```java
  private void updateShift() {
      double dX = getShift(currentCameraPosition.x, previousCameraPosition.x);
      double dZ = getShift(currentCameraPosition.z, previousCameraPosition.z);

      if (dX != 0.0 || dZ != 0.0) {
          applyShift(dX, dZ);
      }
  }
  ```

- **Supporting `getShift(double value, double prevValue)` body:**

  ```java
  private static double getShift(double value, double prevValue) {
      if (Math.abs(value) > WALK_RANGE || Math.abs(value - prevValue) > TP_RANGE) {
          // Only shift by increments of WALK_RANGE - this is required for some packs (like SEUS PTGI) to work properly
          return -(value - (value % WALK_RANGE));
      } else {
          return 0.0;
      }
  }
  ```

- **Supporting `applyShift(double dX, double dZ)` body:**

  ```java
  private void applyShift(double dX, double dZ) {
      shift.x += dX;
      currentCameraPosition.x += dX;
      previousCameraPosition.x += dX;

      shift.z += dZ;
      currentCameraPosition.z += dZ;
      previousCameraPosition.z += dZ;
  }
  ```

- **Notes:** Iris's `addCameraUniforms(UniformHolder, FrameUpdateNotifier)` instantiates one tracker per pipeline. For Stereoscopic's per-eye uniforms in Task 20, the implementer should either (a) construct a second tracker for the right eye with the IPD-offset position substituted for the `getUnshiftedCameraPosition()` call, OR (b) inject an offset into `currentCameraPosition` after `update()` runs but before uniforms are pushed.

### C.4 MatrixUniforms / ViewportUniforms / IrisSamplers

- **MatrixUniforms:**
  - Class: `net.irisshaders.iris.uniforms.MatrixUniforms`
  - Public entry: `public static void addMatrixUniforms(net.irisshaders.iris.gl.uniform.UniformHolder uniforms, net.irisshaders.iris.shaderpack.properties.PackDirectives directives)`
  - Inner: `private record Inverted(java.util.function.Supplier<org.joml.Matrix4fc> parent) implements java.util.function.Supplier<Matrix4fc>` — supplies `gbufferModelViewInverse` / `gbufferProjectionInverse`.
  - Inner: `private static class Previous implements java.util.function.Supplier<org.joml.Matrix4fc>` — supplies `gbufferPreviousModelView` / `gbufferPreviousProjection`.
  - Matrix sources read from: `net.irisshaders.iris.uniforms.CapturedRenderingState.INSTANCE::getGbufferModelView` and `getGbufferProjection` (these are populated by `MixinLevelRenderer.iris$setupPipeline` from the `renderLevel` args).
- **ViewportUniforms:**
  - Class: `net.irisshaders.iris.uniforms.ViewportUniforms`
  - Public entry: `public static void addViewportUniforms(net.irisshaders.iris.gl.uniform.UniformHolder uniforms)`
  - **viewWidth / viewHeight feeders:** the SAME method `addViewportUniforms` registers both, via lambdas. `viewHeight` reads `Minecraft.getInstance().getMainRenderTarget().height`; `viewWidth` reads `Minecraft.getInstance().getMainRenderTarget().width`. There is **no intermediate local** holding `viewWidth` as a `double` — the value flows directly from the field read to the uniform binding. For per-eye half-width override (Task 20), the right approach is **not** `@ModifyVariable` inside this method, but rather a `@Redirect` on the `getMainRenderTarget().width` field-read, OR a higher-level approach that wraps the supplier registration.
- **IrisSamplers:**
  - Class: `net.irisshaders.iris.samplers.IrisSamplers`
  - Depth-sampler-binding method (for the gbuffer / world pass): `public static void addWorldDepthSamplers(net.irisshaders.iris.gl.sampler.SamplerHolder samplers, net.irisshaders.iris.targets.RenderTargets renderTargets)`
  - This method binds the three depthtex samplers: `depthtex0` (current depth), `depthtex1` (no-translucents), `depthtex2` (no-hand) — all reading from `RenderTargets.getDepthTexture*().iris$getGlId()`.
  - Composite-stage depth binding (for the composite/final pass): `public static void addCompositeSamplers(net.irisshaders.iris.gl.sampler.SamplerHolder samplers, net.irisshaders.iris.targets.RenderTargets renderTargets)`.
  - For Task 21 per-eye depth-texture rebind: redirect `RenderTargets.getDepthTexture()` calls inside these two static methods to return the *current eye's* depth GpuTexture.

### C.5 ShadowRenderer

- **Class:** `net.irisshaders.iris.shadows.ShadowRenderer`
- **Shadow-pass entry method:**
  - `public void renderShadows(net.irisshaders.iris.mixin.LevelRendererAccessor levelRenderer, net.minecraft.client.Camera playerCamera, net.minecraft.client.renderer.state.CameraRenderState renderState)` desc (note: this is the Mojang-named source signature; under yarn at runtime `Camera` and `CameraRenderState` are `net.minecraft.client.render.Camera` and `net.minecraft.client.render.state.CameraRenderState`).
- **Other notable methods on `ShadowRenderer`:**
  - `public void setupShadowViewport()`
  - `public void addDebugText(net.irisshaders.iris.gui.debug.DebugScreenDisplayer messages)`
  - `public void destroy()`
- **Static helper for view matrix construction:** `public static com.mojang.blaze3d.vertex.PoseStack createShadowModelView(float sunPathRotation, float intervalSize, float nearPlane, float farPlane)` — called from `MatrixUniforms.addMatrixUniforms` for the `shadowModelView` uniform.
- **Notes:** `renderShadows` is called from Iris's own `MixinLevelRenderer.iris$renderTerrainShadows` (which is an `@Inject` after `prepareCullFrustum` in `WorldRenderer.render`). For Task 22, Stereoscopic does NOT need to mixin this method; the shadow pass is view-independent (sun-relative, not camera-relative), so it should run once and be sampled by both eyes.

### C.6 HandRenderer

- **Class:** `net.irisshaders.iris.pathways.HandRenderer`
- **Singleton access:** `public static final HandRenderer INSTANCE = new HandRenderer();`
- **Solid-hand render entry:**
  - `public void renderSolid(org.joml.Matrix4fc modelMatrix, float tickDelta, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.irisshaders.iris.pipeline.WorldRenderingPipeline pipeline)`
- **Translucent-hand render entry:**
  - `public void renderTranslucent(org.joml.Matrix4fc modelMatrix, float tickDelta, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.irisshaders.iris.pipeline.WorldRenderingPipeline pipeline)`
- **Hand depth-texture:**
  - There is NO `depthTexture` field on `HandRenderer` itself. The "hand depth" copy lives on `RenderTargets` (see C.2) as `noHand` / `getDepthTextureNoHand()`, populated by `RenderTargets.copyPreHandDepth()`.
- **State accessors:** `public boolean isActive()`, `public boolean isRenderingSolid()`.
- **Constants:** `public static final float DEPTH = 0.125F;` (z-scale applied to hand projection so it doesn't clip blocks).
- **Notes:** Per-eye hand rendering for Task 22 means calling `renderSolid` / `renderTranslucent` once with the modelMatrix per eye, OR redirecting the modelMatrix arg.

### C.7 FinalPassRenderer + CompositeRenderer

- **FinalPassRenderer:**
  - Class: `net.irisshaders.iris.pipeline.FinalPassRenderer`
  - Final-blit method: `public void renderFinalPass()` desc `()V` — no args; reads `Minecraft.getInstance().getMainRenderTarget()` and draws/copies the final color-tex to it.
  - Lifecycle: `public void destroy()`.
  - Internal types: `private static final class Pass { Program program; ComputeProgram[] computes; ImmutableSet<Integer> stageReadsFromAlt; ImmutableSet<Integer> mipmappedBuffers; }`, `private static final class SwapPass { int target, width, height; GlFramebuffer from; int targetTexture; }`.
  - Constructed by Iris's `IrisRenderingPipeline` once per pipeline.
- **CompositeRenderer:**
  - Class: `net.irisshaders.iris.pipeline.CompositeRenderer`
  - Inner types: `Pass`, `ComputeOnlyPass`.
  - Public field used by FinalPassRenderer: `public static final … CompositeRenderer.COMPOSITE_PIPELINE` (referenced from `renderFinalPass`).
- **Notes:** Per the v0.1.0 spec, `CompositeRenderer` needs no mixin — composite passes run on per-eye color targets that are already correctly sized by the per-eye `RenderTargets`. `FinalPassRenderer.renderFinalPass()` IS a target for Task 26 (final SBS composite step: blit left+right to the SBS framebuffer instead of the vanilla main framebuffer).

---

## Section D — LWJGL3 Win32 bindings (LWJGL 3.3.3)

**Major architectural finding for Tasks 24–25 (hardware cursor):** LWJGL 3.3.3
publishes only a small subset of Win32 USER32/GDI32 functions in its
generated Java bindings. Most of the calls the 1.7.10 Stereoscopic reference
mod relied on **do not have public LWJGL wrappers in 3.3.3**. The list below
records exactly what IS and ISN'T available; rows that are unavailable include
a `MISSING — replacement plan` describing how to obtain the function pointer
ourselves via `Library.loadNative` / `APIUtil.apiGetFunctionAddress`.

### D.1 User32 methods we use

- **Class:** `org.lwjgl.system.windows.User32` ✓ confirmed to exist at this FQN.
- **`LoadCursor` — AVAILABLE:**
  - Overload 1: `public static long LoadCursor(@NativeType("HINSTANCE") long instance, @NativeType("LPCTSTR") java.nio.ByteBuffer cursorName)`
  - Overload 2: `public static long LoadCursor(@NativeType("HINSTANCE") long instance, @NativeType("LPCTSTR") CharSequence cursorName)`
  - For loading system cursors (e.g. `IDC_ARROW`), pass `instance = 0L` and the cursor name as a `MAKEINTRESOURCE` encoded into a ByteBuffer. The IDC_ constants are public on `User32`:
    - `IDC_ARROW = 32512`, `IDC_IBEAM = 32513`, `IDC_WAIT = 32514`, `IDC_CROSS = 32515`, `IDC_UPARROW = 32516`, `IDC_SIZE = 32640`, `IDC_ICON = 32641`, `IDC_SIZENWSE = 32642`, `IDC_SIZENESW = 32643`, `IDC_SIZEWE = 32644`, `IDC_SIZENS = 32645`, `IDC_SIZEALL = 32646`, `IDC_NO = 32648`, `IDC_HAND = 32649`, `IDC_APPSTARTING = 32650`, `IDC_HELP = 32651`.
- **`GetCursorPos` — AVAILABLE:**
  - `public static boolean GetCursorPos(@NativeType("LPPOINT") org.lwjgl.system.windows.POINT point)`
- **`ClipCursor` — AVAILABLE:**
  - `public static boolean ClipCursor(@Nullable @NativeType("RECT const *") org.lwjgl.system.windows.RECT rect)`
  - **Release the clip by passing `null`** (the `@Nullable` annotation confirms this maps to passing C `NULL`).
- **`ClientToScreen` — AVAILABLE:**
  - `public static boolean ClientToScreen(@NativeType("HWND") long hWnd, @NativeType("LPPOINT") org.lwjgl.system.windows.POINT lpPoint)`
- **`GetIconInfo` — MISSING from LWJGL 3.3.3 public API.**
  - **Replacement plan:** Obtain the user32.dll handle via `User32.getLibrary()` (returns `SharedLibrary`), look up the function address via `SharedLibrary.getFunctionAddress("GetIconInfo")`, and invoke via `org.lwjgl.system.JNI.callPP(funcAddr, hIconArg, iconInfoPtrArg)` — taking an `ICONINFO` struct laid out manually in a `MemoryStack`-allocated buffer (see D.3 for layout).
- **`GetClientRect` — MISSING from LWJGL 3.3.3 public API.**
  - **Replacement plan:** Same pattern as GetIconInfo — `SharedLibrary.getFunctionAddress("GetClientRect")`, then `JNI.callPPI(funcAddr, hwnd, rectPtr)` with a `RECT` (which IS in LWJGL — see D.3) allocated on a `MemoryStack`.
- **`DrawIconEx` — MISSING from LWJGL 3.3.3 public API.**
  - **Replacement plan:** `SharedLibrary.getFunctionAddress("DrawIconEx")` then `JNI.callPIIPIIIPI(...)` with the appropriate argument layout. Constants needed: `DI_NORMAL = 0x0003`, `DI_MASK = 0x0001`, `DI_IMAGE = 0x0002`, `DI_COMPAT = 0x0004`, `DI_DEFAULTSIZE = 0x0008`, `DI_NOMIRROR = 0x0010` (these are documented in WinUser.h, not provided as Java constants by LWJGL — record them as our own `public static final int` block).

### D.2 GDI32 methods we use

- **Class:** `org.lwjgl.system.windows.GDI32` ✓ confirmed to exist at this FQN.
- **What LWJGL 3.3.3's GDI32 actually exposes (the entire public surface):** `ChoosePixelFormat`, `DescribePixelFormat`, `GetPixelFormat`, `SetPixelFormat`, `SwapBuffers`. **None** of the cursor-rendering helpers we need are bound.
- **`CreateCompatibleDC` — MISSING.**
  - **Replacement plan:** `SharedLibrary gdi = GDI32.getLibrary(); long fn = gdi.getFunctionAddress("CreateCompatibleDC"); long hdc = JNI.callPP(fn, sourceHdc);`
- **`SelectObject` — MISSING.**
  - **Replacement plan:** Same pattern — `gdi.getFunctionAddress("SelectObject")` → `JNI.callPPP(fn, hdc, hgdiobj)`.
- **`GetDIBits` — MISSING.**
  - **Replacement plan:** `gdi.getFunctionAddress("GetDIBits")` → `JNI.callPPIIPPI(fn, hdc, hbm, start, lines, lpvBitsPtr, lpbmiPtr, usage)`. Both `BITMAPINFO` and the bits buffer must be allocated manually (see D.3).
- **`CreateCompatibleBitmap` — MISSING.**
  - **Replacement plan:** `gdi.getFunctionAddress("CreateCompatibleBitmap")` → `JNI.callPIIP(fn, hdc, width, height)`.
- **`BitBlt` — MISSING.**
  - **Replacement plan:** `gdi.getFunctionAddress("BitBlt")` → `JNI.callPIIIIPIIII(fn, hdcDest, x, y, w, h, hdcSrc, srcX, srcY, rop)`. Constant: `SRCCOPY = 0x00CC0020` (record ourselves).
- **`DeleteDC` — MISSING.**
  - **Replacement plan:** `gdi.getFunctionAddress("DeleteDC")` → `JNI.callPI(fn, hdc)`.
- **`DeleteObject` — MISSING.**
  - **Replacement plan:** `gdi.getFunctionAddress("DeleteObject")` → `JNI.callPI(fn, hgdiobj)`.

### D.3 LWJGL3 Win32 structs

- **`RECT` — AVAILABLE** (`org.lwjgl.system.windows.RECT`).
  - Layout: `struct { LONG left; LONG top; LONG right; LONG bottom; }` — all 32-bit int fields.
  - Accessor pattern: getter `left()` returns int, setter `left(int)` returns `this RECT` (fluent).
  - Allocation: `RECT.malloc(MemoryStack)`, `RECT.calloc(MemoryStack)`, `RECT.create()`.
- **`POINT` — AVAILABLE** (`org.lwjgl.system.windows.POINT`).
  - Layout: `struct { LONG x; LONG y; }`.
  - Accessor pattern: `x()` / `x(int)`, `y()` / `y(int)`.
  - Allocation: same as RECT.
- **`ICONINFO` — MISSING from LWJGL 3.3.3.**
  - **Replacement plan:** Allocate manually via `MemoryStack.malloc(20)` (sizeof on x64; layout: `BOOL fIcon (4) + DWORD xHotspot (4) + DWORD yHotspot (4) + HBITMAP hbmMask (8) + HBITMAP hbmColor (8) = 32` on x64 with 8-byte alignment — record the exact offsets in a constants class). Field reads via `MemoryUtil.memGetInt(addr+offset)` and `MemoryUtil.memGetAddress(addr+offset)` for the HBITMAP fields.
  - Field accessor names we'll define (mirroring LWJGL naming convention): `fIcon()`/`fIcon(boolean)`, `xHotspot()`/`xHotspot(int)`, `yHotspot()`/`yHotspot(int)`, `hbmMask()`/`hbmMask(long)`, `hbmColor()`/`hbmColor(long)`.
- **`BITMAP` — MISSING from LWJGL 3.3.3.**
  - **Replacement plan:** Layout `struct BITMAP { LONG bmType; LONG bmWidth; LONG bmHeight; LONG bmWidthBytes; WORD bmPlanes; WORD bmBitsPixel; LPVOID bmBits; }` (32 bytes on x64). Allocate via `MemoryStack`, accessors named `bmType()`, `bmWidth()`, `bmHeight()`, `bmWidthBytes()`, `bmPlanes()`, `bmBitsPixel()`, `bmBits()`.
- **`BITMAPINFO` — MISSING from LWJGL 3.3.3.**
  - **Replacement plan:** Layout `struct BITMAPINFO { BITMAPINFOHEADER bmiHeader; RGBQUAD bmiColors[1]; }`. We'll need to lay out both `BITMAPINFOHEADER` (40 bytes: biSize, biWidth, biHeight, biPlanes, biBitCount, biCompression, biSizeImage, biXPelsPerMeter, biYPelsPerMeter, biClrUsed, biClrImportant) and `RGBQUAD` (4 bytes) manually. Constants needed: `BI_RGB = 0`, `BI_BITFIELDS = 3`.

### D.4 Function-pointer access pattern (shared by all MISSING rows above)

LWJGL 3.3.3 exposes a clean reusable pattern for "function the public API
doesn't bind." For every MISSING row in D.1 and D.2, the implementation in
Task 24/25 will look like:

```java
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.JNI;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.GDI32;

private static final long FN_GetClientRect;
private static final long FN_GetIconInfo;
private static final long FN_DrawIconEx;
private static final long FN_CreateCompatibleDC;
private static final long FN_SelectObject;
private static final long FN_GetDIBits;
private static final long FN_CreateCompatibleBitmap;
private static final long FN_BitBlt;
private static final long FN_DeleteDC;
private static final long FN_DeleteObject;

static {
    SharedLibrary user32 = User32.getLibrary();
    SharedLibrary gdi32 = GDI32.getLibrary();
    FN_GetClientRect         = user32.getFunctionAddress("GetClientRect");
    FN_GetIconInfo           = user32.getFunctionAddress("GetIconInfo");
    FN_DrawIconEx            = user32.getFunctionAddress("DrawIconEx");
    FN_CreateCompatibleDC    = gdi32.getFunctionAddress("CreateCompatibleDC");
    FN_SelectObject          = gdi32.getFunctionAddress("SelectObject");
    FN_GetDIBits             = gdi32.getFunctionAddress("GetDIBits");
    FN_CreateCompatibleBitmap = gdi32.getFunctionAddress("CreateCompatibleBitmap");
    FN_BitBlt                = gdi32.getFunctionAddress("BitBlt");
    FN_DeleteDC              = gdi32.getFunctionAddress("DeleteDC");
    FN_DeleteObject          = gdi32.getFunctionAddress("DeleteObject");
}
```

Then each call site uses the appropriate `JNI.callXxx` static, e.g.:

```java
import static org.lwjgl.system.JNI.*;
boolean ok = callPPI(FN_GetClientRect, hwnd, rect.address()) != 0;
```

The `JNI.callXxx` naming convention encodes the return type and argument
types as a suffix: `P`=pointer/long, `I`=int, `J`=long(64), `Z`=boolean(int),
`V`=void. See `org.lwjgl.system.JNI` in `lwjgl-3.3.3.jar` for the full set of
permitted suffixes.

This pattern is well-trodden in LWJGL programs — it's how LWJGL itself
historically grew its bindings before adding generated Java stubs.

---

## Verification checklist (for downstream tasks)

Before starting a mixin task, the implementer should:

1. Re-confirm the jar SHA at the path in **Resolved jar paths** matches what
   they have locally (Loom may have re-downloaded with a different hash).
2. For yarn-named rows, re-run `javap -p -s <class>` on the merged jar to
   confirm the descriptor hasn't shifted in a yarn rebuild (1.21.11+build.6,
   build.7, etc.).
3. For Iris/Sodium source-derived rows, check the version pin in
   `build.gradle.kts` — if Sodium has been bumped past 0.8.11 or Iris past
   1.10.7, those source URLs must be re-fetched against the new tag/branch
   commit before trusting the signatures.
4. For LWJGL Win32 rows in D.1/D.2/D.3: re-confirm against the LWJGL version
   Loom resolved (currently 3.3.3) since LWJGL has been gradually expanding
   its Win32 bindings — by LWJGL 3.4 some of the MISSING rows may have public
   wrappers.
