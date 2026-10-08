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
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.ProgressPeer;

/// A progress bar over a bounded range model, shown by a Codename One
/// slider the user cannot move. An indeterminate bar is that slider in its
/// infinite mode, which animates by itself. The progress string is drawn
/// by this class, centred over the bar.
///
/// Not supported: the string of a vertical bar is drawn upright, not
/// rotated; there is no UI delegate, so `getUI` and `setUI` are absent.
public class JProgressBar extends JComponent implements SwingConstants {

    private static final int LENGTH = 146;
    private static final int MINIMUM_LENGTH = 10;
    private static final int FALLBACK_THICKNESS = 12;

    protected int orientation;
    protected boolean paintBorder = true;
    protected BoundedRangeModel model;
    protected String progressString;
    protected boolean paintString;
    protected ChangeEvent changeEvent;
    protected ChangeListener changeListener;

    private final ChangeListener bridge = new Bridge();
    private boolean indeterminate;

    public JProgressBar() {
        this(HORIZONTAL);
    }

    public JProgressBar(int orient) {
        this(orient, 0, 100);
    }

    public JProgressBar(int min, int max) {
        this(HORIZONTAL, min, max);
    }

    public JProgressBar(int orient, int min, int max) {
        checkOrientation(orient);
        orientation = orient;
        model = new DefaultBoundedRangeModel(min, 0, min, max);
        model.addChangeListener(bridge);
    }

    public JProgressBar(BoundedRangeModel newModel) {
        orientation = HORIZONTAL;
        model = newModel;
        if (newModel != null) {
            newModel.addChangeListener(bridge);
        }
    }

    private static void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException(orientation + " is not a legal orientation");
        }
    }

    // ------------------------------------------------------------- peer

    @Override
    protected com.codename1.ui.Component cn1CreatePeer() {
        return new ProgressPeer(this);
    }

    @Override
    protected void cn1PeerCreated() {
        super.cn1PeerCreated();
        cn1Sync();
    }

    private void cn1Sync() {
        com.codename1.ui.Component p = cn1PeerOrNull();
        if (!(p instanceof ProgressPeer) || model == null) {
            return;
        }
        ProgressPeer s = (ProgressPeer) p;
        long span = (long) model.getMaximum() - (long) model.getMinimum();
        int range = span <= 0 ? 1 : span > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) span;
        long pos = (long) model.getValue() - (long) model.getMinimum();
        s.setVertical(orientation == VERTICAL);
        s.setMinValue(0);
        s.setMaxValue(range);
        s.setInfinite(indeterminate);
        if (!indeterminate) {
            s.setProgress(pos < 0 ? 0 : pos > range ? range : (int) pos);
        }
    }

    private void changed() {
        cn1Sync();
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------ sizes

    private int thickness() {
        Dimension d = super.cn1NativePreferredSize();
        int t = d == null ? FALLBACK_THICKNESS : orientation == VERTICAL ? d.width : d.height;
        if (paintString && getFont() != null) {
            t = Math.max(t, getFontMetrics(getFont()).getHeight() + 2);
        }
        return t;
    }

    /// The preferred size: 146 logical pixels along the bar, and across it
    /// what the native bar wants, or the height of the string if that is
    /// painted and taller.
    @Override
    protected Dimension cn1NativePreferredSize() {
        return orientation == VERTICAL ? new Dimension(thickness(), LENGTH) : new Dimension(LENGTH, thickness());
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return orientation == VERTICAL ? new Dimension(thickness(), MINIMUM_LENGTH)
                : new Dimension(MINIMUM_LENGTH, thickness());
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        return orientation == VERTICAL ? new Dimension(thickness(), Short.MAX_VALUE)
                : new Dimension(Short.MAX_VALUE, thickness());
    }

    // ------------------------------------------------------------ paint

    /// Paints the native bar and, when the string is painted, the string
    /// centred over it.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!paintString) {
            return;
        }
        String s = getString();
        if (s.length() == 0) {
            return;
        }
        FontMetrics fm = g.getFontMetrics();
        Color fg = getForeground();
        g.setColor(fg != null ? fg : Color.BLACK);
        g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
    }

    @Override
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    // ------------------------------------------------------- properties

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int newOrientation) {
        if (orientation != newOrientation) {
            checkOrientation(newOrientation);
            int old = orientation;
            orientation = newOrientation;
            firePropertyChange("orientation", old, newOrientation);
            changed();
        }
    }

    public boolean isStringPainted() {
        return paintString;
    }

    public void setStringPainted(boolean b) {
        boolean old = paintString;
        paintString = b;
        firePropertyChange("stringPainted", old, b);
        if (b != old) {
            revalidate();
            repaint();
        }
    }

    /// The string set with `setString`, or the progress made as a whole
    /// percentage, such as `42%`.
    public String getString() {
        if (progressString != null) {
            return progressString;
        }
        long span = (long) model.getMaximum() - (long) model.getMinimum();
        if (span <= 0) {
            return "0%";
        }
        long done = ((long) model.getValue() - (long) model.getMinimum()) * 100;
        long whole = done / span;
        long twice = 2 * (done % span);
        // Half way rounds to the even percentage, as the desktop's percent
        // format does.
        if (twice > span || (twice == span && (whole & 1) == 1)) {
            whole++;
        }
        return whole + "%";
    }

    public void setString(String s) {
        String old = progressString;
        progressString = s;
        firePropertyChange("string", old, s);
        if (s == null ? old != null : !s.equals(old)) {
            repaint();
        }
    }

    public double getPercentComplete() {
        long span = (long) model.getMaximum() - (long) model.getMinimum();
        double currentValue = model.getValue();
        return (currentValue - model.getMinimum()) / span;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        boolean old = paintBorder;
        paintBorder = b;
        firePropertyChange("borderPainted", old, b);
        if (b != old) {
            repaint();
        }
    }

    public void setIndeterminate(boolean newValue) {
        boolean old = indeterminate;
        indeterminate = newValue;
        firePropertyChange("indeterminate", old, newValue);
        if (newValue != old) {
            cn1Sync();
            repaint();
        }
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    // ------------------------------------------------------------ model

    /// The listener that turns the model's changes into this bar's.
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
        return model;
    }

    public void setModel(BoundedRangeModel newModel) {
        BoundedRangeModel old = model;
        if (newModel != old) {
            if (old != null) {
                old.removeChangeListener(bridge);
            }
            model = newModel;
            if (newModel != null) {
                newModel.addChangeListener(bridge);
            }
            if (model != null) {
                model.setExtent(0);
            }
            cn1Sync();
            repaint();
        }
    }

    public int getValue() {
        return model.getValue();
    }

    public int getMinimum() {
        return model.getMinimum();
    }

    public int getMaximum() {
        return model.getMaximum();
    }

    public void setValue(int n) {
        model.setValue(n);
    }

    public void setMinimum(int n) {
        model.setMinimum(n);
    }

    public void setMaximum(int n) {
        model.setMaximum(n);
    }

    @Override
    protected String paramString() {
        return super.paramString() + ",orientation=" + (orientation == HORIZONTAL ? "HORIZONTAL" : "VERTICAL")
                + ",paintBorder=" + paintBorder + ",paintString=" + paintString + ",progressString="
                + (progressString != null ? progressString : "") + ",indeterminateString=" + indeterminate;
    }

    /// Listens to the model for as long as it is this bar's, and hands
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
