package com.codename1.flutter.animation;

/**
 * Runs a parent animation in reverse — Flutter's {@code ReverseAnimation}. Its
 * {@link #value()} is {@code 1 - parent.value()} and its status is the parent's
 * status with {@code forward}/{@code reverse} swapped. The backdrop title uses
 * it to fade its front-layer title out as the parent reveal animates in.
 */
public class ReverseAnimation extends Animation<Double> {

    private final Animation<Double> parent;

    public ReverseAnimation(Animation<Double> parent) {
        this.parent = parent;
    }

    public Animation<Double> getParent() {
        return parent;
    }

    @Override
    public Double value() {
        double v = parent == null || parent.value() == null ? 0.0 : parent.value();
        return 1.0 - v;
    }

    @Override
    public AnimationStatus status() {
        if (parent == null) {
            return AnimationStatus.dismissed;
        }
        switch (parent.status()) {
            case forward:
                return AnimationStatus.reverse;
            case reverse:
                return AnimationStatus.forward;
            case completed:
                return AnimationStatus.dismissed;
            case dismissed:
                return AnimationStatus.completed;
            default:
                return parent.status();
        }
    }
}
