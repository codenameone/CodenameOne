package com.codename1.simd;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
