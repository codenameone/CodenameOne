package com.codename1.flutter.animation;

import com.codename1.flutter.Color;

/**
 * A {@link Tween} that interpolates ARGB {@link Color}s channel by channel —
 * Flutter's {@code ColorTween}.
 */
public class ColorTween extends Tween<Color> {

    @Override
    public Color lerp(double t) {
        Color b = begin();
        Color e = end();
        if (b == null && e == null) {
            return null;
        }
        if (b == null) {
            return scaleAlpha(e, t);
        }
        if (e == null) {
            return scaleAlpha(b, 1.0 - t);
        }
        int a = lerpChannel(b.alpha(), e.alpha(), t);
        int r = lerpChannel(b.red(), e.red(), t);
        int g = lerpChannel(b.green(), e.green(), t);
        int bl = lerpChannel(b.blue(), e.blue(), t);
        return new Color((a << 24) | (r << 16) | (g << 8) | bl);
    }

    private static Color scaleAlpha(Color c, double t) {
        int a = lerpChannel(0, c.alpha(), t);
        return new Color((a << 24) | (c.value() & 0xFFFFFF));
    }

    private static int lerpChannel(int a, int b, double t) {
        int v = (int) Math.round(a + (b - a) * t);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
