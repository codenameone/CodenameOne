package com.codename1.generated.flutter;

import com.codename1.flutter.Color;

/**
 * An accent color swatch with a primary value plus the four accent shades
 * (100, 200, 400, 700) — Flutter's {@code MaterialAccentColor}. The colors demo
 * indexes it (Dart's {@code swatch[key]}, transpiled to {@link #idx(long)}) to
 * list every accent shade of a palette.
 *
 * <p>Lives in the transpiler's generated package for the same reason as
 * {@link MaterialColor}. Structural for this milestone: every shade resolves to
 * the primary value.</p>
 */
public class MaterialAccentColor extends Color {

    public MaterialAccentColor(long primary) {
        super(primary);
    }

    /**
     * The shade for {@code key} (Dart's {@code operator []}). Returns the
     * primary value for any shade in this structural milestone.
     */
    public Color idx(long key) {
        return this;
    }

    public Color shade100() {
        return idx(100);
    }

    public Color shade200() {
        return idx(200);
    }

    public Color shade400() {
        return idx(400);
    }

    public Color shade700() {
        return idx(700);
    }
}
