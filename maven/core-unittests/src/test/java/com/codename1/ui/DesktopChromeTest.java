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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the desktop window-chrome behavior added to {@link Form}/{@link Toolbar}: the
 * {@code native} mode detaches the CN1 Toolbar (no strip painted) and bridges its commands to the
 * native menu bar; the {@code custom} mode keeps the Toolbar attached so it acts as the window's
 * title bar (undecorated window) while still bridging the commands to the native menu bar. The
 * legacy {@code toolbar} mode and mobile remain unchanged.
 */
class DesktopChromeTest extends UITestBase {

    private void desktopMode(String titleBarMode) {
        implementation.setDesktop(true);
        implementation.setDesktopTitleBarMode(titleBarMode);
        Toolbar.setGlobalToolbar(true);
    }

    @FormTest
    void nativeModeDetachesToolbarAndBridgesCommands() {
        desktopMode("native");
        Form f = new Form("My App");
        Command save = new Command("Save");
        // addCommand routes into the toolbar's menu bar, which getAllNativeMenuCommands harvests
        f.addCommand(save);
        f.show();
        DisplayTest.flushEdt();

        assertNotNull(f.getToolbar(), "toolbar object must still exist for the command API");
        assertNull(f.getToolbar().getParent(), "in native mode the toolbar must not be attached/painted");
        assertEquals("My App", f.getTitle(), "title is still tracked for the OS window");

        Vector bridged = implementation.getLastNativeCommands();
        assertNotNull(bridged, "commands must be bridged to the native menu bar");
        assertTrue(bridged.contains(save), "the side-menu command must be in the native menu set");
    }

    @FormTest
    void customModeKeepsToolbarAsTitleBarAndBridgesCommands() {
        desktopMode("custom");
        Form f = new Form("Custom");
        Command save = new Command("Save");
        f.addCommand(save);
        f.show();
        DisplayTest.flushEdt();

        // the visible Toolbar IS the window title bar in custom mode, so it stays attached/painted
        assertNotNull(f.getToolbar().getParent(), "in custom mode the toolbar is shown as the title bar");
        assertEquals("Custom", f.getTitle(), "title is tracked and shown by the toolbar title bar");

        Vector bridged = implementation.getLastNativeCommands();
        assertNotNull(bridged, "commands must also be bridged to the native menu bar");
        assertTrue(bridged.contains(save), "the side-menu command must be in the native menu set");
    }

    /**
     * A port whose {@code setNativeCommands} discards them must keep drawing the Toolbar.
     *
     * <p>{@code MenuBar.updateCommands} used to call {@code setNativeCommands} and RETURN
     * whenever the behavior was NATIVE -- drawing no soft buttons, because on a platform with
     * a real menu bar drawing them too would duplicate every command. On a platform without
     * one, that meant the commands went to a method that discards them and were never drawn
     * at all. Silently: nothing in that path can tell "handled natively" from "dropped".</p>
     *
     * <p>Latent until a theme asked for it, which the desktop native themes now do --
     * {@code commandBehavior: Native}, right for the platforms they model and not yet
     * honourable by the Windows and Linux ports.</p>
     */
    @FormTest
    void aPortWithNoNativeMenuBarKeepsTheToolbar() {
        desktopMode("native");
        implementation.setNativeCommandsSupported(false);

        Form f = new Form("No native menu");
        Command save = new Command("Save");
        f.addCommand(save);
        f.show();
        DisplayTest.flushEdt();

        assertNotNull(f.getToolbar().getParent(),
                "hiding the toolbar would take away the only place these commands are drawn");
        assertTrue(f.getToolbar().getAllNativeMenuCommands().contains(save),
                "and the command is still in it");
    }

    @FormTest
    void aPortWithANativeMenuBarStillHidesTheToolbar() {
        // The other direction, so the guard above cannot silently disable native mode
        // everywhere: this is the case DesktopChromeTest's first test already covers, kept
        // beside its opposite.
        desktopMode("native");
        implementation.setNativeCommandsSupported(true);

        Form f = new Form("Native menu");
        f.addCommand(new Command("Save"));
        f.show();
        DisplayTest.flushEdt();

        assertNull(f.getToolbar().getParent(),
                "with somewhere for the commands to go, the toolbar is hidden as before");
    }

