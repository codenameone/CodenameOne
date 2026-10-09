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
package com.codename1.desktopcompat;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.event.HierarchyEvent;
import com.codename1.desktopcompat.java.awt.event.HierarchyListener;
import com.codename1.desktopcompat.java.awt.print.PrinterException;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.accessibility.AccessibleContext;
import com.codename1.desktopcompat.javax.accessibility.AccessibleRelation;
import com.codename1.desktopcompat.javax.accessibility.AccessibleRelationSet;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JDesktopPane;
import com.codename1.desktopcompat.javax.swing.JFileChooser;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JInternalFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JRadioButton;
import com.codename1.desktopcompat.javax.swing.JTable;
import com.codename1.desktopcompat.javax.swing.ToolTipManager;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.border.BevelBorder;
import com.codename1.desktopcompat.javax.swing.border.SoftBevelBorder;
import com.codename1.desktopcompat.javax.swing.event.InternalFrameEvent;
import com.codename1.desktopcompat.javax.swing.event.InternalFrameListener;
import com.codename1.desktopcompat.javax.swing.filechooser.FileView;
import com.codename1.desktopcompat.javax.swing.plaf.ColorUIResource;
import com.codename1.desktopcompat.javax.swing.plaf.UIResource;
import com.codename1.desktopcompat.javax.swing.plaf.metal.DefaultMetalTheme;
import com.codename1.desktopcompat.javax.swing.plaf.metal.MetalLookAndFeel;
import com.codename1.desktopcompat.javax.swing.plaf.metal.MetalTheme;
import com.codename1.desktopcompat.javax.swing.plaf.metal.OceanTheme;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Internal frames on a desktop pane, and the smaller pieces a full Swing
/// demo leans on: accessible names, hierarchy listeners, the tool tip
/// manager, file views, the Metal theme data and printing's honest refusal.
public class DesktopPaneAndFriendsTest extends KernelTestBase {

    /// Records the events an internal frame fires, by id.
    private static final class Log implements InternalFrameListener {
        final List<Integer> ids = new ArrayList<Integer>();

        private void add(InternalFrameEvent e) {
            ids.add(Integer.valueOf(e.getID()));
        }

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            add(e);
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            add(e);
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            add(e);
        }

        @Override
        public void internalFrameIconified(InternalFrameEvent e) {
            add(e);
        }

        @Override
        public void internalFrameDeiconified(InternalFrameEvent e) {
            add(e);
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            add(e);
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            add(e);
        }

