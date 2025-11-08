package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.Toolbar;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Hashtable;

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
        assertSame(first, menuBar.getCommand(0));

        Button commandButton = menuBar.findCommandComponent(second);
        assertNotNull(commandButton, "Second command should have a bound button in button bar mode");

        menuBar.setSelectCommand(null);
        assertSame(second, menuBar.getDefaultCommand(), "MenuBar defaults to the most recently added command");
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
        Hashtable theme = new Hashtable();
        theme.put("sideMenuShadowBool", "false");
        theme.put("sideMenuTensileDragBool", "true");
        UIManager.getInstance().addThemeProps(theme);

        Form form = new Form("SideMenu Test", new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
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
        toolbar.addCommandToSideMenu(menuCommand);
        MenuBar toolbarMenu = toolbar.getMenuBar();
        assertNotNull(toolbarMenu);
        assertTrue(toolbarMenu instanceof SideMenuBar);
        SideMenuBar sideMenu = (SideMenuBar) toolbarMenu;
        form.revalidate();

        toolbar.openSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
        assertTrue(sideMenu.isMenuOpen(), "Side menu should report open state after invoking toolbar helper");

        toolbar.closeSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
        assertFalse(sideMenu.isMenuOpen(), "Side menu should report closed state after toolbar close");

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
}
