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

import com.codename1.impl.NativeMenuConventions;
import com.codename1.impl.NativeMenuEncoder;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Standard command roles: each platform's placement, label and shortcut, overrides, platform
 * filtering, menu order and the document-modified state.
 */
class NativeMenuConventionsTest extends UITestBase {

    private static Vector vec(Command... cmds) {
        Vector v = new Vector();
        for (Command c : cmds) {
            v.addElement(c);
        }
        return v;
    }

    private static NativeMenuEncoder.Row only(Command c, String convention) {
        List<NativeMenuEncoder.Row> rows = NativeMenuEncoder.encode(vec(c), 0, false, convention).getRows();
        assertEquals(1, rows.size(), "one row");
        return rows.get(0);
    }

    private static Command role(String role) {
        return new Command(null).setDesktopRole(role);
    }

    private static String roots(List<NativeMenuEncoder.Row> rows) {
        StringBuilder sb = new StringBuilder();
        String last = null;
        for (NativeMenuEncoder.Row r : rows) {
            if (!r.getHint().equals(last)) {
                sb.append(r.getHint()).append(' ');
                last = r.getHint();
            }
        }
        return sb.toString().trim();
    }

    @FormTest
    void settingsGoesWhereEachPlatformKeepsIt() {
        NativeMenuEncoder.Row mac = only(role(Command.DESKTOP_ROLE_SETTINGS), NativeMenuConventions.MAC);
        assertEquals("App", mac.getHint());
        assertEquals("Settings\u2026", mac.getLabel());
        assertEquals(',', mac.getShortcutKeyChar());
        assertEquals(Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY, mac.getShortcutModifiers());

        NativeMenuEncoder.Row win = only(role(Command.DESKTOP_ROLE_SETTINGS), NativeMenuConventions.WINDOWS);
        assertEquals("Tools", win.getHint());
        assertEquals("Options", win.getLabel());
        assertEquals(0, win.getShortcutKeyChar());

        NativeMenuEncoder.Row linux = only(role(Command.DESKTOP_ROLE_SETTINGS), NativeMenuConventions.LINUX);
        assertEquals("Edit", linux.getHint());
        assertEquals("Preferences", linux.getLabel());
    }

    @FormTest
    void quitIsExitOnWindowsAndAbsentFromABrowser() {
        assertEquals("Quit", only(role(Command.DESKTOP_ROLE_QUIT), NativeMenuConventions.LINUX).getLabel());
        assertEquals('Q', only(role(Command.DESKTOP_ROLE_QUIT), NativeMenuConventions.LINUX).getShortcutKeyChar());
        NativeMenuEncoder.Row win = only(role(Command.DESKTOP_ROLE_QUIT), NativeMenuConventions.WINDOWS);
        assertEquals("File", win.getHint());
        assertEquals("Exit", win.getLabel());
        assertTrue(NativeMenuEncoder.encode(vec(role(Command.DESKTOP_ROLE_QUIT)), 0, false,
                NativeMenuConventions.WEB).getRows().isEmpty(), "a page cannot quit");
        assertTrue(NativeMenuEncoder.encode(vec(role(Command.DESKTOP_ROLE_HIDE_APP)), 0, false,
                NativeMenuConventions.WINDOWS).getRows().isEmpty(), "Hide is macOS only");
    }

    @FormTest
    void aboutIsInTheAppMenuOnMacAndUnderHelpElsewhere() {
        assertEquals("App", only(role(Command.DESKTOP_ROLE_ABOUT), NativeMenuConventions.MAC).getHint());
        assertEquals("Help", only(role(Command.DESKTOP_ROLE_ABOUT), NativeMenuConventions.WINDOWS).getHint());
        assertEquals("Help", only(role(Command.DESKTOP_ROLE_ABOUT), NativeMenuConventions.LINUX).getHint());
        assertTrue(only(role(Command.DESKTOP_ROLE_ABOUT), NativeMenuConventions.WINDOWS).getLabel().startsWith("About"));
    }

