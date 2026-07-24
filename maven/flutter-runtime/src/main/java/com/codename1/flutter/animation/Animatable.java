package com.codename1.flutter.animation;

/**
 * A mapping from a {@code double} (typically an animation's 0..1 value) to a
 * value of type {@code T} — Flutter's {@code Animatable<T>}, the supertype of
 * {@link Tween} and {@link CurveTween}.
 */
public abstract class Animatable<T> {

    /** Maps the parametric value {@code t} to a {@code T}. */
    public abstract T transform(double t);

    /** {@code transform(animation.value)}. */
    public T evaluate(Animation<Double> animation) {
        return transform(animation.value());
    }

    /** Returns an {@link Animation} whose value is {@code transform(parent.value)}. */
    public Animation<T> animate(Animation<Double> parent) {
        return new AnimatedEvaluation<T>(parent, this);
    }

    /** Chains this after {@code parent}: {@code transform(parent.transform(t))}. */
    public Animatable<T> chain(Animatable<Double> parent) {
        return new ChainedEvaluation<T>(parent, this);
    }
}
