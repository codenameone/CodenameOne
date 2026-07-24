package com.codename1.flutter.animation;

/**
 * A {@link Tween} that interpolates integers, rounding toward zero like
 * Flutter's {@code IntTween} ({@code begin + (end - begin) * t}, truncated).
 */
public class IntTween extends Tween<Integer> {

    @Override
    public Integer lerp(double t) {
        Integer b = begin();
        Integer e = end();
        int bi = b == null ? 0 : b.intValue();
        int ei = e == null ? 0 : e.intValue();
        return (int) (bi + (ei - bi) * t);
    }
}
