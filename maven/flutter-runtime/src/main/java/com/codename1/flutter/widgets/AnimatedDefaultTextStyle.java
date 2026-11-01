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
 * Flutter's {@code AnimatedDefaultTextStyle}: a DefaultTextStyle whose size and color are animated to over {@code duration}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnimatedDefaultTextStyle extends StatefulWidget {
    private Object style;
    private Object textAlign;
    private Object softWrap;
    private Object overflow;
    private Object maxLines;
    private Object textWidthBasis;
    private Object textHeightBehavior;
    private Widget child;
    private Duration duration;
    private Curve curve;
    private dart.runtime.Funcs.VoidFunc0 onEnd;
    public void style(Object v) { this.style = v; }
    public void textAlign(Object v) { this.textAlign = v; }
    public void softWrap(Object v) { this.softWrap = v; }
    public void overflow(Object v) { this.overflow = v; }
    public void maxLines(Object v) { this.maxLines = v; }
    public void textWidthBasis(Object v) { this.textWidthBasis = v; }
    public void textHeightBehavior(Object v) { this.textHeightBehavior = v; }
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

    private static final class S extends State<AnimatedDefaultTextStyle> {
        private ImplicitTween style;

        @Override
        public void initState() {
            super.initState();
            style = new ImplicitTween(widget().style, this);
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedDefaultTextStyle w = widget();
            style.onEnd(w.onEnd);
            style.retarget(w.style, w.duration, w.curve);
            DefaultTextStyle t = new DefaultTextStyle();
            Object v = style.value();
            if (v instanceof TextStyle) { t.style((TextStyle) v); }
            if (w.textAlign instanceof TextAlign) { t.textAlign((TextAlign) w.textAlign); }
            if (w.softWrap instanceof Boolean) { t.softWrap(((Boolean) w.softWrap).booleanValue()); }
            t.overflow(w.overflow);
            if (w.maxLines instanceof Number) { t.maxLines(((Number) w.maxLines).intValue()); }
            t.child(w.child);
            return t;
        }

        @Override
        public void dispose() {
            style.dispose();
            super.dispose();
        }
    }
}
