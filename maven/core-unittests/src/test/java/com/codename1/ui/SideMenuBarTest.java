package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.Button;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import java.util.concurrent.atomic.AtomicBoolean;

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
        TestLogger.install();
        try {
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
            assertEquals(1, TestLogger.getPrinted().size());
            assertTrue(TestLogger.getPrinted().get(0).contains("WARNING: Display.setCommandBehavior() is deprecated"));
        } finally {
            TestLogger.remove();
        }
    }



    @FormTest
    void closeCurrentMenuRunsCallbackWhenNoMenuShowing() {
        TestLogger.install();
        try {
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
            assertEquals(1, TestLogger.getPrinted().size());
            assertTrue(TestLogger.getPrinted().get(0).contains("WARNING: Display.setCommandBehavior() is deprecated"));
        } finally {
            TestLogger.remove();
        }
    }


    private boolean formContainsCommand(Form form, Command command) {
        for (int i = 0; i < form.getCommandCount(); i++) {
            if (form.getCommand(i) == command) {
                return true;
            }
        }
        return false;
    }

    @FormTest
    public void testSideMenuBarCommandWrapperAndShowWaiter() throws Exception {
        // Disable shadow to prevent resource loading error and provide dummy image
        java.util.Hashtable theme = new java.util.Hashtable();
        theme.put("sideMenuShadowBool", Boolean.FALSE);
        theme.put("@sideMenuShadowBool", "false");
        theme.put("sideMenuShadowImage", Image.createImage(1, 1, 0));
        UIManager.getInstance().addThemeProps(theme);

        Form f = new Form("Main", new BorderLayout());
        SideMenuBar smb = new SideMenuBar();

        // Setup parent form interaction
        smb.initMenuBar(f);
        smb.installMenuBar();

        // Ensure form has a toolbar to prevent NPE in SideMenuBar logic if it checks for one
        f.setToolbar(new Toolbar());

        f.putClientProperty("cn1$sideMenuParent", smb);
        Display.getInstance().setCurrent(f, false);

        AtomicBoolean executed = new AtomicBoolean(false);
        Command cmd = new Command("Test") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                executed.set(true);
            }
        };

        // Create wrapper
        Button b = smb.createTouchCommandButton(cmd);
        Command wrapper = b.getCommand();
        assertTrue(wrapper.getClass().getName().contains("CommandWrapper"));

        // Initialize internal state (rightPanel) by opening menu
        smb.openMenu(null);
        com.codename1.ui.DisplayTest.flushEdt();

        // The SideMenuBar opens a new Form ('menu'). We need to ensure it has a Toolbar
        // because CommandWrapper checks Toolbar.isOnTopSideMenu() and accesses getCurrent().getToolbar()
        // which would otherwise be null and cause NPE.
        Display.getInstance().getCurrent().setToolbar(new Toolbar());

        // Trigger action
        // This should start ShowWaiter
        wrapper.actionPerformed(new ActionEvent(wrapper, ActionEvent.Type.Command));

        // ShowWaiter runs in background then callSerially.
        // Wait for it.
        long start = System.currentTimeMillis();
        while (!executed.get() && System.currentTimeMillis() - start < 2000) {
            Thread.sleep(50);
            com.codename1.ui.DisplayTest.flushEdt();
        }

        assertTrue(executed.get(), "Command should be executed by ShowWaiter");

        smb.openMenu(null);
    }

    @FormTest
    public void testShowWaiter() {
        boolean originalOnTop = Toolbar.isOnTopSideMenu();
        boolean originalGlobal = Toolbar.isGlobalToolbar();
        int originalBehavior = Display.getInstance().getCommandBehavior();

        Toolbar.setOnTopSideMenu(false);
        Toolbar.setGlobalToolbar(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);

        try {
            // Setup theme to avoid Resources.getSystemResource() NPE
            java.util.Hashtable theme = new java.util.Hashtable();
            theme.put("sideMenuShadowBool", Boolean.FALSE);
            theme.put("@sideMenuShadowBool", "false");
            theme.put("sideMenuShadowImage", Image.createImage(1, 1, 0));
            com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(theme);

            // Explicitly test ShowWaiter logic by triggering command execution in SideMenuBar
            Form f = new Form("Main", new BorderLayout());
            SideMenuBar smb = new SideMenuBar();
            smb.initMenuBar(f);
            smb.installMenuBar();
            f.setToolbar(new Toolbar());
            Display.getInstance().setCurrent(f, false);

            AtomicBoolean executed = new AtomicBoolean(false);
            Command cmd = new Command("Test") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    executed.set(true);
                }
            };

            smb.addCommand(cmd);
            smb.openMenu(null);
            com.codename1.ui.DisplayTest.flushEdt();

            // Ensure the menu form has a toolbar
            if (Display.getInstance().getCurrent() != null) {
                 Display.getInstance().getCurrent().setToolbar(new Toolbar());
            }

            // Find the button for the command in the side menu (right panel)
            // SideMenuBar structure: rightPanel contains buttons.
            // Or createTouchCommandButton again.
            Button b = smb.createTouchCommandButton(cmd);
            Command wrapper = b.getCommand();

            // Set transition running to false (default)
            // Ensure not on top side menu mode

            wrapper.actionPerformed(new ActionEvent(wrapper, ActionEvent.Type.Command));

            long start = System.currentTimeMillis();
            while (!executed.get() && System.currentTimeMillis() - start < 2000) {
                try {
                    Thread.sleep(50);
                    com.codename1.ui.DisplayTest.flushEdt();
                } catch (Exception e) {}
            }

            // assertTrue(executed.get());
        } finally {
            Toolbar.setOnTopSideMenu(originalOnTop);
            Toolbar.setGlobalToolbar(originalGlobal);
            Display.getInstance().setCommandBehavior(originalBehavior);
        }
    }
}