        boolean saw(int id) {
            return ids.contains(Integer.valueOf(id));
        }
    }

    private JDesktopPane desktop;
    private JFrame window;

    private JInternalFrame frame(String title, int x, int y) {
        JInternalFrame f = new JInternalFrame(title, true, true, true, true);
        f.setBounds(x, y, 200, 150);
        desktop.add(f);
        f.setVisible(true);
        return f;
    }

    private void desktop() {
        window = new JFrame();
        desktop = new JDesktopPane();
        window.getContentPane().add(desktop, BorderLayout.CENTER);
    }

    @Test
    public void selectingOneFrameDeselectsTheOther() throws Exception {
        desktop();
        JInternalFrame a = frame("A", 10, 10);
        JInternalFrame b = frame("B", 250, 10);
        show(window);
        Log la = new Log();
        Log lb = new Log();
        a.addInternalFrameListener(la);
        b.addInternalFrameListener(lb);
        a.setSelected(true);
        assertTrue(a.isSelected());
        assertSame(a, desktop.getSelectedFrame());
        b.setSelected(true);
        assertTrue(b.isSelected());
        assertFalse(a.isSelected());
        assertSame(b, desktop.getSelectedFrame());
        assertTrue(la.saw(InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED));
        assertTrue(lb.saw(InternalFrameEvent.INTERNAL_FRAME_ACTIVATED));
        assertEquals(2, desktop.getAllFrames().length);
        assertSame(desktop, a.getDesktopPane());
    }

    @Test
    public void aPressInsideAFrameSelectsIt() throws Exception {
        desktop();
        JInternalFrame a = frame("A", 10, 10);
        JInternalFrame b = frame("B", 250, 10);
        JButton inside = new JButton("Inside");
        b.getContentPane().add(inside, BorderLayout.CENTER);
        show(window);
        a.setSelected(true);
        JInternalFrame.cn1PressedIn(inside);
        assertTrue(b.isSelected());
        assertFalse(a.isSelected());
    }

    @Test
    public void maximizingFillsTheDesktopAndRestoringPutsItBack() throws Exception {
        desktop();
        JInternalFrame a = frame("A", 10, 20);
        show(window);
        a.setMaximum(true);
        assertTrue(a.isMaximum());
        assertEquals(0, a.getX());
        assertEquals(0, a.getY());
        assertEquals(desktop.getWidth(), a.getWidth());
        assertEquals(desktop.getHeight(), a.getHeight());
        assertEquals(10, a.getNormalBounds().x);
        assertEquals(200, a.getNormalBounds().width);
        a.setMaximum(false);
        assertEquals(10, a.getX());
        assertEquals(20, a.getY());
        assertEquals(200, a.getWidth());
        assertEquals(150, a.getHeight());
    }

    @Test
    public void iconifyingShrinksTheFrameAndTellsListeners() throws Exception {
        desktop();
        JInternalFrame a = frame("A", 10, 20);
        show(window);
        Log log = new Log();
        a.addInternalFrameListener(log);
        a.setIcon(true);
        assertTrue(a.isIcon());
        assertTrue(a.getHeight() < 150);
        assertTrue(log.saw(InternalFrameEvent.INTERNAL_FRAME_ICONIFIED));
        a.setIcon(false);
        assertFalse(a.isIcon());
        assertEquals(150, a.getHeight());
        assertEquals(20, a.getY());
        assertTrue(log.saw(InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED));
    }

    @Test
    public void closingRemovesTheFrameFromTheDesktop() throws Exception {
        desktop();
        JInternalFrame a = frame("A", 10, 20);
        show(window);
        Log log = new Log();
        a.addInternalFrameListener(log);
        a.doDefaultCloseAction();
        assertTrue(log.saw(InternalFrameEvent.INTERNAL_FRAME_CLOSING));
        assertTrue(log.saw(InternalFrameEvent.INTERNAL_FRAME_CLOSED));
        assertTrue(a.isClosed());
        assertEquals(0, desktop.getAllFrames().length);
        assertNull(a.getParent());
    }

    @Test
    public void aFrameThatDoesNothingOnCloseStaysOpen() {
        desktop();
        JInternalFrame a = frame("A", 10, 20);
        a.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        show(window);
        Log log = new Log();
        a.addInternalFrameListener(log);
        a.doDefaultCloseAction();
        assertTrue(log.saw(InternalFrameEvent.INTERNAL_FRAME_CLOSING));
        assertFalse(log.saw(InternalFrameEvent.INTERNAL_FRAME_CLOSED));
        assertFalse(a.isClosed());
        assertEquals(1, desktop.getAllFrames().length);
    }

    @Test
    public void contentGoesToTheContentPaneBelowTheTitleBar() {
        desktop();
        JInternalFrame a = frame("A", 10, 20);
        JLabel l = new JLabel("x");
        a.add(l, BorderLayout.CENTER);
        show(window);
        assertSame(a.getContentPane(), l.getParent());
        assertTrue(a.getRootPane().getY() > 0);
        assertTrue(a.getRootPane().getHeight() < a.getHeight());
        assertEquals("A", a.getTitle());
    }

    @Test
    public void hierarchyListenersHearAddingRemovingAndShowing() {
        JFrame f = new JFrame();
        JPanel p = new JPanel();
        JButton b = new JButton("b");
        final List<HierarchyEvent> heard = new ArrayList<HierarchyEvent>();
        HierarchyListener l = new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                heard.add(e);
            }
        };
        b.addHierarchyListener(l);
        assertEquals(1, b.getHierarchyListeners().length);
        p.add(b);
        assertEquals(1, heard.size());
        assertTrue((heard.get(0).getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0);
        assertSame(b, heard.get(0).getChanged());
        assertSame(p, heard.get(0).getChangedParent());
        heard.clear();
        f.getContentPane().add(p, BorderLayout.CENTER);
        assertEquals(1, heard.size());
        assertSame(p, heard.get(0).getChanged());
        heard.clear();
        show(f);
        boolean showing = false;
        for (HierarchyEvent e : heard) {
            showing |= (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;
        }
        assertTrue(showing);
        assertTrue(b.isShowing());
        heard.clear();
        b.removeHierarchyListener(l);
        p.remove(b);
        assertEquals(0, heard.size());
        assertEquals(0, b.getHierarchyListeners().length);
    }

    @Test
    public void theAccessibleNameReachesTheWidget() {
        JFrame f = new JFrame();
        JButton b = new JButton("Go");
        f.getContentPane().add(b, BorderLayout.CENTER);
        assertTrue(b instanceof Accessible);
        AccessibleContext ctx = b.getAccessibleContext();
        assertNotNull(ctx);
        assertSame(ctx, b.getAccessibleContext());
        ctx.setAccessibleName("Start the run");
        ctx.setAccessibleDescription("Runs it");
        show(f);
        assertEquals("Start the run", ctx.getAccessibleName());
        assertEquals("Runs it", ctx.getAccessibleDescription());
        assertEquals("Start the run", b.cn1Peer().getAccessibilityText());
        ctx.setAccessibleName("Stop");
        assertEquals("Stop", b.cn1Peer().getAccessibilityText());
    }

    @Test
    public void relationSetsKeepOneRelationPerKey() {
        AccessibleRelationSet set = new AccessibleRelationSet();
        JLabel a = new JLabel("a");
        JLabel b = new JLabel("b");
        assertTrue(set.add(new AccessibleRelation(AccessibleRelation.LABEL_FOR, a)));
        set.add(new AccessibleRelation(AccessibleRelation.LABEL_FOR, b));
        assertEquals(1, set.size());
        assertTrue(set.contains(AccessibleRelation.LABEL_FOR));
        assertEquals(2, set.get(AccessibleRelation.LABEL_FOR).getTarget().length);
        assertNull(set.get(AccessibleRelation.MEMBER_OF));
        assertTrue(set.remove(set.get(AccessibleRelation.LABEL_FOR)));
        assertEquals(0, set.toArray().length);
    }

    @Test
    public void disablingTheToolTipManagerTakesTipsOffWidgets() {
        JFrame f = new JFrame();
        JButton b = new JButton("Go");
        b.setToolTipText("Goes");
        f.getContentPane().add(b, BorderLayout.CENTER);
        show(f);
        ToolTipManager m = ToolTipManager.sharedInstance();
        assertSame(m, ToolTipManager.sharedInstance());
        try {
            assertTrue(m.isEnabled());
            assertEquals("Goes", b.cn1Peer().getTooltip());
            m.setEnabled(false);
            assertNull(b.cn1Peer().getTooltip());
            assertEquals("Goes", b.getToolTipText());
            m.setEnabled(true);
            assertEquals("Goes", b.cn1Peer().getTooltip());
        } finally {
            m.setEnabled(true);
        }
    }

    @Test
    public void aFileViewNamesAndDescribesTheFilesOfAChooser() {
        JFileChooser fc = new JFileChooser();
        File f = new File("/a/picture.png");
        fc.setFileView(new FileView() {
            @Override
            public String getDescription(File file) {
                return "A picture";
            }
        });
        assertNotNull(fc.getFileView());
        assertEquals("A picture", fc.getDescription(f));
        JLabel preview = new JLabel("preview");
        fc.setAccessory(preview);
        assertSame(preview, fc.getAccessory());
        assertTrue(fc.getControlButtonsAreShown());
        fc.setControlButtonsAreShown(false);
        assertFalse(fc.getControlButtonsAreShown());
    }

    @Test
    public void anEmbeddedChooserBuildsItsListAndAccessoryWhenAdded() {
        JFrame f = new JFrame();
        JFileChooser fc = new JFileChooser(new File("/files"));
        JLabel preview = new JLabel("preview");
        fc.setAccessory(preview);
        f.getContentPane().add(fc, BorderLayout.CENTER);
        show(f);
        assertTrue(fc.getComponentCount() > 0);
        assertNotNull(preview.getParent());
        assertTrue(preview.isShowing());
    }

    @Test
    public void printingATableSaysThereIsNoPrinter() {
        JTable t = new JTable(2, 2);
        try {
            t.print();
            fail("there is no print service here");
        } catch (PrinterException e) {
            assertNotNull(e.getMessage());
        }
        try {
            t.print(JTable.PrintMode.FIT_WIDTH);
            fail("there is no print service here");
        } catch (PrinterException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void aSoftBevelTakesThreePixelsASide() {
        SoftBevelBorder b = new SoftBevelBorder(BevelBorder.RAISED);
        Insets i = b.getBorderInsets(new JPanel(), new Insets(0, 0, 0, 0));
        assertEquals(3, i.top);
        assertEquals(3, i.left);
        assertEquals(3, i.bottom);
        assertEquals(3, i.right);
        assertFalse(b.isBorderOpaque());
        assertEquals(BevelBorder.RAISED, b.getBevelType());
    }

    @Test
    public void metalThemesCarryTheJdkColours() {
        MetalTheme was = MetalLookAndFeel.getCurrentTheme();
        try {
            assertNotNull(was);
            DefaultMetalTheme steel = new DefaultMetalTheme();
            assertEquals("Steel", steel.getName());
            assertEquals(new Color(204, 204, 204), steel.getControl());
            assertEquals(new Color(153, 153, 204), steel.getPrimaryControlShadow());
            assertTrue(steel.getControl() instanceof UIResource);
            assertNotNull(steel.getControlTextFont());
            OceanTheme ocean = new OceanTheme();
            assertEquals("Ocean", ocean.getName());
            assertEquals(new Color(238, 238, 238), ocean.getControl());
            MetalLookAndFeel.setCurrentTheme(steel);
            assertSame(steel, MetalLookAndFeel.getCurrentTheme());
            assertEquals("Metal", new MetalLookAndFeel().getName());
            assertEquals(new Color(1, 2, 3), new ColorUIResource(1, 2, 3));
        } finally {
            MetalLookAndFeel.setCurrentTheme(was);
        }
    }

    @Test
    public void theInstalledLookAndFeelIsTheOneInUse() {
        String current = UIManager.getLookAndFeel().getClass().getName();
        boolean found = false;
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            found |= current.equals(info.getClassName());
        }
        assertTrue(found);
    }

    @Test
    public void anIconTakesThePlaceOfARadioButtonsIndicator() {
        JFrame f = new JFrame();
        JRadioButton plain = new JRadioButton("");
        JRadioButton iconed = new JRadioButton("");
        iconed.setIcon(new Icon() {
            @Override
            public void paintIcon(com.codename1.desktopcompat.java.awt.Component c,
                    com.codename1.desktopcompat.java.awt.Graphics g, int x, int y) {
                g.setColor(Color.RED);
                g.fillRect(x, y, 2, 2);
            }

            @Override
            public int getIconWidth() {
                return 2;
            }

            @Override
            public int getIconHeight() {
                return 2;
            }
        });
        JPanel p = new JPanel();
        p.add(plain);
        p.add(iconed);
        f.getContentPane().add(p, BorderLayout.CENTER);
        show(f);
        assertTrue("an icon of two pixels leaves no room for an indicator: " + iconed.getPreferredSize().width
                + " against " + plain.getPreferredSize().width,
                iconed.getPreferredSize().width < plain.getPreferredSize().width);
    }

    /// The title bar, the edges and the three title buttons answer the
    /// pointer: a press selects the frame and its title bar takes the
    /// highlight colour, a drag of the bar moves it, a drag of an edge
    /// resizes it, and the buttons iconify, maximise and close.
    @Test
    public void thePointerMovesResizesAndPressesTheTitleButtons() throws Exception {
        UIManager.put("textHighlight", new Color(0x12, 0x34, 0xab));
        try {
            desktop();
            JInternalFrame a = frame("A", 20, 20);
            JInternalFrame b = frame("B", 300, 200);
            window.setSize(540, 900);
            show(window);
            a.setSelected(true);
            assertTrue(a.isSelected());

            // Unselected, the bar of B is not the highlight; a press makes it so.
            int[][] rows = raster(window);
            assertEquals(0, count(rows, b, 4, 4, 100, 24, 0x1234ab));
            assertTrue(count(rows, a, 4, 4, 100, 24, 0x1234ab) > 500);
            press(window, b, 60, 14);
            assertTrue(b.isSelected());
            assertFalse(a.isSelected());
            rows = raster(window);
            assertTrue(count(rows, b, 4, 4, 100, 24, 0x1234ab) > 500);
            assertEquals(0, count(rows, a, 4, 4, 100, 24, 0x1234ab));

            // The same press, dragged, moves the frame by as much.
            drag(window, b, 70, 34);
            release(window, b, 60, 14);
            assertEquals(310, b.getX());
            assertEquals(220, b.getY());
            assertEquals(200, b.getWidth());

            // The right edge and the bottom edge resize; the corner both.
            press(window, b, 198, 80);
            drag(window, b, 228, 80);
            release(window, b, 227, 80);
            assertEquals(230, b.getWidth());
            assertEquals(150, b.getHeight());
            assertEquals(310, b.getX());
            press(window, b, 228, 148);
            drag(window, b, 208, 168);
            release(window, b, 207, 167);
            assertEquals(210, b.getWidth());
            assertEquals(170, b.getHeight());
            // The left edge moves the frame as it resizes it.
            press(window, b, 1, 80);
            drag(window, b, 11, 80);
            release(window, b, 1, 80);
            assertEquals(320, b.getX());
            assertEquals(200, b.getWidth());

            // From the right: close, maximise, iconify, 18 wide and 3 apart.
            int close = 200 - 4 - 3 - 9;
            press(window, b, close - 42, 16);
            release(window, b, close - 42, 16);
            assertTrue(b.isIcon());
            b.setIcon(false);
            assertFalse(b.isIcon());
            assertEquals(200, b.getWidth());
            press(window, b, close - 21, 16);
            release(window, b, close - 21, 16);
            assertTrue(b.isMaximum());
            assertEquals(desktop.getWidth(), b.getWidth());
            b.setMaximum(false);
            assertEquals(200, b.getWidth());
            assertEquals(320, b.getX());
            // A press that leaves the button before it is let go does nothing.
            press(window, b, close, 16);
            release(window, b, close, 60);
            assertFalse(b.isClosed());
            press(window, b, close, 16);
            release(window, b, close, 16);
            assertTrue(b.isClosed());
            assertNull(b.getParent());
        } finally {
            UIManager.put("textHighlight", null);
        }
    }

    /// In the theme as it comes, where the selection colour is a faint
    /// tint of the window, the title bar of the selected frame is still
    /// clearly darker than that of the others.
    @Test
    public void theSelectedFrameShowsInAThemeWithoutASelectionColour() throws Exception {
        desktop();
        JInternalFrame a = frame("A", 20, 20);
        JInternalFrame b = frame("B", 300, 200);
        window.setSize(540, 900);
        show(window);
        a.setSelected(true);
        int[][] rows = raster(window);
        int on = pixel(rows, a, 100, 8);
        int off = pixel(rows, b, 100, 8);
        int lumaOn = ((on >> 16) & 0xff) * 3 + ((on >> 8) & 0xff) * 6 + (on & 0xff);
        int lumaOff = ((off >> 16) & 0xff) * 3 + ((off >> 8) & 0xff) * 6 + (off & 0xff);
        assertTrue(Integer.toHexString(on) + " against " + Integer.toHexString(off),
                Math.abs(lumaOn - lumaOff) >= 480);
        b.setSelected(true);
        rows = raster(window);
        assertEquals(off & 0xffffff, pixel(rows, a, 100, 8) & 0xffffff);
        assertEquals(on & 0xffffff, pixel(rows, b, 100, 8) & 0xffffff);
    }

    /// A component the application moves or resizes is painted where it
    /// now is: its container is asked to repaint, which a layout pass
    /// does for itself and a call from the application has to ask for.
    @Test
    public void movingAComponentRepaintsItsContainer() {
        final int[] repaints = {0};
        JPanel holder = new JPanel(null) {
            @Override
            public void repaint() {
                repaints[0]++;
                super.repaint();
            }
        };
        JLabel piece = new JLabel("x");
        holder.add(piece);
        piece.setBounds(10, 10, 40, 20);
        JFrame f = new JFrame();
        f.getContentPane().add(holder, BorderLayout.CENTER);
        show(f);
        int before = repaints[0];
        piece.setLocation(60, 30);
        assertEquals(before + 1, repaints[0]);
        piece.setSize(50, 20);
        assertEquals(before + 2, repaints[0]);
        // Bounds that do not change ask for nothing.
        piece.setBounds(60, 30, 50, 20);
        assertEquals(before + 2, repaints[0]);
    }
}
