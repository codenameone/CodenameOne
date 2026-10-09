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
        setLayout(new BoxLayout(this, orientation == VERTICAL ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));
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
            setLayout(new BoxLayout(this, o == VERTICAL ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));
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

        @Override
        public Dimension getMaximumSize() {
            return getSeparatorSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return getSeparatorSize();
        }
    }
}
