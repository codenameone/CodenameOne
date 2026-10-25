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

import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Paint;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;

/// Fills the whole painted area with one paint: a color, or a gradient
/// that can be stretched over the area.
public class MattePainter extends AbstractAreaPainter<Object> {

    public MattePainter() {
    }

    public MattePainter(Paint paint) {
        super(paint);
    }

    public MattePainter(Paint paint, boolean paintStretched) {
        super(paint);
        setPaintStretched(paintStretched);
    }

    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        Paint p = getFillPaint();
        if (p == null) {
            return;
        }
        g.setPaint(cn1Fitted(p, width, height));
        g.fillRect(0, 0, width, height);
    }

    @Override
    protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        return new Rectangle(0, 0, width, height);
    }
}
