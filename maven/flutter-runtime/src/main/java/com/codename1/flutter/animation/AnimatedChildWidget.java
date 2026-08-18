package com.codename1.flutter.animation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;

/**
 * Shared base for the transition and implicitly-animated widgets that wrap a single
 * {@code child} (FadeTransition, ScaleTransition, AnimatedContainer, ...).
 *
 * <p>It is an {@link AnimatedWidget}, so a subclass that names its driving animation
 * through {@link #listenable(com.codename1.flutter.foundation.Listenable)} is rebuilt on
 * every tick. Subclasses override {@link #build} to wrap the child in the effect they
 * describe — {@code Opacity} for a fade, {@code Transform} for a scale or a rotation — and
 * the default is the child unchanged, which is right for the implicitly-animated widgets
 * that have no Animation of their own.</p>
 *
 * <p>Until now the whole family rendered the child through with no effect at all: a
 * FadeTransition never faded, a ScaleTransition never scaled, and every page transition in
 * the app was a cut.</p>
 */
public abstract class AnimatedChildWidget extends AnimatedWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return getChild();
    }

    /** The animation's current value, or {@code fallback} before it has one. */
    protected static double valueOf(Animation<Double> animation, double fallback) {
        if (animation == null) {
            return fallback;
        }
        Double v = animation.value();
        return v == null ? fallback : v.doubleValue();
    }
}
