package com.codename1.flutter.animation;

/**
 * A {@link Tween} over {@code Matrix4} - Flutter's {@code Matrix4Tween}.
 *
 * <p>A matrix that steps teleports its subtree, which is the whole of what the
 * animation was for.</p>
 */
public class Matrix4Tween extends Tween<com.codename1.flutter.vectormath.Matrix4> {

    @Override
    public com.codename1.flutter.vectormath.Matrix4 lerp(double t) {
        return com.codename1.flutter.vectormath.Matrix4.lerp(begin(), end(), t);
    }
}
