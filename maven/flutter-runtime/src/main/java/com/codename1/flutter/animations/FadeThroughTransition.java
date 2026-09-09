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

    @Override
    public Widget build(BuildContext context) {
        double in = value(animation, 1);
        double out = value(secondaryAnimation, 0);

        // Incoming: nothing for the first 30%, then fade up while scaling 92% -> 100%.
        double opacity = FadeScaleTransition.interval(in, 0.3, 1.0);
        double scale = 0.92 + 0.08 * FadeScaleTransition.interval(in, 0.3, 1.0);
        // Outgoing: fade away over the first 30% of the secondary run.
        opacity *= 1 - FadeScaleTransition.interval(out, 0.0, 0.3);

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
