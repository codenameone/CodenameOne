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

import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.ui.Image;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import java.util.ArrayList;

/// The peer of a tabbed pane: a container peer for the pages, plus a strip
/// of Codename One tab buttons.
///
/// The pages are ordinary children, placed by the tabbed pane's own
/// layout. The strip is one more Codename One child that the AWT side
/// knows nothing of; it is kept last, which is on top, so the indexes the
/// pages are added at never see it. The strip is styled as the tabs of a
/// Codename One `Tabs` are: a `TabsContainer` of `Tab` toggle buttons.
///
/// Two things are Swing's and not the theme's. A tab is as wide as its
/// title and the tabs start at the leading end of the strip, which scrolls
/// when they do not fit: a theme that spreads its own tabs evenly over the
/// width (`tabsGridBool`) is not followed, because programs written for
/// the desktop count on short tabs beside each other. And the selected tab
/// is marked with a bar in its text color along the edge that faces the
/// page, so it can be told from the others in a theme whose selected tab
/// differs by nothing but a shade.
public class TabbedPanePeer extends ContainerPeer {

    /// Placements, numbered as the Swing constants are.
    public static final int TOP = 1;
    public static final int LEFT = 2;
    public static final int BOTTOM = 3;
    public static final int RIGHT = 4;

    /// Told when the user picks a tab.
    public interface Listener {
        void tabChosen(int index);
    }

    private final Strip strip;
    private final ArrayList<TabButton> buttons = new ArrayList<TabButton>();
    private final Listener listener;
    private int placement = TOP;
    private int selected = -1;

    public TabbedPanePeer(Container owner, Listener listener) {
        super(owner);
        this.listener = listener;
        strip = new Strip(this);
        applyPlacement();
    }

    /// Adds the strip above the page peers. Called once, after the peers
    /// of the pages that existed when this peer was made were added.
    public void attachStrip() {
        if (strip.getParent() == null) {
            addComponent(strip);
        }
    }

    public int tabCount() {
        return buttons.size();
    }

    /// The button of a tab.
    public com.codename1.ui.Button tab(int index) {
        return buttons.get(index);
    }

