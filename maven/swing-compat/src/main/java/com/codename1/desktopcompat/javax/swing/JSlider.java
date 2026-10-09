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
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.rt.CellTheme;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/// A slider over a bounded range model, painted and dragged by this
/// class.
///
/// It is drawn here -- a track, the part of it up to the value in the
/// theme's accent color, and a round thumb -- and not by a Codename One
/// slider, whose thumb is an image only some themes have and whose vertical
/// form draws no track at all. The colors are the theme's, so it still
/// belongs to the widgets around it; a color put into [UIManager] under
/// `Slider.foreground` (the thumb and the filled part) or
/// `Slider.trackColor` wins.
///
/// Pressing anywhere on the slider moves the thumb there and dragging
/// follows the pointer, setting the model's value with `valueIsAdjusting`
/// true and ending with it false, as on the desktop. The arrow keys, Page
/// Up, Page Down, Home and End move a focused slider. The thumb's centre
/// travels between half a thumb from either end, and the tick marks and
/// the labels of the label table are painted below the track (to its right
/// when vertical) at those same positions, so `paintTicks`, `paintLabels`,
/// `paintTrack`, `inverted` and `snapToTicks` all take effect.
///
/// Not supported: the standard labels made by `createStandardLabels` are
/// made once and do not follow later changes of the range; there is no UI
/// delegate, so `getUI`, `setUI` and the client property
/// `JSlider.isFilled` are absent.
public class JSlider extends JComponent implements Accessible, SwingConstants {

