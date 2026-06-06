package com.codenameone.examples.hellocodenameone.tests;

/**
 * End-to-end coverage for {@code System.nanoTime()} (codenameone/CodenameOne#3076).
 *
 * The high-resolution timer is backed by a different platform clock on every
 * port -- the real JDK on the simulator, the Android runtime on Android,
 * {@code performance.now()} on JavaScript and a native monotonic clock
 * ({@code clock_gettime(CLOCK_MONOTONIC)} / {@code QueryPerformanceCounter}) on
 * the ParparVM iOS / Windows targets. The CLDC11 API surface only ships a
 * 0-returning stub, so a port that forgets to wire up the real implementation
 * would silently return a constant. Running the assertions on the device is
 * what catches that: a constant clock fails the "advances" and "monotonic"
 * checks below.
 */
public class NanoTimeApiTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            // Back-to-back readings must never go backwards (monotonic clock).
            long previous = System.nanoTime();
            for (int iter = 0; iter < 10000; iter++) {
                long current = System.nanoTime();
                assertTrue(current >= previous,
                        "System.nanoTime() went backwards: " + previous + " -> " + current);
                previous = current;
            }

            // Spin for a known wall-clock window using the millisecond clock,
            // then confirm the nanosecond clock measured a matching, non-zero
            // interval. A stubbed-out port returning a constant fails here.
            long nanoStart = System.nanoTime();
            long millisStart = System.currentTimeMillis();
            while (System.currentTimeMillis() - millisStart < 50L) {
                // busy wait -- avoids depending on Thread.sleep precision
            }
            long elapsedMillis = System.currentTimeMillis() - millisStart;
            long elapsedNanos = System.nanoTime() - nanoStart;

            assertTrue(elapsedNanos > 0L,
                    "System.nanoTime() did not advance across a ~50ms window (got " + elapsedNanos + "ns)");

            // The two clocks should roughly agree. Use very loose bounds so the
            // assertion survives CI jitter / scheduling but still rejects a
            // wrong unit (e.g. returning micros or millis instead of nanos).
            long lowerBoundNanos = (elapsedMillis - 40L) * 1000000L;
            long upperBoundNanos = (elapsedMillis + 5000L) * 1000000L;
            assertTrue(elapsedNanos >= lowerBoundNanos,
                    "System.nanoTime() interval too small: " + elapsedNanos + "ns for " + elapsedMillis + "ms");
            assertTrue(elapsedNanos <= upperBoundNanos,
                    "System.nanoTime() interval too large: " + elapsedNanos + "ns for " + elapsedMillis + "ms");
        } catch (Throwable t) {
            fail("nanoTime API test failed: " + t);
            return false;
        }
        done();
        return true;
    }
}
