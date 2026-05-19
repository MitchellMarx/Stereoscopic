package com.mitchellmarx.stereoscopic.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StereoStateTest {

    @BeforeEach
    void resetOptions() {
        StereoOptions.INSTANCE.mode = StereoMode.OFF;
        StereoOptions.INSTANCE.ipd = 0.064f;
        StereoState.INSTANCE.endFrame();
    }

    @Test
    void getEyeOffsetSignsMatchVanilla121Convention() {
        StereoOptions.INSTANCE.mode = StereoMode.SBS_HALF;
        StereoOptions.INSTANCE.ipd = 0.064f;
        StereoState.INSTANCE.beginFrame(1920, 1080);

        StereoState.INSTANCE.setEye(StereoState.Eye.LEFT);
        assertEquals(-0.032f, StereoState.INSTANCE.getEyeOffset(), 1e-7f,
            "LEFT eye shifts along the negative right-vector (physical left eye position)");

        StereoState.INSTANCE.setEye(StereoState.Eye.RIGHT);
        assertEquals(+0.032f, StereoState.INSTANCE.getEyeOffset(), 1e-7f,
            "RIGHT eye shifts along the positive right-vector");

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
}
