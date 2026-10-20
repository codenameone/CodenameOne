/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

import com.codename1.impl.NativeMenuEncoder;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The native menu model: submenu paths, separator groups, check and radio items, enabled state,
 * and the row format the C parsers read.
 */
class NativeMenuEncoderTest extends UITestBase {

    private static Vector vec(Command... cmds) {
        Vector v = new Vector();
        for (Command c : cmds) {
            v.addElement(c);
        }
        return v;
    }

    private static String describe(List<NativeMenuEncoder.Row> rows) {
        StringBuilder sb = new StringBuilder();
        for (NativeMenuEncoder.Row r : rows) {
            String[] path = r.getPath();
            for (int i = 0; i < path.length; i++) {
                sb.append(i == 0 ? "" : ">").append(path[i]);
            }
            sb.append(':').append(r.isSeparator() ? "---" : r.getLabel()).append('|');
        }
        return sb.toString();
    }

    @FormTest
    void parsesPathsWithEscapedSlash() {
        assertArrayEquals(new String[]{""}, NativeMenuEncoder.parsePath(null));
        assertArrayEquals(new String[]{"File"}, NativeMenuEncoder.parsePath("File"));
        assertArrayEquals(new String[]{"File", "Open Recent"}, NativeMenuEncoder.parsePath("File/Open Recent"));
        assertArrayEquals(new String[]{"Tools", "A/B"}, NativeMenuEncoder.parsePath("Tools/A//B"));
        assertArrayEquals(new String[]{"File", "Sub"}, NativeMenuEncoder.parsePath("/File/Sub/"));
    }

    @FormTest
    void submenusAreEmittedDepthFirstAtTheirPlace() {
        Command open = new Command("Open").setDesktopMenu("File");
        Command r1 = new Command("a.txt").setDesktopMenu("File/Open Recent");
        Command close = new Command("Close").setDesktopMenu("File");
        Command r2 = new Command("b.txt").setDesktopMenu("File/Open Recent");
        Command about = new Command("About").setDesktopMenu("Help");
        NativeMenuEncoder e = NativeMenuEncoder.encode(vec(open, r1, close, r2, about), 100);
        assertEquals("File:Open|File>Open Recent:a.txt|File>Open Recent:b.txt|File:Close|Help:About|",
                describe(e.getRows()));
        assertEquals(5, e.getIdCount());
        assertEquals(100, e.getRows().get(0).getId());
    }

    @FormTest
    void groupsOrderItemsAndInsertSeparators() {
        Command quit = new Command("Quit").setDesktopMenu("File").setDesktopMenuGroup(9);
        Command newDoc = new Command("New").setDesktopMenu("File");
        Command save = new Command("Save").setDesktopMenu("File").setDesktopMenuGroup(1);
        Command open = new Command("Open").setDesktopMenu("File");
        NativeMenuEncoder e = NativeMenuEncoder.encode(vec(quit, newDoc, save, open), 0);
        assertEquals("File:New|File:Open|File:---|File:Save|File:---|File:Quit|", describe(e.getRows()));
    }

    @FormTest
    void explicitSeparatorsAreNeverLeadingTrailingOrDoubled() {
        Command a = new Command("A").setDesktopMenu("Edit");
        Command b = new Command("B").setDesktopMenu("Edit");
        NativeMenuEncoder e = NativeMenuEncoder.encode(vec(
                Command.createDesktopSeparator("Edit"), a,
                Command.createDesktopSeparator("Edit"), Command.createDesktopSeparator("Edit"), b,
                Command.createDesktopSeparator("Edit")), 0);
        assertEquals("Edit:A|Edit:---|Edit:B|", describe(e.getRows()));
    }

    @FormTest
    void flagsAndWireFormat() {
        Command plain = new Command("Plain\tName").setDesktopMenu("View");
        plain.setDesktopShortcut('p');
        Command off = new Command("Off").setDesktopMenu("View");
        off.setEnabled(false);
        Command check = new Command("Sidebar").setDesktopMenu("View").setDesktopChecked(true);
        Command radio = new Command("100%").setDesktopMenu("View/Zoom").setDesktopRadioGroup("zoom");
        NativeMenuEncoder e = NativeMenuEncoder.encode(vec(plain, off, check, radio), 7);
        String wire = e.toWireFormat(false);
        String[] rows = wire.split("\n");
        assertEquals(4, rows.length);
        assertEquals("View\tPlain Name\t80\t1\t7\t1\t", rows[0]);
        assertEquals("View\tOff\t0\t0\t8\t0\t", rows[1]);
        assertEquals("View\tSidebar\t0\t0\t9\t" + (1 | 2 | 4) + "\t", rows[2]);
        assertEquals("View\u001fZoom\t100%\t0\t0\t10\t" + (1 | 2 | 8) + "\t", rows[3]);
    }

