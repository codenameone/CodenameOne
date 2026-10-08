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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.SliderPeer;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.events.DataChangedListener;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/// A slider over a bounded range model, shown and dragged by a Codename
/// One slider.
///
/// Dragging sets the model's value with `valueIsAdjusting` true and ends
/// with it false, as on the desktop; a value set through the model moves
/// the native thumb without coming back as a change. The native track is
/// drawn in a band as thick as the Codename One slider wants; the tick
/// marks and the labels of the label table are painted below it (to its
/// right when vertical) by this class, so `paintTicks`, `paintLabels`,
/// `paintTrack`, `inverted` and `snapToTicks` all take effect.
///
/// Not supported: the standard labels made by `createStandardLabels` are
/// made once and do not follow later changes of the range; there is no UI
/// delegate, so `getUI`, `setUI` and the client property
/// `JSlider.isFilled` are absent; the keyboard moves the thumb only as far
/// as the Codename One slider implements it.
public class JSlider extends JComponent implements SwingConstants {

    private static final int LENGTH = 200;
    private static final int MINIMUM_LENGTH = 36;
    private static final int FALLBACK_THICKNESS = 16;
    private static final int MAJOR_TICK = 8;
    private static final int MINOR_TICK = 4;
    private static final int MAX_TICKS = 2000;

    protected BoundedRangeModel sliderModel;
    protected int majorTickSpacing;
    protected int minorTickSpacing;
    protected boolean snapToTicks;
    protected int orientation;
    protected ChangeListener changeListener;
    protected ChangeEvent changeEvent;

    private final ChangeListener bridge = new Bridge();
    private boolean paintTicks;
    private boolean paintTrack = true;
    private boolean paintLabels;
    private boolean inverted;
    @SuppressWarnings("rawtypes")
    private Dictionary labelTable;
    private boolean syncing;

    public JSlider() {
        this(HORIZONTAL, 0, 100, 50);
    }

    public JSlider(int orientation) {
        this(orientation, 0, 100, 50);
    }

    public JSlider(int min, int max) {
        this(HORIZONTAL, min, max, (min + max) / 2);
    }

    public JSlider(int min, int max, int value) {
        this(HORIZONTAL, min, max, value);
    }

    public JSlider(int orientation, int min, int max, int value) {
        checkOrientation(orientation);
        this.orientation = orientation;
        sliderModel = new DefaultBoundedRangeModel(value, 0, min, max);
        sliderModel.addChangeListener(bridge);
    }

    public JSlider(BoundedRangeModel brm) {
        orientation = HORIZONTAL;
        sliderModel = brm;
        if (brm != null) {
            brm.addChangeListener(bridge);
        }
    }

