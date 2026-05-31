package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
