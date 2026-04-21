/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

/// Pure-Java extract of the density-selection and convertToPixels math used by
/// HTML5Implementation.getDeviceDensity() and HTML5Implementation.convertToPixels().
///
/// This class has no @JSBody natives and no CN1 UI dependencies, so it can be
/// compiled and tested standalone under plain JUnit. HTML5Implementation delegates
/// to it so that the live port and unit tests exercise the same code path.
///
/// Keep the arithmetic identical to the production code - the unit tests compare
/// the result of these helpers against the CN1Constants.DENSITY_* ladder and the
/// ppi ladder the production code documents inline.
public final class JavaScriptDisplayMetrics {

    public static final int DENSITY_VERY_LOW = 10;
    public static final int DENSITY_LOW = 20;
    public static final int DENSITY_MEDIUM = 30;
    public static final int DENSITY_HIGH = 40;
    public static final int DENSITY_VERY_HIGH = 50;
    public static final int DENSITY_HD = 60;
    public static final int DENSITY_560 = 65;
    public static final int DENSITY_2HD = 70;
    public static final int DENSITY_4K = 80;

    public enum FormFactor {
        PHONE,
        TABLET,
        DESKTOP
    }

    private JavaScriptDisplayMetrics() {
    }

    /// Mirrors the ladder in HTML5Implementation.getDeviceDensity(): pick a
    /// CN1Constants.DENSITY_* value from the devicePixelRatio and the form
    /// factor. An overrideDensity > 0 short-circuits and is returned verbatim
    /// (matches the `density` URL query-string override).
    public static int pickDensity(double ratio, FormFactor formFactor, int overrideDensity) {
        if (overrideDensity > 0) {
            return overrideDensity;
        }
        if (formFactor == FormFactor.PHONE) {
            if (ratio < 2) {
                return DENSITY_MEDIUM;
            } else if (ratio < 2.5) {
                return DENSITY_VERY_HIGH;
            } else if (ratio < 4) {
                return DENSITY_HD;
            } else if (ratio < 5) {
                return DENSITY_560;
            } else if (ratio < 7) {
                return DENSITY_2HD;
            } else {
                return DENSITY_4K;
            }
        }
        if (formFactor == FormFactor.TABLET) {
            if (ratio < 1.9) {
                return DENSITY_MEDIUM;
            }
            return DENSITY_VERY_HIGH;
        }
        // Desktop
        if (ratio < 1.9) {
            return DENSITY_MEDIUM;
        } else if (ratio < 2.9) {
            return DENSITY_VERY_HIGH;
        } else {
            return DENSITY_HD;
        }
    }

    /// Returns the CSS pixels-per-mm for a CN1Constants.DENSITY_* value. This
    /// is the ppi / 25.4 conversion that HTML5Implementation.convertToPixels
    /// uses, pulled out so unit tests can assert the ratio directly without
    /// standing up the whole port.
    public static double pixelsPerMillimeter(int density) {
        switch (density) {
            case DENSITY_VERY_LOW:
                return 72.0 / 25.4;
            case DENSITY_LOW:
                return 120.0 / 25.4;
            case DENSITY_MEDIUM:
                return 160.0 / 25.4;
            case DENSITY_HIGH:
                return 240.0 / 25.4;
            case DENSITY_VERY_HIGH:
                return 320.0 / 25.4;
            case DENSITY_HD:
                return 540.0 / 25.4;
            case DENSITY_560:
                return 750.0 / 25.4;
            case DENSITY_2HD:
                return 1080.0 / 25.4;
            case DENSITY_4K:
                return 1280.0 / 25.4;
            default:
                return 160.0 / 25.4;
        }
    }

    /// Converts a dipCount (millimeters in CN1 API terms) to CSS pixels for
    /// the given density. Matches HTML5Implementation.convertToPixels(int, boolean)
    /// which ignores the horizontal flag.
    public static int convertToPixels(int dipCount, int density) {
        double ppm = pixelsPerMillimeter(density);
        return (int) Math.round(((float) dipCount) * ppm);
    }
}
