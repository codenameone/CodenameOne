package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Localized unit tests for the density / convertToPixels math that backs
/// HTML5Implementation.getDeviceDensity() and HTML5Implementation.convertToPixels().
///
/// Compiles JavaScriptDisplayMetrics.java in isolation (no @JSBody natives, no CN1
/// UI deps) and drives it via reflection. If any of these fail, the port is
/// picking the wrong density / wrong ppi and Component padding / Switch track
/// width will be off by a constant multiplier. If they all pass, the huge-pill
/// Switch rendering we see in the Kotlin screenshot is not caused by the
/// density ladder - look upstream (Style padding, BoxLayout stretch, etc).
class JavaScriptDisplayMetricsTest {
    private static final Path METRICS_SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java",
            "com", "codename1", "impl", "html5", "JavaScriptDisplayMetrics.java");

    @Test
    void densityLadderMatchesCn1ConstantsDensityRungs() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Method pickDensity = metrics.getMethod("pickDensity", double.class, loadFormFactorEnum(metrics), int.class);

        // CN1Constants.DENSITY_* rungs - keep in lockstep with CodenameOne/src/com/codename1/ui/CN1Constants.java
        assertEquals(30, metrics.getField("DENSITY_MEDIUM").getInt(null));
        assertEquals(50, metrics.getField("DENSITY_VERY_HIGH").getInt(null));
        assertEquals(60, metrics.getField("DENSITY_HD").getInt(null));
        assertEquals(65, metrics.getField("DENSITY_560").getInt(null));
        assertEquals(70, metrics.getField("DENSITY_2HD").getInt(null));
        assertEquals(80, metrics.getField("DENSITY_4K").getInt(null));
    }

    @Test
    void desktopAtDpr1PicksMedium() throws Exception {
        // Headless Chromium runs the JS port with DPR=1 by default. The
        // screenshot suite therefore should resolve to DENSITY_MEDIUM on
        // desktop. If this test fails the port is selecting HD / VERY_HIGH /
        // HD and multiplying every mm padding by 1.5-3.4x, which explains
        // enormous Switch pills and over-padded Sheet content.
        Class<?> metrics = loadMetricsClass();
        Object desktop = enumValue(metrics, "DESKTOP");
        Method pickDensity = metrics.getMethod("pickDensity", double.class, loadFormFactorEnum(metrics), int.class);

        assertEquals(30, pickDensity.invoke(null, 1.0, desktop, 0),
                "Desktop at DPR=1 should resolve to DENSITY_MEDIUM (30)");
        assertEquals(30, pickDensity.invoke(null, 1.5, desktop, 0),
                "Desktop at DPR=1.5 should still resolve to DENSITY_MEDIUM");
        assertEquals(30, pickDensity.invoke(null, 1.89, desktop, 0),
                "Desktop just below 1.9 cutoff should stay MEDIUM");
    }

    @Test
    void desktopAtRetinaPicksVeryHighThenHd() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Object desktop = enumValue(metrics, "DESKTOP");
        Method pickDensity = metrics.getMethod("pickDensity", double.class, loadFormFactorEnum(metrics), int.class);

        assertEquals(50, pickDensity.invoke(null, 1.9, desktop, 0));
        assertEquals(50, pickDensity.invoke(null, 2.0, desktop, 0));
        assertEquals(50, pickDensity.invoke(null, 2.89, desktop, 0));
        assertEquals(60, pickDensity.invoke(null, 2.9, desktop, 0));
        assertEquals(60, pickDensity.invoke(null, 3.5, desktop, 0));
    }

    @Test
    void phoneLadderFiresAtExpectedBoundaries() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Object phone = enumValue(metrics, "PHONE");
        Method pickDensity = metrics.getMethod("pickDensity", double.class, loadFormFactorEnum(metrics), int.class);

        assertEquals(30, pickDensity.invoke(null, 1.0, phone, 0));
        assertEquals(50, pickDensity.invoke(null, 2.0, phone, 0));
        assertEquals(60, pickDensity.invoke(null, 2.5, phone, 0));
        assertEquals(65, pickDensity.invoke(null, 4.0, phone, 0));
        assertEquals(70, pickDensity.invoke(null, 5.0, phone, 0));
        assertEquals(80, pickDensity.invoke(null, 7.0, phone, 0));
    }

    @Test
    void tabletLadderMatchesProductionThresholds() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Object tablet = enumValue(metrics, "TABLET");
        Method pickDensity = metrics.getMethod("pickDensity", double.class, loadFormFactorEnum(metrics), int.class);

        assertEquals(30, pickDensity.invoke(null, 1.0, tablet, 0));
        assertEquals(30, pickDensity.invoke(null, 1.89, tablet, 0));
        assertEquals(50, pickDensity.invoke(null, 1.9, tablet, 0));
        assertEquals(50, pickDensity.invoke(null, 4.0, tablet, 0));
    }

    @Test
    void overrideDensityShortCircuitsLadder() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Object desktop = enumValue(metrics, "DESKTOP");
        Method pickDensity = metrics.getMethod("pickDensity", double.class, loadFormFactorEnum(metrics), int.class);

        // A positive override always wins, matching the density= URL query override
        // the live port honors before touching devicePixelRatio.
        assertEquals(40, pickDensity.invoke(null, 99.0, desktop, 40));
        assertEquals(80, pickDensity.invoke(null, 1.0, desktop, 80));
        assertEquals(30, pickDensity.invoke(null, 1.0, desktop, 0),
                "override=0 should fall through to the ladder");
        assertEquals(30, pickDensity.invoke(null, 1.0, desktop, -1),
                "negative override should fall through (matches getDensityOverride() returning 0 on absent query arg)");
    }

    @Test
    void convertToPixelsMatchesPpiLadderAtMediumDensity() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Method convertToPixels = metrics.getMethod("convertToPixels", int.class, int.class);
        Method ppm = metrics.getMethod("pixelsPerMillimeter", int.class);

        double mediumPpm = (Double) ppm.invoke(null, 30);
        assertEqualsWithinHalf(mediumPpm * 10, 63.0);

        // 10mm (1cm) at MEDIUM density -> ~63 CSS pixels. This is the baseline
        // and the assumption Switch.calcPreferredSize relies on implicitly
        // through getStyle().getHorizontalPadding() / getVerticalPadding().
        assertEquals(63, ((Integer) convertToPixels.invoke(null, 10, 30)).intValue(),
                "10mm at MEDIUM (160 DPI) should convert to ~63 px");

        // 3mm padding - the typical CN1 default edge inset around components -
        // must land in the ballpark that the screenshot suite expects. If this
        // jumps, Switch / Sheet / Picker padding explodes.
        assertEquals(19, ((Integer) convertToPixels.invoke(null, 3, 30)).intValue(),
                "3mm at MEDIUM should convert to ~19 px");
    }

    @Test
    void convertToPixelsScalesMonotonicallyAcrossDensityLadder() throws Exception {
        Class<?> metrics = loadMetricsClass();
        Method convertToPixels = metrics.getMethod("convertToPixels", int.class, int.class);

        int[] rungs = {10, 20, 30, 40, 50, 60, 65, 70, 80};
        int previous = 0;
        for (int rung : rungs) {
            int px = (Integer) convertToPixels.invoke(null, 10, rung);
            assertTrue(px > previous,
                    "convertToPixels(10mm, density=" + rung + ")=" + px
                            + " should be strictly greater than previous rung's " + previous);
            previous = px;
        }

        // Unknown rung falls through to MEDIUM.
        assertEquals(63, ((Integer) convertToPixels.invoke(null, 10, 9999)).intValue(),
                "Unknown density should fall back to MEDIUM ppi");
    }

    @Test
    void observedKotlinSwitchPillSizeBacktracksToRoughlyHdOr4kDensity() throws Exception {
        // The Kotlin screenshot shows switches ~600 px wide, ~300 px tall.
        // Switch.calcPreferredSize = (horizontalPadding + fontSize*trackScaleX=3)
        //                          x (verticalPadding + fontSize*thumbScaleY=1.5)
        //
        // For fontSize=16 and trackScaleX=3, trackWidth=48 px. That leaves
        // ~552 px of horizontal padding (left+right) to reach 600 px, i.e.
        // ~276 px per side. This backtrack tells us *which* density + mm
        // padding combo could have produced the observed pill:
        //
        //   MEDIUM  ppm=6.3  -> 276 px = 43 mm per side (clearly wrong)
        //   HD      ppm=21.3 -> 276 px = 13 mm per side (implausible theme value)
        //   560     ppm=29.5 ->  9.4 mm per side (still unusual)
        //   4K      ppm=50.4 ->  5.5 mm per side (plausible theme value!)
        //
        // So if the live port renders 600 px pills with fontSize=16 and a
        // standard ~5mm padding, it must be running at something close to
        // 4K density. In our desktop-at-DPR=1 unit tests MEDIUM is selected,
        // so the discrepancy has to come from a different DPR reading at
        // runtime (likely overridePixelRatio, window.devicePixelRatio, or
        // the worker-bridged window proxy returning something unexpected).
        Class<?> metrics = loadMetricsClass();
        Method convertToPixels = metrics.getMethod("convertToPixels", int.class, int.class);

        int trackWidth = 48; // fontSize=16 * trackScaleX=3
        int observedPillWidth = 600;
        int paddingBudgetPerSide = (observedPillWidth - trackWidth) / 2; // 276

        int[] densitiesToProbe = {30, 50, 60, 65, 70, 80};
        int matchingDensity = -1;
        int matchingMm = -1;
        for (int density : densitiesToProbe) {
            for (int mm = 1; mm <= 15; mm++) {
                int px = (Integer) convertToPixels.invoke(null, mm, density);
                if (Math.abs(px - paddingBudgetPerSide) <= 15) {
                    matchingDensity = density;
                    matchingMm = mm;
                    break;
                }
            }
            if (matchingDensity != -1) {
                break;
            }
        }

        // We expect a match at a density/mm combo. If this fails, either the
        // observed 600 px measurement was wrong, or the pill is not driven
        // by the convertToPixels path at all (which would in itself be a
        // useful finding - look at image scaling or DPR-double-apply).
        assertTrue(matchingDensity != -1,
                "No density/mm combo reproduces a 276 px per-side padding - the huge-pill bug "
                        + "likely isn't coming from Style padding * convertToPixels at all. "
                        + "Investigate image scaling or a DPR multiplier applied twice.");
    }

    @Test
    void switchPreferredSizeAtMediumDensityStaysInHumanRange() throws Exception {
        // Reconstruct the Switch.calcPreferredSize() formula with known inputs
        // (fontSize=16, thumbScaleY=1.5, trackScaleX=3, padding=3mm). If this
        // produces a ~60x30 result, huge-pill rendering in the Kotlin screenshot
        // has to come from somewhere other than the density/ppi math - most likely
        // Component bounds exceeding preferredSize (BoxLayout.encloseX stretch)
        // or a theme overriding the Switch padding to mm values much larger than 3.
        Class<?> metrics = loadMetricsClass();
        Method convertToPixels = metrics.getMethod("convertToPixels", int.class, int.class);

        int fontSize = 16;
        int trackWidth = (int) (fontSize * 3);
        int trackHeight = (int) (fontSize * 0.9);
        int thumbHeight = (int) (fontSize * 1.5);
        int hPadMm = 3;
        int vPadMm = 3;
        int hPadPx = ((Integer) convertToPixels.invoke(null, hPadMm * 2, 30)).intValue(); // left+right
        int vPadPx = ((Integer) convertToPixels.invoke(null, vPadMm * 2, 30)).intValue(); // top+bottom

        int preferredWidth = hPadPx + trackWidth;
        int preferredHeight = vPadPx + Math.max(thumbHeight, trackHeight);

        // Sanity: not zero, not absurd. The Kotlin screenshot shows ~600 px wide
        // switches, so if the density+formula stays in a "tens-to-low-hundreds"
        // range we know the bug is elsewhere.
        assertTrue(preferredWidth > 40 && preferredWidth < 250,
                "preferredWidth at MEDIUM should land in 40-250 px range, got " + preferredWidth);
        assertTrue(preferredHeight > 20 && preferredHeight < 150,
                "preferredHeight at MEDIUM should land in 20-150 px range, got " + preferredHeight);
    }

    // ---- helpers ----

    private static Class<?> loadFormFactorEnum(Class<?> metrics) {
        for (Class<?> c : metrics.getDeclaredClasses()) {
            if (c.getSimpleName().equals("FormFactor")) {
                return c;
            }
        }
        throw new IllegalStateException("FormFactor enum not found on JavaScriptDisplayMetrics");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> metrics, String name) {
        Class<?> ff = loadFormFactorEnum(metrics);
        return Enum.valueOf((Class<Enum>) ff, name);
    }

    private static void assertEqualsWithinHalf(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.5) {
            throw new AssertionError("Expected " + expected + " ~ " + actual);
        }
    }

    private static Class<?> loadMetricsClass() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-density-metrics-src");
        Path classesDir = Files.createTempDirectory("js-density-metrics-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(METRICS_SOURCE, packageDir.resolve("JavaScriptDisplayMetrics.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptDisplayMetrics.java").toString()
        ));
        assertEquals(0, compileResult, "JavaScriptDisplayMetrics should compile standalone (no @JSBody / UI deps)");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        return loader.loadClass("com.codename1.impl.html5.JavaScriptDisplayMetrics");
    }
}
