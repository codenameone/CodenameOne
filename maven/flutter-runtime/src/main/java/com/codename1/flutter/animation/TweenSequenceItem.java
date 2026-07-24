package com.codename1.flutter.animation;

/**
 * One weighted segment of a {@link TweenSequence}: the {@link Animatable} to
 * evaluate over this segment and its relative {@code weight}.
 */
public class TweenSequenceItem<T> {

    private Animatable<T> tween;
    private double weight = 1.0;

    public void tween(Animatable<T> v) {
        this.tween = v;
    }

    public Animatable<T> tween() {
        return tween;
    }

    public void weight(double v) {
        this.weight = v;
    }

    public double weight() {
        return weight;
    }
}
