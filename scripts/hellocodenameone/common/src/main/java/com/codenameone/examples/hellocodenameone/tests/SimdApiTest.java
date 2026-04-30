package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.util.Simd;

public class SimdApiTest extends BaseTest {
    @Override
    public boolean runTest() {
        try {
            Simd simd = Simd.get();
            if (!simd.isSupported()) {
                int[] a = new int[]{1, 2, 3, 4};
                int[] b = new int[]{9, 8, 7, 6};
                int[] out = new int[4];
                simd.add(a, b, out, 0, 4);
                if (out[0] != 10 || out[1] != 10 || out[2] != 10 || out[3] != 10) {
                    fail("Fallback SIMD API add failed on unsupported platform");
                    return false;
                }
                done();
                return true;
            }

            int[] a = simd.allocInt(16);
            int[] b = simd.allocInt(16);
            int[] out = simd.allocInt(16);
            for (int i = 0; i < 8; i++) {
                a[i] = i + 1;
                b[i] = 9 - i;
            }
            simd.add(a, b, out, 0, 8);
            for (int i = 0; i < 8; i++) {
                if (out[i] != 10) {
                    fail("Unexpected int add result at " + i + ": " + out[i]);
                    return false;
                }
            }

            float[] fa = simd.allocFloat(16);
            float[] fb = simd.allocFloat(16);
            float[] fo = simd.allocFloat(16);
            fa[0] = 1.5f;
            fa[1] = -2f;
            fa[2] = 3f;
            fa[3] = -4f;
            fb[0] = 2f;
            fb[1] = 3f;
            fb[2] = -1f;
            fb[3] = 0.5f;
            simd.mul(fa, fb, fo, 0, 4);
            if (Math.abs(fo[0] - 3f) > 0.0001f || Math.abs(fo[1] + 6f) > 0.0001f
                    || Math.abs(fo[2] + 3f) > 0.0001f || Math.abs(fo[3] + 2f) > 0.0001f) {
                fail("Unexpected float mul results");
                return false;
            }

            byte[] ba = simd.allocByte(16);
            byte[] bb = simd.allocByte(16);
            byte[] bo = simd.allocByte(16);
            ba[0] = 120;
            ba[1] = 10;
            ba[2] = -120;
            bb[0] = 20;
            bb[1] = -40;
            bb[2] = -20;
            simd.add(ba, bb, bo, 0, 3);
            if (bo[0] != 127 || bo[1] != -30 || bo[2] != -128) {
                fail("Unexpected saturating byte add results");
                return false;
            }

            if (CN.isSimulator()) {
                try {
                    simd.add(new int[4], new int[4], new int[4], 0, 4);
                    fail("Expected simulator registry guard to reject non-alloc arrays");
                    return false;
                } catch (IllegalArgumentException expected) {
                    // expected
                }
            }

            done();
            return true;
        } catch (Throwable t) {
            fail("SimdApiTest failed: " + t);
            return false;
        }
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
