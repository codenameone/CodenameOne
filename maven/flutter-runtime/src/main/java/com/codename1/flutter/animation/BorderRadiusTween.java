package com.codename1.flutter.animation;

/**
 * A {@link Tween} over {@code BorderRadius} - Flutter's {@code BorderRadiusTween}.
 *
 * <p>A corner that steps is a corner that changes shape in one frame, halfway
 * through whatever motion it was meant to accompany.</p>
 */
public class BorderRadiusTween extends Tween<com.codename1.flutter.BorderRadius> {

    @Override
    public com.codename1.flutter.BorderRadius lerp(double t) {
        return com.codename1.flutter.BorderRadius.lerp(begin(), end(), t);
    }
}
