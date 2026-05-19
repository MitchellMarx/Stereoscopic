package com.mitchellmarx.stereoscopic.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mitchellmarx.stereoscopic.cursor.CursorPresentThread;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Suppress MC's {@code glfwSwapBuffers} when the cursor present thread is
 * driving window presentation itself. Two swaps per frame on a
 * double-buffered chain (cursor thread's GDI {@code SwapBuffers(mainHdc)}
 * + MC's {@code glfwSwapBuffers}) flip front/back/front/back in alternation;
 * MC's swap immediately presents the other backbuffer (which nothing wrote
 * to since we also cancel {@code Framebuffer.blitToScreen}), producing the
 * fresh/stale/fresh/stale flicker the user sees.
 *
 * <p>Only the swap is skipped; the {@code pollEvents}/{@code beginFrame}/etc.
 * work inside {@code flipFrame} still runs. When the cursor thread isn't
 * running (mono, non-Windows, setup failure), the wrapper passes through.
 */
@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {

    @WrapOperation(
        method = "flipFrame(Lnet/minecraft/client/util/Window;Lnet/minecraft/client/util/tracy/TracyFrameCapturer;)V",
        at = @At(value = "INVOKE",
                 target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V")
    )
    private static void stereoscopic$skipMcSwapWhenCursorThreadOwnsPresent(long window, Operation<Void> original) {
        if (CursorPresentThread.isRunning()) return;
        original.call(window);
    }
}
