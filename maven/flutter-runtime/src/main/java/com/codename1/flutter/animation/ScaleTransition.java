package com.codename1.flutter.animation;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Transform;

/**
 * Scales its child about its centre from an {@link Animation} — Flutter's
 * {@code ScaleTransition}.
 */
public class ScaleTransition extends AnimatedChildWidget {

    private Animation<Double> scale;
    private Alignment alignment;

    public void scale(Animation<Double> v) {
        this.scale = v;
        listenable(v);
    }

    public void alignment(Alignment v) {
        this.alignment = v;
    }

    public Animation<Double> getScale() {
        return scale;
    }

    @Override
    public Widget build(BuildContext context) {
        return Transform.scale(null, Double.valueOf(valueOf(scale, 1.0)),
                null, null, null, alignment, null, null, getChild());
    }
}
