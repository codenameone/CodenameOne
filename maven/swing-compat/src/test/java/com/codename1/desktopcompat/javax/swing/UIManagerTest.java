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
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import org.junit.After;
import org.junit.Test;

/// The look and feel calls an application makes at start-up do no harm,
/// and the defaults answer what was put, or else the theme.
public class UIManagerTest extends KernelTestBase {

    private static final String[] KEYS = {"Table.gridColor", "Panel.background", "Mine.color", "Mine.border",
        "Mine.insets", "Mine.size", "Mine.count", "Mine.flag", "Mine.text", "List.selectionBackground",
        "Table.background"};

    @After
    public void forget() throws Exception {
        for (int i = 0; i < KEYS.length; i++) {
            UIManager.put(KEYS[i], null);
        }
        UIManager.setLookAndFeel((LookAndFeel) null);
        ScrollDelegate.setBarThickness(-1);
    }

    @Test
    public void settingALookAndFeelNeverFails() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        UIManager.setLookAndFeel("com.example.NoSuchLookAndFeel");
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        assertNotNull(UIManager.getSystemLookAndFeelClassName());

        // The loop every Nimbus application starts with.
        UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
        assertEquals(1, infos.length);
        for (int i = 0; i < infos.length; i++) {
            assertNotNull(infos[i].getName());
            UIManager.setLookAndFeel(infos[i].getClassName());
        }
        assertEquals(UIManager.getSystemLookAndFeelClassName(), infos[0].getClassName());
        infos[0] = null;
        assertNotNull("a copy was handed out", UIManager.getInstalledLookAndFeels()[0]);
        UIManager.installLookAndFeel("Mine", "com.example.Mine");
        assertEquals(2, UIManager.getInstalledLookAndFeels().length);
        assertEquals("Mine", UIManager.getInstalledLookAndFeels()[1].getName());
        UIManager.setInstalledLookAndFeels(new UIManager.LookAndFeelInfo[]{infos.length > 0
                ? new UIManager.LookAndFeelInfo("Codename One", UIManager.getSystemLookAndFeelClassName()) : null});
        assertEquals(1, UIManager.getInstalledLookAndFeels().length);
    }

    private static final class Mine extends LookAndFeel {
        private final boolean supported;
        boolean initialized;

        Mine(boolean supported) {
            this.supported = supported;
        }

        @Override
        public String getName() {
            return "Mine";
        }

        @Override
        public String getID() {
            return "Mine";
        }

        @Override
        public String getDescription() {
            return "A look of the application's own";
        }

        @Override
        public boolean isNativeLookAndFeel() {
            return false;
        }

        @Override
        public boolean isSupportedLookAndFeel() {
            return supported;
        }

        @Override
        public void initialize() {
            initialized = true;
        }
    }

    @Test
    public void aLookAndFeelObjectIsRememberedAndInitialized() throws Exception {
        LookAndFeel builtIn = UIManager.getLookAndFeel();
        assertNotNull(builtIn);
        assertEquals("Codename One", builtIn.getName());
        assertTrue(builtIn.isSupportedLookAndFeel());
        assertSame(UIManager.getLookAndFeelDefaults(), builtIn.getDefaults());
        Mine mine = new Mine(true);
        UIManager.setLookAndFeel(mine);
        assertSame(mine, UIManager.getLookAndFeel());
        assertTrue(mine.initialized);
        assertNull(mine.getDefaults());
        assertTrue(mine.toString().indexOf("A look of the application's own") >= 0);
        try {
            UIManager.setLookAndFeel(new Mine(false));
            fail("it says it is not supported");
        } catch (UnsupportedLookAndFeelException expected) {
            assertSame(mine, UIManager.getLookAndFeel());
        }
    }

    @Test
    public void whatWasPutIsWhatComesBack() {
        Color c = new Color(1, 2, 3);
        Border b = BorderFactory.createLineBorder(Color.RED);
        Insets in = new Insets(1, 2, 3, 4);
        Dimension d = new Dimension(5, 6);
        UIManager.put("Mine.color", c);
        UIManager.put("Mine.border", b);
        UIManager.put("Mine.insets", in);
        UIManager.put("Mine.size", d);
        UIManager.put("Mine.count", Integer.valueOf(7));
        UIManager.put("Mine.flag", Boolean.TRUE);
        UIManager.put("Mine.text", "text");
        assertSame(c, UIManager.getColor("Mine.color"));
        assertSame(b, UIManager.getBorder("Mine.border"));
        assertSame(in, UIManager.getInsets("Mine.insets"));
        assertSame(d, UIManager.getDimension("Mine.size"));
        assertEquals(7, UIManager.getInt("Mine.count"));
        assertTrue(UIManager.getBoolean("Mine.flag"));
        assertEquals("text", UIManager.getString("Mine.text"));
        // A getter of another type answers nothing.
        assertNull(UIManager.getColor("Mine.text"));
        assertNull(UIManager.getBorder("Mine.color"));
        assertEquals(0, UIManager.getInt("Mine.text"));

        UIDefaults defaults = UIManager.getDefaults();
        // The look and feel has a table of its own beneath this one.
        assertTrue(defaults != UIManager.getLookAndFeelDefaults());
        assertSame(c, defaults.getColor("Mine.color"));
        assertSame(c, defaults.get("Mine.color"));
        defaults.put("Mine.color", Color.BLUE);
        assertSame(Color.BLUE, UIManager.getColor("Mine.color"));
        UIManager.put("Mine.color", null);
        assertNull(UIManager.get("Mine.color"));
        assertNull(UIManager.get("No.such.key"));
        assertNull(UIManager.getBorder("No.such.border"));
    }

    @Test
    public void commonKeysAnswerFromTheTheme() {
        Color background = UIManager.getColor("Panel.background");
        assertNotNull(background);
        assertEquals(background, UIManager.getColor("control"));
        assertEquals(background, UIManager.getDefaults().getColor("Label.background"));
        assertNotNull(UIManager.getColor("Label.foreground"));
        assertEquals(UIManager.getColor("Label.foreground"), UIManager.getColor("controlText"));
        Font f = UIManager.getFont("Label.font");
        assertNotNull(f);
        assertEquals(f, UIManager.getFont("Button.font"));
        assertNotNull(UIManager.getColor("Table.gridColor"));
        assertNotNull(UIManager.getColor("List.selectionBackground"));
        assertNotNull(UIManager.getColor("Table.selectionForeground"));
        assertEquals(UIManager.getColor("Table.selectionBackground"), UIManager.getColor("textHighlight"));
        // The grid color is the one a table draws with.
        assertEquals(UIManager.getColor("Table.gridColor"), new JTable(2, 2).getGridColor());
        ScrollDelegate.setBarThickness(14);
        assertEquals(14, UIManager.getInt("ScrollBar.width"));
    }

    @Test
    public void theWidgetsTakeWhatTheApplicationPut() {
        Color grid = new Color(10, 20, 30);
        Color selection = new Color(40, 50, 60);
        Color cells = new Color(70, 80, 90);
        UIManager.put("Table.gridColor", grid);
        UIManager.put("List.selectionBackground", selection);
        UIManager.put("Table.background", cells);
        assertSame(grid, UIManager.getColor("Table.gridColor"));
        JTable table = new JTable(2, 2);
        assertEquals(grid, table.getGridColor());
        assertEquals(cells, table.getBackground());
        assertEquals(selection, new JList<String>().getSelectionBackground());
        UIManager.put("List.selectionBackground", null);
        assertEquals("a list keeps its own default when nothing was put", new Color(0x38, 0x75, 0xd7),
                new JList<String>().getSelectionBackground());
    }
}
