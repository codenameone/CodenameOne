package com.codename1.flutter.animation;

/**
 * A {@link Tween} over {@code RelativeRect} - Flutter's {@code RelativeRectTween}.
 *
 * <p>The gallery's settings panel is animated by one of these: it begins a full
 * screen-height above the viewport and slides down over 80ms. Stepping meant it
 * hung off-screen and then appeared already in place, which is a menu that pops
 * rather than one that slides.</p>
 */
public class RelativeRectTween extends Tween<com.codename1.flutter.RelativeRect> {

    @Override
    public com.codename1.flutter.RelativeRect lerp(double t) {
        return com.codename1.flutter.RelativeRect.lerp(begin(), end(), t);
    }
}
