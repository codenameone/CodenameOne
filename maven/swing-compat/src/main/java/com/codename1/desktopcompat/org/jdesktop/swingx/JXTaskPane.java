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

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.LayoutManager;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.BorderFactory;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.rt.CellTheme;
import com.codename1.desktopcompat.rt.Fonts;
import com.codename1.desktopcompat.rt.ScrollDelegate;

/// A titled group that opens and closes: a title bar with a chevron over
/// a [JXCollapsiblePane].
///
/// A click or tap on the title bar toggles the group. Components added to
/// the task pane, and the layout set on it, go to the content pane of the
/// collapsible pane; [#add(Action)] adds a [JXHyperlink] for an action.
///
/// The title bar is drawn in the colors Codename One's theme gives a
/// table header, stronger for a special pane, and the title itself is a
/// Codename One label.
///
/// ## What differs from SwingX
///
///  - There is no look and feel delegate, so the bar does not take a
///    platform's task pane look, and there is no rollover effect.
///  - Mnemonics are absent, as is the keyboard toggle.
public class JXTaskPane extends JPanel implements JXCollapsiblePane.CollapsiblePaneContainer {

    public static final String SCROLL_ON_EXPAND_CHANGED_KEY = "scrollOnExpand";
    public static final String TITLE_CHANGED_KEY = "title";
    public static final String ICON_CHANGED_KEY = "icon";
    public static final String SPECIAL_CHANGED_KEY = "special";
    public static final String ANIMATED_CHANGED_KEY = "animated";

    private static final int CHEVRON = 24;
    private static final int PAD = 8;

    private boolean constructed;
    private JXCollapsiblePane collapsePane;
    private TitleBar bar;
    private String title;
    private Icon icon;
    private boolean special;
    private boolean scrollOnExpand;

    public JXTaskPane() {
        this((String) null);
    }

    public JXTaskPane(String title) {
        this(title, null);
    }

    public JXTaskPane(Icon icon) {
        this(null, icon);
    }

