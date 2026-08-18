package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.animation.AnimatedWidget;
import com.codename1.flutter.widgets.FractionalTranslationRenderElement;
import com.codename1.flutter.widgets.Opacity;
import com.codename1.flutter.widgets.Transform;

/**
 * Slides and cross-fades between two pages along a shared axis — the {@code animations}
 * package's {@code SharedAxisTransition}.
 *
 * <p>Horizontal and vertical variants slide by 30% of the page; the scaled (Z) variant
 * grows from 80% instead. In every case the incoming page fades in over the last 70% of the
 * run while the outgoing one fades out over the first 30%, so the two never overlap at full
 * strength.</p>
 */
public class SharedAxisTransition extends AnimatedWidget
        implements FractionalTranslationRenderElement.FractionSource {

    private Animation<Double> animation;
    private Animation<Double> secondaryAnimation;
    private SharedAxisTransitionType transitionType;
    private Color fillColor;
    private Widget child;

    public void animation(Animation<Double> v) {
        this.animation = v;
        listenable(v);
    }

    public void secondaryAnimation(Animation<Double> v) {
        this.secondaryAnimation = v;
    }

    public void transitionType(SharedAxisTransitionType v) {
        this.transitionType = v;
    }

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public SharedAxisTransitionType getTransitionType() {
        return transitionType;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        double in = value(animation, 1);
        double out = value(secondaryAnimation, 0);

        double opacity = FadeScaleTransition.interval(in, 0.3, 1.0)
                * (1 - FadeScaleTransition.interval(out, 0.0, 0.3));

        Opacity layer = new Opacity();
        layer.opacity(opacity);
        if (transitionType == SharedAxisTransitionType.scaled) {
            double scale = 0.80 + 0.20 * in;
            layer.child(Transform.scale(null, Double.valueOf(scale), null, null, null, null,
                    null, null, child));
            return layer;
        }
        // Horizontal/vertical: the slide is a fraction of the page, so it rides the
        // fractional-translation element, which reads the offset at paint time.
        Slide slide = new Slide(this);
        slide.setChild(layer);
        layer.child(child);
        return slide;
    }

    /** The current slide offset, as a fraction of the page. */
    @Override
    public Offset fraction() {
        double in = value(animation, 1);
        double out = value(secondaryAnimation, 0);
        // Incoming slides in from +30%, outgoing continues to -30%.
        double f = (1 - in) * 0.3 - out * 0.3;
        if (transitionType == SharedAxisTransitionType.vertical) {
            return new Offset(0, f);
        }
        return new Offset(f, 0);
    }

    @Override
    public Widget child() {
        return child;
    }

    @Override
    public com.codename1.flutter.foundation.Listenable driver() {
        return animation;
    }

    private static double value(Animation<Double> a, double fallback) {
        return a == null || a.value() == null ? fallback : a.value().doubleValue();
    }

    /**
     * Carries the fraction from the transition to a paint-time translation, wrapping
     * whatever layer the build produced.
     */
    static final class Slide extends Widget
            implements FractionalTranslationRenderElement.FractionSource {

        private final SharedAxisTransition owner;
        private Widget wrapped;

        Slide(SharedAxisTransition owner) {
            this.owner = owner;
        }

        void setChild(Widget w) {
            this.wrapped = w;
        }

        @Override
        public Offset fraction() {
            return owner.fraction();
        }

        @Override
        public Widget child() {
            return wrapped;
        }

        @Override
        public com.codename1.flutter.foundation.Listenable driver() {
            return owner.driver();
        }

        @Override
        public Element createElement() {
            return new FractionalTranslationRenderElement(this);
        }
    }
}
