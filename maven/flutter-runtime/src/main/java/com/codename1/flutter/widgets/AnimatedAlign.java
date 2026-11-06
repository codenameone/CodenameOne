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
 * Flutter's {@code AnimatedAlign}: a new alignment and size factors are moved to over {@code duration} along {@code curve}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnimatedAlign extends StatefulWidget {
    private Object alignment = Alignment.center;
    private Object widthFactor;
    private Object heightFactor;
    private Widget child;
    private Duration duration;
    private Curve curve;
    private dart.runtime.Funcs.VoidFunc0 onEnd;
    public void alignment(Object v) { this.alignment = v; }
    public void widthFactor(double v) { this.widthFactor = Double.valueOf(v); }
    public void heightFactor(double v) { this.heightFactor = Double.valueOf(v); }
    public void child(Widget v) { this.child = v; }
    public void duration(Duration v) { this.duration = v; }
    public void curve(Curve v) { this.curve = v; }
    public void onEnd(dart.runtime.Funcs.VoidFunc0 v) { this.onEnd = v; }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new S();
    }

    private static double d(Object v, double def) {
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    private static final class S extends State<AnimatedAlign> {
        private ImplicitTween alignment;
        private ImplicitTween widthFactor;
        private ImplicitTween heightFactor;

        @Override
        public void initState() {
            super.initState();
            alignment = new ImplicitTween(widget().alignment, this);
            widthFactor = new ImplicitTween(widget().widthFactor, this);
            heightFactor = new ImplicitTween(widget().heightFactor, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedAlign w = widget();
            alignment.onEnd(w.onEnd);
            alignment.retarget(w.alignment, w.duration, w.curve);
            widthFactor.retarget(w.widthFactor, w.duration, w.curve);
            heightFactor.retarget(w.heightFactor, w.duration, w.curve);
            Align a = new Align();
            Object v = alignment.value();
            a.alignment(v instanceof Alignment ? (Alignment) v : Alignment.center);
            v = widthFactor.value();
            a.widthFactor(v instanceof Number ? Double.valueOf(((Number) v).doubleValue()) : null);
            v = heightFactor.value();
            a.heightFactor(v instanceof Number ? Double.valueOf(((Number) v).doubleValue()) : null);
            a.child(w.child);
            return a;
        }

        @Override
        public void dispose() {
            alignment.dispose();
            widthFactor.dispose();
            heightFactor.dispose();
            super.dispose();
        }
    }
}
