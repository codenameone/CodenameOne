package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Vector;

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
    void sideMenuBarOpenAndCloseUpdatesState() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
        Toolbar.setOnTopSideMenu(false);

        Form form = new Form("SideMenu Test", new BorderLayout());
        ResourceFreeSideMenuBar sideMenu = new ResourceFreeSideMenuBar();
        form.setMenuBar(sideMenu);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        final int[] invocations = {0};
        Command menuCommand = new Command("Menu Item") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                invocations[0]++;
            }
        };
        menuCommand.putClientProperty(SideMenuBar.COMMAND_PLACEMENT_KEY, SideMenuBar.COMMAND_PLACEMENT_VALUE_LEFT);
        form.addCommand(menuCommand);
        form.revalidate();
        form.getAnimationManager().flush();
        flushSerialCalls();

        sideMenu.openMenu(SideMenuBar.COMMAND_PLACEMENT_VALUE_LEFT);
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
        assertTrue(sideMenu.isMenuOpen(), "Side menu should report open state after invoking openMenu");

        sideMenu.closeMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
        assertFalse(sideMenu.isMenuOpen(), "Side menu should report closed state after invoking closeMenu");

        final boolean[] callbackInvoked = {false};
        SideMenuBar.closeCurrentMenu(new Runnable() {
            public void run() {
                callbackInvoked[0] = true;
            }
        });
        flushSerialCalls();
        assertTrue(callbackInvoked[0], "Callback should run even when menu is already closed");
        assertEquals(0, invocations[0], "Command should not execute while menu is opened and closed programmatically");
    }

    private void awaitAnimations(Form form) {
        CountDownLatch latch = new CountDownLatch(1);
        form.getAnimationManager().flushAnimation(latch::countDown);
        form.getAnimationManager().flush();
        flushSerialCalls();
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for animations");
        }
        flushSerialCalls();
    }

    private static class ResourceFreeSideMenuBar extends SideMenuBar {
        @Override
        protected Container createSideNavigationPanel(Vector commands, String placement) {
            Container menu = new Container(BoxLayout.y());
            for (int i = 0; i < commands.size(); i++) {
                Command command = (Command) commands.elementAt(i);
                if (command != null) {
                    menu.add(createTouchCommandButton(command));
                }
            }
            return menu;
        }
    }
}
