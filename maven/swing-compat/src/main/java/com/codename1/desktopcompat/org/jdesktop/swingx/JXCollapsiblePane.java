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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.Timer;
import com.codename1.desktopcompat.javax.swing.border.Border;

/// A panel that hides its content by shrinking to nothing along one axis,
/// in steps when animated.
///
/// Components added to the pane, and the layout set on it, go to its
/// content pane, a [JXPanel] with a [VerticalLayout] of gap 2. The pane's
/// preferred size is the content pane's, with the length along the
/// collapse axis scaled by how far open the pane is; the content keeps
/// its full size and is cut off by the pane, so it slides rather than
/// squeezes. Once fully collapsed the content pane is made invisible.
///
/// An animated change runs in eight steps of 30 milliseconds on a
/// [Timer]. The `collapsed` property changes when [#setCollapsed(boolean)]
/// is called, and the `animationState` property is fired with `reinit`
/// when a change starts and `expanded` or `collapsed` when it has
/// finished.
///
/// ## What differs from SwingX
///
///  - There are no action maps in this layer, so the toggle action
///    cannot be looked up under [#TOGGLE_ACTION]; call
///    [#setCollapsed(boolean)] from a listener instead.
///  - Component orientation is absent: `LEADING` and `START` are left
///    and top, `TRAILING` and `END` are right and bottom.
///  - A border, opacity and minimum or preferred size set on the pane
///    stay on the pane; only the border is handed to the content pane.
public class JXCollapsiblePane extends JXPanel {

    public static final String TOGGLE_ACTION = "toggle";
    public static final String COLLAPSE_ICON = "collapseIcon";
    public static final String EXPAND_ICON = "expandIcon";

    private static final int STEPS = 8;
    private static final int STEP_MILLIS = 30;

    /// The side the pane shrinks toward.
    public enum Direction {
        LEFT(false),
        RIGHT(false),
        UP(true),
        DOWN(true),
        LEADING(false),
        TRAILING(false),
        START(true),
        END(true) {
            @Override
            boolean far() {
                return true;
            }
        };

        private final boolean vertical;

        Direction(boolean vertical) {
            this.vertical = vertical;
        }

        /// Whether the pane collapses along the vertical axis.
        public boolean isVertical() {
            return vertical;
        }

        /// Whether the content stays put at the pane's origin while the
        /// pane shrinks, rather than sliding out through it.
        boolean far() {
            return this == RIGHT || this == DOWN || this == TRAILING;
        }
    }

    /// Implemented by an ancestor that wants to be the one laid out again
    /// while a collapsible pane inside it changes size.
    public interface CollapsiblePaneContainer {

        Container getValidatingContainer();
    }

    private Container contentPane;
    private boolean constructed;
    private boolean collapsed;
    private boolean animated = true;
    private Direction direction = Direction.UP;
    /// How far open the pane is, from 0 to `STEPS`.
    private int step = STEPS;
    private Timer timer;

    public JXCollapsiblePane() {
        this(Direction.UP);
    }

    public JXCollapsiblePane(Direction direction) {
        super.setLayout(new Slide());
        this.direction = direction == null ? Direction.UP : direction;
        constructed = true;
        setContentPane(createContentPane());
    }

