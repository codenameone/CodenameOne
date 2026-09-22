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

import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;

/**
 * Element for {@link LayoutBuilder}: runs the builder DURING layout, with the
 * constraints the parent actually handed down.
 *
 * <p>It used to run once at build time against the whole display, which is a
 * plausible-looking approximation and wrong wherever the widget is not the
 * whole screen. The 2D-transformations demo centres its board on the viewport
 * the builder reports, and was handed the screen instead — so the board was
 * offset by exactly the app bar plus the footer and hung off the bottom.
 *
 * <p>Two details make a layout-time build safe here. The builder's result is
 * cached against the constraints that produced it, so a second layout pass with
 * the same box does not rebuild — without that, building inside layout is an
 * easy way to loop. And the constraints are converted to LOGICAL pixels first:
 * layout runs in device pixels, and a builder written against Flutter's
 * coordinate system would otherwise see numbers a factor of the device pixel
 * ratio too large.
 */
public class LayoutBuilderElement extends SingleChildRenderElement {

    private BoxConstraints builtFor;
    private Widget built;

    /// Whether one unbounded pass has already been sat out; see performLayout.
    private boolean skippedUnbounded;

    /**
     * Whether these constraints are a speculative MEASUREMENT rather than a box
     * the child will occupy.
     *
     * <p>Unbounded on its own is not the signal, and reading it that way is a
     * bug: a viewport's child is legitimately unbounded along the scroll axis
     * and Flutter hands it infinity too. What separates the two is the CROSS
     * axis. A viewport gives its child a tight cross-axis extent -- a vertical
     * list hands down a tight width -- whereas "how big would you like to be"
     * is loose in both directions. Sitting out a real viewport pass returns a
     * zero size that the list then keeps, which silently emptied the reply
     * study's entire mail list while the diff score went DOWN, because blank
     * background differs from the reference less than mis-rendered cards do.</p>
     */
    private static boolean isSpeculativeMeasurement(BoxConstraints c) {
        boolean unbounded = Double.isInfinite(c.maxWidth()) || Double.isInfinite(c.maxHeight());
        return unbounded && !c.hasTightWidth() && !c.hasTightHeight();
    }

    private static long builderMs;
    private static long syncMs;
    private static long layoutMs;

    /** Where the time inside layout-time building actually goes. */
    public static String cost() {
        return "builder=" + builderMs + "ms mount=" + syncMs + "ms childLayout=" + layoutMs + "ms";
    }

    public LayoutBuilderElement(LayoutBuilder widget) {
        super(widget);
    }

    @Override
    protected Widget childWidget() {
        return built;
    }

    @Override
    public void update(Widget newWidget) {
        // A new configuration means a new builder; the cached result is stale.
        builtFor = null;
        super.update(newWidget);
    }

