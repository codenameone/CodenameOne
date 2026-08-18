package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.animation.AnimatedWidget;
import com.codename1.flutter.widgets.Opacity;
import com.codename1.flutter.widgets.Transform;

/**
 * Fades and scales its child in and out for modal reveals — the {@code animations}
 * package's {@code FadeScaleTransition}.
 *
 * <p>Follows the package's own curves: the fade runs over the first 30% of the animation
 * and the scale grows from 80% to full over the first 40%, so the child arrives already
 * visible and settles rather than popping in at the end.</p>
 */
public class FadeScaleTransition extends AnimatedWidget {

    private Animation<Double> animation;
    private Widget child;

    public void animation(Animation<Double> v) {
        this.animation = v;
        listenable(v);
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Animation<Double> getAnimation() {
        return animation;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        double t = 1;
        if (animation != null && animation.value() != null) {
            t = animation.value().doubleValue();
        }
        double fade = interval(t, 0.0, 0.3);
        double scale = 0.80 + 0.20 * interval(t, 0.0, 0.4);

        Opacity fadeLayer = new Opacity();
        fadeLayer.opacity(fade);
        fadeLayer.child(Transform.scale(null, Double.valueOf(scale), null, null, null, null,
                null, null, child));
        return fadeLayer;
    }

    /** {@code t} remapped onto [begin, end] and clamped — Flutter's Interval curve. */
    static double interval(double t, double begin, double end) {
        if (end <= begin) {
            return t >= end ? 1 : 0;
        }
        double v = (t - begin) / (end - begin);
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
