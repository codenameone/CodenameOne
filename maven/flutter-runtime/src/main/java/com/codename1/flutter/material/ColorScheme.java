package com.codename1.flutter.material;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;

/**
 * A material color scheme. {@link #fromSeed(Color, Brightness)} derives the
 * scheme from a seed color with a simple HSL-based approximation of Material
 * 3 tonal palettes (not the full HCT algorithm — M1 scope). The light scheme
 * (brightness null or {@code light}):
 * <ul>
 *   <li>primary — seed hue/saturation at 40% lightness (tone 40)</li>
 *   <li>onPrimary — white</li>
 *   <li>inversePrimary — seed hue at 80% lightness (tone 80)</li>
 *   <li>secondary — desaturated seed at 45% lightness</li>
 *   <li>surface — near-white tinted with the seed hue (98% lightness)</li>
 *   <li>onSurface — the M3 near-black 0xFF1C1B1F</li>
 * </ul>
 *
 * <p>The dark scheme inverts the tone mapping (an approximation of M3's dark
 * tonal assignments — tone 80 primary on tone 6 surfaces — using HSL
 * lightness in place of HCT tone):</p>
 * <ul>
 *   <li>primary — seed hue at 80% lightness (tone 80)</li>
 *   <li>onPrimary — seed hue at 20% lightness (tone 20)</li>
 *   <li>inversePrimary — seed hue at 40% lightness (tone 40)</li>
 *   <li>secondary — desaturated seed at 70% lightness</li>
 *   <li>surface — near-black tinted with the seed hue (6% lightness)</li>
 *   <li>onSurface — the M3 near-white 0xFFE6E1E5</li>
 * </ul>
 */
public class ColorScheme {

    private final Color primary;
    private final Color inversePrimary;
    private final Color onPrimary;
    private final Color surface;
    private final Color onSurface;
    private final Color secondary;

    public ColorScheme(Color primary, Color inversePrimary, Color onPrimary,
                       Color surface, Color onSurface, Color secondary) {
        this.primary = primary;
        this.inversePrimary = inversePrimary;
        this.onPrimary = onPrimary;
        this.surface = surface;
        this.onSurface = onSurface;
        this.secondary = secondary;
    }

    public static ColorScheme fromSeed(Color seedColor) {
        return fromSeed(seedColor, null);
    }

    /**
     * Canonical two-parameter form: a null brightness means light.
     */
    public static ColorScheme fromSeed(Color seedColor, Brightness brightness) {
        double[] hsl = toHsl(seedColor.value());
        double h = hsl[0];
        double s = hsl[1];
        if (brightness == Brightness.dark) {
            return new ColorScheme(
                    fromHsl(h, Math.min(1, s + 0.15), 0.80),
                    fromHsl(h, s, 0.40),
                    fromHsl(h, s, 0.20),
                    fromHsl(h, Math.min(0.25, s), 0.06),
                    new Color(0xFFE6E1E5),
                    fromHsl(h, s * 0.35, 0.70));
        }
        return new ColorScheme(
                fromHsl(h, s, 0.40),
                fromHsl(h, Math.min(1, s + 0.15), 0.80),
                new Color(0xFFFFFFFF),
                fromHsl(h, Math.min(0.35, s), 0.98),
                new Color(0xFF1C1B1F),
                fromHsl(h, s * 0.35, 0.45));
    }

    public Color primary() {
        return primary;
    }

    public Color inversePrimary() {
        return inversePrimary;
    }

    public Color onPrimary() {
        return onPrimary;
    }

    public Color surface() {
        return surface;
    }

    public Color onSurface() {
        return onSurface;
    }

    public Color secondary() {
        return secondary;
    }

    // ------------------------------------------------------------------
    // HSL helpers
    // ------------------------------------------------------------------

    /**
     * @return {hue (0..360), saturation (0..1), lightness (0..1)}
     */
    static double[] toHsl(int argb) {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2;
        double h;
        double s;
        if (max == min) {
            h = 0;
            s = 0;
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = ((g - b) / d + (g < b ? 6 : 0)) * 60;
            } else if (max == g) {
                h = ((b - r) / d + 2) * 60;
            } else {
                h = ((r - g) / d + 4) * 60;
            }
        }
        return new double[]{h, s, l};
    }

    static Color fromHsl(double h, double s, double l) {
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double hh = (h % 360) / 60;
        double x = c * (1 - Math.abs(hh % 2 - 1));
        double r = 0;
        double g = 0;
        double b = 0;
        if (hh < 1) {
            r = c;
            g = x;
        } else if (hh < 2) {
            r = x;
            g = c;
        } else if (hh < 3) {
            g = c;
            b = x;
        } else if (hh < 4) {
            g = x;
            b = c;
        } else if (hh < 5) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        double m = l - c / 2;
        int ri = (int) Math.round((r + m) * 255);
        int gi = (int) Math.round((g + m) * 255);
        int bi = (int) Math.round((b + m) * 255);
        ri = Math.max(0, Math.min(255, ri));
        gi = Math.max(0, Math.min(255, gi));
        bi = Math.max(0, Math.min(255, bi));
        return new Color(0xFF000000 | (ri << 16) | (gi << 8) | bi);
    }
}
