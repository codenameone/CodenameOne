package com.codename1.flutter.animation;

/**
 * An {@link Animation} that delegates to a swappable inner animation —
 * Flutter's {@code ProxyAnimation}. When {@link #parent(Animation)} is null the
 * proxy holds the last value/status it saw. Reassigning the parent redirects
 * value and status queries to the new animation.
 */
public class ProxyAnimation extends Animation<Double> {

    private Animation<Double> parent;
    private double cachedValue;
    private AnimationStatus cachedStatus = AnimationStatus.dismissed;

    public ProxyAnimation() {
    }

    public ProxyAnimation(Animation<Double> animation) {
        parent(animation);
    }

    public void parent(Animation<Double> v) {
        if (parent != null) {
            cachedValue = value();
            cachedStatus = status();
        }
        this.parent = v;
    }

    public Animation<Double> getParent() {
        return parent;
    }

    @Override
    public Double value() {
        if (parent == null) {
            return cachedValue;
        }
        Double v = parent.value();
        return v == null ? 0.0 : v;
    }

    @Override
    public AnimationStatus status() {
        return parent == null ? cachedStatus : parent.status();
    }
}
