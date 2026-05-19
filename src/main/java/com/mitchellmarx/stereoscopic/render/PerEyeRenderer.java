package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.compat.iris.PerEyeRenderTargetHooks;
import com.mitchellmarx.stereoscopic.core.StereoOptions;
import com.mitchellmarx.stereoscopic.core.StereoState;
// GlStateManager._viewport directly, not RenderSystem.viewport — 1.21.11 has
// no RenderSystem.viewport(IIII) wrapper. Yarn 1.21.11 also moves
// GlStateManager from com.mojang.blaze3d.platform to com.mojang.blaze3d.opengl.
import com.mitchellmarx.stereoscopic.Stereoscopic;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;

public final class PerEyeRenderer {

    private static boolean bypass;

    public static boolean isBypassActive() { return bypass; }

    private static boolean scratchFbActive;
    private static SimpleFramebuffer scratchFb;
    private static int scratchFbW = -1;
    private static int scratchFbH = -1;

    public static boolean isScratchFbActive() { return scratchFbActive; }
    public static void setScratchFbActive(boolean active) { scratchFbActive = active; }
    public static SimpleFramebuffer getScratchFb() { return scratchFb; }
    public static int getScratchFbW() { return scratchFbW; }
    public static int getScratchFbH() { return scratchFbH; }

    public static SimpleFramebuffer ensureScratchFb(int w, int h) {
        if (scratchFb != null && scratchFbW == w && scratchFbH == h) return scratchFb;
        if (scratchFb != null) {
            try { scratchFb.delete(); }
            catch (Throwable t) { Stereoscopic.LOG.warn("Scratch FB delete failed during resize; GPU FB leaked", t); }
            scratchFb = null;
        }
        scratchFb = new SimpleFramebuffer("stereoscopic-world-scratch", w, h, true);
        scratchFbW = w;
        scratchFbH = h;
        return scratchFb;
    }

    public static void disposeScratch() {
        if (scratchFb != null) {
            try { scratchFb.delete(); }
            catch (Throwable t) { Stereoscopic.LOG.warn("Scratch FB delete failed on shutdown; GPU FB leaked", t); }
            scratchFb = null;
        }
        scratchFbW = -1;
        scratchFbH = -1;
    }

    public static void viewportRaw(int x, int y, int w, int h) {
        bypass = true;
        try { GlStateManager._viewport(x, y, w, h); }
        finally { bypass = false; }
    }

    public static void scissorRaw(int x, int y, int w, int h) {
        bypass = true;
        try {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(x, y, w, h);
        } finally { bypass = false; }
    }

    public static void scissorDisableRaw() {
        bypass = true;
        try { GlStateManager._disableScissorTest(); }
        finally { bypass = false; }
    }

    public enum Pass { WORLD, GUI }

    public static void runForEachEye(Pass pass, Runnable body) {
        StereoState s = StereoState.INSTANCE;
        if (!s.isActive()) { body.run(); return; }

        int fbW = s.getFrameFbW();
        int fbH = s.getFrameFbH();

        StereoState.Eye[] eyes = { StereoState.Eye.LEFT, StereoState.Eye.RIGHT };
        for (int i = 0; i < eyes.length; i++) {
            // Between LEFT and RIGHT WORLD iters, advance Iris's
            // SystemTimeUniforms.COUNTER so each eye's ProgramUniforms.update()
            // sees currentFrame != lastFrame and re-uploads PER_FRAME uniforms
            // with the RIGHT eye's cameraPosition + per-eye state. ONLY the
            // COUNTER tick — re-firing TIMER.beginFrame or setRealTickDelta
            // between eyes drifted time-of-day-derived shader state (sun
            // position, fog scattering) between LEFT and RIGHT at
            // sunrise/sunset. See PerEyeRenderTargetHooks.irisStartFrameBetweenEyes.
            if (i > 0 && pass == Pass.WORLD) {
                PerEyeRenderTargetHooks.irisStartFrameBetweenEyes();
            }
            renderOneEye(eyes[i], pass, body, fbW, fbH, s);
        }
        viewportRaw(0, 0, fbW, fbH);
        scissorDisableRaw();
        // Separate scissor state on RenderSystem consulted by the GPU command
        // pipeline / GuiRenderer flush — needs its own disable.
        RenderSystem.disableScissorForRenderTypeDraws();
    }

    private static void renderOneEye(StereoState.Eye eye, Pass pass, Runnable body,
                                     int fbW, int fbH, StereoState s) {
        s.setEye(eye);
        if (pass == Pass.WORLD) {
            PerEyeRenderTargetHooks.setActiveEye(s.currentEyeIndex());
        }
        ViewportMath.Rect r = ViewportMath.eyeRect(eye, StereoOptions.INSTANCE.mode, fbW, fbH);
        viewportRaw(r.x(), r.y(), r.w(), r.h());
        // Scissor pinned to eye rect:
        //   WORLD: glClear inside WorldRenderer would wipe the previous eye otherwise.
        //   GUI: 1.21's GuiRenderer flush goes through CommandEncoder.createRenderPass,
        //        which rasterizes to the full color-attachment view ignoring glViewport.
        //        Classic GL scissor clips that on the OpenGL backend; the new
        //        RenderSystem.scissorStateForRenderTypeDraws clips the render-type
        //        draw path independently.
        scissorRaw(r.x(), r.y(), r.w(), r.h());
        if (pass == Pass.GUI) {
            RenderSystem.enableScissorForRenderTypeDraws(r.x(), r.y(), r.w(), r.h());
        }
        switch (pass) {
            case WORLD -> s.enterWorldPass(r.x(), r.y(), r.w(), r.h());
            case GUI   -> s.enterGuiPass(r.x(), r.y(), r.w(), r.h());
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
