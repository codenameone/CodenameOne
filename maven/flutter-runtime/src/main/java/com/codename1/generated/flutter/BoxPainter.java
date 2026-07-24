package com.codename1.generated.flutter;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.ImageConfiguration;
import com.codename1.flutter.Offset;

/**
 * The object that paints a {@link com.codename1.flutter.Decoration}, created by
 * {@code Decoration.createBoxPainter} — Flutter's {@code BoxPainter}. The Rally
 * pie-chart outline and the Crane tab indicator subclass it to draw directly on
 * the canvas.
 *
 * <p>Lives in the transpiler's generated package because new_gallery's custom
 * {@code Decoration}s reference it unqualified (the Flutter SDK type carries no
 * {@code @JavaName} mapping); the concrete painters emitted next to it extend
 * this base.</p>
 */
public abstract class BoxPainter {

    /**
     * Paints the decoration onto {@code canvas} at {@code offset} for the box
     * described by {@code configuration} (notably its size).
     */
    public abstract void paint(Canvas canvas, Offset offset, ImageConfiguration configuration);

    /** Releases resources held by this painter (no-op in this milestone). */
    public void dispose() {
    }
}
