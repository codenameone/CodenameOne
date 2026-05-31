package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the desktop window-chrome behavior added to {@link Form}/{@link Toolbar}: in the
 * {@code native} and {@code custom} desktop title-bar modes the CN1 Toolbar is detached (no
 * title strip painted), the form title is still tracked, and the toolbar's commands are bridged
 * to the native menu bar. The legacy {@code toolbar} mode and mobile remain unchanged.
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
    void customModeInstallsTitleBarChrome() {
        desktopMode("custom");
        Form f = new Form("Custom");
        f.show();
        DisplayTest.flushEdt();

        assertNull(f.getToolbar().getParent(), "toolbar itself is never attached in custom mode");
        assertTrue(containsUiid(f, "WindowTitleBar"), "custom mode must install a WindowTitleBar");
        assertTrue(containsUiid(f, "WindowCloseButton"), "custom mode must add a close caption button");
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

    private boolean containsUiid(Container root, String uiid) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (uiid.equals(c.getUIID())) {
                return true;
            }
            if (c instanceof Container && containsUiid((Container) c, uiid)) {
                return true;
            }
        }
        return false;
    }
}
