/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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

    /// The pattern's three curves: arrive, leave, and the one both scales run on.
    private static final com.codename1.flutter.animation.Cubic DECELERATE =
            new com.codename1.flutter.animation.Cubic(0.0, 0.0, 0.2, 1.0);
    private static final com.codename1.flutter.animation.Cubic ACCELERATE =
            new com.codename1.flutter.animation.Cubic(0.4, 0.0, 1.0, 1.0);
    private static final com.codename1.flutter.animation.Cubic STANDARD =
            new com.codename1.flutter.animation.Cubic(0.4, 0.0, 0.2, 1.0);

    @Override
    public Widget build(BuildContext context) {
        double in = value(animation, 1);
        double out = value(secondaryAnimation, 0);

        // Every leg is EASED, and on three different curves: the incoming content
        // decelerates in, the outgoing accelerates out, and both scales run on the
        // standard curve. Running them linearly -- which this did -- is most of what
        // makes a shared axis read as a cut with a dissolve bolted on rather than as one
        // surface handing over to another.
        double opacity = DECELERATE.transform(
                        FadeScaleTransition.interval(in, 0.3, 1.0))
                * (1 - ACCELERATE.transform(
                        FadeScaleTransition.interval(out, 0.0, 0.3)));

        Opacity layer = new Opacity();
        layer.opacity(opacity);
        if (transitionType == SharedAxisTransitionType.scaled) {
            // The child on its way OUT keeps growing, to 110%; only the one arriving
            // comes up from 80%. Ours held the outgoing child at 100% for the whole
            // run, so the surface being replaced simply faded where it should have
            // continued through the screen.
            double scale = out > 0
                    ? 1.00 + 0.10 * STANDARD.transform(out)
                    : 0.80 + 0.20 * STANDARD.transform(in);
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
