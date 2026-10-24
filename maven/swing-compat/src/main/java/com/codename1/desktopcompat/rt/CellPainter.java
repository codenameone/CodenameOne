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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Graphics;

/// Paints a renderer component -- one that is in no window and is used
/// like a rubber stamp -- into a cell.
///
/// The component is given the cell's bounds, laid out if it has children,
/// and its `paint` runs with a graphics moved and clipped to the cell.
/// A renderer backed by a Codename One widget (a label, a check box)
/// paints that widget; one that overrides `paintComponent` paints itself.
public final class CellPainter {

    private CellPainter() {
    }

    /// Paints `c` over the rectangle of `g` given in logical pixels.
    public static void paint(Graphics g, Component c, int x, int y, int w, int h) {
        if (c == null || w <= 0 || h <= 0) {
            return;
        }
        c.setBounds(x, y, w, h);
        if (c instanceof Container) {
            ((Container) c).validate();
        }
        Graphics cg = g.create(x, y, w, h);
        try {
            c.paint(cg);
        } finally {
            cg.dispose();
        }
    }
}
