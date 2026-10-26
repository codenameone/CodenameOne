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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.Adjustable;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.event.AdjustmentEvent;
import com.codename1.desktopcompat.java.awt.event.AdjustmentListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;

/// A scroll bar over a [BoundedRangeModel].
///
/// A track with a thumb, drawn here, that the user drags; a press on the
/// track moves the value by a block. There are no arrow buttons.
///
/// The scroll bars of a [JScrollPane] are these too. A desktop port gives
/// them room beside the viewport; on a touch device they have no size,
/// the user scrolls the content itself and Codename One draws its own
/// scroll indicator, while the bar's model still follows and steers the
/// view.
public class JScrollBar extends JComponent implements Accessible, Adjustable {

    private static final int THICKNESS = 12;

    protected BoundedRangeModel model;

    protected int orientation;

    protected int unitIncrement;

    protected int blockIncrement;

    private final ChangeListener forward = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            fireAdjustmentValueChanged(AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, AdjustmentEvent.TRACK,
                    model.getValue(), model.getValueIsAdjusting());
            repaint();
        }
    };
    private int dragOffset = -1;

    public JScrollBar(int orientation, int value, int extent, int min, int max) {
        checkOrientation(orientation);
        this.unitIncrement = 1;
        this.blockIncrement = extent == 0 ? 1 : extent;
        this.orientation = orientation;
        this.model = new DefaultBoundedRangeModel(value, extent, min, max);
        this.model.addChangeListener(forward);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public JScrollBar(int orientation) {
        this(orientation, 0, 10, 0, 100);
    }

    public JScrollBar() {
        this(VERTICAL);
    }

    private static void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    @Override
    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        checkOrientation(orientation);
        int old = this.orientation;
        this.orientation = orientation;
        firePropertyChange("orientation", old, orientation);
        if (old != orientation) {
            revalidate();
        }
    }

    public BoundedRangeModel getModel() {
        return model;
    }

    public void setModel(BoundedRangeModel newModel) {
        BoundedRangeModel old = model;
        if (old != null) {
            old.removeChangeListener(forward);
        }
        model = newModel;
        if (newModel != null) {
            newModel.addChangeListener(forward);
        }
        firePropertyChange("model", old, newModel);
        repaint();
    }

    public int getUnitIncrement(int direction) {
        return unitIncrement;
    }

    @Override
    public void setUnitIncrement(int unitIncrement) {
        int old = this.unitIncrement;
        this.unitIncrement = unitIncrement;
        firePropertyChange("unitIncrement", old, unitIncrement);
    }

    public int getBlockIncrement(int direction) {
        return blockIncrement;
    }

    @Override
    public void setBlockIncrement(int blockIncrement) {
        int old = this.blockIncrement;
        this.blockIncrement = blockIncrement;
        firePropertyChange("blockIncrement", old, blockIncrement);
    }

    @Override
    public int getUnitIncrement() {
        return unitIncrement;
    }

    @Override
    public int getBlockIncrement() {
        return blockIncrement;
    }

    @Override
    public int getValue() {
        return model.getValue();
    }

    @Override
    public void setValue(int value) {
        model.setValue(value);
    }

    @Override
    public int getVisibleAmount() {
        return model.getExtent();
    }

    @Override
    public void setVisibleAmount(int extent) {
        model.setExtent(extent);
    }

    @Override
    public int getMinimum() {
        return model.getMinimum();
    }

    @Override
    public void setMinimum(int minimum) {
        model.setMinimum(minimum);
    }

    @Override
    public int getMaximum() {
        return model.getMaximum();
    }

    @Override
    public void setMaximum(int maximum) {
        model.setMaximum(maximum);
    }

    public boolean getValueIsAdjusting() {
        return model.getValueIsAdjusting();
    }

    public void setValueIsAdjusting(boolean b) {
        model.setValueIsAdjusting(b);
    }

    public void setValues(int newValue, int newExtent, int newMin, int newMax) {
        model.setRangeProperties(newValue, newExtent, newMin, newMax, model.getValueIsAdjusting());
    }

    @Override
    public void addAdjustmentListener(AdjustmentListener l) {
        listenerList.add(AdjustmentListener.class, l);
    }

    @Override
    public void removeAdjustmentListener(AdjustmentListener l) {
        listenerList.remove(AdjustmentListener.class, l);
    }

    public AdjustmentListener[] getAdjustmentListeners() {
        return listenerList.getListeners(AdjustmentListener.class);
    }

    protected void fireAdjustmentValueChanged(int id, int type, int value) {
        fireAdjustmentValueChanged(id, type, value, getValueIsAdjusting());
    }

    private void fireAdjustmentValueChanged(int id, int type, int value, boolean isAdjusting) {
        AdjustmentListener[] ls = getAdjustmentListeners();
        AdjustmentEvent e = null;
        for (int i = ls.length - 1; i >= 0; i--) {
            if (e == null) {
                e = new AdjustmentEvent(this, id, type, value, isAdjusting);
            }
            ls[i].adjustmentValueChanged(e);
        }
    }

    // ------------------------------------------------------------ look

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return orientation == VERTICAL ? new Dimension(THICKNESS, 48) : new Dimension(48, THICKNESS);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return orientation == VERTICAL ? new Dimension(THICKNESS, 5) : new Dimension(5, THICKNESS);
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return orientation == VERTICAL ? new Dimension(THICKNESS, Short.MAX_VALUE)
                : new Dimension(Short.MAX_VALUE, THICKNESS);
    }

    private int trackLength() {
        return orientation == VERTICAL ? getHeight() : getWidth();
    }

    /// The thumb as `{start, length}` along the track.
    private int[] thumb() {
        int track = trackLength();
        int range = model.getMaximum() - model.getMinimum();
        if (range <= 0 || track <= 0) {
            return new int[]{0, track};
        }
        int len = Math.max(Math.min(track, 16), (int) ((long) track * model.getExtent() / range));
        if (len > track) {
            len = track;
        }
        int free = range - model.getExtent();
        int start = free <= 0 ? 0 : (int) ((long) (track - len) * (model.getValue() - model.getMinimum()) / free);
        return new int[]{start, len};
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color fg = getForeground();
        if (fg == null) {
            fg = Color.GRAY;
        }
        g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40));
        g.fillRect(0, 0, getWidth(), getHeight());
        int[] t = thumb();
        g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), isEnabled() ? 150 : 70));
        if (orientation == VERTICAL) {
            g.fillRoundRect(2, t[0], Math.max(1, getWidth() - 4), t[1], 6, 6);
        } else {
            g.fillRoundRect(t[0], 2, t[1], Math.max(1, getHeight() - 4), 6, 6);
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (!isEnabled()) {
            return;
        }
        int at = orientation == VERTICAL ? e.getY() : e.getX();
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            int[] t = thumb();
            if (at >= t[0] && at < t[0] + t[1]) {
                dragOffset = at - t[0];
                model.setValueIsAdjusting(true);
            } else {
                dragOffset = -1;
                model.setValue(model.getValue() + (at < t[0] ? -blockIncrement : blockIncrement));
            }
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED && dragOffset >= 0) {
            dragOffset = -1;
            model.setValueIsAdjusting(false);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && dragOffset >= 0) {
            int at = (orientation == VERTICAL ? e.getY() : e.getX()) - dragOffset;
            int[] t = thumb();
            int room = trackLength() - t[1];
            int free = model.getMaximum() - model.getMinimum() - model.getExtent();
            if (room > 0 && free > 0) {
                model.setValue(model.getMinimum() + (int) ((long) at * free / room));
            }
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",orientation=" + (orientation == HORIZONTAL ? "HORIZONTAL" : "VERTICAL");
    }
}
