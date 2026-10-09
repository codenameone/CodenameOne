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

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatDarculaLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatDarkLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatIntelliJLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.FlatLightLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.codename1.desktopcompat.com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.codename1.desktopcompat.com.formdev.flatlaf.util.UIScale;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.GridLayout;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.plaf.metal.MetalLookAndFeel;
import com.codename1.desktopcompat.rt.LafTheme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

/// A look and feel selects the light or the dark palette, the widgets are
/// painted in it at once, and the defaults answer from it.
public class LookAndFeelSwitchTest extends KernelTestBase {

    /// The keys a real application was found to read, and FlatLaf's own.
    private static final String[] COLORS = {"Label.disabledForeground", "Separator.foreground", "Panel.background",
        "Label.foreground", "Tree.selectionBackground", "Tree.foreground", "Panel.foreground", "controlShadow",
        "Tree.selectionForeground", "TextField.inactiveBackground", "TextArea.foreground", "TextArea.background",
        "Table.background", "Component.linkColor", "Component.accentColor", "Button.foreground", "Button.background",
        "Component.borderColor", "Component.focusColor", "Actions.Red", "Actions.Green", "Actions.Yellow",
        "Actions.Blue", "Actions.Grey", "Table.gridColor", "Component.error.focusedBorderColor",
        "Button.hoverBackground", "List.selectionBackground", "TextField.placeholderForeground", "controlDkShadow",
        "controlHighlight", "ProgressBar.foreground", "ToolTip.background", "TabbedPane.underlineColor"};

    @After
    public void forget() throws Exception {
        UIManager.put("Mine.kept", null);
        UIManager.put("Panel.background", null);
        FlatLaf.setGlobalExtraDefaults(null);
        UIManager.setLookAndFeel((LookAndFeel) null);
        HeadlessImplementation.darkMode = false;
        LafTheme.reset();
    }

    private static int light(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
    }

    private static int light(int rgb) {
        return light(new Color(rgb));
    }

    @Test
    public void aDarkLookAndFeelTurnsTheWidgetsDarkAndALightOneBack() {
        JButton button = new JButton("Go");
        JTable table = new JTable(new Object[][]{{"a", "b"}, {"c", "d"}}, new Object[]{"One", "Two"});
        JTree tree = new JTree();
        JTextArea area = new JTextArea("text");
        JList<String> list = new JList<String>(new String[]{"x", "y"});
        JTextField field = new JTextField("f");
        JLabel label = new JLabel("label");
        JPanel grid = new JPanel(new GridLayout(2, 2));
        JScrollPane tablePane = new JScrollPane(table);
        grid.add(tablePane);
        grid.add(new JScrollPane(tree));
        grid.add(new JScrollPane(area));
        grid.add(new JScrollPane(list));
        JFrame f = new JFrame();
        f.add(grid, BorderLayout.CENTER);
        f.add(button, BorderLayout.SOUTH);
        f.add(field, BorderLayout.NORTH);
        f.add(label, BorderLayout.WEST);
        f.setSize(400, 300);
        show(f);

        assertTrue(FlatLightLaf.setup());
        assertFalse(FlatLaf.isLafDark());
        int[][] rows = raster(f);
        Component[] dark = {button, table.getTableHeader(), tree, area, list, field, label, table};
        int[] lightBefore = new int[dark.length];
        for (int i = 0; i < dark.length; i++) {
            lightBefore[i] = light(pixel(rows, dark[i], dark[i].getWidth() - 4, dark[i].getHeight() - 4));
            assertTrue(dark[i].getClass().getName() + " is light in the light palette: " + lightBefore[i],
                    lightBefore[i] >= 128);
        }
        assertTrue(light(UIManager.getColor("Panel.background")) >= 128);

        assertTrue(FlatDarkLaf.setup());
        assertTrue(FlatLaf.isLafDark());
        assertTrue(UIManager.getBoolean("laf.dark"));
        assertTrue(light(UIManager.getColor("Panel.background")) < 128);
        assertTrue(light(UIManager.getColor("Label.foreground")) >= 128);
        rows = raster(f);
        for (int i = 0; i < dark.length; i++) {
            int now = light(pixel(rows, dark[i], dark[i].getWidth() - 4, dark[i].getHeight() - 4));
            assertTrue(dark[i].getClass().getName() + " is dark in the dark palette: " + now, now < 128);
        }
        assertTrue("the label's text is light", light(label.getForeground()) >= 128);
        assertTrue("the table's text is light", light(table.getForeground()) >= 128);

        assertTrue(FlatMacLightLaf.setup());
        rows = raster(f);
        for (int i = 0; i < dark.length; i++) {
            int now = light(pixel(rows, dark[i], dark[i].getWidth() - 4, dark[i].getHeight() - 4));
            assertTrue(dark[i].getClass().getName() + " is light again: " + now, now >= 128);
        }
        assertFalse(UIManager.getBoolean("laf.dark"));
    }

