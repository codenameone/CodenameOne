package com.codename1.generated.flutter;

/**
 * Base class for the shapes that draw the pieces of a {@code Slider} (thumb,
 * value indicator, overlay, tick marks) — Flutter's {@code SliderComponentShape}.
 * The sliders demo subclasses it with a custom thumb and value-indicator shape.
 *
 * <p>Lives in the transpiler's generated package because the demo references it
 * unqualified (the Flutter SDK type carries no {@code @JavaName} mapping). The
 * concrete {@code getPreferredSize} / {@code paint} overrides are supplied by
 * the generated subclasses; kept as an open base so their exact (Animation /
 * TextPainter / RenderBox / SliderThemeData) signatures compile without the
 * base pinning them.</p>
 */
public abstract class SliderComponentShape {
}
