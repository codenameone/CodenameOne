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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.accessibility.Accessible;

/// A row or column of buttons and other components.
///
/// It is an ordinary container laid out along its orientation; it cannot
/// be dragged out of its window, and the floatable and rollover
/// properties are recorded only. A button made from an action shows the
/// action's icon, or its name when it has none, takes its short
/// description as the tool tip, and follows the action's later changes.
public class JToolBar extends JComponent implements Accessible, SwingConstants {

    private int orientation;
    private boolean floatable = true;
    private boolean rollover;
    private boolean paintBorder = true;
    private Insets margin;
    /// The layout this tool bar gave itself. Rows are only made under it:
    /// a layout the application set is the application's to run.
    private LayoutManager ownLayout;

    public JToolBar() {
        this(null, HORIZONTAL);
    }

    public JToolBar(int orientation) {
        this(null, orientation);
    }

    public JToolBar(String name) {
        this(name, HORIZONTAL);
    }

    public JToolBar(String name, int orientation) {
        setName(name);
        check(orientation);
        this.orientation = orientation;
        ownLayout = new BoxLayout(this, orientation == VERTICAL ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS);
        setLayout(ownLayout);
    }

    // ------------------------------------------------------------ rows

    /// Whether this is a horizontal bar, under its own layout, that was
    /// given less width than its components want side by side.
    ///
    /// On the desktop such a bar cuts its last components off, and the
    /// user widens the window. Where the window is the display that
    /// cannot be done, so the bar goes on in a further row instead: it
    /// then prefers the height of its rows, and its minimum width is that
    /// of its widest component. A bar that is wide enough is the one row
    /// its layout makes.
    private boolean rows() {
        if (orientation != HORIZONTAL || getLayout() != ownLayout || ownLayout == null || getWidth() <= 0) {
            return false;
        }
        return super.getPreferredSize().width > getWidth();
    }

