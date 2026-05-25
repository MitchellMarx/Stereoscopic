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
        o.convergence = 8.0f;

        StereoOptionsIO.writeTo(file, o);

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);

        assertEquals(StereoMode.SBS_HALF, loaded.mode);
        assertEquals(0.075f, loaded.ipd, 1e-6f);
        assertEquals(8.0f, loaded.convergence, 1e-6f);
    }

    @Test
    void missingFileLeavesDefaults(@TempDir Path tmp) throws Exception {
        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(tmp.resolve("nope.json"), loaded);
        assertEquals(StereoMode.OFF, loaded.mode);
        assertEquals(0.064f, loaded.ipd, 1e-6f);
        assertEquals(4.0f, loaded.convergence, 1e-6f);
    }

    @Test
    void partialJsonKeepsDefaultsForMissingKeys(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("partial.json");
        Files.writeString(file, "{ \"ipd\": 0.080 }");

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);
        assertEquals(StereoMode.OFF, loaded.mode);
        assertEquals(0.080f, loaded.ipd, 1e-6f);
        assertEquals(4.0f, loaded.convergence, 1e-6f,
            "convergence missing from JSON, default preserved");
    }

    @Test
    void ipdIsClampedOnRead(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("oob.json");
        Files.writeString(file, "{ \"ipd\": 99.0 }");

        StereoOptions loaded = new StereoOptions();
        StereoOptionsIO.readInto(file, loaded);
        assertEquals(0.5f, loaded.ipd, 1e-6f, "IPD clamped to [0.0, 0.5]");
    }

    @Test
    void convergenceIsClampedOnRead(@TempDir Path tmp) throws Exception {
        Path hi = tmp.resolve("hi.json");
        Files.writeString(hi, "{ \"convergence\": 99.0 }");
        StereoOptions loadedHi = new StereoOptions();
        StereoOptionsIO.readInto(hi, loadedHi);
        assertEquals(16.0f, loadedHi.convergence, 1e-6f,
            "convergence clamped to [0, 16]");

        Path lo = tmp.resolve("lo.json");
        Files.writeString(lo, "{ \"convergence\": -5.0 }");
        StereoOptions loadedLo = new StereoOptions();
        StereoOptionsIO.readInto(lo, loadedLo);
        assertEquals(0.0f, loadedLo.convergence, 1e-6f,
            "convergence clamped to [0, 16]; 0 = parallel-axis");
    }
}
