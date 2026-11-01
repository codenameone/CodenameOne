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
import com.codename1.flutter.gestures.DragEndDetails;
import com.codename1.flutter.gestures.DragUpdateDetails;
import com.codename1.flutter.gestures.GestureDragEndCallback;
import com.codename1.flutter.gestures.GestureDragUpdateCallback;
import com.codename1.flutter.rendering.BoxConstraints;

import dart.core.DartList;
import dart.runtime.Funcs;

/// Flutter's {@code DraggableScrollableSheet}: a bottom-aligned sheet whose height
/// is a fraction (its size) of the space it is given.
///
/// It opens at initialChildSize. A vertical drag on the sheet moves its size
/// between minChildSize and maxChildSize; with snap, releasing it settles on the
/// nearest of minChildSize, snapSizes and maxChildSize (or, after a fling, the next
/// one in the fling's direction). The ScrollController handed to the builder is the
/// sheet's own: while the sheet is below maxChildSize, scrolling the content grows
/// the sheet instead and the content stays at its top; at maxChildSize the content
/// scrolls. A drag down on content scrolled to its top does not shrink the sheet,
/// since the list here reports no overscroll; a drag on any non-scrolling part of
/// the sheet does. A {@link DraggableScrollableController} reads and moves the size.
public class DraggableScrollableSheet extends StatefulWidget {

    private double initialChildSize = 0.5;
    private double minChildSize = 0.25;
    private double maxChildSize = 1.0;
    private boolean expand = true;
    private boolean snap;
    private DartList<Double> snapSizes;
    private DraggableScrollableController controller;
    private Funcs.Func2<BuildContext, ScrollController, Widget> builder;

    public void initialChildSize(double v) { this.initialChildSize = v; }
    public void minChildSize(double v) { this.minChildSize = v; }
    public void maxChildSize(double v) { this.maxChildSize = v; }
    public void expand(boolean v) { this.expand = v; }
    public void snap(boolean v) { this.snap = v; }
    public void snapSizes(DartList<Double> v) { this.snapSizes = v; }
    public void controller(DraggableScrollableController v) { this.controller = v; }
    public void shouldCloseOnMinExtent(boolean v) { }
    public void builder(Funcs.Func2<BuildContext, ScrollController, Widget> v) { this.builder = v; }

    public double getInitialChildSize() { return initialChildSize; }
    public double getMinChildSize() { return minChildSize; }
    public double getMaxChildSize() { return maxChildSize; }
    public boolean getExpand() { return expand; }

    @Override
    public State<? extends StatefulWidget> createState() {
        return new SheetState();
    }

    /// Where a released sheet settles: the nearest stop, or after a fling (velocity in
    /// sizes per second, positive meaning up/growing) the next stop that way.
    static double snapTarget(double size, double velocity, double[] stops) {
        double best = stops[0];
        if (Math.abs(velocity) > 0.5) {
            if (velocity > 0) {
                best = stops[stops.length - 1];
                for (int i = stops.length - 1; i >= 0; i--) {
                    if (stops[i] > size + 1e-9) {
                        best = stops[i];
                    }
                }
            } else {
                for (int i = 0; i < stops.length; i++) {
                    if (stops[i] < size - 1e-9) {
                        best = stops[i];
                    }
                }
            }
            return best;
        }
        for (int i = 1; i < stops.length; i++) {
            if (Math.abs(stops[i] - size) < Math.abs(best - size)) {
                best = stops[i];
            }
        }
        return best;
    }

