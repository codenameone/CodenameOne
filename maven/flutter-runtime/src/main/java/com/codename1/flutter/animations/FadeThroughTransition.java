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
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.animation.AnimatedWidget;
import com.codename1.flutter.widgets.Opacity;
import com.codename1.flutter.widgets.Transform;

/**
 * Fades the outgoing child out, then the incoming one in while it grows slightly — the
 * Material "fade through" motion, from the {@code animations} package.
 *
 * <p>The two halves do not overlap, which is the whole point of the pattern: the incoming
 * content waits until the outgoing has gone rather than cross-dissolving with it. The
 * secondary animation drives the outgoing half.</p>
 */
public class FadeThroughTransition extends AnimatedWidget {

    private Animation<Double> animation;
    private Animation<Double> secondaryAnimation;
    private Color fillColor;
    private Widget child;

    public void animation(Animation<Double> v) {
        this.animation = v;
        listenable(v);
    }

    public void secondaryAnimation(Animation<Double> v) {
        this.secondaryAnimation = v;
    }

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    /// The pattern's own curves: it leaves on one and arrives on the other.
    private static final com.codename1.flutter.animation.Cubic IN_CURVE =
            new com.codename1.flutter.animation.Cubic(0.0, 0.0, 0.2, 1.0);
    private static final com.codename1.flutter.animation.Cubic OUT_CURVE =
            new com.codename1.flutter.animation.Cubic(0.4, 0.0, 1.0, 1.0);

    @Override
    public Widget build(BuildContext context) {
        double in = value(animation, 1);
        double out = value(secondaryAnimation, 0);

        // Incoming: nothing for the first 30%, then fade up while scaling 92% -> 100%.
        // Outgoing: fade away over that first 30%.
        //
        // Both eased, and on DIFFERENT curves -- the pattern leaves fast and arrives
        // slow, and running either leg linearly is most of what makes it read as a cut
        // rather than as a dissolve. The old page is the one you actually watch: it
        // carries the whole screen for the first 30% while the new one is still
        // invisible, and linearly it is only 44% gone a third of the way in where the
        // curve has it at 58%.
        double enter = IN_CURVE.transform(
                FadeScaleTransition.interval(in, 0.3, 1.0));
        double opacity = enter;
        double scale = 0.92 + 0.08 * enter;
        opacity *= 1 - OUT_CURVE.transform(
                FadeScaleTransition.interval(out, 0.0, 0.3));

        Opacity layer = new Opacity();
        layer.opacity(opacity);
        layer.child(Transform.scale(null, Double.valueOf(scale), null, null, null, null,
                null, null, child));
        return layer;
    }

    private static double value(Animation<Double> a, double fallback) {
        return a == null || a.value() == null ? fallback : a.value().doubleValue();
    }
}
