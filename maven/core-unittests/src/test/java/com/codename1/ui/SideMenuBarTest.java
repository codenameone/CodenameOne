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
    void addingCommandsIncrementsCount() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        SideMenuBar sideMenu = new SideMenuBar();
        form.setMenuBar(sideMenu);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        int initialCount = form.getCommandCount();
        Command cmd = new Command("Item");
        form.addCommand(cmd);
        form.revalidate();
        form.getAnimationManager().flush();
        flushSerialCalls();

        assertEquals(initialCount + 1, form.getCommandCount(), "Command count should increase after adding a command");
        assertTrue(formContainsCommand(form, cmd), "Form should report the newly added command");

        form.removeCommand(cmd);
        form.revalidate();

        form.getAnimationManager().flush();

        flushSerialCalls();
        assertEquals(initialCount, form.getCommandCount(), "Removing the command should restore original count");
        assertFalse(formContainsCommand(form, cmd), "Form should no longer contain removed command");
    }

    @FormTest
    void closeCurrentMenuRunsCallbackWhenNoMenuShowing() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        SideMenuBar sideMenu = new SideMenuBar();
        form.setMenuBar(sideMenu);
        form.show();
        form.getAnimationManager().flush();


        final boolean[] invoked = {false};
        SideMenuBar.closeCurrentMenu(() -> invoked[0] = true);
        form.getAnimationManager().flush();

        assertTrue(invoked[0], "Callback should run even when no menu is showing");
        assertFalse(SideMenuBar.isShowing(), "Menu should remain hidden");
    }

    private boolean formContainsCommand(Form form, Command command) {
        for (int i = 0; i < form.getCommandCount(); i++) {
            if (form.getCommand(i) == command) {
                return true;
            }
        }
        return false;
    }
}
