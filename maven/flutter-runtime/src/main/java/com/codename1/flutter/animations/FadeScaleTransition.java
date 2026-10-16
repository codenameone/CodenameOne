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
