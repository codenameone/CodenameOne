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
 * Flutter's {@code AnimatedRotation}: the child turned by {@code turns} (1.0 is a full turn) about {@code alignment}, animated over {@code duration}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnimatedRotation extends StatefulWidget {
    private Object turns = Double.valueOf(0);
    private Object alignment;
    private Widget child;
    private Duration duration;
    private Curve curve;
    private Object onEnd;
    public void turns(double v) { this.turns = Double.valueOf(v); }
    public void alignment(Object v) { this.alignment = v; }
    public void child(Widget v) { this.child = v; }
    public void duration(Duration v) { this.duration = v; }
    public void curve(Curve v) { this.curve = v; }
    public void onEnd(Object v) { this.onEnd = v; }
    public void filterQuality(Object v) { }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new S();
    }

    private static double d(Object v, double def) {
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    private static final class S extends State<AnimatedRotation> {
        private ImplicitTween turns;

        @Override
        public void initState() {
            super.initState();
            turns = new ImplicitTween(widget().turns, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedRotation w = widget();
            turns.onEnd(w.onEnd);
            turns.retarget(w.turns, w.duration, w.curve);
            return Transform.rotate(null, d(turns.value(), 0) * 2 * Math.PI, null,
                    w.alignment == null ? Alignment.center : w.alignment, null, null, w.child);
        }

        @Override
        public void dispose() {
            turns.dispose();
            super.dispose();
        }
    }
}
