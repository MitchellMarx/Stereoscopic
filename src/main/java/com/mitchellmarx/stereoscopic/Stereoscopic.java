package com.mitchellmarx.stereoscopic;

import com.mitchellmarx.stereoscopic.core.StereoOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Stereoscopic implements ClientModInitializer {
    public static final String MOD_ID = "stereoscopic";
    public static final Logger LOG = LoggerFactory.getLogger("Stereoscopic");

    @Override
    public void onInitializeClient() {
        StereoOptions.INSTANCE.load();
        LOG.info("Stereoscopic initialized (mode={}, ipd={})",
            StereoOptions.INSTANCE.mode, StereoOptions.INSTANCE.ipd);
        FabricLoader.getInstance().getModContainer("iris")
            .ifPresent(c -> LOG.info("Iris detected: version={}",
                c.getMetadata().getVersion().getFriendlyString()));
    }
}
