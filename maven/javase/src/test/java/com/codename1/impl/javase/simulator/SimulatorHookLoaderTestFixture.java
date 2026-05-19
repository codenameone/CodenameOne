package com.codename1.impl.javase.simulator;

/**
 * Static methods used by {@link SimulatorHookLoaderTest} as action targets.
 * Kept on a separate class so the test can verify reflective resolution works
 * for typical cn1lib-style entry points (a public class with public static
 * void no-arg methods).
 */
public class SimulatorHookLoaderTestFixture {

    public static volatile int alphaCount;
    public static volatile int betaCount;

    public static void alpha() {
        alphaCount++;
    }

    public static void beta() {
        betaCount++;
    }

    /** Not static — used to verify the loader rejects non-static methods. */
    public void instanceOnly() {
        // intentionally empty
    }

    static void resetCounters() {
        alphaCount = 0;
        betaCount = 0;
    }
}
