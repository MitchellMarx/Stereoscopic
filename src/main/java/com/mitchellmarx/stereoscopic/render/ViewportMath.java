// src/main/java/com/mitchellmarx/stereoscopic/render/ViewportMath.java
package com.mitchellmarx.stereoscopic.render;

import com.mitchellmarx.stereoscopic.core.StereoMode;
import com.mitchellmarx.stereoscopic.core.StereoState;

public final class ViewportMath {
    private ViewportMath() {}

    public record Rect(int x, int y, int w, int h) {}

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
