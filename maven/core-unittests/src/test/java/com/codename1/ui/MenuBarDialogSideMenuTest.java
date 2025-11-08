package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.Toolbar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class MenuBarDialogSideMenuTest extends UITestBase {
    private int originalCommandBehavior;

    @BeforeEach
    void captureCommandBehavior() {
        originalCommandBehavior = Display.getInstance().getCommandBehavior();
    }

    @AfterEach
    void restoreCommandBehavior() {
        Display.getInstance().setCommandBehavior(originalCommandBehavior);
        Form form = Display.getInstance().getCurrent();
        form.setMenuBar(new MenuBar());
        form.revalidate();
    }

    @FormTest
    void menuBarAddsCommandsAndTracksDefaults() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_BUTTON_BAR);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        MenuBar menuBar = new MenuBar();
        form.setMenuBar(menuBar);
        form.revalidate();

        Command first = new Command("First");
        Command second = new Command("Second");
        menuBar.addCommand(first);
        menuBar.addCommand(second);
        form.revalidate();

        assertEquals(2, menuBar.getCommandCount());
        assertSame(first, menuBar.getCommand(0));

        Button commandButton = menuBar.findCommandComponent(second);
        assertNotNull(commandButton, "Second command should have a bound button in button bar mode");

        menuBar.setDefaultCommand(first);
        assertSame(first, menuBar.getDefaultCommand());

        menuBar.setBackCommand(second);
        assertSame(second, menuBar.getBackCommand());

        form.removeCommand(second);
        assertEquals(1, menuBar.getCommandCount());

        menuBar.removeEmptySoftbuttons();
    }

    @FormTest
    void dialogShowPackedAndDisposeLifecycle() {
        implementation.setBuiltinSoundsEnabled(false);

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
    void sideMenuBarOpenAndCloseUpdatesState() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        final int[] invocations = {0};
        Command menuCommand = new Command("Menu Item") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                invocations[0]++;
            }
        };
        toolbar.addCommandToSideMenu(menuCommand);
        SideMenuBar sideMenu = (SideMenuBar) toolbar.getMenuBar();
        form.revalidate();

        toolbar.openSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        assertTrue(SideMenuBar.isShowing(), "Side menu should report as showing after openMenu");

        SideMenuBar.closeCurrentMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        assertFalse(SideMenuBar.isShowing(), "Side menu should not be showing after closeCurrentMenu");

        final boolean[] callbackInvoked = {false};
        SideMenuBar.closeCurrentMenu(new Runnable() {
            public void run() {
                callbackInvoked[0] = true;
            }
        });
        assertTrue(callbackInvoked[0], "Callback should run even when menu is already closed");
        assertEquals(0, invocations[0], "Command should not execute while menu is opened and closed programmatically");
    }
}