    /// A switch takes every widget's style from the theme again, so what
    /// the application set on a component has to be given back to it.
    @Test
    public void whatWasSetOnAComponentOutlastsASwitch() {
        assertTrue(FlatLightLaf.setup());
        JLabel title = new JLabel("Title");
        title.setFont(title.getFont().deriveFont(31f));
        title.setForeground(new Color(0x12, 0x34, 0x56));
        JToolBar bar = new JToolBar();
        JButton inBar = new JButton("In bar");
        bar.add(inBar);
        JFrame f = new JFrame();
        f.add(bar, BorderLayout.NORTH);
        f.add(title, BorderLayout.CENTER);
        f.setSize(400, 300);
        show(f);
        com.codename1.ui.Component titlePeer = title.cn1Peer();
        com.codename1.ui.Component barPeer = inBar.cn1Peer();
        int size = titlePeer.getUnselectedStyle().getFont().getHeight();
        assertTrue("a tool bar button has no border",
                barPeer.getUnselectedStyle().getBorder().isEmptyBorder());

        assertTrue(FlatDarkLaf.setup());
        assertEquals(size, titlePeer.getUnselectedStyle().getFont().getHeight());
        assertEquals(0x123456, titlePeer.getUnselectedStyle().getFgColor() & 0xffffff);
        assertTrue("a tool bar button still has no border",
                barPeer.getUnselectedStyle().getBorder().isEmptyBorder());
        assertNull("the button's margin is its own again", inBar.getMargin());

        assertTrue(FlatLightLaf.setup());
        assertEquals(size, titlePeer.getUnselectedStyle().getFont().getHeight());
        assertTrue(barPeer.getUnselectedStyle().getBorder().isEmptyBorder());
    }

