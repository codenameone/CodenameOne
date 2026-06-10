package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.util.Simd;

/**
 * Tallies the native SIMD speedup on the running platform. For a large workload
 * it times the native vectorized kernel ({@code CN.getSimd().add/mul}) against an
 * equivalent inline Java scalar loop, verifies the two produce identical results,
 * and logs the measured speedup so CI shows the concrete performance benefit of
 * the SSE2/NEON backend (the Windows analog of the iOS NEON layer). On platforms
 * with no native SIMD ({@code isSupported()==false}) it just confirms correctness
 * and reports 1x.
 */
public class SimdBenchmarkTest extends BaseTest {
    private static final int N = 1 << 16;     // elements per pass
    private static final int ITER = 300;      // passes (timing stability)

    @Override
    public boolean runTest() {
        try {
            Simd simd = Simd.get();
            boolean native_ = simd.isSupported();

            // ---- int add ----
            int[] ia = simd.allocInt(N);
            int[] ib = simd.allocInt(N);
            int[] iOut = simd.allocInt(N);
            int[] iRef = new int[N];
            for (int i = 0; i < N; i++) {
                ia[i] = i * 3 - 7;
                ib[i] = (i ^ 0x5a5a) + 11;
                iRef[i] = ia[i] + ib[i];
            }
            long tn = now();
            for (int it = 0; it < ITER; it++) {
                simd.add(ia, ib, iOut, 0, N);
            }
            long nativeIntMs = now() - tn;
            // Verify the NATIVE result now, before the Java loop overwrites iOut.
            for (int i = 0; i < N; i++) {
                if (iOut[i] != iRef[i]) {
                    fail("native int add mismatch at " + i + ": " + iOut[i] + " != " + iRef[i]);
                    return false;
                }
            }
            long tj = now();
            long sink = 0;
            for (int it = 0; it < ITER; it++) {
                for (int i = 0; i < N; i++) {
                    iOut[i] = ia[i] + ib[i];
                }
                sink += iOut[0];
            }
            long javaIntMs = now() - tj;

            // ---- float mul ----
            float[] fa = simd.allocFloat(N);
            float[] fb = simd.allocFloat(N);
            float[] fOut = simd.allocFloat(N);
            float[] fRef = new float[N];
            for (int i = 0; i < N; i++) {
                fa[i] = (i % 97) * 0.5f - 3f;
                fb[i] = (i % 31) * 0.25f + 1f;
                fRef[i] = fa[i] * fb[i];
            }
            long tnf = now();
            for (int it = 0; it < ITER; it++) {
                simd.mul(fa, fb, fOut, 0, N);
            }
            long nativeFloatMs = now() - tnf;
            // Verify the NATIVE result before the Java loop overwrites fOut.
            for (int i = 0; i < N; i++) {
                if (Math.abs(fOut[i] - fRef[i]) > 0.001f) {
                    fail("native float mul mismatch at " + i + ": " + fOut[i] + " != " + fRef[i]);
                    return false;
                }
            }
            long tjf = now();
            for (int it = 0; it < ITER; it++) {
                for (int i = 0; i < N; i++) {
                    fOut[i] = fa[i] * fb[i];
                }
                sink += (long) fOut[0];
            }
            long javaFloatMs = now() - tjf;

            String intSpeedup = ratio(javaIntMs, nativeIntMs);
            String floatSpeedup = ratio(javaFloatMs, nativeFloatMs);
            Log.p("CN1SS:SIMD:BENCH native=" + native_
                    + " int-add native=" + nativeIntMs + "ms java=" + javaIntMs + "ms speedup="
                    + intSpeedup
                    + " float-mul native=" + nativeFloatMs + "ms java=" + javaFloatMs + "ms speedup="
                    + floatSpeedup
                    + " (sink=" + sink + ")");

            // Emit raw-kernel numbers via the shared CN1SS:STAT: marker (same as
            // BaseTest.emitStat), so the CI capture harness collects them into
            // windows-benchmark-stats.txt alongside Base64NativePerformanceTest's
            // base64 + image metrics and the cn1ss report renders them together.
            Log.p("CN1SS:STAT:SIMD kernel backend: "
                    + (native_ ? "SSE2 (x64) / NEON (arm64) native kernels" : "scalar fallback (no native SIMD)"));
            Log.p("CN1SS:STAT:SIMD int-add (64K x" + ITER + "): java " + javaIntMs
                    + "ms / native " + nativeIntMs + "ms = " + intSpeedup + " speedup");
            Log.p("CN1SS:STAT:SIMD float-mul (64K x" + ITER + "): java " + javaFloatMs
                    + "ms / native " + nativeFloatMs + "ms = " + floatSpeedup + " speedup");
            Log.p("CN1SS:STAT:SIMD kernel correctness: PASS (native result == scalar reference)");

            done();
            return true;
        } catch (Throwable t) {
            fail("SimdBenchmarkTest failed: " + t);
            return false;
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static String ratio(long javaMs, long nativeMs) {
        if (nativeMs <= 0) {
            return "n/a";
        }
        long x10 = javaMs * 10 / nativeMs;
        return (x10 / 10) + "." + (x10 % 10) + "x";
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
