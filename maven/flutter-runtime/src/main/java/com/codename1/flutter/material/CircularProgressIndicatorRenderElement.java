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
package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

/**
 * Leaf render box for {@link CircularProgressIndicator}: paints the arc.
 *
 * <p>The widget used to build an empty {@code SizedBox} -- it reserved the
 * right amount of space and drew nothing at all, so the progress-indicator demo
 * showed a blank page where the reference shows two spinning arcs.</p>
 *
 * <p>A determinate indicator sweeps {@code value} of a full turn from twelve
 * o'clock. An indeterminate one sweeps a fixed-length arc whose head advances
 * with the clock, which is the part of Flutter's two-phase animation that reads
 * as motion; the arc's length also breathes, and that is not reproduced here.
 * Codename One measures arc angles anticlockwise from three o'clock, so both
 * the start and the direction have to be converted.</p>
 */
public class CircularProgressIndicatorRenderElement extends RenderElement {

    /** Material's default indicator diameter and stroke, in logical pixels. */
    private static final double DIAMETER_LP = 36;
    private static final double STROKE_LP = 4;
    /** The fraction of a full turn an indeterminate arc covers. */
    private static final double SWEEP_FRACTION = 0.75;
    /** How long the indeterminate head takes to go once round, in milliseconds. */
    private static final long PERIOD_MS = 1400;

    public CircularProgressIndicatorRenderElement(CircularProgressIndicator widget) {
        super(widget);
    }

    private CircularProgressIndicator indicator() {
        return (CircularProgressIndicator) widget();
    }

    private int strokePx() {
        Double w = indicator().getStrokeWidth();
        return (int) Math.max(1, Math.round(Dp.px(w == null ? STROKE_LP : w.doubleValue())));
    }

    private int arcRgb() {
        Color c = indicator().getColor();
        if (c != null) {
            return c.rgb();
        }
        try {
            ThemeData t = Theme.of(this);
            if (t != null && t.colorScheme() != null && t.colorScheme().primary() != null) {
                return t.colorScheme().primary().rgb();
            }
        } catch (Throwable noTheme) {
            // an unthemed indicator still has to paint
        }
        return 0x6200EE;
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        return new Arc();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double d = Dp.px(DIAMETER_LP);
        double w = constraints.hasBoundedWidth() && constraints.maxWidth() < d
                ? constraints.maxWidth() : d;
        double h = constraints.hasBoundedHeight() && constraints.maxHeight() < d
                ? constraints.maxHeight() : d;
        return constraints.constrain(new Size(w, h));
    }

    /** The component that does the drawing, and keeps the spin running. */
    private final class Arc extends Component {

        Arc() {
            setUIID("Container");
            getAllStyles().setBgTransparency(0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setMargin(0, 0, 0, 0);
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            // A bare Component prefers nothing at all, and a parent that lays
            // its children out by preferred size then gives this one a zero box
            // -- which paints nothing, however correct the arc code is.
            int d = (int) Math.round(Dp.px(DIAMETER_LP));
            return new com.codename1.ui.geom.Dimension(d, d);
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            com.codename1.ui.Form f = getComponentForm();
            // Null-checked: initComponent can run before the component has a
            // form, and an exception thrown here leaves the component
            // uninitialised -- which shows up not as a crash but as a box that
            // is laid out correctly and never paints.
            if (f != null && indicator().getValue() == null) {
                f.registerAnimated(this);
            }
        }

        @Override
        protected void deinitialize() {
            com.codename1.ui.Form f = getComponentForm();
            if (f != null) {
                f.deregisterAnimated(this);
            }
            super.deinitialize();
        }

        @Override
        public boolean animate() {
            // Indeterminate only; a determinate arc repaints when its value changes.
            return indicator().getValue() == null;
        }

        @Override
        public void paint(Graphics g) {
            int stroke = strokePx();
            int d = Math.min(getWidth(), getHeight()) - stroke;
            if (d <= 0) {
                return;
            }
            int x = getX() + (getWidth() - d) / 2;
            int y = getY() + (getHeight() - d) / 2;
            Double v = indicator().getValue();
            int startDeg;
            int sweepDeg;
            if (v != null) {
                // Clockwise from twelve o'clock: Codename One's zero is three
                // o'clock and its positive direction is anticlockwise, so the
                // sweep is negated and the start is a quarter turn ahead.
                startDeg = 90;
                sweepDeg = -(int) Math.round(360 * Math.max(0, Math.min(1, v.doubleValue())));
            } else {
                long phase = System.currentTimeMillis() % PERIOD_MS;
                startDeg = 90 - (int) Math.round(360.0 * phase / PERIOD_MS);
                sweepDeg = -(int) Math.round(360 * SWEEP_FRACTION);
            }
            g.setColor(arcRgb());
            g.setAntiAliased(true);
            if (g.isShapeSupported()) {
                // A STROKED shape: Codename One's Graphics has no stroke width,
                // so a plain arc is a hairline whatever Material asks for.
                // GeneralPath.arc takes RADIANS.
                com.codename1.ui.geom.GeneralPath path =
                        new com.codename1.ui.geom.GeneralPath();
                path.arc(x, y, d, d,
                        Math.toRadians(startDeg), Math.toRadians(sweepDeg));
                g.drawShape(path, new com.codename1.ui.Stroke(stroke,
                        com.codename1.ui.Stroke.CAP_BUTT,
                        com.codename1.ui.Stroke.JOIN_MITER, 1f));
            } else {
                // Without shapes, a ring of concentric hairlines. Cruder, but a
                // visible indicator beats a correct one nobody can see.
                for (int i = 0; i < stroke; i++) {
                    g.drawArc(x + i, y + i, d - 2 * i, d - 2 * i, startDeg, sweepDeg);
                }
            }
        }
    }
}