    /// Lays the content pane out at its full size, shifted so that the
    /// part still showing is the one next to the edge the pane does not
    /// shrink toward.
    private final class Slide implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Container c = contentPane;
            return c == null ? new Dimension(0, 0) : scaled(c.getPreferredSize());
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container parent) {
            Container c = contentPane;
            if (c == null) {
                return;
            }
            Dimension d = c.getPreferredSize();
            int w = getWidth();
            int h = getHeight();
            if (direction.isVertical()) {
                int full = step >= STEPS ? Math.max(h, 0) : d.height;
                c.setBounds(0, direction.far() ? 0 : h - full, w, full);
            } else {
                int full = step >= STEPS ? Math.max(w, 0) : d.width;
                c.setBounds(direction.far() ? 0 : w - full, 0, full, h);
            }
        }
    }

    /// Makes the pane components are added to: a [JXPanel] with a
    /// [VerticalLayout] of gap 2.
    protected Container createContentPane() {
        return new JXPanel(new VerticalLayout(2));
    }

    public Container getContentPane() {
        return contentPane;
    }

    /// Replaces the pane components are added to. The components of the
    /// old one are not moved over.
    public void setContentPane(Container contentPanel) {
        if (contentPanel == null) {
            throw new IllegalArgumentException("Content pane can't be null");
        }
        if (contentPane != null) {
            super.remove(contentPane);
        }
        contentPane = contentPanel;
        contentPane.setVisible(step > 0);
        super.addImpl(contentPane, null, -1);
        revalidate();
    }

    /// Sets the layout of the content pane.
    @Override
    public void setLayout(LayoutManager mgr) {
        if (contentPane != null) {
            contentPane.setLayout(mgr);
        }
    }

    /// Adds to the content pane.
    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (!constructed || contentPane == null) {
            super.addImpl(comp, constraints, index);
            return;
        }
        contentPane.add(comp, constraints, index);
    }

    /// Removes from the content pane.
    @Override
    public void remove(Component comp) {
        if (contentPane != null) {
            contentPane.remove(comp);
        }
    }

    /// Removes from the content pane.
    @Override
    public void remove(int index) {
        if (contentPane != null) {
            contentPane.remove(index);
        }
    }

    /// Empties the content pane.
    @Override
    public void removeAll() {
        if (contentPane != null) {
            contentPane.removeAll();
        }
    }

    /// Sets whether a change of the collapsed state runs in steps.
    public void setAnimated(boolean animated) {
        boolean old = this.animated;
        this.animated = animated;
        if (old != animated) {
            firePropertyChange("animated", old, animated);
        }
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setDirection(Direction direction) {
        if (timer != null && timer.isRunning()) {
            throw new IllegalStateException("cannot be change direction while collapsing.");
        }
        Direction old = this.direction;
        this.direction = direction == null ? Direction.UP : direction;
        if (old != this.direction) {
            revalidate();
            firePropertyChange("direction", old, this.direction);
        }
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    /// Hides or shows the content, in steps when animated and showing.
    public void setCollapsed(boolean val) {
        if (collapsed == val) {
            return;
        }
        collapsed = val;
        firePropertyChange("animationState", null, "reinit");
        if (animated && isShowing()) {
            if (!val) {
                contentPane.setVisible(true);
            }
            if (timer == null) {
                timer = new Timer(STEP_MILLIS, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        cn1AnimationStep();
                    }
                });
            }
            if (!timer.isRunning()) {
                timer.start();
            }
        } else {
            cn1Finish();
        }
        firePropertyChange("collapsed", !val, val);
    }

    /// Moves a running change one step on, and ends it at the last. The
    /// timer calls this; so may a test.
    void cn1AnimationStep() {
        int goal = collapsed ? 0 : STEPS;
        if (step == goal) {
            cn1Finish();
            return;
        }
        step += collapsed ? -1 : 1;
        if (step == goal) {
            cn1Finish();
        } else {
            cn1Relayout();
        }
    }

    /// Whether a change is still running.
    boolean cn1Animating() {
        return step != (collapsed ? 0 : STEPS);
    }

    private void cn1Finish() {
        if (timer != null) {
            timer.stop();
        }
        step = collapsed ? 0 : STEPS;
        contentPane.setVisible(!collapsed);
        cn1Relayout();
        firePropertyChange("animationState", null, collapsed ? "collapsed" : "expanded");
    }

    private void cn1Relayout() {
        invalidate();
        Container validating = null;
        for (Container p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof CollapsiblePaneContainer) {
                validating = ((CollapsiblePaneContainer) p).getValidatingContainer();
                break;
            }
        }
        if (validating instanceof JComponent) {
            ((JComponent) validating).revalidate();
        }
        revalidate();
        repaint();
    }

    /// The content pane's border.
    @Override
    public Border getBorder() {
        if (contentPane instanceof JComponent) {
            return ((JComponent) contentPane).getBorder();
        }
        return null;
    }

    /// Sets the border on the content pane.
    @Override
    public void setBorder(Border border) {
        if (contentPane instanceof JComponent) {
            ((JComponent) contentPane).setBorder(border);
        }
    }

    /// The content's minimum size, with nothing along the collapse axis.
    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet() || contentPane == null) {
            return super.getMinimumSize();
        }
        Dimension d = contentPane.getMinimumSize();
        if (direction.isVertical()) {
            return new Dimension(d.width, 0);
        }
        return new Dimension(0, d.height);
    }

    /// The content's preferred size, with the length along the collapse
    /// axis scaled by how far open the pane is.
    @Override
    public Dimension getPreferredSize() {
        if (contentPane == null) {
            return new Dimension(0, 0);
        }
        return scaled(isPreferredSizeSet() ? super.getPreferredSize() : contentPane.getPreferredSize());
    }

    private Dimension scaled(Dimension d) {
        if (direction.isVertical()) {
            return new Dimension(d.width, d.height * step / STEPS);
        }
        return new Dimension(d.width * step / STEPS, d.height);
    }
}