    /// Places the components in rows no wider than `width` when `place`
    /// is set, and answers the height the rows take with the insets.
    private int rows(int width, boolean place) {
        Insets in = getInsets();
        int right = Math.max(in.left + 1, width - in.right);
        int x = in.left;
        int y = in.top;
        int rowHeight = 0;
        int first = 0;
        int n = getComponentCount();
        for (int i = 0; i <= n; i++) {
            Dimension d = null;
            if (i < n) {
                Component c = getComponent(i);
                if (!c.isVisible()) {
                    continue;
                }
                d = c.getPreferredSize();
            }
            if (d == null || (x > in.left && x + d.width > right)) {
                if (place) {
                    // A row is as tall as its tallest component, and the
                    // others stand in the middle of it.
                    int rx = in.left;
                    for (int j = first; j < i; j++) {
                        Component c = getComponent(j);
                        if (c.isVisible()) {
                            Dimension cd = c.getPreferredSize();
                            c.setBounds(rx, y + (rowHeight - cd.height) / 2, cd.width, cd.height);
                            rx += cd.width;
                        }
                    }
                }
                y += rowHeight;
                x = in.left;
                rowHeight = 0;
                first = i;
            }
            if (d != null) {
                x += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
        }
        return y + in.bottom;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (!isPreferredSizeSet() && rows()) {
            return new Dimension(d.width, rows(getWidth(), false));
        }
        return d;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = super.getMinimumSize();
        if (isMinimumSizeSet() || orientation != HORIZONTAL || getLayout() != ownLayout) {
            return d;
        }
        Insets in = getInsets();
        int w = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c.isVisible()) {
                w = Math.max(w, c.getPreferredSize().width);
            }
        }
        return new Dimension(Math.min(d.width, w + in.left + in.right),
                rows() ? rows(getWidth(), false) : d.height);
    }

    @Override
    public void doLayout() {
        if (rows()) {
            rows(getWidth(), true);
        } else {
            super.doLayout();
        }
    }

    /// A change of width changes the number of rows, and with it the
    /// height this bar prefers: the container that measured it before is
    /// asked to lay it out again.
    @Override
    public void setBounds(int x, int y, int width, int height) {
        if (width == getWidth()) {
            super.setBounds(x, y, width, height);
            return;
        }
        int was = getPreferredSize().height;
        super.setBounds(x, y, width, height);
        if (getPreferredSize().height != was) {
            com.codename1.desktopcompat.rt.PeerSupport.layoutAgain(this);
        }
    }

    private static void check(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("orientation must be either HORIZONTAL or VERTICAL");
        }
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int o) {
        check(o);
        if (orientation != o) {
            int old = orientation;
            orientation = o;
            ownLayout = new BoxLayout(this, o == VERTICAL ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS);
            setLayout(ownLayout);
            firePropertyChange("orientation", old, o);
            revalidate();
            repaint();
        }
    }

    public boolean isFloatable() {
        return floatable;
    }

    public void setFloatable(boolean b) {
        floatable = b;
    }

    public boolean isRollover() {
        return rollover;
    }

    public void setRollover(boolean rollover) {
        this.rollover = rollover;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        paintBorder = b;
        repaint();
    }

    public void setMargin(Insets m) {
        margin = m == null ? null : new Insets(m.top, m.left, m.bottom, m.right);
        revalidate();
    }

    public Insets getMargin() {
        return margin == null ? new Insets(0, 0, 0, 0) : new Insets(margin.top, margin.left, margin.bottom,
                margin.right);
    }

    public int getComponentIndex(Component c) {
        return getComponentZOrder(c);
    }

    public Component getComponentAtIndex(int i) {
        return i < 0 || i >= getComponentCount() ? null : getComponent(i);
    }

    public void addSeparator() {
        addSeparator(null);
    }

    public void addSeparator(Dimension size) {
        add(new JToolBar.Separator(size));
    }

    /// Adds a button that runs the action and follows it.
    public JButton add(Action a) {
        JButton b = createActionComponent(a);
        b.addActionListener(a);
        PropertyChangeListener l = createActionChangeListener(b);
        if (a != null) {
            a.addPropertyChangeListener(l);
        }
        add(b);
        return b;
    }

    protected JButton createActionComponent(Action a) {
        JButton b = new JButton();
        if (a != null) {
            Object icon = a.getValue(Action.SMALL_ICON);
            Object name = a.getValue(Action.NAME);
            if (icon instanceof Icon) {
                b.setIcon((Icon) icon);
            } else if (name instanceof String) {
                b.setText((String) name);
            }
            Object tip = a.getValue(Action.SHORT_DESCRIPTION);
            if (tip instanceof String) {
                b.setToolTipText((String) tip);
            }
            Object cmd = a.getValue(Action.ACTION_COMMAND_KEY);
            if (cmd instanceof String) {
                b.setActionCommand((String) cmd);
            }
            b.setEnabled(a.isEnabled());
        }
        return b;
    }

    protected PropertyChangeListener createActionChangeListener(JButton b) {
        return new Follower(b);
    }

    /// Copies an action's changes to the button made from it.
    private static final class Follower implements PropertyChangeListener {

        private final JButton button;

        Follower(JButton button) {
            this.button = button;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            Object v = evt.getNewValue();
            if ("enabled".equals(name)) {
                button.setEnabled(Boolean.TRUE.equals(v));
            } else if (Action.NAME.equals(name)) {
                if (button.getIcon() == null) {
                    button.setText(v instanceof String ? (String) v : "");
                }
            } else if (Action.SMALL_ICON.equals(name)) {
                button.setIcon(v instanceof Icon ? (Icon) v : null);
                if (v instanceof Icon) {
                    button.setText("");
                }
            } else if (Action.SHORT_DESCRIPTION.equals(name)) {
                button.setToolTipText(v instanceof String ? (String) v : null);
            }
        }
    }

    /// The gap between two groups of components of a tool bar.
    public static class Separator extends JSeparator {

        private Dimension separatorSize;

        public Separator() {
            this(null);
        }

        public Separator(Dimension size) {
            super(JSeparator.HORIZONTAL);
            setSeparatorSize(size);
        }

        public void setSeparatorSize(Dimension size) {
            separatorSize = size == null ? new Dimension(10, 10) : new Dimension(size);
            revalidate();
        }

        public Dimension getSeparatorSize() {
            return new Dimension(separatorSize);
        }

        @Override
        public Dimension getMinimumSize() {
            return getSeparatorSize();
        }

        /// As wide as the gap along the tool bar and as long as the bar
        /// across it, so the line it draws runs the bar's whole height.
        @Override
        public Dimension getMaximumSize() {
            Dimension d = getSeparatorSize();
            if (cn1AcrossHorizontalBar()) {
                d.height = Short.MAX_VALUE;
            } else {
                d.width = Short.MAX_VALUE;
            }
            return d;
        }

        private boolean cn1AcrossHorizontalBar() {
            java.lang.Object p = getParent();
            return !(p instanceof JToolBar) || ((JToolBar) p).getOrientation() == HORIZONTAL;
        }

        /// A line across the tool bar in the middle of the gap, where the
        /// separator of a menu draws one along its top.
        @Override
        protected void paintComponent(com.codename1.desktopcompat.java.awt.Graphics g) {
            com.codename1.desktopcompat.java.awt.Color line = com.codename1.desktopcompat.rt.LafTheme.line();
            if (line == null) {
                return;
            }
            g.setColor(line);
            int w = getWidth();
            int h = getHeight();
            if (cn1AcrossHorizontalBar()) {
                int pad = Math.min(4, h / 4);
                g.fillRect(w / 2, pad, 1, Math.max(0, h - 2 * pad));
            } else {
                int pad = Math.min(4, w / 4);
                g.fillRect(pad, h / 2, Math.max(0, w - 2 * pad), 1);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return getSeparatorSize();
        }
    }
}
