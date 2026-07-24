package com.codename1.flutter.animation;

/**
 * Animates the opacity of its child from an {@link Animation} — Flutter's
 * {@code FadeTransition}. This pass hosts the child; opacity compositing is
 * deferred.
 */
public class FadeTransition extends AnimatedChildWidget {

    private Animation<Double> opacity;

    public void opacity(Animation<Double> v) {
        this.opacity = v;
    }

    public Animation<Double> getOpacity() {
        return opacity;
    }
}
