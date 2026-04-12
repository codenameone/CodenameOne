package com.codename1.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimdTest extends UITestBase {

    @FormTest
    void baseFallbackOpsWork() {
        Simd simd = new Simd();

        int[] a = new int[]{1, 2, 3, 4};
        int[] b = new int[]{4, 3, 2, 1};
        int[] out = new int[4];
        simd.add(a, b, out, 0, 4);
        assertEquals(5, out[0]);
        assertEquals(5, out[3]);

        float[] fa = new float[]{1f, -2f, 3f};
        float[] fb = new float[]{4f, 5f, -6f};
        float[] fo = new float[3];
        simd.mul(fa, fb, fo, 0, 3);
        assertEquals(4f, fo[0], 0.0001f);
        assertEquals(-18f, fo[2], 0.0001f);

        byte[] ba = new byte[]{120, 100, -128};
        byte[] bb = new byte[]{20, 100, -1};
        byte[] bo = new byte[3];
        simd.add(ba, bb, bo, 0, 3);
        assertEquals(127, bo[0]);
        assertEquals(127, bo[1]);
        assertEquals(-128, bo[2]);
    }

    @FormTest
    void javaseRegistryGuardInSimulator() {
        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }

        int[] regA = simd.allocInt(4);
        int[] regB = simd.allocInt(4);
        int[] regO = simd.allocInt(4);
        simd.add(regA, regB, regO, 0, 4);

        if (CN.isSimulator()) {
            int[] plainA = new int[4];
            int[] plainB = new int[4];
            int[] plainO = new int[4];
            Throwable t = assertThrows(IllegalArgumentException.class, () -> simd.add(plainA, plainB, plainO, 0, 4));
            assertTrue(t.getMessage().indexOf("Simd.alloc") >= 0);
        }
    }
}
