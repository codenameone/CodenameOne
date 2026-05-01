package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.util.Simd;

/// Device-running regression for the ParparVM alloca-overflow that broke
/// `Image.createMask()` on iOS for image-scale buffers (StackOverflow report:
/// a 410x410 round-mask blew the per-thread stack because the Simd alloca
/// call lowered to ~656 KB of `__builtin_alloca`).
///
/// The unit-test side cannot reach the alloca lowering at all (JavaSE just
/// returns a `new int[]`). This test runs in the actual ParparVM-translated
/// binary on iOS so it exercises the real macro path, including the
/// CN1_SIMD_STACK_HEAP_THRESHOLD heap fallback that was added defensively.
///
/// Note: the alloca-direct phase (`exerciseLargeAllocaDirectly`) deliberately
/// keeps every alloca array reference inside the method body and only emits
/// constant-string `fail()` diagnostics. The bytecode compliance checker
/// treats `array.length` and `array[i]` as alloca-tainted values; including
/// them in a string-concat'd error message would emit invokedynamic /
/// Integer.valueOf calls on alloca-tainted operands and trip the checker.
public class SimdLargeAllocaTest extends BaseTest {
    /// Big enough to push every alloca call past the 32 KB stack threshold:
    /// 410*410*4 = 656 KB for ints, 168 KB for bytes. Mirrors the original
    /// crash repro from the user report.
    private static final int IMAGE_SIZE = 410;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            if (!exerciseLargeAllocaDirectly()) {
                return false;
            }
            if (!exerciseCreateAndApplyMaskRepro()) {
                return false;
            }
            done();
            return true;
        } catch (Throwable t) {
            fail("SimdLargeAllocaTest threw: " + t);
            return false;
        }
    }

    /// Direct alloca calls at image scale - on ParparVM these are the actual
    /// lowering target the bug report tripped on. We touch the endpoints so a
    /// stack overflow / corrupt fallback would surface as a crash or wrong
    /// readback rather than passing silently. Error messages here are
    /// hardcoded - see class doc for why.
    private boolean exerciseLargeAllocaDirectly() {
        Simd simd = Simd.get();
        if (simd == null) {
            fail("Simd.get() returned null");
            return false;
        }

        int len = IMAGE_SIZE * IMAGE_SIZE;
        int half = len / 2;
        int last = len - 1;

        int[] ints = simd.allocaInt(len);
        if (ints == null) {
            fail("allocaInt returned null at image scale");
            return false;
        }
        if (ints.length != len) {
            fail("allocaInt returned wrong-length array at image scale");
            return false;
        }
        ints[0] = 0x1234abcd;
        ints[half] = -42;
        ints[last] = 0x7eadbeef;
        if (ints[0] != 0x1234abcd || ints[half] != -42 || ints[last] != 0x7eadbeef) {
            fail("allocaInt readback mismatch at image scale");
            return false;
        }

        byte[] bytes = simd.allocaByte(len);
        if (bytes == null) {
            fail("allocaByte returned null at image scale");
            return false;
        }
        if (bytes.length != len) {
            fail("allocaByte returned wrong-length array at image scale");
            return false;
        }
        bytes[0] = (byte) 0x5a;
        bytes[half] = (byte) 0xa5;
        bytes[last] = (byte) 0x33;
        if (bytes[0] != (byte) 0x5a || bytes[half] != (byte) 0xa5 || bytes[last] != (byte) 0x33) {
            fail("allocaByte readback mismatch at image scale");
            return false;
        }

        int[] zeroed = simd.allocaIntZeroed(len);
        if (zeroed.length != len) {
            fail("allocaIntZeroed returned wrong-length array at image scale");
            return false;
        }
        if (zeroed[0] != 0 || zeroed[half] != 0 || zeroed[last] != 0) {
            fail("allocaIntZeroed returned non-zero value at image scale");
            return false;
        }

        byte[] filled = simd.allocaByteFilled(len, (byte) 0x77);
        if (filled.length != len) {
            fail("allocaByteFilled returned wrong-length array at image scale");
            return false;
        }
        if (filled[0] != (byte) 0x77 || filled[half] != (byte) 0x77 || filled[last] != (byte) 0x77) {
            fail("allocaByteFilled did not fill array at image scale");
            return false;
        }

        // Real SIMD work over the image-scale alloca buffer: this catches
        // alignment issues that would only surface when the heap-fallback
        // path is taken (allocArrayAligned vs the alloca-aligned shim).
        int[] simdInts = simd.allocaInt(len);
        for (int i = 0; i < len; i += 1024) {
            simdInts[i] = i;
        }
        int[] resultInts = simd.allocaInt(len);
        simd.add(simdInts, simdInts, resultInts, 0, len);
        for (int i = 0; i < len; i += 1024) {
            if (resultInts[i] != i + i) {
                fail("simd.add over alloca-int at image scale produced wrong result");
                return false;
            }
        }

        return true;
    }

    /// Exact StackOverflow repro: a 410x410 round-rect mutable image fed
    /// through createMask() and applyMask(). Pre-fix this faulted on iOS
    /// inside the createMask() alloca call. We additionally verify the SIMD
    /// path agrees with the scalar path at this scale. The arrays here come
    /// from `Image.getRGB()` which is heap-allocated, so error messages can
    /// freely reference array contents without tripping the alloca checker.
    private boolean exerciseCreateAndApplyMaskRepro() {
        Image roundMask = Image.createImage(IMAGE_SIZE, IMAGE_SIZE, 0xff000000);
        Graphics g = roundMask.getGraphics();
        g.setColor(0xffffff);
        g.fillRoundRect(0, 0, IMAGE_SIZE, IMAGE_SIZE, 60, 60);

        boolean originalSimd = Image.isSimdOptimizationsEnabled();
        try {
            Image.setSimdOptimizationsEnabled(false);
            Object scalarMask = roundMask.createMask();
            if (scalarMask == null) {
                fail("scalar createMask returned null");
                return false;
            }
            Image scalarApplied = roundMask.applyMask(scalarMask);
            if (scalarApplied == null) {
                fail("scalar applyMask returned null");
                return false;
            }

            Image.setSimdOptimizationsEnabled(true);
            Object simdMask = roundMask.createMask();
            if (simdMask == null) {
                fail("simd createMask returned null - alloca path likely overflowed");
                return false;
            }
            Image simdApplied = roundMask.applyMask(simdMask);
            if (simdApplied == null) {
                fail("simd applyMask returned null - alloca path likely overflowed");
                return false;
            }

            // applyMask consumes the mask byte array internally, so verifying
            // the post-applyMask RGB matches between scalar and simd is the
            // strongest end-to-end check we can express from outside the
            // com.codename1.ui package (IndexedImage is package-private).
            int[] scalarRgb = scalarApplied.getRGB();
            int[] simdRgb = simdApplied.getRGB();
            if (scalarRgb.length != simdRgb.length) {
                fail("applyMask RGB-length mismatch scalar=" + scalarRgb.length + " simd=" + simdRgb.length);
                return false;
            }
            for (int i = 0; i < scalarRgb.length; i++) {
                if (scalarRgb[i] != simdRgb[i]) {
                    fail("applyMask RGB mismatch at " + i + ": scalar=" + Integer.toHexString(scalarRgb[i])
                            + " simd=" + Integer.toHexString(simdRgb[i]));
                    return false;
                }
            }

            // Spot-check the round-rect geometry so a degenerate mask (e.g. all
            // zero or all opaque) does not silently pass: corners outside the
            // 60-pixel radius must be transparent, the centre must be opaque.
            int center = (IMAGE_SIZE / 2) + (IMAGE_SIZE / 2) * IMAGE_SIZE;
            int corner = 0;
            int centerAlpha = (simdRgb[center] >>> 24) & 0xff;
            int cornerAlpha = (simdRgb[corner] >>> 24) & 0xff;
            if (centerAlpha == 0) {
                fail("applyMask centre pixel was transparent (alpha=" + centerAlpha + ")");
                return false;
            }
            if (cornerAlpha != 0) {
                fail("applyMask corner pixel was opaque (alpha=" + cornerAlpha + ")");
                return false;
            }
        } finally {
            Image.setSimdOptimizationsEnabled(originalSimd);
        }

        return true;
    }
}
