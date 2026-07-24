package com.codename1.flutter.painting;

import com.codename1.flutter.Canvas;
import com.codename1.flutter.ImageConfiguration;
import com.codename1.flutter.Offset;

/**
 * The object a {@code Decoration} produces to paint itself — Flutter's
 * {@code BoxPainter}. A decoration returns one from {@code createBoxPainter};
 * the render layer calls {@link #paint} with the {@link Canvas}, the top-left
 * {@link Offset} of the box and an {@link ImageConfiguration} carrying the box
 * size. The tab-indicator and Rally pie-chart decorations subclass this to draw
 * custom borders. The optional repaint {@code onChanged} callback is captured by
 * the decoration; {@link #dispose()} releases any held resources.
 */
public abstract class BoxPainter {

    /**
     * Paints the decoration onto {@code canvas}. The box occupies the rectangle
     * whose top-left is {@code offset} and whose size is
     * {@code configuration.size}.
     */
    public abstract void paint(Canvas canvas, Offset offset, ImageConfiguration configuration);

    /** Releases resources held by this painter. */
    public void dispose() {
    }
}
