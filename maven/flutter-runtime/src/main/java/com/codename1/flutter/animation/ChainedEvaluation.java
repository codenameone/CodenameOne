package com.codename1.flutter.animation;

/**
 * The {@link Animatable} produced by {@code evaluatable.chain(parent)}:
 * {@code transform(t) == evaluatable.transform(parent.transform(t))}. Used to
 * compose a {@link CurveTween} in front of another tween.
 */
public class ChainedEvaluation<T> extends Animatable<T> {

    private final Animatable<Double> parent;
    private final Animatable<T> evaluatable;

    public ChainedEvaluation(Animatable<Double> parent, Animatable<T> evaluatable) {
        this.parent = parent;
        this.evaluatable = evaluatable;
    }

    @Override
    public T transform(double t) {
        return evaluatable.transform(parent.transform(t));
    }
}
