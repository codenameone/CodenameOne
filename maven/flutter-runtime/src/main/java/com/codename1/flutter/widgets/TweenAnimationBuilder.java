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
import com.codename1.flutter.animation.Tween;
import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * Flutter's {@code TweenAnimationBuilder}: {@code builder} is called with a value that runs
 * from the tween's begin to its end when first built, and from wherever it is to the new end
 * whenever the tween's end changes, over {@code duration} along {@code curve}.
 */
public class TweenAnimationBuilder extends StatefulWidget {
    private Tween<?> tween;
    private Duration duration;
    private Curve curve;
    private Object onEnd;
    private Widget child;
    private Funcs.Func3<BuildContext, Object, Widget, Widget> builder;

    public void tween(Tween<?> v) { this.tween = v; }
    public void duration(Duration v) { this.duration = v; }
    public void curve(Curve v) { this.curve = v; }
    public void onEnd(Object v) { this.onEnd = v; }
    public void child(Widget v) { this.child = v; }
    @SuppressWarnings("unchecked")
    public void builder(Funcs.Func3<?, ?, ?, ?> v) {
        this.builder = (Funcs.Func3<BuildContext, Object, Widget, Widget>) v;
    }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new S();
    }

    private static final class S extends State<TweenAnimationBuilder> {
        private ImplicitTween value;

        @Override
        public void initState() {
            super.initState();
            Tween<?> t = widget().tween;
            Object begin = t == null ? null : t.begin();
            value = new ImplicitTween(begin == null && t != null ? t.end() : begin, this);
        }

        @Override
        public Widget build(BuildContext context) {
            TweenAnimationBuilder w = widget();
            value.lerpWith(w.tween);
            value.onEnd(w.onEnd);
            value.retarget(w.tween == null ? null : w.tween.end(), w.duration, w.curve);
            return w.builder == null ? w.child : w.builder.call(context, value.value(), w.child);
        }

        @Override
        public void dispose() {
            value.dispose();
            super.dispose();
        }
    }
}
