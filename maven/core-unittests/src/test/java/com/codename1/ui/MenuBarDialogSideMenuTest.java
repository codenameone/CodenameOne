package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class MenuBarDialogSideMenuTest extends UITestBase {
    private int originalCommandBehavior;
    private boolean originalOnTopSideMenu;

    @BeforeEach
    void captureCommandBehavior() {
        originalCommandBehavior = Display.getInstance().getCommandBehavior();
        originalOnTopSideMenu = Toolbar.isOnTopSideMenu();
    }

    @AfterEach
    void restoreCommandBehavior() {
        Display.getInstance().setCommandBehavior(originalCommandBehavior);
        Toolbar.setOnTopSideMenu(originalOnTopSideMenu);
        Form form = Display.getInstance().getCurrent();
        form.setMenuBar(new MenuBar());
        form.revalidate();
    }

    @FormTest
    void menuBarAddsCommandsAndTracksDefaults() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR);

        Form form = new Form("MenuBar Test", new BorderLayout());
        form.show();

        MenuBar menuBar = new MenuBar();
        form.setMenuBar(menuBar);
        form.revalidate();

        Command first = new Command("First");
        Command second = new Command("Second");
        menuBar.addCommand(first);
        menuBar.addCommand(second);
        form.revalidate();

        assertEquals(2, menuBar.getCommandCount());
        assertSame(second, menuBar.getCommand(0));
        assertSame(first, menuBar.getCommand(1));

        Button commandButton = menuBar.findCommandComponent(second);
        assertNotNull(commandButton, "Second command should have a bound button in button bar mode");

        menuBar.setSelectCommand(null);
        assertNull(menuBar.getDefaultCommand(), "MenuBar should clear the default command when select command is removed");
        menuBar.setDefaultCommand(second);
        assertSame(second, menuBar.getDefaultCommand());

        menuBar.setBackCommand(second);
        assertSame(second, menuBar.getBackCommand());

        form.removeCommand(second);
        assertEquals(1, menuBar.getCommandCount());

        menuBar.removeEmptySoftbuttons();
    }

    @FormTest
    void dialogShowPackedAndDisposeLifecycle() {
        implementation.setBuiltinSoundsEnabled(false);

        Form owner = new Form("Dialog Owner", new BorderLayout());
        owner.show();

        Dialog dialog = new Dialog("Alert", new BorderLayout());
        dialog.setDisposeWhenPointerOutOfBounds(true);
        dialog.setDialogUIID("CustomDialog");
        dialog.add(BorderLayout.CENTER, new Label("Body"));
        Command ok = new Command("OK");
        dialog.addCommand(ok);

        dialog.showPacked(BorderLayout.CENTER, false);
        flushSerialCalls();
        assertFalse(dialog.isDisposed(), "Dialog should be visible after showPacked");

        dialog.dispose();
        flushSerialCalls();
        assertTrue(dialog.isDisposed(), "Dialog should be disposed after calling dispose");
    }

    @FormTest
    void sideMenuBarRegistersCommandsWithPlacementMetadata() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
        Toolbar.setOnTopSideMenu(false);

        Form form = new Form("SideMenu Test", new BorderLayout());
        SideMenuBar sideMenu = new SideMenuBar();
        form.setMenuBar(sideMenu);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        Command left = new Command("Left");
        Command right = new Command("Right");
        right.putClientProperty(SideMenuBar.COMMAND_PLACEMENT_KEY, SideMenuBar.COMMAND_PLACEMENT_VALUE_RIGHT);

        sideMenu.addCommand(left);
        sideMenu.addCommand(right);
        form.revalidate();
        form.getAnimationManager().flush();
        flushSerialCalls();

        assertEquals(2, sideMenu.getCommandCount(), "Both commands should be registered with the side menu");
        assertSame(left, sideMenu.getCommand(0));
        assertSame(right, sideMenu.getCommand(1));
        assertEquals(SideMenuBar.COMMAND_PLACEMENT_VALUE_RIGHT,
                right.getClientProperty(SideMenuBar.COMMAND_PLACEMENT_KEY));

        sideMenu.removeCommand(right);
        form.revalidate();
        form.getAnimationManager().flush();
        flushSerialCalls();

        assertEquals(1, sideMenu.getCommandCount(), "Removing a command should update the side menu state");

        final boolean[] callbackInvoked = {false};
        SideMenuBar.closeCurrentMenu(new Runnable() {
            public void run() {
                callbackInvoked[0] = true;
            }
        });
        flushSerialCalls();
        assertTrue(callbackInvoked[0], "Callback should still run when no side menu is showing");
    }
}