    @Override
    protected void performRebuild() {
        // A REBUILD re-runs the builder, even at the same constraints.
        //
        // Caching against the constraints alone is right for repeated layout
        // passes and wrong for setState: Flutter's LayoutBuilder rebuilds its
        // builder whenever the element is dirty, because the builder closes
        // over state that the constraints know nothing about. Keeping the
        // cached result meant a LayoutBuilder whose state changed kept showing
        // what it built the first time -- the gallery's feature discovery is
        // exactly that shape, and the coach mark stayed at the zero radius it
        // was born with however far its animation ran.
        builtFor = null;
        invalidateLayoutCache();
        super.performRebuild();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        BoxConstraints logical = toLogical(constraints);
        // A DRY pass must never run the builder. Codename One measures a
        // container far more often than it lays one out, and it measures with
        // loose, unbounded constraints -- so the builder would be handed a
        // viewport of infinity. Flutter does not support dry layout for
        // LayoutBuilder at all, for exactly this reason. It matters beyond the
        // measurement itself because a builder is allowed to latch: the
        // 2D-transformations demo computes its home matrix from the FIRST
        // viewport it is shown and keeps it forever, so one speculative call
        // with the wrong box mis-centres the board for the life of the screen.
        if (isDryPass() && builtFor == null) {
            return constraints.smallest();
        }
        // Same hazard, reached by a pass that is not dry. Codename One asks a box
        // how wide it would like to be -- a real layout call, with the width
        // unbounded -- before it asks again with the box the child will actually
        // occupy. A builder that latches keeps whatever it was shown FIRST, so
        // that speculative call decides the screen: the 2D-transformations demo
        // centres its board against `constraints.maxWidth` and never recomputes,
        // and an infinite width left the board drawn in the corner for the life
        // of the route. Sit out one unbounded pass. If the next one is unbounded
        // too then this really is an unbounded layout -- a viewport's child, say
        // -- and the builder runs against it as Flutter would.
        if (builtFor == null && !skippedUnbounded && isSpeculativeMeasurement(logical)) {
            skippedUnbounded = true;
            // The next pass can carry these same constraints, and the layout
            // cache would hand it this placeholder instead of running the
            // builder at all -- which is how sitting out once turned into never
            // building for a viewport's child.
            invalidateLayoutCache();
            return constraints.smallest();
        }
        if (!isDryPass() && (builtFor == null || !same(builtFor, logical))) {
            LayoutBuilder w = (LayoutBuilder) widget();
            long t0 = System.currentTimeMillis();
            // The builder runs Dart code, but not from performRebuild -- so
            // without this a `!` failure inside it named no location at all,
            // which is exactly how a startup crash here read as an anonymous
            // TypeError seven frames deep in the layout recursion.
            Object previous = dart.runtime.DartRuntime.diagnosticContextValue();
            dart.runtime.DartRuntime.diagnosticContext(
                    "running the LayoutBuilder in " + describeParentWidget());
            try {
                built = w.getBuilder() == null ? null : w.getBuilder().call(this, logical);
            } finally {
                dart.runtime.DartRuntime.diagnosticContext(previous);
            }
            long t1 = System.currentTimeMillis();
            builtFor = logical;
            syncChildren();
            long t2 = System.currentTimeMillis();
            // Split so the cost can be attributed instead of described: calling
            // the transpiled builder is Dart-side widget construction, while
            // syncChildren is element mounting plus Codename One component
            // creation. They are different problems with different fixes.
            builderMs += t1 - t0;
            syncMs += t2 - t1;
        }
        RenderElement child = renderChild();
        if (child == null) {
            return constraints.smallest();
        }
        long lt = System.currentTimeMillis();
        Size cs = child.layout(constraints);
        layoutMs += System.currentTimeMillis() - lt;
        setChildOffset(child, 0, 0);
        return constraints.constrain(cs);
    }

    /** The nearest enclosing application widget, for the diagnostic above. */
    private String describeParentWidget() {
        com.codename1.flutter.Element e = this;
        for (int depth = 0; e != null && depth < 12; depth++) {
            Widget w = e.widget();
            if (w != null && !w.getClass().getName().startsWith("com.codename1.flutter.")) {
                return w.getClass().getName();
            }
            e = e.parent();
        }
        return "an unnamed subtree";
    }

    private static BoxConstraints toLogical(BoxConstraints c) {
        double scale = Dp.scale();
        if (scale <= 0) {
            scale = 1;
        }
        return new BoxConstraints(div(c.minWidth(), scale), div(c.maxWidth(), scale),
                div(c.minHeight(), scale), div(c.maxHeight(), scale));
    }

    private static double div(double v, double scale) {
        return Double.isInfinite(v) ? v : v / scale;
    }

    private static boolean same(BoxConstraints a, BoxConstraints b) {
        return a.minWidth() == b.minWidth() && a.maxWidth() == b.maxWidth()
                && a.minHeight() == b.minHeight() && a.maxHeight() == b.maxHeight();
    }
}
