package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class SideMenuBarTest extends UITestBase {
    private int originalCommandBehavior;

    @BeforeEach
    void captureCommandBehavior() {
        originalCommandBehavior = Display.getInstance().getCommandBehavior();
    }

    @AfterEach
    void restoreCommandBehavior() {
        Display.getInstance().setCommandBehavior(originalCommandBehavior);
    }

    @FormTest
    void openAndCloseMenuUpdatesStaticState() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);

        Form form = new Form("SideMenu", new BorderLayout());
        SideMenuBar sideMenu = new SideMenuBar();
        form.setMenuBar(sideMenu);
        Command menuCommand = new Command("Item");
        form.addCommand(menuCommand);
        form.show();
        form.getAnimationManager().flush();

        assertFalse(SideMenuBar.isShowing(), "Side menu should not be showing before open");

        sideMenu.openMenu(null);
        form.getAnimationManager().flush();

        assertTrue(SideMenuBar.isShowing(), "Side menu should report showing after open");

        sideMenu.closeMenu();
        form.getAnimationManager().flush();

        assertFalse(SideMenuBar.isShowing(), "Side menu should no longer be showing after close");
    }

    @FormTest
    void closeCurrentMenuCallbackInvoked() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);

        Form form = new Form("SideMenu", new BorderLayout());
        SideMenuBar sideMenu = new SideMenuBar();
        form.setMenuBar(sideMenu);
        form.addCommand(new Command("First"));
        form.show();
        form.getAnimationManager().flush();

        sideMenu.openMenu(null);
        form.getAnimationManager().flush();

        final boolean[] called = {false};
        SideMenuBar.closeCurrentMenu(() -> called[0] = true);
        form.getAnimationManager().flush();

        assertTrue(called[0], "Callback should be invoked after menu closes");
        assertFalse(SideMenuBar.isShowing(), "Menu should be closed after callback execution");
    }
}
