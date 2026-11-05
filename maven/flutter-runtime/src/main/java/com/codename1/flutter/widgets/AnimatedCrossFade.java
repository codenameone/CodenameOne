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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Alignment;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Curve;
import dart.core.Duration;

/**
 * Flutter's {@code AnimatedCrossFade}: {@code firstChild} or {@code secondChild}, as
 * {@code crossFadeState} says. A change fades one out and the other in over
 * {@code duration}, both stacked while the fade runs; at rest only the shown child is
 * laid out, so the widget takes its size.
 */
public class AnimatedCrossFade extends StatefulWidget {
    private Widget firstChild;
    private Widget secondChild;
    private CrossFadeState crossFadeState = CrossFadeState.showFirst;
    private Duration duration;
    private Duration reverseDuration;
    private Curve sizeCurve;
    private Object alignment;
    public void firstChild(Widget v) { this.firstChild = v; }
    public void secondChild(Widget v) { this.secondChild = v; }
    public void crossFadeState(CrossFadeState v) { if (v != null) { this.crossFadeState = v; } }
    public void duration(Duration v) { this.duration = v; }
    public void reverseDuration(Duration v) { this.reverseDuration = v; }
    public void firstCurve(Object v) { }
    public void secondCurve(Object v) { }
    public void sizeCurve(Curve v) { this.sizeCurve = v; }
    public void alignment(Object v) { this.alignment = v; }
    public void layoutBuilder(Object v) { }
    public void excludeBottomFocus(boolean v) { }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new FadeState();
    }

    private static final class FadeState extends State<AnimatedCrossFade> {
        private ImplicitValue value;

        private static double target(AnimatedCrossFade w) {
            return w.crossFadeState == CrossFadeState.showSecond ? 1 : 0;
        }

        @Override
        public void initState() {
            super.initState();
            value = new ImplicitValue(target(widget()), this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedCrossFade w = widget();
            double t = target(w);
            Duration d = t == 0 && w.reverseDuration != null ? w.reverseDuration : w.duration;
            value.retarget(t, d, w.sizeCurve);
            double v = value.value();
            if (!value.isAnimating() || v <= 0 || v >= 1) {
                return v >= 0.5 ? w.secondChild : w.firstChild;
            }
            Opacity first = new Opacity();
            first.opacity(1 - v);
            first.child(w.firstChild);
            Opacity second = new Opacity();
            second.opacity(v);
            second.child(w.secondChild);
            Stack s = new Stack();
            s.alignment(w.alignment instanceof Alignment ? (Alignment) w.alignment : Alignment.topCenter);
            s.children(Kit.list(first, second));
            return s;
        }

        @Override
        public void dispose() {
            value.dispose();
            super.dispose();
        }
    }
}
