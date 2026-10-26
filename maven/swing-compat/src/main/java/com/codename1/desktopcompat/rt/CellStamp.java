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
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// A Codename One component that paints a component of this layer, for the
/// places where Codename One asks for a renderer of its own: the rows of a
/// combo box popup.
///
/// The cell is never added to anything. It is given the size this stamp was
/// laid out at, in logical pixels, and painted through the same graphics a
/// peer paints its owner with.
public final class CellStamp extends com.codename1.ui.Component {

    private Component cell;

    public CellStamp() {
        PeerSupport.strip(this);
        setFocusable(false);
    }

    /// The component to paint next; `null` paints nothing.
    public void setCell(Component cell) {
        this.cell = cell;
        setShouldCalcPreferredSize(true);
    }

    @Override
    protected Dimension calcPreferredSize() {
        if (cell == null) {
            return new Dimension(0, 0);
        }
        com.codename1.desktopcompat.java.awt.Dimension d = cell.getPreferredSize();
        return new Dimension(Units.toDevice(d.width), Units.toDevice(d.height));
    }

    @Override
    public void paint(Graphics g) {
        if (cell == null) {
            return;
        }
        cell.setBounds(0, 0, Units.toLogical(getWidth()), Units.toLogical(getHeight()));
        cell.validate();
        G2D g2 = G2D.forPeer(g, getX(), getY(), getWidth(), getHeight());
        try {
            cell.paint(g2);
        } finally {
            g2.finish();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
    }
}
