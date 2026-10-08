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
package com.codename1.desktopcompat.org.jdesktop.swingx.table;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.JCheckBoxMenuItem;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JMenuItem;
import com.codename1.desktopcompat.javax.swing.JPopupMenu;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.ScrollPaneConstants;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTable;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The column control: its menu shows and hides columns.
public class ColumnControlButtonTest extends KernelTestBase {

    private static JXTable table() {
        return new JXTable(new DefaultTableModel(new Object[][]{{"a", "b", "c"}}, new Object[]{"One", "Two",
            "Three"}));
    }

    private static JMenuItem item(JPopupMenu menu, String text) {
        for (int i = 0; i < menu.getComponentCount(); i++) {
            Component c = menu.getComponent(i);
            if (c instanceof JMenuItem && text.equals(((JMenuItem) c).getText())) {
                return (JMenuItem) c;
            }
        }
        return null;
    }

    @Test
    public void theMenuHidesAndShowsColumns() {
        JXTable t = table();
        assertTrue(t.getColumnControl() instanceof ColumnControlButton);
        ColumnControlButton b = (ColumnControlButton) t.getColumnControl();
        JPopupMenu menu = b.cn1BuildPopup();
        JMenuItem two = item(menu, "Two");
        assertTrue(two instanceof JCheckBoxMenuItem);
        assertTrue(((JCheckBoxMenuItem) two).getState());
        two.doClick();
        assertEquals(2, t.getColumnCount());
        assertFalse(t.getColumnExt("Two").isVisible());

        menu = b.cn1BuildPopup();
        two = item(menu, "Two");
        assertFalse("a hidden column is still in the menu", ((JCheckBoxMenuItem) two).getState());
        two.doClick();
        assertEquals(3, t.getColumnCount());
        assertEquals("Two", t.getColumnName(1));

        t.getColumnExt("Three").setHideable(false);
        menu = b.cn1BuildPopup();
        assertSame(null, item(menu, "Three"));
        item(menu, "One").doClick();
        item(menu, "Two").doClick();
        assertEquals("the last visible column stays", 1, t.getColumnCount());

        item(menu, "Horizontal Scroll").doClick();
        assertTrue(t.isHorizontalScrollEnabled());
        b.setAdditionalActionsVisible(false);
        assertSame(null, item(b.cn1BuildPopup(), "Pack All Columns"));
    }

    @Test
    public void theButtonOpensAndClosesItsMenuAndIsTheScrollPanesCorner() {
        JXTable t = table();
        JScrollPane pane = new JScrollPane(t);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.CENTER);
        f.add(t.getColumnControl(), BorderLayout.NORTH);
        show(f);
        t.setColumnControlVisible(true);
        assertTrue(t.isColumnControlVisible());
        assertSame(t.getColumnControl(), pane.getCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER));
        t.setColumnControlVisible(false);
        assertSame(null, pane.getCorner(ScrollPaneConstants.UPPER_TRAILING_CORNER));
        ColumnControlButton b = (ColumnControlButton) t.getColumnControl();
        b.togglePopup();
        b.togglePopup();
    }
}
