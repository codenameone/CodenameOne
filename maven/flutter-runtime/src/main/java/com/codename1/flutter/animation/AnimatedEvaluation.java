package com.codename1.flutter.animation;

import dart.runtime.Funcs;

/**
 * The {@link Animation} produced by {@code animatable.animate(parent)}: its
 * value is {@code animatable.transform(parent.value)} and it forwards status
 * and listener registration straight to {@code parent}.
 */
public class AnimatedEvaluation<T> extends Animation<T> {

    private final Animation<Double> parent;
    private final Animatable<T> evaluatable;

    public AnimatedEvaluation(Animation<Double> parent, Animatable<T> evaluatable) {
        this.parent = parent;
        this.evaluatable = evaluatable;
    }

    @Override
    public T value() {
        return evaluatable.transform(parent.value());
    }

    @Override
    public AnimationStatus status() {
        return parent.status();
    }

    @Override
    public void addListener(Funcs.VoidFunc0 listener) {
        parent.addListener(listener);
    }

    @Override
    public void removeListener(Funcs.VoidFunc0 listener) {
        parent.removeListener(listener);
    }

    @Override
    public void addStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        parent.addStatusListener(listener);
    }

    @Override
    public void removeStatusListener(Funcs.VoidFunc1<AnimationStatus> listener) {
        parent.removeStatusListener(listener);
    }
}