    public JXTaskPane(String title, Icon icon) {
        super.setLayout(new Stack());
        setOpaque(false);
        bar = new TitleBar();
        collapsePane = new JXCollapsiblePane();
        collapsePane.setOpaque(false);
        collapsePane.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        super.addImpl(bar, null, -1);
        super.addImpl(collapsePane, null, -1);
        collapsePane.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String name = evt.getPropertyName();
                if ("collapsed".equals(name)) {
                    bar.repaint();
                    firePropertyChange("collapsed", evt.getOldValue(), evt.getNewValue());
                } else if ("animationState".equals(name) && "expanded".equals(evt.getNewValue())
                        && scrollOnExpand) {
                    ScrollDelegate.reveal(JXTaskPane.this, new Rectangle(0, 0, getWidth(),
                            getPreferredSize().height));
                }
            }
        });
        constructed = true;
        setTitle(title);
        setIcon(icon);
    }

    /// The bar over the content: background, title label and chevron.
    private final class TitleBar extends JComponent {

        private final JLabel label = new JLabel();

        TitleBar() {
            label.setFont(Fonts.defaultFont().deriveFont(Font.BOLD));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            add(label);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (JXTaskPane.this.isEnabled()) {
                        setCollapsed(!isCollapsed());
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = label.getPreferredSize();
            int min = CellTheme.touch() ? 40 : 26;
            return new Dimension(d.width + 2 * PAD + CHEVRON, Math.max(min, d.height + PAD));
        }

        @Override
        public void doLayout() {
            label.setBounds(PAD, 0, Math.max(0, getWidth() - 2 * PAD - CHEVRON), getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color back = CellTheme.headerBackground(null);
            Color text = CellTheme.headerForeground(null);
            if (special) {
                back = CellTheme.mix(back, text, 0.25f);
            }
            g.setColor(back);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(text);
            int cx = getWidth() - PAD - CHEVRON / 2;
            int cy = getHeight() / 2;
            int tip = isCollapsed() ? 3 : -3;
            g.drawLine(cx - 5, cy - tip, cx, cy + tip);
            g.drawLine(cx, cy + tip, cx + 5, cy - tip);
        }
    }

    /// The bar on top at its preferred height, the collapsible pane under
    /// it at its own.
    private final class Stack implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = getInsets();
            if (bar == null) {
                return new Dimension(in.left + in.right, in.top + in.bottom);
            }
            Dimension b = bar.getPreferredSize();
            Dimension c = collapsePane.getPreferredSize();
            return new Dimension(Math.max(b.width, c.width) + in.left + in.right,
                    b.height + c.height + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets in = getInsets();
            if (bar == null) {
                return new Dimension(in.left + in.right, in.top + in.bottom);
            }
            Dimension b = bar.getPreferredSize();
            return new Dimension(b.width + in.left + in.right, b.height + in.top + in.bottom);
        }

        @Override
        public void layoutContainer(Container parent) {
            if (bar == null) {
                return;
            }
            Insets in = getInsets();
            int w = Math.max(0, getWidth() - in.left - in.right);
            int bh = bar.getPreferredSize().height;
            bar.setBounds(in.left, in.top, w, bh);
            int ch = collapsePane.getPreferredSize().height;
            collapsePane.setBounds(in.left, in.top + bh, w, ch);
        }
    }

    /// The pane the components of the group are in.
    public Container getContentPane() {
        return collapsePane.getContentPane();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String old = this.title;
        this.title = title;
        bar.label.setText(title == null ? "" : title);
        firePropertyChange(TITLE_CHANGED_KEY, old, title);
        revalidate();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        Icon old = this.icon;
        this.icon = icon;
        bar.label.setIcon(icon);
        firePropertyChange(ICON_CHANGED_KEY, old, icon);
        revalidate();
    }

    public boolean isSpecial() {
        return special;
    }

    /// Marks the group as the one that stands out; its bar is drawn
    /// stronger.
    public void setSpecial(boolean special) {
        boolean old = this.special;
        this.special = special;
        if (old != special) {
            firePropertyChange(SPECIAL_CHANGED_KEY, old, special);
            bar.repaint();
        }
    }

    /// Sets whether the enclosing scroll pane is scrolled to show the
    /// whole group once it has opened.
    public void setScrollOnExpand(boolean scrollOnExpand) {
        boolean old = this.scrollOnExpand;
        this.scrollOnExpand = scrollOnExpand;
        if (old != scrollOnExpand) {
            firePropertyChange(SCROLL_ON_EXPAND_CHANGED_KEY, old, scrollOnExpand);
        }
    }

    public boolean isScrollOnExpand() {
        return scrollOnExpand;
    }

    public void setCollapsed(boolean collapsed) {
        collapsePane.setCollapsed(collapsed);
    }

    public boolean isCollapsed() {
        return collapsePane.isCollapsed();
    }

    public void setAnimated(boolean animated) {
        boolean old = isAnimated();
        collapsePane.setAnimated(animated);
        if (old != animated) {
            firePropertyChange(ANIMATED_CHANGED_KEY, old, animated);
        }
    }

    public boolean isAnimated() {
        return collapsePane.isAnimated();
    }

    /// Adds a link that fires `action` and answers it.
    public Component add(Action action) {
        JXHyperlink link = new JXHyperlink(action);
        link.setHorizontalAlignment(SwingConstants.LEFT);
        add(link);
        return link;
    }

    /// The parent, which is what has to be laid out again when the group
    /// opens or closes.
    @Override
    public Container getValidatingContainer() {
        return getParent();
    }

    /// Adds to the content pane.
    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (!constructed) {
            super.addImpl(comp, constraints, index);
            return;
        }
        getContentPane().add(comp, constraints, index);
    }

    /// Sets the layout of the content pane.
    @Override
    public void setLayout(LayoutManager mgr) {
        if (constructed) {
            getContentPane().setLayout(mgr);
        }
    }

    /// Removes from the content pane.
    @Override
    public void remove(Component comp) {
        getContentPane().remove(comp);
    }

    /// Removes from the content pane.
    @Override
    public void remove(int index) {
        getContentPane().remove(index);
    }

    /// Empties the content pane.
    @Override
    public void removeAll() {
        getContentPane().removeAll();
    }

    /// The title bar; for the tests of this package.
    JComponent cn1TitleBar() {
        return bar;
    }

    /// The collapsible pane under the bar; for the tests of this package.
    JXCollapsiblePane cn1CollapsiblePane() {
        return collapsePane;
    }
}