    private static final int LENGTH = 200;
    private static final int MINIMUM_LENGTH = 36;
    private static final int THUMB = 16;
    private static final int TOUCH_THUMB = 24;
    private static final int TRACK = 4;
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
    private boolean dragging;

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
        cn1Listen();
    }

    public JSlider(BoundedRangeModel brm) {
        cn1Listen();
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

    // ------------------------------------------------------------ input

    private void cn1Listen() {
        enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        setFocusable(true);
    }

    /// The diameter of the thumb, which is also how thick the band of
    /// the track is: larger where the pointer is a finger.
    private static int thumbSize() {
        return CellTheme.touch() ? TOUCH_THUMB : THUMB;
    }

    /// The length of the slider and how far in from either end the
    /// thumb's centre stops.
    private int length() {
        return orientation == VERTICAL ? getHeight() : getWidth();
    }

    private int inset() {
        return Math.min(thumbSize() / 2, Math.max(0, length() / 2));
    }

    /// Moves the value to where the pointer is, `at` logical pixels along
    /// the slider.
    private void cn1MoveTo(int at, boolean adjusting) {
        BoundedRangeModel m = sliderModel;
        if (m == null) {
            return;
        }
        int min = m.getMinimum();
        int max = m.getMaximum();
        int inset = inset();
        int room = length() - 2 * inset;
        double f = room <= 0 ? 0 : (double) (at - inset) / (double) room;
        f = f < 0 ? 0 : f > 1 ? 1 : f;
        if (orientation == VERTICAL) {
            f = 1 - f;
        }
        if (inverted) {
            f = 1 - f;
        }
        long v = min + Math.round(f * ((double) max - (double) min));
        cn1Set(v, adjusting);
    }

    private void cn1Set(long value, boolean adjusting) {
        BoundedRangeModel m = sliderModel;
        int min = m.getMinimum();
        int max = m.getMaximum();
        int extent = m.getExtent();
        long v = value;
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
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (!isEnabled() || sliderModel == null) {
            return;
        }
        int at = orientation == VERTICAL ? e.getY() : e.getX();
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            dragging = true;
            requestFocusInWindow();
            cn1MoveTo(at, true);
        } else if (e.getID() == MouseEvent.MOUSE_RELEASED && dragging) {
            dragging = false;
            sliderModel.setValueIsAdjusting(false);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && dragging && isEnabled() && sliderModel != null) {
            cn1MoveTo(orientation == VERTICAL ? e.getY() : e.getX(), true);
        }
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (e.getID() != KeyEvent.KEY_PRESSED || e.isConsumed() || !isEnabled() || sliderModel == null) {
            return;
        }
        long span = (long) getMaximum() - (long) getMinimum();
        long unit = snapToTicks && minorTickSpacing > 0 ? minorTickSpacing
                : snapToTicks && majorTickSpacing > 0 ? majorTickSpacing : 1;
        long block = Math.max(unit, majorTickSpacing > 0 ? majorTickSpacing : span / 10);
        long dir = inverted ? -1 : 1;
        long v = getValue();
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_UP) {
            v += dir * unit;
        } else if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_DOWN) {
            v -= dir * unit;
        } else if (code == KeyEvent.VK_PAGE_UP) {
            v += dir * block;
        } else if (code == KeyEvent.VK_PAGE_DOWN) {
            v -= dir * block;
        } else if (code == KeyEvent.VK_HOME) {
            v = getMinimum();
        } else if (code == KeyEvent.VK_END) {
            v = getMaximum();
        } else {
            return;
        }
        cn1Set(v, false);
        e.consume();
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
        return thumbSize() + 4;
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

    private static Color put(String key) {
        return UIManager.cn1PutColor(key);
    }

    /// The color of the thumb and of the track up to it: the one put
    /// under `Slider.foreground`, else the background the theme gives the
    /// filled part of its own slider, else the text color.
    private Color accent() {
        Color c = put("Slider.foreground");
        if (c != null) {
            return c;
        }
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.ui.plaf.Style st =
                    com.codename1.ui.plaf.UIManager.getInstance().getComponentStyle("SliderFull");
            Color bg = getBackground();
            int rgb = st.getBgColor() & 0xffffff;
            if ((st.getBgTransparency() & 0xff) != 0 && (bg == null || (bg.getRGB() & 0xffffff) != rgb)) {
                return new Color(rgb);
            }
        }
        return CellTheme.mix(base(), ink(), 0.75f);
    }

    private Color base() {
        Color bg = getBackground();
        return bg != null ? bg : Color.WHITE;
    }

    private Color ink() {
        Color fg = getForeground();
        return fg != null ? fg : Color.BLACK;
    }

    /// Paints the track in its band with the thumb on it, then the tick
    /// marks and the labels beside it.
    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(base());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        boolean vertical = orientation == VERTICAL;
        int thick = nativeThickness();
        int room = vertical ? getWidth() : getHeight();
        int start = Math.max(0, (room - across()) / 2);
        int thumb = Math.min(thumbSize(), Math.max(1, Math.min(room, length())));
        int centre = start + Math.min(thick, room) / 2;
        int inset = inset();
        int len = length();
        int pos = sliderModel == null ? inset : position(getValue());
        Color track = put("Slider.trackColor");
        if (track == null) {
            track = CellTheme.mix(base(), ink(), 0.22f);
        }
        Color accent = isEnabled() ? accent() : CellTheme.mix(base(), ink(), 0.35f);
        if (paintTrack) {
            int t = Math.min(TRACK, thumb);
            g.setColor(track);
            // The filled part runs from the end the minimum is at.
            boolean fromStart = vertical == inverted;
            if (vertical) {
                g.fillRoundRect(centre - t / 2, inset, t, Math.max(0, len - 2 * inset), t, t);
                g.setColor(accent);
                if (fromStart) {
                    g.fillRoundRect(centre - t / 2, inset, t, Math.max(0, pos - inset), t, t);
                } else {
                    g.fillRoundRect(centre - t / 2, pos, t, Math.max(0, len - inset - pos), t, t);
                }
            } else {
                g.fillRoundRect(inset, centre - t / 2, Math.max(0, len - 2 * inset), t, t, t);
                g.setColor(accent);
                if (fromStart) {
                    g.fillRoundRect(inset, centre - t / 2, Math.max(0, pos - inset), t, t, t);
                } else {
                    g.fillRoundRect(pos, centre - t / 2, Math.max(0, len - inset - pos), t, t, t);
                }
            }
        }
        int tx = (vertical ? centre : pos) - thumb / 2;
        int ty = (vertical ? pos : centre) - thumb / 2;
        // An outline in the background color keeps the thumb apart from
        // the track on either side of it.
        g.setColor(base());
        g.fillOval(tx - 1, ty - 1, thumb + 2, thumb + 2);
        g.setColor(accent);
        g.fillOval(tx, ty, thumb, thumb);
        int at = start + thick;
        if (paintTicks) {
            g.setColor(CellTheme.mix(base(), ink(), isEnabled() ? 0.7f : 0.35f));
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
        int inset = inset();
        int length = length() - 2 * inset;
        long min = getMinimum();
        long span = (long) getMaximum() - min;
        double f = span <= 0 ? 0 : (double) (value - min) / (double) span;
        if (inverted) {
            f = 1 - f;
        }
        if (orientation == VERTICAL) {
            f = 1 - f;
        }
        return inset + (int) Math.round(f * Math.max(0, length));
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
