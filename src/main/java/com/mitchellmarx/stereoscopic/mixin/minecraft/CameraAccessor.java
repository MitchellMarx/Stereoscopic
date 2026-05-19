package com.mitchellmarx.stereoscopic.mixin.minecraft;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker for {@link Camera}'s protected {@code setPos}. Shifting Camera.pos
 * is the architecturally correct fix for 1.21 stereo — propagates to every
 * downstream consumer that reads camera position (Sodium chunk transforms,
 * frustum culling, fog distance, dynamic lighting, Iris's cameraPosition
 * uniform). Shifting the modelview matrix downstream misses those consumers.
 */
@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPos") void stereoscopic$setPos(Vec3d pos);
}