    /**
     * The same guard one level down, in {@code MenuBar.updateCommands}, which is the path a
     * form takes when the application sets {@code Display.COMMAND_BEHAVIOR_NATIVE} directly
     * rather than through the desktop title-bar mode.
     */
    @FormTest
    void nativeCommandBehaviourStillDrawsSoftButtonsWithNoNativeMenuBar() {
        implementation.setDesktop(true);
        implementation.setNativeCommandsSupported(false);
        Toolbar.setGlobalToolbar(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE);
        try {
            Form f = new Form("Soft buttons");
            Command save = new Command("Save");
            f.addCommand(save);
            f.show();
            DisplayTest.flushEdt();

            assertTrue(hasButtonLabelled(f, "Save"),
                    "with nowhere native to put it, the command has to be drawn");
        } finally {
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_DEFAULT);
            Toolbar.setGlobalToolbar(true);
        }
    }

    /**
     * The Initializr's barebones template does exactly this in {@code runApp()}. In native mode
     * the side menu used to be built on {@code getComponentForm()}, which is null while the
     * toolbar is detached, so the app died with a NullPointerException before its first form
     * was shown -- a blank desktop window for every project generated with
     * {@code desktop.titleBar=native}.
     */
    @FormTest
    void nativeModeBridgesSideMenuCommandsInsteadOfBuildingASideMenu() {
        desktopMode("native");

        Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new Button("Hello World"));
        Command hello = hi.getToolbar().addMaterialCommandToSideMenu("Hello Command",
                FontImage.MATERIAL_CHECK, 4, e -> { });
        hi.show();
        DisplayTest.flushEdt();

        assertNull(hi.getToolbar().getParent(), "the toolbar stays detached in native mode");
        Vector bridged = implementation.getLastNativeCommands();
        assertNotNull(bridged, "commands must be bridged to the native menu bar");
        assertTrue(bridged.contains(hello), "the side-menu command must reach the native menu bar");
    }

    /** The rest of the Toolbar API that assumed an attached form. */
    @FormTest
    void nativeModeToolbarCallsDoNotNeedAnAttachedForm() {
        desktopMode("native");

        Form f = new Form("Detached");
        Toolbar tb = f.getToolbar();
        Command item = new Command("Item");
        tb.addComponentToSideMenu(new Label("Header"));
        tb.addComponentToSideMenu(new Button("Row"), item);
        tb.addComponentToRightSideMenu(new Label("Right header"));
        Command back = new Command("Back");
        tb.setBackCommand(back);
        assertEquals(back, f.getBackCommand(), "the back command still reaches the form");
        tb.hideToolbar();
        tb.showToolbar();
        tb.openSideMenu();
        tb.openRightSideMenu();
        f.show();
        DisplayTest.flushEdt();

        assertTrue(implementation.getLastNativeCommands().contains(item),
                "a component row's command reaches the native menu bar");
    }

    @FormTest
    void nativeModeRemovedSideMenuCommandLeavesTheNativeMenu() {
        desktopMode("native");

        Form f = new Form("Remove");
        Command gone = new Command("Gone");
        f.getToolbar().addCommandToSideMenu(gone);
        f.getToolbar().removeCommand(gone);

        assertFalse(f.getToolbar().getAllNativeMenuCommands().contains(gone),
                "a removed command must not be bridged");
    }

    @FormTest
    void nativeModeSideMenuCommandsChangedAfterShowReachTheNativeMenu() {
        desktopMode("native");

        Form f = new Form("Later");
        f.show();
        DisplayTest.flushEdt();

        Command later = new Command("Later");
        f.getToolbar().addCommandToSideMenu(later);
        assertTrue(implementation.getLastNativeCommands().contains(later),
                "a command added while the form is showing must be published");

        f.getToolbar().removeCommand(later);
        assertFalse(implementation.getLastNativeCommands().contains(later),
                "and removing it must unpublish it");
    }

    private boolean hasButtonLabelled(Container root, String text) {
        for (int iter = 0; iter < root.getComponentCount(); iter++) {
            Component c = root.getComponentAt(iter);
            if (c instanceof Button && text.equals(((Button) c).getText())) {
                return true;
            }
            if (c instanceof Container && hasButtonLabelled((Container) c, text)) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    void desktopShortcutHintRoundTrips() {
        Command save = new Command("Save");
        assertEquals(0, save.getDesktopShortcutKeyChar(), "no shortcut by default");
        assertEquals(0, save.getDesktopShortcutModifiers(), "no modifiers by default");

        // single-arg form uses the platform-primary modifier and upper-cases the key
        save.setDesktopShortcut('s');
        assertEquals('S', save.getDesktopShortcutKeyChar());
        assertEquals(Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY, save.getDesktopShortcutModifiers());

        Command saveAs = new Command("Save As");
        saveAs.setDesktopShortcut('s',
                Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
        assertEquals('S', saveAs.getDesktopShortcutKeyChar());
        assertEquals(Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT,
                saveAs.getDesktopShortcutModifiers());
    }

    @FormTest
    void toolbarModeKeepsToolbarAttached() {
        // default desktop mode is "toolbar": behavior must be unchanged from today
        implementation.setDesktop(true);
        Toolbar.setGlobalToolbar(true);
        Form f = new Form("Legacy");
        f.show();
        DisplayTest.flushEdt();

        assertNotNull(f.getToolbar().getParent(), "in toolbar mode the toolbar is shown as today");
    }
}
