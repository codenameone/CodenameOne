package com.codename1.simd;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SIMDTest extends UITestBase {

    @FormTest
    void testSupportDefaultsToFalse() {
        assertFalse(SIMD.isSupported());
    }

    @FormTest
    void testFloat4OperationsFallbackScalar() {
        float[] a = new float[] {1f, 2f, 3f, 4f};
        float[] b = new float[] {10f, 20f, 30f, 40f};
        float[] out = new float[4];
        SIMD.Float4 va = SIMD.load(a, 0);
        SIMD.Float4 vb = SIMD.load(b, 0);
        SIMD.Float4 vc = SIMD.makeFloat4(5f, 5f, 5f, 5f);
        SIMD.store(out, 0, SIMD.fma(va, vb, vc));
        assertArrayEquals(new float[] {15f, 45f, 95f, 165f}, out);
    }

    @FormTest
    void testInt4OperationsFallbackScalar() {
        SIMD.Int4 a = SIMD.makeInt4(1, 2, 3, 4);
        SIMD.Int4 b = SIMD.makeInt4(10, 20, 30, 40);
        SIMD.Int4 c = SIMD.add(a, b);
        assertEquals(11, c.x);
        assertEquals(22, c.y);
        assertEquals(33, c.z);
        assertEquals(44, c.w);
    }

    @FormTest
    void testU8x16LoadAndLane() {
        byte[] values = new byte[16];
        for (int i = 0; i < 16; i++) {
            values[i] = (byte)(i + 1);
        }
        SIMD.U8x16 v = SIMD.loadU8(values, 0);
        assertEquals(1, SIMD.laneU8(v, 0));
        assertEquals(16, SIMD.laneU8(v, 15));
    }
}