    @FormTest
    void theRoleTravelsInTheFlags() {
        NativeMenuEncoder.Row quit = only(role(Command.DESKTOP_ROLE_QUIT), NativeMenuConventions.MAC);
        assertEquals(NativeMenuConventions.ordinalOf(Command.DESKTOP_ROLE_QUIT), quit.getRoleOrdinal());
        assertEquals(3, quit.getRoleOrdinal(), "the ordinals are wire format");
        assertEquals(0, only(new Command("Plain"), NativeMenuConventions.MAC).getRoleOrdinal());
        assertTrue((quit.getFlags() & NativeMenuEncoder.FLAG_ENABLED) != 0, "the role bits leave the others alone");
    }

    @FormTest
    void whatTheCommandSetsWinsOverItsRole() {
        Command c = new Command("Leave").setDesktopRole(Command.DESKTOP_ROLE_QUIT)
                .setDesktopMenu("Session").setDesktopShortcut('L');
        NativeMenuEncoder.Row r = only(c, NativeMenuConventions.WINDOWS);
        assertEquals("Session", r.getHint());
        assertEquals("Leave", r.getLabel());
        assertEquals('L', r.getShortcutKeyChar());
    }

    @FormTest
    void standardItemsOpenAndCloseTheirMenu() {
        Command mine = new Command("Export").setDesktopMenu("File");
        Command exit = role(Command.DESKTOP_ROLE_QUIT);
        Command open = role(Command.DESKTOP_ROLE_OPEN);
        List<NativeMenuEncoder.Row> rows = NativeMenuEncoder.encode(vec(exit, mine, open), 0, false,
                NativeMenuConventions.WINDOWS).getRows();
        assertEquals("Open\u2026", rows.get(0).getLabel());
        assertTrue(rows.get(1).isSeparator());
        assertEquals("Export", rows.get(2).getLabel());
        assertTrue(rows.get(3).isSeparator());
        assertEquals("Exit", rows.get(4).getLabel());
    }

    @FormTest
    void legacyPlacementsAreRoles() {
        Command prefs = new Command("Prefs").setDesktopMenu(Command.DESKTOP_MENU_PREFERENCES);
        assertEquals("Edit", only(prefs, NativeMenuConventions.LINUX).getHint());
        assertEquals("Tools", only(prefs, NativeMenuConventions.WINDOWS).getHint());
        assertEquals("App", only(prefs, NativeMenuConventions.MAC).getHint());
        assertEquals("Prefs", only(prefs, NativeMenuConventions.MAC).getLabel(), "its own name is kept");
        Command app = new Command("Thing").setDesktopMenu(Command.DESKTOP_MENU_APP);
        assertEquals("File", only(app, NativeMenuConventions.WINDOWS).getHint(), "only macOS has an app menu");
        assertEquals("File", only(new Command("x").setDesktopMenu("file"), NativeMenuConventions.WINDOWS).getHint());
    }

    @FormTest
    void platformListsFilterTheMenuBar() {
        Command c = new Command("Only Mac").setDesktopPlatforms("mac");
        assertEquals(1, NativeMenuEncoder.encode(vec(c), 0, false, NativeMenuConventions.MAC).getRows().size());
        assertTrue(NativeMenuEncoder.encode(vec(c), 0, false, NativeMenuConventions.LINUX).getRows().isEmpty());
        c.setDesktopPlatforms();
        assertTrue(c.isDesktopPlatform("linux"), "an empty list publishes everywhere");
    }

    @FormTest
    void menusComeOutInTheStandardOrder() {
        Vector v = vec(new Command("h").setDesktopMenu("Help"), new Command("c").setDesktopMenu("Mine"),
                new Command("e").setDesktopMenu("Edit"), role(Command.DESKTOP_ROLE_SETTINGS),
                new Command("f").setDesktopMenu("File"), new Command("w").setDesktopMenu("Window"));
        assertEquals("File Edit Mine Tools Window Help",
                roots(NativeMenuEncoder.encode(v, 0, false, NativeMenuConventions.WINDOWS).getRows()));
        assertEquals("App File Edit Mine Window Help",
                roots(NativeMenuEncoder.encode(v, 0, false, NativeMenuConventions.MAC).getRows()));
    }

