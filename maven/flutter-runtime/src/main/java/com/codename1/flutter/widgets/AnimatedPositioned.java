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

import com.codename1.flutter.*;
import com.codename1.flutter.animation.Curve;
import com.codename1.flutter.widgets.*;
import dart.core.Duration;

/**
 * Flutter's {@code AnimatedPositioned}: a Positioned whose edges and size are animated to over {@code duration} along {@code curve}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnimatedPositioned extends StatefulWidget {
    private Object left;
    private Object top;
    private Object right;
    private Object bottom;
    private Object width;
    private Object height;
    private Widget child;
    private Duration duration;
    private Curve curve;
    private Object onEnd;
    public void left(double v) { this.left = Double.valueOf(v); }
    public void top(double v) { this.top = Double.valueOf(v); }
    public void right(double v) { this.right = Double.valueOf(v); }
    public void bottom(double v) { this.bottom = Double.valueOf(v); }
    public void width(double v) { this.width = Double.valueOf(v); }
    public void height(double v) { this.height = Double.valueOf(v); }
    public void child(Widget v) { this.child = v; }
    public void duration(Duration v) { this.duration = v; }
    public void curve(Curve v) { this.curve = v; }
    public void onEnd(Object v) { this.onEnd = v; }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new S();
    }

    private static double d(Object v, double def) {
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    private static final class S extends State<AnimatedPositioned> {
        private ImplicitTween left;
        private ImplicitTween top;
        private ImplicitTween right;
        private ImplicitTween bottom;
        private ImplicitTween width;
        private ImplicitTween height;

        @Override
        public void initState() {
            super.initState();
            left = new ImplicitTween(widget().left, this);
            top = new ImplicitTween(widget().top, this);
            right = new ImplicitTween(widget().right, this);
            bottom = new ImplicitTween(widget().bottom, this);
            width = new ImplicitTween(widget().width, this);
            height = new ImplicitTween(widget().height, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedPositioned w = widget();
            left.onEnd(w.onEnd);
            left.retarget(w.left, w.duration, w.curve);
            top.retarget(w.top, w.duration, w.curve);
            right.retarget(w.right, w.duration, w.curve);
            bottom.retarget(w.bottom, w.duration, w.curve);
            width.retarget(w.width, w.duration, w.curve);
            height.retarget(w.height, w.duration, w.curve);
            Positioned p = new Positioned();
            Object v = left.value();
            if (v instanceof Number) { p.left(((Number) v).doubleValue()); }
            v = top.value();
            if (v instanceof Number) { p.top(((Number) v).doubleValue()); }
            v = right.value();
            if (v instanceof Number) { p.right(((Number) v).doubleValue()); }
            v = bottom.value();
            if (v instanceof Number) { p.bottom(((Number) v).doubleValue()); }
            v = width.value();
            if (v instanceof Number) { p.width(((Number) v).doubleValue()); }
            v = height.value();
            if (v instanceof Number) { p.height(((Number) v).doubleValue()); }
            p.child(w.child);
            return p;
        }

        @Override
        public void dispose() {
            left.dispose();
            top.dispose();
            right.dispose();
            bottom.dispose();
            width.dispose();
            height.dispose();
            super.dispose();
        }
    }
}
