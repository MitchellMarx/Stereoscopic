package com.mitchellmarx.stereoscopic.mixin.minecraft;

import com.mitchellmarx.stereoscopic.core.StereoState;
import com.mitchellmarx.stereoscopic.render.PerEyeRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Force {@code getCloudsFramebuffer()} to return null while stereo is active
 * so {@link net.minecraft.client.render.CloudRenderer} picks the main-FB
 * branch (= per-eye scratch via the {@code getFramebuffer()} substitution),
 * not the framegraph-allocated cloud FB with its independent freshly-cleared
 * depth attachment.
 *
 * <p>{@code CloudRenderer.renderClouds} chooses its render target by checking
 * the non-null clouds FB before falling back to MC main FB. The framegraph
 * allocates a {@code SimpleFramebufferFactory(W,H,true,0)} when the improved
 * transparency post-effect ("Fabulous") is active. That FB carries its own
 * cleared-to-1.0 depth attachment never written by terrain — clouds would
 * depth-test against an empty depth buffer and every fragment passes, then
 * the resulting cloud color overdraws solid geometry once the transparency
 * post-effect composites it back. Iris normally avoids this by force-disabling
 * Fabulous on shader-pack load, but that's option-driven and depends on the
 * resource-load ordering having fired before the first stereo frame — a
 * pre-flip stereo enable would race it.
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Inject(method = "getCloudsFramebuffer()Lnet/minecraft/client/gl/Framebuffer;", at = @At("HEAD"), cancellable = true)
    private void stereoscopic$forceMainFbForClouds(CallbackInfoReturnable<Framebuffer> cir) {
        if (!StereoState.INSTANCE.isActive()) return;
        if (!PerEyeRenderer.isScratchFbActive()) return;
        cir.setReturnValue(null);
    }
}