    @Test
    public void everyNameIsAcceptedAndSaysWhichPalette() throws Exception {
        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        assertTrue(UIManager.getLookAndFeel() instanceof FlatDarkLaf);
        assertTrue(LafTheme.isDark());
        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        assertTrue(UIManager.getLookAndFeel() instanceof MetalLookAndFeel);
        assertFalse(LafTheme.isDark());
        UIManager.setLookAndFeel("com.example.themes.DraculaLookAndFeel");
        assertTrue(LafTheme.isDark());
        assertEquals("Dracula", UIManager.getLookAndFeel().getName());
        assertTrue(UIManager.getLookAndFeel().isSupportedLookAndFeel());
        // A name that says nothing keeps the palette.
        UIManager.setLookAndFeel("com.example.NoSuchLookAndFeel");
        assertTrue(LafTheme.isDark());
        assertEquals("NoSuch", UIManager.getLookAndFeel().getName());
        String[] lights = {"javax.swing.plaf.nimbus.NimbusLookAndFeel",
            "com.sun.java.swing.plaf.windows.WindowsLookAndFeel", "com.apple.laf.AquaLookAndFeel",
            "com.sun.java.swing.plaf.gtk.GTKLookAndFeel", "com.sun.java.swing.plaf.motif.MotifLookAndFeel"};
        for (int i = 0; i < lights.length; i++) {
            UIManager.setLookAndFeel("com.formdev.flatlaf.themes.FlatMacDarkLaf");
            assertTrue(UIManager.getLookAndFeel() instanceof FlatMacDarkLaf);
            assertTrue(LafTheme.isDark());
            UIManager.setLookAndFeel(lights[i]);
            assertFalse(lights[i], LafTheme.isDark());
        }
        UIManager.setLookAndFeel(new FlatDarculaLaf());
        assertTrue(LafTheme.isDark());
        assertEquals("FlatLaf - FlatLaf Darcula", UIManager.getLookAndFeel().getID());
        UIManager.setLookAndFeel(new FlatIntelliJLaf());
        assertFalse(LafTheme.isDark());
        // The built-in look follows the platform.
        HeadlessImplementation.darkMode = true;
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        assertTrue(LafTheme.isDark());
        assertEquals("Codename One", UIManager.getLookAndFeel().getName());
        HeadlessImplementation.darkMode = false;
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        assertFalse(LafTheme.isDark());
    }

    /// A look of the application's own that says it is dark the FlatLaf
    /// way, and has defaults.
    private static final class Mine extends FlatLaf {
        boolean initialized;
        boolean uninitialized;

        @Override
        public String getName() {
            return "Mine";
        }

        @Override
        public String getDescription() {
            return "Mine";
        }

        @Override
        public boolean isDark() {
            return true;
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void uninitialize() {
            uninitialized = true;
        }

        @Override
        public UIDefaults getDefaults() {
            UIDefaults d = super.getDefaults();
            d.put("Mine.own", Color.MAGENTA);
            return d;
        }
    }

