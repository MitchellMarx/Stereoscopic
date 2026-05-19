package com.mitchellmarx.stereoscopic.compat.voxy;

import com.mitchellmarx.stereoscopic.Stereoscopic;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.targets.RenderTarget;

import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL45C.glNamedFramebufferTexture;

/**
 * Voxy's Iris-mode pipeline ({@code IrisVoxyRenderPipeline.<init>}) snapshots
 * Iris colortex texture IDs into its own two framebuffers' color attachments
 * once, at pipeline build. Those FBs aren't in Iris's {@code ownedFramebuffers},
 * so {@code MixinRenderTargets.stereoscopic$setActiveEye}'s per-eye attachment
 * walk misses them and voxy keeps writing LOD pixels into the LEFT-bank
 * colortex on both eyes — Iris's RIGHT-eye composite then reads an empty
 * gbuffer and paints sky color where LODs should be.
 *
 * <p>This facade rebinds voxy's two FBs to the active eye's bank on every
 * eye switch. No-op when voxy isn't installed; isolating the voxy-class
 * references in their own method body keeps the JVM verifier from trying
 * to resolve them at class load.
 */
public final class VoxyEyeRebindHooks {

    private VoxyEyeRebindHooks() {}

    // Captured once at classload; FabricLoader's mod set is fixed for the JVM lifetime.
    public static final boolean VOXY_PRESENT =
        FabricLoader.getInstance().isModLoaded("voxy");

    public static void rebindForEye(RenderTarget[] sourceBank, Int2IntMap reverseMap) {
        if (!VOXY_PRESENT) return;
        try {
            doRebind(sourceBank, reverseMap);
        } catch (Throwable t) {
            Stereoscopic.LOG.warn("Voxy FB rebind for active eye failed; LODs may render one-eyed this frame", t);
        }
    }

    private static void doRebind(RenderTarget[] sourceBank, Int2IntMap reverseMap) {
        net.irisshaders.iris.pipeline.WorldRenderingPipeline wp =
            net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable();
        if (!(wp instanceof me.cortex.voxy.client.iris.IGetIrisVoxyPipelineData getter)) return;
        me.cortex.voxy.client.iris.IrisVoxyRenderPipelineData data = getter.voxy$getPipelineData();
        if (data == null) return;
        me.cortex.voxy.client.core.IrisVoxyRenderPipeline pipe = data.thePipeline;
        if (pipe == null) return;

        rebindOne(pipe.fb.framebuffer.id,            data.opaqueDrawTargets,     sourceBank, reverseMap);
        rebindOne(pipe.fbTranslucent.framebuffer.id, data.translucentDrawTargets, sourceBank, reverseMap);
    }

    /**
     * {@code capturedOriginalTexIds} holds the LEFT-bank texture IDs voxy
     * captured at pipeline build, by attachment-slot index. Those originals
     * stay in the array even after we glNamedFramebufferTexture the live FB
     * to a different bank — they're our lookup key into {@code reverseMap},
     * which packs {@code (colortexSlot << 1) | (isAlt ? 1 : 0)}.
     */
    private static void rebindOne(int fbId, int[] capturedOriginalTexIds,
                                   RenderTarget[] bank, Int2IntMap reverseMap) {
        for (int i = 0; i < capturedOriginalTexIds.length; i++) {
            int meta = reverseMap.get(capturedOriginalTexIds[i]);
            if (meta < 0) continue;
            int slot = meta >>> 1;
            boolean isAlt = (meta & 1) == 1;
            if (slot >= bank.length) continue;
            RenderTarget rt = bank[slot];
            if (rt == null) continue;
            int newTex = isAlt ? rt.getAltTexture() : rt.getMainTexture();
            glNamedFramebufferTexture(fbId, GL_COLOR_ATTACHMENT0 + i, newTex, 0);
        }
    }
}