    @FormTest
    void legacySingleSegmentHintsKeepTheirPlacement() {
        Command c = new Command("Save").setDesktopMenu(Command.DESKTOP_MENU_FILE);
        Command none = new Command("Go");
        NativeMenuEncoder e = NativeMenuEncoder.encode(vec(c, none), 0);
        assertEquals("File", e.getRows().get(0).getHint());
        assertEquals("", e.getRows().get(1).getHint());
        assertFalse(e.toWireFormat(false).contains("\u001f"));
    }

    @FormTest
    void choosingToggleAndRadioItemsUpdatesStateBeforeTheAction() {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode("native");
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Doc");
        final boolean[] seen = new boolean[1];
        Command sidebar = new Command("Sidebar") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                seen[0] = isDesktopChecked();
            }
        };
        sidebar.setDesktopMenu("View").setDesktopChecked(false);
        Command z1 = new Command("100%").setDesktopMenu("View/Zoom").setDesktopRadioGroup("zoom");
        Command z2 = new Command("200%").setDesktopMenu("View/Zoom").setDesktopRadioGroup("zoom");
        Command disabled = new Command("Never").setDesktopMenu("View").setDesktopChecked(false);
        disabled.setEnabled(false);
        f.addCommand(sidebar);
        f.addCommand(z1);
        f.addCommand(z2);
        f.addCommand(disabled);
        f.show();
        DisplayTest.flushEdt();

        implementation.chooseNativeMenuCommand(sidebar);
        assertTrue(sidebar.isDesktopChecked());
        assertTrue(seen[0], "the action reads the new state");
        implementation.chooseNativeMenuCommand(sidebar);
        assertFalse(sidebar.isDesktopChecked());

        implementation.chooseNativeMenuCommand(z1);
        assertTrue(z1.isDesktopChecked());
        implementation.chooseNativeMenuCommand(z2);
        assertTrue(z2.isDesktopChecked());
        assertFalse(z1.isDesktopChecked(), "radio group is exclusive");
        implementation.chooseNativeMenuCommand(z2);
        assertTrue(z2.isDesktopChecked(), "choosing the checked radio item keeps it checked");

        implementation.chooseNativeMenuCommand(disabled);
        assertFalse(disabled.isDesktopChecked());
    }

    @FormTest
    void stateChangesRepublishTheMenuOnce() {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode("native");
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Doc");
        Command c = new Command("Sidebar").setDesktopMenu("View");
        f.addCommand(c);
        f.show();
        DisplayTest.flushEdt();
        Vector before = implementation.getLastNativeCommands();
        assertTrue(before.contains(c));
        implementation.setNativeCommandsForTest(null);
        c.setDesktopChecked(true);
        c.setEnabled(false);
        assertNull(implementation.getLastNativeCommands(), "coalesced to the next EDT cycle");
        DisplayTest.flushEdt();
        assertTrue(implementation.getLastNativeCommands().contains(c));
    }

    @FormTest
    void anExplicitSeparatorStaysWhereItWasAddedInAGroupedMenu() {
        Command a = new Command("A").setDesktopMenu("Edit").setDesktopMenuGroup(1);
        Command b = new Command("B").setDesktopMenu("Edit").setDesktopMenuGroup(1);
        NativeMenuEncoder e = NativeMenuEncoder.encode(vec(a, Command.createDesktopSeparator("Edit"), b), 0);
        assertEquals("Edit:A|Edit:---|Edit:B|", describe(e.getRows()));
    }

    @FormTest
    void clearingTheRadioGroupMakesAnOrdinaryItem() {
        Command c = new Command("Small").setDesktopRadioGroup("size");
        assertTrue(c.isDesktopCheckable());
        c.setDesktopRadioGroup(null);
        assertFalse(c.isDesktopCheckable());
        assertEquals(NativeMenuEncoder.FLAG_ENABLED, NativeMenuEncoder.encode(vec(c), 0).getRows().get(0).getFlags());
    }

    @FormTest
    void aRadioGroupCheckedWhileUnpublishedKeepsTheLatestCheck() {
        // Neither command is on a published surface, so checking b cannot see a yet.
        Command a = new Command("A").setDesktopRadioGroup("g").setDesktopChecked(true);
        Command b = new Command("B").setDesktopRadioGroup("g").setDesktopChecked(true);
        Vector v = vec(a, b);
        Command.settleRadioGroups(v);
        assertFalse(a.isDesktopChecked());
        assertTrue(b.isDesktopChecked());
    }

    @FormTest
    void checkingARadioOnAnotherSurfaceLeavesTheShownGroupAlone() {
        Form shown = Display.getInstance().getCurrent();
        Command visible = new Command("Visible").setDesktopRadioGroup("g").setDesktopChecked(true);
        shown.addCommand(visible);
        Command elsewhere = new Command("Elsewhere").setDesktopRadioGroup("g");
        elsewhere.setDesktopChecked(true);
        assertTrue(visible.isDesktopChecked(), "a command not on the shown form is another group");
    }
}
