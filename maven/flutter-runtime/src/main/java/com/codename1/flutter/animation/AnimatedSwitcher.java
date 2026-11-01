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

import com.codename1.flutter.Alignment;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.ImplicitValue;
import com.codename1.flutter.widgets.Kit;
import com.codename1.flutter.widgets.Opacity;
import com.codename1.flutter.widgets.Stack;
import dart.core.Duration;

/**
 * Flutter's {@code AnimatedSwitcher}: when the child is replaced by one that cannot update
 * it (a different type or key) the old child fades out while the new one fades in, over
 * {@code duration} (the outgoing child over {@code reverseDuration} when given) along the
 * switch curves. A custom {@code transitionBuilder} is accepted and the default fade used.
 */
public class AnimatedSwitcher extends StatefulWidget {

    private Widget child;
    private Duration duration;
    private Duration reverseDuration;
    private Curve switchInCurve;
    private Curve switchOutCurve;

    public void child(Widget v) { this.child = v; }
    public Widget getChild() { return child; }
    public void duration(Duration v) { this.duration = v; }
    public void reverseDuration(Duration v) { this.reverseDuration = v; }
    public void switchInCurve(Curve v) { this.switchInCurve = v; }
    public void switchOutCurve(Curve v) { this.switchOutCurve = v; }
    public void transitionBuilder(Object v) { }
    public void layoutBuilder(Object v) { }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new S();
    }

    private static final class S extends State<AnimatedSwitcher> {
        private ImplicitValue fade;
        private Widget current;
        private Widget previous;
        private int switches;

        @Override
        public void initState() {
            super.initState();
            fade = new ImplicitValue(0, this);
            current = widget().child;
        }

        @Override
        public Widget build(BuildContext context) {
            AnimatedSwitcher w = widget();
            Widget next = w.child;
            if (next != current) {
                if (current != null && next != null && Widget.canUpdate(current, next)) {
                    current = next;
                } else {
                    previous = current;
                    current = next;
                    switches++;
                    fade.retarget(switches, w.duration, w.switchInCurve);
                }
            }
            double t = fade.value() - (switches - 1);
            if (switches == 0 || previous == null || t >= 1 || !fade.isAnimating()) {
                previous = null;
                return current == null ? new Stack() : current;
            }
            Opacity out = new Opacity();
            out.opacity(Math.max(0, Math.min(1, 1 - t)));
            out.child(previous);
            if (current == null) {
                return out;
            }
            Opacity in = new Opacity();
            in.opacity(Math.max(0, Math.min(1, t)));
            in.child(current);
            Stack s = new Stack();
            s.alignment(Alignment.center);
            s.children(Kit.list(out, in));
            return s;
        }

        @Override
        public void dispose() {
            fade.dispose();
            super.dispose();
        }
    }
}