    static final class SheetState extends State<DraggableScrollableSheet>
            implements DraggableScrollableController.Sheet {
        private double size;
        private double available;
        private final ScrollController scroll = new ScrollController();
        private DraggableScrollableController attachedTo;
        private boolean handing;

        @Override
        public void initState() {
            size = clamp(widget().initialChildSize);
            scroll.addListener(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    contentScrolled();
                }
            });
            attachController();
        }

        @Override
        public void didUpdateWidget(DraggableScrollableSheet oldWidget) {
            attachController();
            size = clamp(size);
        }

        @Override
        public void dispose() {
            if (attachedTo != null) {
                attachedTo.detach(this);
            }
            scroll.dispose();
        }

        private void attachController() {
            DraggableScrollableController c = widget().controller;
            if (c != attachedTo) {
                if (attachedTo != null) {
                    attachedTo.detach(this);
                }
                if (c != null) {
                    c.attach(this);
                }
                attachedTo = c;
            }
        }

        private double clamp(double s) {
            DraggableScrollableSheet w = widget();
            return Math.max(w.minChildSize, Math.min(w.maxChildSize, s));
        }

        @Override
        public double size() {
            return size;
        }

        @Override
        public double availableHeight() {
            return available;
        }

        @Override
        public void moveTo(double s) {
            final double next = clamp(s);
            if (next == size) {
                return;
            }
            setState(new Funcs.VoidFunc0() {
                @Override
                public void call() {
                    size = next;
                }
            });
            if (attachedTo != null) {
                attachedTo.notifyListeners();
            }
        }

        /// The handoff: content scrolled while the sheet can still grow moves the sheet.
        void contentScrolled() {
            if (handing || available <= 0) {
                return;
            }
            double off;
            try {
                off = scroll.offset();
            } catch (RuntimeException e) {
                return;
            }
            double max = widget().maxChildSize;
            if (off > 0 && size < max) {
                double grow = Math.min(off / available, max - size);
                moveTo(size + grow);
                handing = true;
                try {
                    scroll.jumpTo(Math.max(0, off - grow * available));
                } finally {
                    handing = false;
                }
            }
        }

        void dragBy(double dy) {
            if (available > 0) {
                moveTo(size - dy / available);
            }
        }

        void release(double velocityPx) {
            DraggableScrollableSheet w = widget();
            if (!w.snap || available <= 0) {
                return;
            }
            java.util.TreeSet<Double> set = new java.util.TreeSet<Double>();
            set.add(w.minChildSize);
            set.add(w.maxChildSize);
            if (w.snapSizes != null) {
                for (int i = 0; i < w.snapSizes.size(); i++) {
                    Object o = w.snapSizes.get(i);
                    if (o instanceof Number) {
                        set.add(clamp(((Number) o).doubleValue()));
                    }
                }
            }
            double[] stops = new double[set.size()];
            int i = 0;
            for (Double d : set) {
                stops[i++] = d;
            }
            moveTo(snapTarget(size, -velocityPx / available, stops));
        }

        @Override
        public Widget build(BuildContext context) {
            final DraggableScrollableSheet w = widget();
            LayoutBuilder lb = new LayoutBuilder();
            lb.builder(new Funcs.Func2<BuildContext, BoxConstraints, Widget>() {
                @Override
                public Widget call(BuildContext c, BoxConstraints constraints) {
                    double h = constraints.maxHeight();
                    available = Double.isInfinite(h) ? 0 : h;
                    Widget content = w.builder != null ? w.builder.call(c, scroll) : new SizedBox();
                    GestureDetector drag = new GestureDetector();
                    drag.onVerticalDragUpdate(new GestureDragUpdateCallback() {
                        @Override
                        public void call(DragUpdateDetails d) {
                            Double p = d.primaryDelta();
                            dragBy(p != null ? p.doubleValue() : (d.delta() != null ? d.delta().dy() : 0));
                        }
                    });
                    drag.onVerticalDragEnd(new GestureDragEndCallback() {
                        @Override
                        public void call(DragEndDetails d) {
                            Double v = d.primaryVelocity();
                            release(v != null ? v.doubleValue() : 0);
                        }
                    });
                    drag.child(content);
                    FractionallySizedBox box = new FractionallySizedBox();
                    box.heightFactor(size);
                    box.alignment(com.codename1.flutter.Alignment.bottomCenter);
                    box.child(drag);
                    if (!w.expand) {
                        Align a = new Align();
                        a.alignment(com.codename1.flutter.Alignment.bottomCenter);
                        a.child(box);
                        return a;
                    }
                    return box;
                }
            });
            return lb;
        }
    }
}
