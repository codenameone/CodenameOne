package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;

/**
 * Animates the scale of its child from an {@link Animation} — Flutter's
 * {@code ScaleTransition}. This pass hosts the child; the scale transform is
 * deferred.
 */
public class ScaleTransition extends AnimatedChildWidget {

    private Animation<Double> scale;
    private Alignment alignment;

    public void scale(Animation<Double> v) {
        this.scale = v;
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public Animation<Double> getScale() {
        return scale;
    }
}
