package com.mitchellmarx.stereoscopic.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class StereoOptionsIOTest {

    @Test
    void roundTrip(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("opts.json");

        StereoOptions o = new StereoOptions();
        o.mode = StereoMode.SBS_HALF;
        o.ipd = 0.075f;

        StereoOptionsIO.writeTo(file, o);

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);

        assertEquals(StereoMode.SBS_HALF, loaded.mode);
        assertEquals(0.075f, loaded.ipd, 1e-6f);
    }

    @Test
    void missingFileLeavesDefaults(@TempDir Path tmp) throws Exception {
        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(tmp.resolve("nope.json"), loaded);
        assertEquals(StereoMode.OFF, loaded.mode);
        assertEquals(0.064f, loaded.ipd, 1e-6f);
    }

    @Test
    void partialJsonKeepsDefaultsForMissingKeys(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("partial.json");
        Files.writeString(file, "{ \"ipd\": 0.080 }");

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);
        assertEquals(StereoMode.OFF, loaded.mode);
        assertEquals(0.080f, loaded.ipd, 1e-6f);
    }

    @Test
    void ipdIsClampedOnRead(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("oob.json");
        Files.writeString(file, "{ \"ipd\": 99.0 }");

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);
        assertEquals(0.5f, loaded.ipd, 1e-6f, "IPD clamped to [0.0, 0.5]");
    }
}
