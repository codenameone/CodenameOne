/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.geom.Ellipse2D;

/// Lays a soft highlight over the top or bottom half of the area: the
/// part of a wide ellipse that falls inside it, in a translucent paint.
/// It is meant to be the last painter of a compound.
public class GlossPainter extends AbstractPainter<Object> {

    /// The half of the area the highlight covers.
    public enum GlossPosition {
        TOP,
        BOTTOM
    }

    private Paint paint;
    private GlossPosition position;

    /// A white highlight of a fifth opacity at the top.
    public GlossPainter() {
        this(new Color(1.0f, 1.0f, 1.0f, 0.2f), GlossPosition.TOP);
    }

    public GlossPainter(Paint paint) {
        this(paint, GlossPosition.TOP);
    }

    public GlossPainter(GlossPosition position) {
        this(new Color(1.0f, 1.0f, 1.0f, 0.2f), position);
    }

    public GlossPainter(Paint paint, GlossPosition position) {
        this.paint = paint;
        this.position = position == null ? GlossPosition.TOP : position;
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        if (paint == null) {
            return;
        }
        // An ellipse a third wider than the area on either side whose
        // lower (or upper) edge crosses the middle of the area.
        double w = width * 5.0 / 3.0;
        double h = height * 1.2;
        double x = -width / 3.0;
        double y = position == GlossPosition.TOP ? height / 2.0 - h : height / 2.0;
        g.clipRect(0, 0, width, height);
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(x, y, w, h));
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        Paint old = this.paint;
        this.paint = paint;
        setDirty(true);
        firePropertyChange("paint", old, paint);
    }

    public GlossPosition getPosition() {
        return position;
    }

    /// Sets the half that is highlighted; `null` is the top.
    public void setPosition(GlossPosition position) {
        GlossPosition old = this.position;
        this.position = position == null ? GlossPosition.TOP : position;
        setDirty(true);
        firePropertyChange("position", old, this.position);
    }
}