    private static void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    // ------------------------------------------------------------- peer

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new SliderPeer(this);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof SliderPeer) {
            final SliderPeer s = (SliderPeer) p;
            s.addDataChangedListener(new DataChangedListener() {
                @Override
                public void dataChanged(int type, int index) {
                    // CHANGED is the slider told its value by this class;
                    // the user's drags and keys report ADDED or REMOVED.
                    if (!syncing && type != DataChangedListener.CHANGED) {
                        cn1FromNative(index, true);
                    }
                }
            });
            s.addActionListener(new com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>() {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                    if (!syncing) {
                        cn1FromNative(s.getProgress(), false);
                    }
                }
            });
        }
        cn1Sync();
    }

    /// The length of the native slider's range: the model's, or one for
    /// an empty range, which the native slider cannot draw.
    private int nativeRange() {
        long span = (long) getMaximum() - (long) getMinimum();
        if (span <= 0) {
            return 1;
        }
        return span > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) span;
    }

    private void cn1Sync() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (!(p instanceof SliderPeer) || sliderModel == null) {
            return;
        }
        SliderPeer s = (SliderPeer) p;
        boolean was = syncing;
        syncing = true;
        try {
            int range = nativeRange();
            long pos = (long) getValue() - (long) getMinimum();
            if (pos < 0) {
                pos = 0;
            }
            if (pos > range) {
                pos = range;
            }
            if (inverted) {
                pos = range - pos;
            }
            s.setVertical(orientation == VERTICAL);
            s.setMinValue(0);
            s.setMaxValue(range);
            s.setProgress((int) pos);
        } finally {
            syncing = was;
        }
    }

    /// The native slider was moved by the user to `pos` of its range.
    private void cn1FromNative(int pos, boolean adjusting) {
        BoundedRangeModel m = sliderModel;
        if (m == null) {
            return;
        }
        int min = m.getMinimum();
        int max = m.getMaximum();
        int extent = m.getExtent();
        int range = nativeRange();
        long v = min;
        if (max > min) {
            int at = pos < 0 ? 0 : pos > range ? range : pos;
            v = (long) min + (inverted ? range - at : at);
        }
        if (snapToTicks) {
            v = snap(v, min);
        }
        if (v > (long) max - extent) {
            v = (long) max - extent;
        }
        if (v < min) {
            v = min;
        }
        m.setRangeProperties((int) v, extent, min, max, adjusting);
        // The model may have refused or rounded the position.
        cn1Sync();
    }

    private long snap(long value, int min) {
        int spacing = minorTickSpacing > 0 ? minorTickSpacing : majorTickSpacing;
        if (spacing <= 0) {
            return value;
        }
        long off = value - min;
        long down = off - off % spacing;
        return min + (off - down >= spacing - (off - down) ? down + spacing : down);
    }

    // ------------------------------------------------------------ sizes

    private int nativeThickness() {
        Dimension d = super.cn1NativePreferredSize();
        if (d == null) {
            return FALLBACK_THICKNESS;
        }
        return orientation == VERTICAL ? d.width : d.height;
    }

    /// The width (for a vertical slider) or height of the largest label,
    /// or zero when labels are not painted.
    private int labelExtent() {
        if (!paintLabels || labelTable == null) {
            return 0;
        }
        int most = 0;
        Enumeration<?> e = labelTable.elements();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof Component) {
                Dimension d = ((Component) o).getPreferredSize();
                most = Math.max(most, orientation == VERTICAL ? d.width : d.height);
            }
        }
        return most;
    }

    private int across() {
        return nativeThickness() + (paintTicks ? MAJOR_TICK : 0) + labelExtent();
    }

    /// The preferred size: 200 logical pixels along the slider, and across
    /// it the native track, the tick marks and the labels.
    @Override
    protected Dimension cn1NativePreferredSize() {
        return orientation == VERTICAL ? new Dimension(across(), LENGTH) : new Dimension(LENGTH, across());
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return orientation == VERTICAL ? new Dimension(across(), MINIMUM_LENGTH)
                : new Dimension(MINIMUM_LENGTH, across());
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return orientation == VERTICAL ? new Dimension(across(), Short.MAX_VALUE)
                : new Dimension(Short.MAX_VALUE, across());
    }

    // ------------------------------------------------------------ paint

    /// Paints the native track in its band, then the tick marks and the
    /// labels beside it.
    @Override
    protected void paintComponent(Graphics g) {
        boolean vertical = orientation == VERTICAL;
        int thick = nativeThickness();
        int room = vertical ? getWidth() : getHeight();
        int start = Math.max(0, (room - across()) / 2);
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (p instanceof SliderPeer) {
            ((SliderPeer) p).setBand(Units.toDevice(start), Units.toDeviceSize(start, Math.min(thick, room)));
        }
        if (paintTrack) {
            super.paintComponent(g);
        }
        int at = start + thick;
        if (paintTicks) {
            Color fg = getForeground();
            g.setColor(fg != null ? fg : Color.GRAY);
            paintTickMarks(g, at, minorTickSpacing, MINOR_TICK);
            paintTickMarks(g, at, majorTickSpacing, MAJOR_TICK);
            at += MAJOR_TICK;
        }
        if (paintLabels && labelTable != null) {
            paintLabelTable(g, at);
        }
    }

    /// Where along the slider `value` is, in logical pixels.
    private int position(long value) {
        int length = (orientation == VERTICAL ? getHeight() : getWidth()) - 1;
        long min = getMinimum();
        long span = (long) getMaximum() - min;
        double f = span <= 0 ? 0 : (double) (value - min) / (double) span;
        if (inverted) {
            f = 1 - f;
        }
        if (orientation == VERTICAL) {
            f = 1 - f;
        }
        return (int) Math.round(f * Math.max(0, length));
    }

    private void paintTickMarks(Graphics g, int at, int spacing, int length) {
        if (spacing <= 0) {
            return;
        }
        long min = getMinimum();
        long max = getMaximum();
        if ((max - min) / spacing > MAX_TICKS) {
            return;
        }
        for (long v = min; v <= max; v += spacing) {
            int pos = position(v);
            if (orientation == VERTICAL) {
                g.drawLine(at, pos, at + length - 1, pos);
            } else {
                g.drawLine(pos, at, pos, at + length - 1);
            }
        }
    }

    private void paintLabelTable(Graphics g, int at) {
        Enumeration<?> keys = labelTable.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object label = labelTable.get(key);
            if (!(key instanceof Number) || !(label instanceof Component)) {
                continue;
            }
            Component c = (Component) label;
            Dimension d = c.getPreferredSize();
            if (c.getWidth() != d.width || c.getHeight() != d.height) {
                c.setSize(d.width, d.height);
            }
            int pos = position(((Number) key).longValue());
            int x;
            int y;
            if (orientation == VERTICAL) {
                x = at;
                y = clamp(pos - d.height / 2, getHeight() - d.height);
            } else {
                x = clamp(pos - d.width / 2, getWidth() - d.width);
                y = at;
            }
            Graphics lg = g.create(x, y, d.width, d.height);
            try {
                c.paint(lg);
            } finally {
                lg.dispose();
            }
        }
    }

    private static int clamp(int v, int most) {
        return v > most ? Math.max(0, most) : v < 0 ? 0 : v;
    }

    // ------------------------------------------------------------ model

    /// The listener that turns the model's changes into this slider's.
    protected ChangeListener createChangeListener() {
        return new ModelListener();
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    public BoundedRangeModel getModel() {
        return sliderModel;
    }

    public void setModel(BoundedRangeModel newModel) {
        BoundedRangeModel old = sliderModel;
        if (old != null) {
            old.removeChangeListener(bridge);
        }
        sliderModel = newModel;
        if (newModel != null) {
            newModel.addChangeListener(bridge);
        }
        firePropertyChange("model", old, newModel);
        cn1Sync();
        repaint();
    }

    public int getValue() {
        return sliderModel.getValue();
    }

    public void setValue(int n) {
        BoundedRangeModel m = sliderModel;
        if (m.getValue() != n) {
            m.setValue(n);
        }
    }

    public int getMinimum() {
        return sliderModel.getMinimum();
    }

    public void setMinimum(int minimum) {
        int old = sliderModel.getMinimum();
        sliderModel.setMinimum(minimum);
        firePropertyChange("minimum", old, minimum);
    }

    public int getMaximum() {
        return sliderModel.getMaximum();
    }

    public void setMaximum(int maximum) {
        int old = sliderModel.getMaximum();
        sliderModel.setMaximum(maximum);
        firePropertyChange("maximum", old, maximum);
    }

    public boolean getValueIsAdjusting() {
        return sliderModel.getValueIsAdjusting();
    }

    public void setValueIsAdjusting(boolean b) {
        sliderModel.setValueIsAdjusting(b);
    }

    public int getExtent() {
        return sliderModel.getExtent();
    }

    public void setExtent(int extent) {
        sliderModel.setExtent(extent);
    }

    // ------------------------------------------------------- properties

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        checkOrientation(orientation);
        int old = this.orientation;
        this.orientation = orientation;
        firePropertyChange("orientation", old, orientation);
        if (orientation != old) {
            changed();
        }
    }

    private void changed() {
        cn1Sync();
        revalidate();
        repaint();
    }

    @SuppressWarnings("rawtypes")
    public Dictionary getLabelTable() {
        return labelTable;
    }

    /// Sets what is drawn at which value: a dictionary from `Integer` to
    /// component, usually a label.
    @SuppressWarnings("rawtypes")
    public void setLabelTable(Dictionary labels) {
        Dictionary old = labelTable;
        labelTable = labels;
        updateLabelUIs();
        firePropertyChange("labelTable", old, labels);
        if (labels != old) {
            revalidate();
            repaint();
        }
    }

    /// Gives every label of the label table its preferred size.
    protected void updateLabelUIs() {
        if (labelTable == null) {
            return;
        }
        Enumeration<?> e = labelTable.elements();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof JComponent) {
                JComponent c = (JComponent) o;
                Dimension d = c.getPreferredSize();
                c.setSize(d.width, d.height);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public Hashtable createStandardLabels(int increment) {
        return createStandardLabels(increment, getMinimum());
    }

    /// A label table with a text label for `start` and every `increment`
    /// after it up to the maximum.
    @SuppressWarnings("rawtypes")
    public Hashtable createStandardLabels(int increment, int start) {
        if (start > getMaximum() || start < getMinimum()) {
            throw new IllegalArgumentException("Slider label start point out of range.");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("Label incremement must be > 0");
        }
        Hashtable<Integer, JComponent> table = new Hashtable<Integer, JComponent>();
        long max = getMaximum();
        for (long v = start; v <= max; v += increment) {
            table.put(Integer.valueOf((int) v), new JLabel(String.valueOf(v), CENTER));
        }
        return table;
    }

    public boolean getInverted() {
        return inverted;
    }

    public void setInverted(boolean b) {
        boolean old = inverted;
        inverted = b;
        firePropertyChange("inverted", old, b);
        if (b != old) {
            cn1Sync();
            repaint();
        }
    }

    public int getMajorTickSpacing() {
        return majorTickSpacing;
    }

    public void setMajorTickSpacing(int n) {
        int old = majorTickSpacing;
        majorTickSpacing = n;
        if (labelTable == null && n > 0 && paintLabels) {
            setLabelTable(createStandardLabels(n));
        }
        firePropertyChange("majorTickSpacing", old, n);
        if (n != old && paintTicks) {
            repaint();
        }
    }

    public int getMinorTickSpacing() {
        return minorTickSpacing;
    }

    public void setMinorTickSpacing(int n) {
        int old = minorTickSpacing;
        minorTickSpacing = n;
        firePropertyChange("minorTickSpacing", old, n);
        if (n != old && paintTicks) {
            repaint();
        }
    }

    public boolean getSnapToTicks() {
        return snapToTicks;
    }

    public void setSnapToTicks(boolean b) {
        boolean old = snapToTicks;
        snapToTicks = b;
        firePropertyChange("snapToTicks", old, b);
    }

    public boolean getPaintTicks() {
        return paintTicks;
    }

    public void setPaintTicks(boolean b) {
        boolean old = paintTicks;
        paintTicks = b;
        firePropertyChange("paintTicks", old, b);
        if (b != old) {
            revalidate();
            repaint();
        }
    }

    public boolean getPaintTrack() {
        return paintTrack;
    }

    public void setPaintTrack(boolean b) {
        boolean old = paintTrack;
        paintTrack = b;
        firePropertyChange("paintTrack", old, b);
        if (b != old) {
            repaint();
        }
    }

    public boolean getPaintLabels() {
        return paintLabels;
    }

    public void setPaintLabels(boolean b) {
        boolean old = paintLabels;
        paintLabels = b;
        if (labelTable == null && majorTickSpacing > 0) {
            setLabelTable(createStandardLabels(majorTickSpacing));
        }
        firePropertyChange("paintLabels", old, b);
        if (b != old) {
            revalidate();
            repaint();
        }
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",isInverted=" + inverted + ",majorTickSpacing=" + majorTickSpacing
                + ",minorTickSpacing=" + minorTickSpacing + ",orientation="
                + (orientation == HORIZONTAL ? "HORIZONTAL" : "VERTICAL") + ",paintLabels=" + paintLabels
                + ",paintTicks=" + paintTicks + ",paintTrack=" + paintTrack + ",snapToTicks=" + snapToTicks
                + ",snapToValue=true";
    }

    /// Listens to the model for as long as it is this slider's, and hands
    /// what it hears to the listener `createChangeListener` makes.
    private final class Bridge implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (changeListener == null) {
                changeListener = createChangeListener();
            }
            cn1Sync();
            if (changeListener != null) {
                changeListener.stateChanged(e);
            }
            repaint();
        }
    }

    private final class ModelListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }
}
