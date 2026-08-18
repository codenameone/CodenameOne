package com.codename1.flutter.animation;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Opacity;

/**
 * Animates the opacity of its child from an {@link Animation} — Flutter's
 * {@code FadeTransition}. The child is wrapped in an {@link Opacity}, which composites the
 * whole subtree as one layer, so overlapping children fade together rather than each
 * showing through the others.
 */
public class FadeTransition extends AnimatedChildWidget {

    private Animation<Double> opacity;

    public void opacity(Animation<Double> v) {
        this.opacity = v;
        listenable(v);
    }

    public Animation<Double> getOpacity() {
        return opacity;
    }

    @Override
    public Widget build(BuildContext context) {
        Opacity o = new Opacity();
        o.opacity(valueOf(opacity, 1.0));
        o.child(getChild());
        return o;
    }
}