    @Test
    public void developerDefaultsStayAndLookAndFeelDefaultsGo() throws Exception {
        final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
        PropertyChangeListener l = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                events.add(e);
            }
        };
        UIManager.addPropertyChangeListener(l);
        try {
            UIManager.put("Mine.kept", Color.ORANGE);
            UIManager.getLookAndFeelDefaults().put("Mine.gone", Color.PINK);
            assertSame(Color.PINK, UIManager.getColor("Mine.gone"));
            Mine mine = new Mine();
            UIManager.setLookAndFeel(mine);
            assertTrue(mine.initialized);
            assertSame(mine, UIManager.getLookAndFeel());
            assertEquals(1, events.size());
            assertEquals("lookAndFeel", events.get(0).getPropertyName());
            assertSame(mine, events.get(0).getNewValue());
            assertTrue(LafTheme.isDark());
            assertSame(Color.ORANGE, UIManager.getColor("Mine.kept"));
            assertNull(UIManager.getColor("Mine.gone"));
            assertSame(Color.MAGENTA, UIManager.getColor("Mine.own"));
            assertSame(Color.MAGENTA, UIManager.getLookAndFeelDefaults().get("Mine.own"));
            // What the developer put wins over the look and feel.
            UIManager.put("Panel.background", Color.RED);
            assertSame(Color.RED, UIManager.getColor("Panel.background"));
            FlatLightLaf.setup();
            assertTrue(mine.uninitialized);
            assertNull(UIManager.getColor("Mine.own"));
            assertSame(Color.RED, UIManager.getColor("Panel.background"));
            assertEquals(2, events.size());
            assertEquals(1, UIManager.getPropertyChangeListeners().length);
        } finally {
            UIManager.removePropertyChangeListener(l);
        }
    }

    @Test
    public void theKeysApplicationsReadAnswerInBothPalettes() {
        FlatLightLaf.setup();
        Color[] light = new Color[COLORS.length];
        for (int i = 0; i < COLORS.length; i++) {
            light[i] = UIManager.getColor(COLORS[i]);
            assertNotNull(COLORS[i] + " in the light palette", light[i]);
        }
        assertTrue(light(UIManager.getColor("Label.disabledForeground")) > light(UIManager.getColor("Label.foreground")));
        assertNotNull(UIManager.getFont("defaultFont"));
        assertNotNull(UIManager.getFont("Label.font"));
        assertTrue(UIManager.getFont("h1.font").getSize() > UIManager.getFont("defaultFont").getSize());
        assertTrue(UIManager.getFont("small.font").getSize() < UIManager.getFont("defaultFont").getSize());
        assertNotNull(UIManager.getBorder("TextField.border"));
        assertNotNull(UIManager.getBorder("ScrollPane.border"));
        assertNotNull(UIManager.getInsets("Button.margin"));
        assertTrue(UIManager.getInt("Component.arc") > 0);
        FlatDarkLaf.setup();
        int changed = 0;
        for (int i = 0; i < COLORS.length; i++) {
            Color c = UIManager.getColor(COLORS[i]);
            assertNotNull(COLORS[i] + " in the dark palette", c);
            if (!c.equals(light[i])) {
                changed++;
            }
        }
        assertTrue("most colors differ between the palettes: " + changed, changed > COLORS.length * 2 / 3);
        assertTrue(light(UIManager.getColor("Label.disabledForeground")) < light(UIManager.getColor("Label.foreground")));
        assertTrue(light(UIManager.getColor("TextArea.background")) < 128);
        assertTrue(light(UIManager.getColor("Button.background")) < 128);
        assertTrue(light(UIManager.getColor("Button.foreground")) >= 128);
    }

    @Test
    public void extraDefaultsAreReadWhenTheyArePlain() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("@accentColor", "#ff0080");
        extra.put("Mine.color", "#123");
        extra.put("Mine.count", "12");
        extra.put("Mine.flag", "true");
        extra.put("Mine.insets", "1,2,3,4");
        extra.put("Mine.expression", "lighten(@background,5%)");
        FlatLaf.setGlobalExtraDefaults(extra);
        FlatLightLaf.setup();
        assertEquals(new Color(0xff, 0x00, 0x80), UIManager.getColor("Component.accentColor"));
        assertEquals(new Color(0x11, 0x22, 0x33), UIManager.getColor("Mine.color"));
        assertEquals(12, UIManager.getInt("Mine.count"));
        assertTrue(UIManager.getBoolean("Mine.flag"));
        assertEquals(3, UIManager.getInsets("Mine.insets").bottom);
        assertNull(UIManager.get("Mine.expression"));
        FlatLaf.setGlobalExtraDefaults(null);
        FlatLightLaf.setup();
        assertNull(UIManager.get("Mine.color"));
        assertFalse(new Color(0xff, 0x00, 0x80).equals(UIManager.getColor("Component.accentColor")));
    }

    @Test
    public void theSmallApisAnswer() {
        assertEquals(7, UIScale.scale(7));
        assertEquals(7, UIScale.unscale(7));
        assertEquals(1f, UIScale.getUserScaleFactor(), 0f);
        assertEquals(new Dimension(3, 4), UIScale.scale(new Dimension(3, 4)));
        FlatAnimatedLafChange.showSnapshot();
        FlatDarkLaf.setup();
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        assertTrue(LafTheme.isDark());
        assertFalse(FlatLaf.supportsNativeWindowDecorations());
        int before = UIManager.getInstalledLookAndFeels().length;
        FlatDarkLaf.installLafInfo();
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        assertEquals(before + 1, infos.length);
        assertEquals(FlatDarkLaf.NAME, infos[before].getName());
        UIManager.setLookAndFeel(infos[before].getClassName());
        assertTrue(UIManager.getLookAndFeel() instanceof FlatDarkLaf);
        UIManager.LookAndFeelInfo[] one = new UIManager.LookAndFeelInfo[before];
        System.arraycopy(infos, 0, one, 0, before);
        UIManager.setInstalledLookAndFeels(one);
    }
}
