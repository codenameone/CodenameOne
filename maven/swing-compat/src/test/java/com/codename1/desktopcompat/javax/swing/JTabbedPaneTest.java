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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.TabbedPanePeer;
import com.codename1.desktopcompat.rt.Units;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// The tabbed pane: its tab bookkeeping and change events against the
/// JDK's own, and its strip, pages and touch selection on a display.
public class JTabbedPaneTest extends KernelTestBase {

    private static JPanel page(String name, int w, int h) {
        JPanel p = new JPanel(null);
        p.setName(name);
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    /// Runs the same script on our tabbed pane and on the JDK's and
    /// records, after every step, the change events fired so far, the
    /// selected index, the tab count and the titles.
    private static final class Script {
        final JTabbedPane ours = new JTabbedPane();
        final javax.swing.JTabbedPane real = new javax.swing.JTabbedPane();
        final List<JPanel> ourPages = new ArrayList<JPanel>();
        final List<javax.swing.JPanel> realPages = new ArrayList<javax.swing.JPanel>();
        final int[] ourEvents = new int[1];
        final int[] realEvents = new int[1];
        final StringBuilder ourLog = new StringBuilder();
        final StringBuilder realLog = new StringBuilder();

        Script() {
            ours.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    ourEvents[0]++;
                }
            });
            real.addChangeListener(new javax.swing.event.ChangeListener() {
                @Override
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    realEvents[0]++;
                }
            });
            for (int i = 0; i < 6; i++) {
                ourPages.add(new JPanel());
                realPages.add(new javax.swing.JPanel());
            }
        }

        void log(String step) {
            StringBuilder a = new StringBuilder();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < ours.getTabCount(); i++) {
                a.append(ours.getTitleAt(i)).append(ourPages.indexOf(ours.getComponentAt(i))).append(
                        ours.getComponentAt(i).isVisible() ? "v" : "h").append(' ');
            }
            for (int i = 0; i < real.getTabCount(); i++) {
                b.append(real.getTitleAt(i)).append(realPages.indexOf(real.getComponentAt(i))).append(
                        real.getComponentAt(i).isVisible() ? "v" : "h").append(' ');
            }
            ourLog.append(step).append(": events=").append(ourEvents[0]).append(" sel=").append(
                    ours.getSelectedIndex()).append(" tabs=").append(a).append(" children=").append(
                    ours.getComponentCount()).append('\n');
            realLog.append(step).append(": events=").append(realEvents[0]).append(" sel=").append(
                    real.getSelectedIndex()).append(" tabs=").append(b).append(" children=").append(
                    real.getComponentCount()).append('\n');
        }

        void addTab(String t, int p) {
            ours.addTab(t, ourPages.get(p));
            real.addTab(t, realPages.get(p));
            log("addTab " + t);
        }

        void insertTab(String t, int p, int at) {
            ours.insertTab(t, null, ourPages.get(p), null, at);
            real.insertTab(t, null, realPages.get(p), null, at);
            log("insertTab " + t + "@" + at);
        }

        void select(int i) {
            ours.setSelectedIndex(i);
            real.setSelectedIndex(i);
            log("select " + i);
        }

        void removeTabAt(int i) {
            ours.removeTabAt(i);
            real.removeTabAt(i);
            log("removeTabAt " + i);
        }

        void remove(int p) {
            ours.remove(ourPages.get(p));
            real.remove(realPages.get(p));
            log("remove page " + p);
        }

        void removeAll() {
            ours.removeAll();
            real.removeAll();
            log("removeAll");
        }

        void check() {
            assertEquals(realLog.toString(), ourLog.toString());
        }
    }

    @Test
    public void tabBookkeepingAndChangeEventsMatchTheJdk() {
        Script s = new Script();
        s.addTab("A", 0);
        s.addTab("B", 1);
        s.addTab("C", 2);
        s.select(2);
        s.select(2);
        s.insertTab("D", 3, 0);
        s.insertTab("E", 4, 4);
        s.select(1);
        s.removeTabAt(0);
        s.removeTabAt(0);
        s.remove(4);
        s.select(-1);
        s.select(1);
        s.removeTabAt(1);
        // Adding a page that is already a tab moves it.
        s.addTab("A2", 0);
        s.insertTab("C2", 2, 0);
        s.removeAll();
        s.addTab("Z", 5);
        s.check();
    }

    @Test
    public void addOverloadsMakeTabsAsTheJdkDoes() {
        Script s = new Script();
        s.ourPages.get(0).setName("named");
        s.realPages.get(0).setName("named");
        s.ours.add(s.ourPages.get(0));
        s.real.add(s.realPages.get(0));
        s.log("add(c)");
        s.ours.add("T1", s.ourPages.get(1));
        s.real.add("T1", s.realPages.get(1));
        s.log("add(title, c)");
        s.ours.add(s.ourPages.get(2), "T2");
        s.real.add(s.realPages.get(2), "T2");
        s.log("add(c, title)");
        s.ours.add(s.ourPages.get(3), "T3", 0);
        s.real.add(s.realPages.get(3), "T3", 0);
        s.log("add(c, title, 0)");
        s.ours.add(s.ourPages.get(4), 1);
        s.real.add(s.realPages.get(4), 1);
        s.log("add(c, 1)");
        s.check();
        assertEquals("named", s.ours.getTitleAt(2));
        assertEquals(2, s.ours.indexOfTab("named"));
        assertEquals(0, s.ours.indexOfComponent(s.ourPages.get(3)));
        assertEquals(-1, s.ours.indexOfComponent(s.ourPages.get(5)));
    }

    @Test
    public void exactlyOneChangeEventPerSelectionChange() {
        JTabbedPane t = new JTabbedPane();
        final List<Object> sources = new ArrayList<Object>();
        t.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sources.add(e.getSource());
            }
        });
        JPanel a = new JPanel();
        JPanel b = new JPanel();
        t.addTab("A", a);
        assertEquals(1, sources.size());
        assertSame(t, sources.get(0));
        t.addTab("B", b);
        assertEquals(1, sources.size());
        t.setSelectedIndex(1);
        assertEquals(2, sources.size());
        t.setSelectedIndex(1);
        assertEquals(2, sources.size());
        t.setSelectedComponent(a);
        assertEquals(3, sources.size());
        assertSame(a, t.getSelectedComponent());
        assertTrue(a.isVisible());
        assertFalse(b.isVisible());
        t.getModel().setSelectedIndex(1);
        assertEquals(4, sources.size());
        assertTrue(b.isVisible());
        assertFalse(a.isVisible());
    }

    @Test
    public void badIndexesAndUnknownComponentsAreRefused() {
        JTabbedPane t = new JTabbedPane();
        t.addTab("A", new JPanel());
        try {
            t.setSelectedIndex(1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
            assertEquals(0, t.getSelectedIndex());
        }
        try {
            t.setSelectedComponent(new JPanel());
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(0, t.getSelectedIndex());
        }
        try {
            t.setTabPlacement(17);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.TOP, t.getTabPlacement());
        }
    }

    @Test
    public void perTabPropertiesAreKept() {
        JTabbedPane t = new JTabbedPane(SwingConstants.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);
        assertEquals(SwingConstants.BOTTOM, t.getTabPlacement());
        assertEquals(JTabbedPane.SCROLL_TAB_LAYOUT, t.getTabLayoutPolicy());
        JPanel a = new JPanel();
        JPanel b = new JPanel();
        t.addTab("A", null, a, "tip a");
        t.addTab("B", b);
        assertEquals("tip a", t.getToolTipTextAt(0));
        assertNull(t.getToolTipTextAt(1));
        t.setToolTipTextAt(1, "tip b");
        assertEquals("tip b", t.getToolTipTextAt(1));
        t.setTitleAt(1, "Bee");
        assertEquals("Bee", t.getTitleAt(1));
        assertEquals(1, t.indexOfTab("Bee"));
        assertTrue(t.isEnabledAt(1));
        t.setEnabledAt(1, false);
        assertFalse(t.isEnabledAt(1));
        JPanel c = new JPanel();
        t.setComponentAt(1, c);
        assertSame(c, t.getComponentAt(1));
        assertNull(b.getParent());
        assertSame(t, c.getParent());
        assertFalse(c.isVisible());
        JLabel tab = new JLabel("Custom");
        t.setTabComponentAt(0, tab);
        assertSame(tab, t.getTabComponentAt(0));
        assertEquals(0, t.indexOfTabComponent(tab));
        assertEquals(-1, t.indexOfTabComponent(new JLabel()));
    }

    private static TabbedPanePeer peer(JTabbedPane t) {
        return (TabbedPanePeer) t.cn1Peer();
    }

    @Test
    public void theSelectedPageLiesBelowTheStripAndTheOthersAreHidden() {
        JFrame f = new JFrame();
        JTabbedPane t = new JTabbedPane();
        JPanel a = page("a", 100, 50);
        JPanel b = page("b", 120, 40);
        t.addTab("First", a);
        t.addTab("Second", b);
        f.add(t, BorderLayout.CENTER);
        show(f);
        TabbedPanePeer p = peer(t);
        assertEquals(2, p.tabCount());
        assertEquals("First", p.tab(0).getText());
        assertEquals("Second", p.tab(1).getText());
        assertTrue(p.tab(0).isSelected());
        assertFalse(p.tab(1).isSelected());

        int stripH = Units.toLogicalCeil(p.stripPreferredSize().getHeight());
        assertTrue(stripH > 0);
        assertEquals(new Rectangle(0, stripH, t.getWidth(), t.getHeight() - stripH), a.getBounds());
        assertEquals(a.getBounds(), b.getBounds());
        assertTrue(a.isVisible());
        assertFalse(b.isVisible());
        assertTrue(a.cn1Peer().isVisible());
        assertFalse(b.cn1Peer().isVisible());

        // The strip is the last Codename One child, above the page peers.
        assertEquals(3, p.getComponentCount());
        assertEquals("TabsContainer", p.getComponentAt(2).getUIID());
        assertEquals(0, p.getComponentAt(p.getComponentCount() - 1).getY());
        assertEquals(Units.toDevice(stripH), p.getComponentAt(p.getComponentCount() - 1).getHeight());
        assertEquals(p.getWidth(), p.getComponentAt(p.getComponentCount() - 1).getWidth());
        assertEquals(Units.toDevice(a.getY()), a.cn1Peer().getY());

        Dimension pref = t.getPreferredSize();
        assertEquals(50 + stripH, pref.height);
        assertEquals(Math.max(120, Units.toLogicalCeil(p.stripPreferredSize().getWidth())), pref.width);
        Rectangle tab1 = t.getBoundsAt(1);
        assertNotNull(tab1);
        assertEquals(1, t.indexAtLocation(tab1.x + tab1.width / 2, tab1.y + tab1.height / 2));
        assertEquals(-1, t.indexAtLocation(5, stripH + 20));
    }

    @Test
    public void touchingATabSelectsItAndFiresOneChangeEvent() {
        JFrame f = new JFrame();
        JTabbedPane t = new JTabbedPane();
        JPanel a = page("a", 100, 50);
        JPanel b = page("b", 100, 50);
        JPanel c = page("c", 100, 50);
        t.addTab("One", a);
        t.addTab("Two", b);
        t.addTab("Three", c);
        t.setEnabledAt(2, false);
        f.add(t, BorderLayout.CENTER);
        show(f);
        final int[] events = new int[1];
        t.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                events[0]++;
            }
        });
        Rectangle r = t.getBoundsAt(1);
        int x = r.x + r.width / 2;
        int y = r.y + r.height / 2;
        press(f, t, x, y);
        release(f, t, x, y);
        assertEquals(1, events[0]);
        assertEquals(1, t.getSelectedIndex());
        assertTrue(b.isVisible());
        assertFalse(a.isVisible());
        TabbedPanePeer p = peer(t);
        assertTrue(p.tab(1).isSelected());
        assertFalse(p.tab(0).isSelected());

        // The selected tab again: nothing changes.
        press(f, t, x, y);
        release(f, t, x, y);
        assertEquals(1, events[0]);
        assertTrue(p.tab(1).isSelected());

        // A disabled tab takes no touch.
        assertFalse(p.tab(2).isEnabled());
        Rectangle r2 = t.getBoundsAt(2);
        press(f, t, r2.x + r2.width / 2, r2.y + r2.height / 2);
        release(f, t, r2.x + r2.width / 2, r2.y + r2.height / 2);
        assertEquals(1, events[0]);
        assertEquals(1, t.getSelectedIndex());
        assertFalse(p.tab(2).isSelected());

        // A selection made by the program reaches the strip.
        t.setSelectedIndex(0);
        assertEquals(2, events[0]);
        assertTrue(p.tab(0).isSelected());
        assertFalse(p.tab(1).isSelected());
    }

    @Test
    public void theStripFollowsTheTabPlacement() {
        JFrame f = new JFrame();
        JTabbedPane t = new JTabbedPane(SwingConstants.BOTTOM);
        JPanel a = page("a", 100, 50);
        t.addTab("One", a);
        t.addTab("Two", page("b", 100, 50));
        f.add(t, BorderLayout.CENTER);
        show(f);
        TabbedPanePeer p = peer(t);
        int stripH = Units.toLogicalCeil(p.stripPreferredSize().getHeight());
        assertEquals(new Rectangle(0, 0, t.getWidth(), t.getHeight() - stripH), a.getBounds());
        assertEquals(Units.toDevice(t.getHeight() - stripH), p.getComponentAt(p.getComponentCount() - 1).getY());
        assertTrue(p.tab(1).getX() > p.tab(0).getX());
        assertEquals(p.tab(0).getY(), p.tab(1).getY());

        t.setTabPlacement(SwingConstants.LEFT);
        f.validate();
        int stripW = Units.toLogicalCeil(p.stripPreferredSize().getWidth());
        assertTrue(stripW > 0 && stripW < t.getWidth());
        assertEquals(new Rectangle(stripW, 0, t.getWidth() - stripW, t.getHeight()), a.getBounds());
        assertEquals(0, p.getComponentAt(p.getComponentCount() - 1).getX());
        assertEquals(p.getHeight(), p.getComponentAt(p.getComponentCount() - 1).getHeight());
        assertTrue(p.tab(1).getY() > p.tab(0).getY());
        assertEquals(Math.max(50, Units.toLogicalCeil(p.stripPreferredSize().getHeight())),
                t.getPreferredSize().height);
        assertEquals(100 + stripW, t.getPreferredSize().width);

        t.setTabPlacement(SwingConstants.RIGHT);
        f.validate();
        assertEquals(new Rectangle(0, 0, t.getWidth() - stripW, t.getHeight()), a.getBounds());
        assertEquals(Units.toDevice(t.getWidth() - stripW), p.getComponentAt(p.getComponentCount() - 1).getX());
    }

    @Test
    public void tabsAddedAndRemovedWhileShowingReachTheStrip() {
        JFrame f = new JFrame();
        JTabbedPane t = new JTabbedPane();
        t.addTab("One", page("a", 100, 50));
        f.add(t, BorderLayout.CENTER);
        show(f);
        TabbedPanePeer p = peer(t);
        JPanel b = page("b", 100, 50);
        t.insertTab("<html><b>Zero</b></html>", null, b, null, 0);
        f.validate();
        assertEquals(2, p.tabCount());
        assertEquals("Zero", p.tab(0).getText());
        assertEquals(1, t.getSelectedIndex());
        assertTrue(p.tab(1).isSelected());
        assertEquals("TabsContainer", p.getComponentAt(p.getComponentCount() - 1).getUIID());
        t.setTitleAt(1, "Uno");
        assertEquals("Uno", p.tab(1).getText());
        t.setTabComponentAt(1, new JLabel("From label"));
        assertEquals("From label", p.tab(1).getText());
        t.removeTabAt(1);
        f.validate();
        assertEquals(1, p.tabCount());
        assertEquals(0, t.getSelectedIndex());
        assertTrue(b.isVisible());
        assertTrue(p.tab(0).isSelected());
        List<Object[]> text = paint(f);
        assertNotNull(find(text, "Zero"));
    }

    @Test
    public void theModelAloneHoldsTheSelection() {
        DefaultSingleSelectionModel m = new DefaultSingleSelectionModel();
        final int[] events = new int[1];
        m.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                events[0]++;
            }
        });
        assertEquals(-1, m.getSelectedIndex());
        assertFalse(m.isSelected());
        m.setSelectedIndex(3);
        m.setSelectedIndex(3);
        assertEquals(1, events[0]);
        assertTrue(m.isSelected());
        m.clearSelection();
        assertEquals(2, events[0]);
        assertEquals(-1, m.getSelectedIndex());
        assertEquals(1, m.getChangeListeners().length);
    }

    /// Tabs are as wide as their titles and start at the leading end,
    /// whatever the theme does with its own tabs, and the selected one
    /// carries a bar along the edge facing the page -- on all four sides.
    @Test
    public void tabsSizeToTheirTitlesAndTheSelectedOneIsMarked() {
        java.util.Hashtable<String, Object> theme = new java.util.Hashtable<String, Object>();
        theme.put("@tabsGridBool", "true");
        com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(theme);
        try {
            JTabbedPane t = new JTabbedPane();
            t.addTab("A", page("a", 50, 50));
            t.addTab("A much longer title", page("b", 50, 50));
            t.addTab("Mid one", page("c", 50, 50));
            JFrame f = new JFrame();
            f.add(t, BorderLayout.CENTER);
            show(f);
            TabbedPanePeer peer = (TabbedPanePeer) t.cn1Peer();
            for (int i = 0; i < 3; i++) {
                peer.tab(i).getPressedStyle().setFgColor(0x12ab34);
                peer.tab(i).getUnselectedStyle().setFgColor(0x777777);
                // Room around the text, which the headless display draws as a box.
                peer.tab(i).getAllStyles().setPadding(12, 12, 12, 12);
                peer.tab(i).setShouldCalcPreferredSize(true);
            }
            // The strip is measured again when the placement changes.
            t.setTabPlacement(JTabbedPane.RIGHT);
            int[] placements = {JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT};
            for (int k = 0; k < placements.length; k++) {
                int placement = placements[k];
                t.setTabPlacement(placement);
                t.setSelectedIndex(1);
                f.validate();
                Rectangle a = t.getBoundsAt(0);
                Rectangle b = t.getBoundsAt(1);
                Rectangle c = t.getBoundsAt(2);
                boolean across = placement == JTabbedPane.TOP || placement == JTabbedPane.BOTTOM;
                if (across) {
                    assertEquals(0, a.x);
                    assertTrue("tabs follow one another", b.x >= a.x + a.width && c.x >= b.x + b.width);
                    assertTrue(a.width + " < " + c.width + " < " + b.width, a.width < c.width && c.width < b.width);
                    assertTrue("the strip is not filled", c.x + c.width < t.getWidth() - 50);
                } else {
                    assertEquals(0, a.y);
                    assertTrue(b.y >= a.y + a.height && c.y >= b.y + b.height);
                    assertTrue(c.y + c.height < t.getHeight() - 50);
                }
                int[][] px = raster(f);
                // The bar: three logical pixels along the edge towards the page.
                int bx = placement == JTabbedPane.LEFT ? b.x + b.width - 2 : b.x + 1;
                int by = placement == JTabbedPane.TOP ? b.y + b.height - 2 : b.y + 1;
                int bw = across ? b.width - 2 : 1;
                int bh = across ? 1 : b.height - 2;
                assertEquals("placement " + placement, bw * bh * 4, count(px, t, bx, by, bw, bh, 0x12ab34));
                assertEquals("placement " + placement, 0, count(px, t, a.x + 1, a.y + 1, a.width - 2, a.height - 2, 0x12ab34));
                assertEquals("placement " + placement, 0, count(px, t, c.x + 1, c.y + 1, c.width - 2, c.height - 2, 0x12ab34));
            }
        } finally {
            theme.put("@tabsGridBool", "false");
            com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(theme);
        }
    }

    /// A theme that gives its tabs no padding still leaves the bar of the
    /// selected tab clear of the title: a tab is taller than its text by
    /// the bar and a pixel on the bar's side and as much on the other, and
    /// wider by as much when the tabs are at the side.
    @Test
    public void aTabHasRoomForItsBarBesideTheTitle() {
        JTabbedPane t = new JTabbedPane();
        t.addTab("Title", page("a", 50, 50));
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        TabbedPanePeer peer = (TabbedPanePeer) t.cn1Peer();
        com.codename1.ui.Button tab = peer.tab(0);
        tab.getAllStyles().setPadding(0, 0, 0, 0);
        tab.getAllStyles().setMargin(0, 0, 0, 0);
        tab.getAllStyles().setBorder(null);
        tab.setShouldCalcPreferredSize(true);
        com.codename1.ui.Font font = tab.getStyle().getFont();
        int room = 2 * (Math.max(2, com.codename1.desktopcompat.rt.Units.toDevice(3))
                + com.codename1.desktopcompat.rt.Units.toDevice(1));
        assertEquals(font.getHeight() + room, tab.getPreferredH());
        int across = tab.getPreferredW();
        t.setTabPlacement(JTabbedPane.LEFT);
        tab = peer.tab(0);
        tab.setShouldCalcPreferredSize(true);
        assertEquals(font.getHeight(), tab.getPreferredH());
        assertEquals(across + room, tab.getPreferredW());
    }

    /// A title is written in the color set for its tab, and an HTML title
    /// in the color it asks for; a tab with neither keeps the theme's, and
    /// gets it back when the color is taken away.
    @Test
    public void aTabTitleTakesTheColourAskedFor() {
        JTabbedPane t = new JTabbedPane();
        t.addTab("Plain", page("a", 50, 50));
        t.addTab("<html><font color=blue><b>Blue</b></font></html>", page("b", 50, 50));
        t.addTab("Set", page("c", 50, 50));
        JFrame f = new JFrame();
        f.add(t, BorderLayout.CENTER);
        show(f);
        TabbedPanePeer peer = (TabbedPanePeer) t.cn1Peer();
        int theme = peer.tab(0).getUnselectedStyle().getFgColor() & 0xffffff;
        assertEquals("Blue", peer.tab(1).getText());
        assertEquals(0x0000ff, peer.tab(1).getUnselectedStyle().getFgColor() & 0xffffff);
        assertEquals(0x0000ff, peer.tab(1).getPressedStyle().getFgColor() & 0xffffff);
        assertEquals(theme, peer.tab(2).getUnselectedStyle().getFgColor() & 0xffffff);
        t.setForegroundAt(2, new com.codename1.desktopcompat.java.awt.Color(0x10, 0x80, 0x20));
        assertEquals(0x108020, peer.tab(2).getUnselectedStyle().getFgColor() & 0xffffff);
        assertEquals(0x108020, peer.tab(2).getSelectedStyle().getFgColor() & 0xffffff);
        assertEquals(theme, peer.tab(0).getUnselectedStyle().getFgColor() & 0xffffff);
        t.setForegroundAt(2, null);
        assertEquals(theme, peer.tab(2).getUnselectedStyle().getFgColor() & 0xffffff);
    }
}