    private void applyPlacement() {
        boolean across = placement == TOP || placement == BOTTOM;
        strip.setLayout(new BoxLayout(across ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
        strip.setScrollableX(across);
        strip.setScrollableY(!across);
    }

    /// Makes the strip show these tabs, one per element, on the given
    /// side. Buttons are reused, so a tab the user is pressing survives.
    public void setTabs(String[] titles, Image[] icons, boolean[] enabled, int[] inks, int selectedIndex,
            int tabPlacement) {
        int n = titles.length;
        while (buttons.size() > n) {
            TabButton b = buttons.remove(buttons.size() - 1);
            strip.removeComponent(b);
        }
        while (buttons.size() < n) {
            TabButton b = new TabButton(this, buttons.size());
            buttons.add(b);
            strip.addComponent(b);
        }
        placement = tabPlacement;
        applyPlacement();
        for (int i = 0; i < n; i++) {
            TabButton b = buttons.get(i);
            b.setText(titles[i] == null ? "" : titles[i]);
            b.setIcon(icons[i]);
            b.setEnabled(enabled[i]);
            b.ink(inks[i]);
        }
        strip.setShouldCalcPreferredSize(true);
        select(selectedIndex);
    }

    /// Shows the tab at `index` as the selected one; -1 for none.
    public void select(int index) {
        selected = index;
        for (int i = 0; i < buttons.size(); i++) {
            TabButton b = buttons.get(i);
            if (b.isSelected() != (i == index)) {
                b.setSelected(i == index);
            }
        }
        if (index >= 0 && index < buttons.size() && strip.getComponentForm() != null) {
            strip.scrollComponentToVisible(buttons.get(index));
        }
    }

    /// The size the strip wants, in device pixels; nothing without tabs.
    public Dimension stripPreferredSize() {
        if (buttons.isEmpty()) {
            return new Dimension(0, 0);
        }
        return new Dimension(strip.getPreferredW(), strip.getPreferredH());
    }

    /// Places the strip, in device pixels relative to this peer, and lays
    /// its buttons out.
    public void placeStrip(int x, int y, int width, int height) {
        strip.setX(x);
        strip.setY(y);
        strip.setWidth(width);
        strip.setHeight(height);
        strip.setVisible(width > 0 && height > 0);
        strip.relayout();
    }

    /// The bounds of a tab's button in device pixels relative to this
    /// peer: `{x, y, width, height}`.
    public int[] tabBounds(int index) {
        TabButton b = buttons.get(index);
        return new int[]{strip.getX() + b.getX() - strip.getScrollX(), strip.getY() + b.getY() - strip.getScrollY(),
            b.getWidth(), b.getHeight()};
    }

    void chosen(TabButton b) {
        int index = buttons.indexOf(b);
        if (index < 0) {
            return;
        }
        listener.tabChosen(index);
        // The owner decides; show what it decided, not what was pressed.
        select(selected);
    }

    boolean routeRepaint() {
        return support() != null && support().routeRepaint();
    }

    /// The row or column of tab buttons.
    private static final class Strip extends com.codename1.ui.Container {
        private final TabbedPanePeer peer;

        Strip(TabbedPanePeer peer) {
            this.peer = peer;
            setUIID("TabsContainer");
        }

        void relayout() {
            setShouldLayout(true);
            layoutContainer();
        }

        @Override
        public void repaint() {
            if (peer == null || !peer.routeRepaint()) {
                super.repaint();
            }
        }
    }

    /// One tab: a toggle button that reports being chosen.
    private static final class TabButton extends RadioButton implements ActionListener<ActionEvent> {
        private final TabbedPanePeer peer;
        private int ink = -1;

        TabButton(TabbedPanePeer peer, int index) {
            this.peer = peer;
            setToggle(true);
            setUIID("Tab");
            setName("tab" + index);
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            peer.chosen(this);
        }

        /// Writes the title in `rgb` whatever the state of the tab, or in
        /// the theme's colors again for -1.
        void ink(int rgb) {
            if (rgb == ink) {
                return;
            }
            if (ink >= 0) {
                // The styles hold the color that was asked for last; the
                // theme's come back with the styles themselves.
                setUIID("Tab");
            }
            ink = rgb;
            if (rgb >= 0) {
                getAllStyles().setFgColor(rgb);
            }
        }

        /// The thickness of the bar that marks the selected tab.
        private static int bar() {
            return Math.max(2, Units.toDevice(3));
        }

        /// Room for the bar on the side it is on and as much opposite, so
        /// that it does not strike through a title in a theme that gives
        /// its tabs no padding and the title stays in the middle.
        @Override
        protected Dimension calcPreferredSize() {
            Dimension d = super.calcPreferredSize();
            int room = 2 * (bar() + Units.toDevice(1));
            if (peer.placement == TabbedPanePeer.LEFT || peer.placement == TabbedPanePeer.RIGHT) {
                return new Dimension(d.getWidth() + room, d.getHeight());
            }
            return new Dimension(d.getWidth(), d.getHeight() + room);
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            int cx = g.getClipX();
            int cy = g.getClipY();
            int cw = g.getClipWidth();
            int ch = g.getClipHeight();
            super.paint(g);
            if (!isSelected()) {
                return;
            }
            // Painting the text may leave the clip narrowed to it.
            g.setClip(cx, cy, cw, ch);
            int bar = bar();
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            // A toggled button is drawn in its pressed style. The names of
            // the sides are qualified: a button inherits constants of the
            // same names with other values.
            g.setColor(getPressedStyle().getFgColor());
            int alpha = g.getAlpha();
            g.setAlpha(255);
            if (peer.placement == TabbedPanePeer.BOTTOM) {
                g.fillRect(x, y, w, Math.min(bar, h));
            } else if (peer.placement == TabbedPanePeer.LEFT) {
                g.fillRect(x + Math.max(0, w - bar), y, Math.min(bar, w), h);
            } else if (peer.placement == TabbedPanePeer.RIGHT) {
                g.fillRect(x, y, Math.min(bar, w), h);
            } else {
                g.fillRect(x, y + Math.max(0, h - bar), w, Math.min(bar, h));
            }
            g.setAlpha(alpha);
        }

        @Override
        public void repaint() {
            if (peer == null || !peer.routeRepaint()) {
                super.repaint();
            }
        }
    }
}
