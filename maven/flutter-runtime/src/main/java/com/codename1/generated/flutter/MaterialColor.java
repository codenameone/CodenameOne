package com.codename1.generated.flutter;

import com.codename1.flutter.Color;

/**
 * A color swatch with a primary value plus ten indexed shades (50, 100..900) —
 * Flutter's {@code MaterialColor}. The colors demo indexes it (Dart's
 * {@code swatch[key]}, transpiled to {@link #idx(long)}) to list every shade of
 * a palette.
 *
 * <p>Lives in the transpiler's generated package because new_gallery references
 * it unqualified (the Flutter SDK type carries no {@code @JavaName} mapping) and
 * {@code _Palette} names it in the same package. Structural for this milestone:
 * every shade resolves to the primary value; a later milestone can carry the
 * real per-shade swatch.</p>
 */
public class MaterialColor extends Color {

    public MaterialColor(long primary) {
        super(primary);
    }

    /**
     * The shade for {@code key} (Dart's {@code operator []}). Returns the
     * primary value for any shade in this structural milestone.
     */
    public Color idx(long key) {
        return this;
    }

    public Color shade50() {
        return idx(50);
    }

    public Color shade100() {
        return idx(100);
    }

    public Color shade200() {
        return idx(200);
    }

    public Color shade300() {
        return idx(300);
    }

    public Color shade400() {
        return idx(400);
    }

    public Color shade500() {
        return idx(500);
    }

    public Color shade600() {
        return idx(600);
    }

    public Color shade700() {
        return idx(700);
    }

    public Color shade800() {
        return idx(800);
    }

    public Color shade900() {
        return idx(900);
    }
}
