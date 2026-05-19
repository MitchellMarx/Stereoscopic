package com.mitchellmarx.stereoscopic.core;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public final class StereoOptions {
    public static final StereoOptions INSTANCE = new StereoOptions();

    public StereoMode mode = StereoMode.OFF;
    public float ipd = 0.064f;       // meters

    StereoOptions() {} // package-private for tests; production code uses INSTANCE

    public Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("stereoscopic-options.json");
    }

    public void save() { StereoOptionsIO.writeTo(configFile(), this); }
    public void load() { StereoOptionsIO.readInto(configFile(), this); }
}
