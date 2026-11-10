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
import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;

import dart.core.Duration;

/**
 * Flutter's {@code AnimatedSize}: when its child's size changes, this box
 * tweens from the size it had to the child's new one over {@code duration},
 * aligning the child inside it (center by default) and clipping what does not
 * fit yet. The first layout takes the child's size at once.
 */
public class AnimatedSize extends Widget {

    private Widget child;
    private Duration duration;
    private Duration reverseDuration;
    private Curve curve;
    private Alignment alignment = Alignment.center;
    private dart.runtime.Funcs.VoidFunc0 onEnd;

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    public void duration(Duration v) {
        this.duration = v;
    }

    public void reverseDuration(Duration v) {
        this.reverseDuration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    public void alignment(Object v) {
        this.alignment = v instanceof Alignment ? (Alignment) v : Alignment.center;
    }

    public void clipBehavior(Object v) {
    }

    public void onEnd(dart.runtime.Funcs.VoidFunc0 v) {
        this.onEnd = v;
    }

    @Override
    public Element createElement() {
        return new E(this);
    }

    static final class E extends SingleChildRenderElement {
        private double fromW;
        private double fromH;
        private double toW = -1;
        private double toH = -1;
        private double curW;
        private double curH;
        private long startedAt;
        private boolean animating;
        private boolean scheduled;

        E(AnimatedSize w) {
            super(w);
        }

        private AnimatedSize w() {
            return (AnimatedSize) widget();
        }

        @Override
        protected Widget childWidget() {
            return w().child;
        }

        private long durationMs(boolean shrinking) {
            Duration d = shrinking && w().reverseDuration != null ? w().reverseDuration : w().duration;
            return d == null ? 0 : d.inMilliseconds();
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            RenderElement child = renderChild();
            Size cs = child == null ? new Size(0, 0)
                    : child.layout(new BoxConstraints(0, constraints.maxWidth(), 0, constraints.maxHeight()));
            double tw = cs.width();
            double th = cs.height();
            long now = MotionClock.now();
            if (toW < 0) {
                curW = tw;
                curH = th;
                toW = tw;
                toH = th;
            } else if (tw != toW || th != toH) {
                fromW = curW;
                fromH = curH;
                toW = tw;
                toH = th;
                startedAt = now;
                animating = true;
            }
            if (animating) {
                long dur = durationMs(toW * toH < fromW * fromH);
                double t = dur <= 0 ? 1 : (now - startedAt) / (double) dur;
                if (t >= 1) {
                    animating = false;
                    curW = toW;
                    curH = toH;
                    fireEnd();
                } else {
                    double e = w().curve == null ? t : w().curve.transform(Math.max(0, t));
                    curW = fromW + (toW - fromW) * e;
                    curH = fromH + (toH - fromH) * e;
                    scheduleFrame();
                }
            }
            Size box = constraints.constrain(new Size(curW, curH));
            if (child != null) {
                Alignment a = w().alignment;
                double ox = (box.width() - tw) * (a.x() + 1) / 2;
                double oy = (box.height() - th) * (a.y() + 1) / 2;
                setChildOffset(child, ox, oy);
            }
            return box;
        }

        private void fireEnd() {
            Object cb = w().onEnd;
            if (cb instanceof dart.runtime.Funcs.VoidFunc0) {
                try {
                    ((dart.runtime.Funcs.VoidFunc0) cb).call();
                } catch (Throwable t) {
                    com.codename1.flutter.FlutterErrorReport.record(t);
                }
            }
        }

        /** Asks for the next layout pass while the size is still moving. */
        private void scheduleFrame() {
            if (scheduled) {
                return;
            }
            scheduled = true;
            com.codename1.ui.CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    scheduled = false;
                    if (isMounted()) {
                        markNeedsLayout();
                    }
                }
            });
        }
    }
}