    @FormTest
    void createDesktopRoleNamesTheCommandForThePlatform() {
        Command c = Command.createDesktopRole(Command.DESKTOP_ROLE_SAVE, null);
        assertEquals("Save", c.getCommandName());
        assertEquals(Command.DESKTOP_ROLE_SAVE, c.getDesktopRole());
        assertTrue(c.isDesktopEnabledWhenModified(), "Save follows the modified state by default");
        assertFalse(Command.createDesktopRole(Command.DESKTOP_ROLE_OPEN, null).isDesktopEnabledWhenModified());
        assertNull(NativeMenuConventions.labelOf(NativeMenuConventions.WINDOWS, Command.DESKTOP_ROLE_HIDE_APP));
    }

    @FormTest
    void saveFollowsTheDocumentModifiedState() {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode("native");
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Doc");
        Command save = Command.createDesktopRole(Command.DESKTOP_ROLE_SAVE, null);
        Command other = new Command("Other").setDesktopMenu("File");
        f.getToolbar().addCommandToOverflowMenu(save);
        f.getToolbar().addCommandToOverflowMenu(other);
        f.show();
        DisplayTest.flushEdt();
        assertTrue(save.isEnabled(), "nothing changes until the application reports a state");
        f.setDocumentModified(false);
        assertFalse(save.isEnabled());
        assertTrue(other.isEnabled(), "an ordinary command is left alone");
        f.setDocumentModified(true);
        assertTrue(save.isEnabled());
        assertTrue(f.isDocumentModified());
        assertEquals("*Doc", implementation.getNativeWindowTitle(f));
        f.setDocumentModified(false);
        assertEquals("Doc", implementation.getNativeWindowTitle(f));
    }

    @FormTest
    void aSaveAddedLaterFollowsTheReportedState() {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode("native");
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Doc");
        f.show();
        DisplayTest.flushEdt();
        f.setDocumentModified(false);
        Command save = Command.createDesktopRole(Command.DESKTOP_ROLE_SAVE, null);
        f.getToolbar().addCommandToOverflowMenu(save);
        assertFalse(save.isEnabled(), "a clean document disables a Save added after the state");
    }

    @FormTest
    void aLeadingSeparatorDoesNotPlaceItsSubmenu() {
        Command a = new Command("A").setDesktopMenu("File").setDesktopMenuGroup(1);
        Command sep = Command.createDesktopSeparator("File/Sub");
        Command b = new Command("B").setDesktopMenu("File/Sub").setDesktopMenuGroup(2);
        List<NativeMenuEncoder.Row> rows = NativeMenuEncoder.encode(vec(a, sep, b), 0, false,
                NativeMenuConventions.WINDOWS).getRows();
        assertEquals("A", rows.get(0).getLabel(), "the submenu sorts by B's group, not the separator's");
    }

    @FormTest
    void aSaveReaddedAfterALaterReportTakesIt() {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode("native");
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Doc");
        Command save = Command.createDesktopRole(Command.DESKTOP_ROLE_SAVE, null);
        f.getToolbar().addCommandToOverflowMenu(save);
        f.show();
        DisplayTest.flushEdt();
        f.setDocumentModified(false);
        assertFalse(save.isEnabled());
        f.getToolbar().removeOverflowCommand(save);
        f.setDocumentModified(true);
        f.getToolbar().addCommandToOverflowMenu(save);
        assertTrue(save.isEnabled(), "the later report reaches the re-added command");
    }

    @FormTest
    void aRoleNameIsLookedUpWhenRead() {
        Command c = Command.createDesktopRole(Command.DESKTOP_ROLE_QUIT, null);
        assertEquals(NativeMenuConventions.labelOf(NativeMenuConventions.current(), Command.DESKTOP_ROLE_QUIT),
                c.getCommandName());
        c.setCommandName("Leave");
        assertEquals("Leave", c.getCommandName(), "the application's own name wins");
    }
}
