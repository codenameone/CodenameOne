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
package com.codename1.desktopcompat.org.jdesktop.swingx.rollover;

import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseListener;
import com.codename1.desktopcompat.java.awt.event.MouseMotionListener;
import com.codename1.desktopcompat.javax.swing.JComponent;

/// Turns pointer events over a component into two client properties of
/// it: [#ROLLOVER_KEY], the cell under the pointer, and [#CLICKED_KEY],
/// the cell that was clicked. Both hold a `Point` whose `x` is the column
/// and whose `y` is the row, `-1` for none.
///
/// A touch screen has no pointer to hover with, so there the rollover
/// cell is the cell last touched.
public abstract class RolloverProducer implements MouseListener, MouseMotionListener {

    public static final String CLICKED_KEY = "swingx.clicked";
    public static final String ROLLOVER_KEY = "swingx.rollover";

    /// The cell under the pointer as of the last event.
    protected Point rollover = new Point(-1, -1);

    private Point cn1Pressed;
    private boolean cn1Dragged;

    public RolloverProducer() {
    }

    public void install(JComponent component) {
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
    }

    public void release(JComponent component) {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point pressed = cn1Pressed;
        boolean dragged = cn1Dragged;
        cn1Pressed = null;
        cn1Dragged = false;
        updateRollover(e, ROLLOVER_KEY, false);
        if (isClick(e, pressed, dragged)) {
            updateRollover(e, CLICKED_KEY, true);
        }
    }

    /// Whether a release over the current cell is a click: the press was
    /// on the same cell and the pointer was not dragged between.
    protected boolean isClick(MouseEvent e, Point oldRollover, boolean wasDragging) {
        return oldRollover != null && oldRollover.equals(rollover) && !wasDragging;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        updateRollover(e, ROLLOVER_KEY, false);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        rollover.setLocation(-1, -1);
        Object source = e.getSource();
        if (source instanceof JComponent) {
            ((JComponent) source).putClientProperty(ROLLOVER_KEY, null);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        updateRollover(e, ROLLOVER_KEY, false);
        cn1Pressed = new Point(rollover);
        cn1Dragged = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        cn1Dragged = true;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateRollover(e, ROLLOVER_KEY, false);
    }

    protected void updateRollover(MouseEvent e, String property, boolean fireAlways) {
        Object source = e.getSource();
        if (!(source instanceof JComponent)) {
            return;
        }
        JComponent component = (JComponent) source;
        updateRolloverPoint(component, e.getPoint());
        updateClientProperty(component, property, fireAlways);
    }

    /// Writes the current cell into the property. Without `fireAlways`
    /// nothing is written when the property already holds that cell.
    protected void updateClientProperty(JComponent component, String property, boolean fireAlways) {
        if (fireAlways) {
            component.putClientProperty(property, null);
            component.putClientProperty(property, new Point(rollover));
            return;
        }
        Object old = component.getClientProperty(property);
        if (rollover.equals(old)) {
            return;
        }
        component.putClientProperty(property, new Point(rollover));
    }

    /// Sets [#rollover] to the cell of `component` at `mousePoint`.
    protected abstract void updateRolloverPoint(JComponent component, Point mousePoint);
}
