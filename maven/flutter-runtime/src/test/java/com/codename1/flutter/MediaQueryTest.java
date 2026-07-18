package com.codename1.flutter;

import com.codename1.flutter.rendering.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MediaQueryData reports LOGICAL pixels: device pixels divided by the
 * bucketed devicePixelRatio (Dp.scale()), matching Flutter.
 */
public class MediaQueryTest {

    @Test
    public void sizeIsDevicePixelsDividedByRatio() {
        MediaQueryData d = MediaQueryData.compute(1170, 2532, 3.0, Boolean.FALSE);
        Size s = d.size();
        assertEquals(390.0, s.width(), 0.001);
        assertEquals(844.0, s.height(), 0.001);
        assertEquals(3.0, d.devicePixelRatio(), 0.001);
        assertEquals(Brightness.light, d.platformBrightness());
    }

    @Test
    public void ratioOneIsIdentity() {
        MediaQueryData d = MediaQueryData.compute(800, 600, 1.0, null);
        assertEquals(800.0, d.size().width(), 0.001);
        assertEquals(600.0, d.size().height(), 0.001);
    }

    @Test
    public void darkModeFlagMapsToBrightness() {
        assertEquals(Brightness.dark,
                MediaQueryData.compute(100, 100, 2.0, Boolean.TRUE).platformBrightness());
        assertEquals(Brightness.light,
                MediaQueryData.compute(100, 100, 2.0, null).platformBrightness(),
                "unknown platform brightness defaults to light");
    }

    @Test
    public void nonPositiveScaleFallsBackToOne() {
        MediaQueryData d = MediaQueryData.compute(400, 400, 0, Boolean.FALSE);
        assertEquals(1.0, d.devicePixelRatio(), 0.001);
        assertEquals(400.0, d.size().width(), 0.001);
    }
}
