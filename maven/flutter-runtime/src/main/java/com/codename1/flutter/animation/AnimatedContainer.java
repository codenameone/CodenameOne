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
package com.codename1.flutter.animation;

import com.codename1.flutter.*;
import com.codename1.flutter.animation.Curve;
import com.codename1.flutter.widgets.*;
import dart.core.Duration;

/**
 * Flutter's {@code AnimatedContainer}: size, color, padding, margin, alignment and decoration color are animated to over {@code duration} along {@code curve}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnimatedContainer extends StatefulWidget {
    private Object width;
    private Object height;
    private Object color;
    private Object padding;
    private Object margin;
    private Object alignment;
    private Object decoration;
    private Object foregroundDecoration;
    private Object constraints;
    private Object transform;
    private Object transformAlignment;
    private Object clipBehavior;
    private Widget child;
    private Duration duration;
    private Curve curve;
    private Object onEnd;
    public void width(double v) { this.width = Double.valueOf(v); }
    public void height(double v) { this.height = Double.valueOf(v); }
    public void color(Color v) { this.color = v; }
    public void padding(Object v) { this.padding = v; }
    public void margin(Object v) { this.margin = v; }
    public void alignment(Object v) { this.alignment = v; }
    public void decoration(Object v) { this.decoration = v; }
    public void foregroundDecoration(Object v) { this.foregroundDecoration = v; }
    public void constraints(Object v) { this.constraints = v; }
    public void transform(Object v) { this.transform = v; }
    public void transformAlignment(Object v) { this.transformAlignment = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }
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

    private static final class S extends State<AnimatedContainer> {
        private ImplicitTween width;
        private ImplicitTween height;
        private ImplicitTween color;
        private ImplicitTween padding;
        private ImplicitTween margin;
        private ImplicitTween alignment;
        private ImplicitTween decoration;

        @Override
        public void initState() {
            super.initState();
            width = new ImplicitTween(widget().width, this);
            height = new ImplicitTween(widget().height, this);
            color = new ImplicitTween(widget().color, this);
            padding = new ImplicitTween(widget().padding, this);
            margin = new ImplicitTween(widget().margin, this);
            alignment = new ImplicitTween(widget().alignment, this);
            decoration = new ImplicitTween(widget().decoration, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedContainer w = widget();
            width.onEnd(w.onEnd);
            width.retarget(w.width, w.duration, w.curve);
            height.retarget(w.height, w.duration, w.curve);
            color.retarget(w.color, w.duration, w.curve);
            padding.retarget(w.padding, w.duration, w.curve);
            margin.retarget(w.margin, w.duration, w.curve);
            alignment.retarget(w.alignment, w.duration, w.curve);
            decoration.retarget(w.decoration, w.duration, w.curve);
            Container c = new Container();
            Object v = width.value();
            if (v instanceof Number) { c.width(((Number) v).doubleValue()); }
            v = height.value();
            if (v instanceof Number) { c.height(((Number) v).doubleValue()); }
            v = color.value();
            if (v instanceof Color) { c.color((Color) v); }
            c.padding(padding.value());
            c.margin(margin.value());
            c.alignment(alignment.value());
            v = decoration.value();
            c.decoration(v);
            c.foregroundDecoration(w.foregroundDecoration);
            if (w.constraints instanceof com.codename1.flutter.rendering.BoxConstraints) { c.constraints((com.codename1.flutter.rendering.BoxConstraints) w.constraints); }
            c.transform(w.transform);
            c.transformAlignment(w.transformAlignment);
            if (w.clipBehavior instanceof Clip) { c.clipBehavior((Clip) w.clipBehavior); }
            c.child(w.child);
            return c;
        }

        @Override
        public void dispose() {
            width.dispose();
            height.dispose();
            color.dispose();
            padding.dispose();
            margin.dispose();
            alignment.dispose();
            decoration.dispose();
            super.dispose();
        }
    }
}
