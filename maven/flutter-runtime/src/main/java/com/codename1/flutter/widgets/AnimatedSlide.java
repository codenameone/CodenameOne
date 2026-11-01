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
 * Flutter's {@code AnimatedSlide}: the child translated by {@code offset} times its size, animated over {@code duration}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnimatedSlide extends StatefulWidget {
    private Object offset = Offset.zero;
    private Widget child;
    private Duration duration;
    private Curve curve;
    private Object onEnd;
    public void offset(Object v) { this.offset = v; }
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

    private static final class S extends State<AnimatedSlide> {
        private ImplicitTween offset;

        @Override
        public void initState() {
            super.initState();
            offset = new ImplicitTween(widget().offset, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedSlide w = widget();
            offset.onEnd(w.onEnd);
            offset.retarget(w.offset, w.duration, w.curve);
            FractionalTranslation f = new FractionalTranslation();
            Object v = offset.value();
            f.translation(v instanceof Offset ? (Offset) v : Offset.zero);
            f.child(w.child);
            return f;
        }

        @Override
        public void dispose() {
            offset.dispose();
            super.dispose();
        }
    }
}
