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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Curve;
import dart.core.Duration;

/**
 * Flutter's {@code AnimatedScale}: the child scaled by {@code scale} about
 * {@code alignment}; a new scale is animated to over {@code duration} along {@code curve}.
 */
public class AnimatedScale extends StatefulWidget {
    private double scale = 1;
    private Widget child;
    private Object alignment;
    private Duration duration;
    private Curve curve;
    private dart.runtime.Funcs.VoidFunc0 onEnd;
    public void scale(double v) { this.scale = v; }
    public void child(Widget v) { this.child = v; }
    public void alignment(Object v) { this.alignment = v; }
    public void duration(Duration v) { this.duration = v; }
    public void curve(Curve v) { this.curve = v; }
    public void onEnd(dart.runtime.Funcs.VoidFunc0 v) { this.onEnd = v; }
    public void filterQuality(Object v) { }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new ScaleState();
    }

    private static final class ScaleState extends State<AnimatedScale> {
        private ImplicitValue value;

        @Override
        public void initState() {
            super.initState();
            value = new ImplicitValue(widget().scale, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedScale w = widget();
            value.onEnd(w.onEnd);
            value.retarget(w.scale, w.duration, w.curve);
            return Transform.scale(null, Double.valueOf(value.value()), null, null, null,
                    w.alignment == null ? com.codename1.flutter.Alignment.center : w.alignment,
                    null, null, w.child);
        }

        @Override
        public void dispose() {
            value.dispose();
            super.dispose();
        }
    }
}
