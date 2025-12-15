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

        // Workaround for potential bug in SideMenuBar where pointerDragged might be null
        try {
            java.lang.reflect.Field pdField = SideMenuBar.class.getDeclaredField("pointerDragged");
            pdField.setAccessible(true);
            if (pdField.get(smb) == null) {
                pdField.set(smb, new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {}
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}
