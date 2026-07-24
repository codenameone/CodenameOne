package com.codename1.flutter.animation;

/**
 * An {@link Animation} that is permanently stopped at a single value —
 * Flutter's {@code AlwaysStoppedAnimation<T>}. Its listeners never fire.
 */
public class AlwaysStoppedAnimation<T> extends Animation<T> {

    private final T value;

    public AlwaysStoppedAnimation(T value) {
        this.value = value;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public AnimationStatus status() {
        return AnimationStatus.forward;
    }
}
