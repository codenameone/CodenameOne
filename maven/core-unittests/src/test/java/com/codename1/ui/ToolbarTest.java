package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.FontImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ToolbarTest extends UITestBase {
    private boolean originalOnTop;
    private boolean originalCentered;
    private int originalCommandBehavior;

    @BeforeEach
    void captureStatics() {
        originalOnTop = Toolbar.isOnTopSideMenu();
        originalCentered = Toolbar.isCenteredDefault();
        originalCommandBehavior = Display.getInstance().getCommandBehavior();
    }

    @AfterEach
    void restoreStatics() {
        Toolbar.setOnTopSideMenu(originalOnTop);
        Toolbar.setCenteredDefault(originalCentered);
        Display.getInstance().setCommandBehavior(originalCommandBehavior);
    }

    @FormTest
    void sideMenuCommandRegistration() {
        TestLogger.install();
        try {
            implementation.setBuiltinSoundsEnabled(false);
            Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
            Toolbar.setOnTopSideMenu(true);

            Form form = Display.getInstance().getCurrent();
            Toolbar toolbar = new Toolbar();
            form.setToolbar(toolbar);
            form.show();
            form.getAnimationManager().flush();
            flushSerialCalls();

            final int[] invocation = {0};
            Command command = toolbar.addCommandToSideMenu("Execute", null, evt -> invocation[0]++);

            form.revalidate();
            form.getAnimationManager().flush();
            flushSerialCalls();

            command.actionPerformed(new ActionEvent(command));
            assertEquals(1, invocation[0], "Command action should invoke registered listener");

            toolbar.openSideMenu();
            form.getAnimationManager().flush();
            flushSerialCalls();
            awaitAnimations(form);
            assertTrue(toolbar.isSideMenuShowing(), "Side menu should be showing after openSideMenu");

            toolbar.closeSideMenu();
            form.getAnimationManager().flush();
            flushSerialCalls();
            awaitAnimations(form);

            toolbar.removeCommand(command);
            assertEquals(1, TestLogger.getPrinted().size());
            assertTrue(TestLogger.getPrinted().get(0).contains("WARNING: Display.setCommandBehavior() is deprecated"));
        } finally {
            TestLogger.remove();
        }
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
            fail("Interrupted while waiting for animations to finish");
        }
        flushSerialCalls();
    }

    @FormTest
    void titleCenteringFollowsStaticSetting() {
        implementation.setBuiltinSoundsEnabled(false);

        Toolbar.setCenteredDefault(false);

        Form form = new Form("Title", new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();

        assertFalse(toolbar.isTitleCentered(), "Title should not be centered when centeredDefault is false");

        toolbar.setTitle("Hello");
        toolbar.setTitleCentered(true);

        assertTrue(toolbar.isTitleCentered(), "setTitleCentered(true) should center the title");
    }

    @FormTest
    void rightBarCommandsAndTitleComponentCustomization() {
        implementation.setBuiltinSoundsEnabled(false);

        Form form = Display.getInstance().getCurrent();
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        Command right = toolbar.addMaterialCommandToRightBar("Settings", FontImage.MATERIAL_SETTINGS, evt -> {
        });
        Button rightButton = toolbar.findCommandComponent(right);
        assertNotNull(rightButton, "Right bar command should create a visible button");

        Label customTitle = new Label("Custom Title");
        toolbar.setTitleComponent(customTitle);
        assertSame(customTitle, toolbar.getTitleComponent());

        toolbar.removeCommand(right);
        assertNull(toolbar.findCommandComponent(right), "Removed command should no longer have a button");
    }

    @FormTest
    void rightSideMenuCommandsDispatch() {
        implementation.setBuiltinSoundsEnabled(false);
        Toolbar.setOnTopSideMenu(true);

        Form form = new Form(new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        final int[] invocation = {0};
        Command rightSide = toolbar.addCommandToRightSideMenu("RightMenu", null, evt -> invocation[0]++);

        toolbar.openRightSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);

        Button rightButton = toolbar.findCommandComponent(rightSide);
        assertNotNull(rightButton, "Right side menu button should be created");

        int px = rightButton.getAbsoluteX() + rightButton.getWidth() / 2;
        int py = rightButton.getAbsoluteY() + rightButton.getHeight() / 2;
        rightButton.pointerPressed(px, py);
        rightButton.pointerReleased(px, py);
        flushSerialCalls();

        assertEquals(1, invocation[0], "Pointer events should fire right side menu command");

        toolbar.closeRightSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
    }

    @FormTest
    void sideMenuAndOverflowCommands() {
        implementation.setBuiltinSoundsEnabled(false);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
        Toolbar.setOnTopSideMenu(true);

        Form form = new Form(new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        final int[] sideInvocation = {0};
        final int[] overflowInvocation = {0};
        Command side = toolbar.addCommandToSideMenu("Side", null, evt -> sideInvocation[0]++);
        Command overflow = toolbar.addCommandToOverflowMenu("Overflow", null, evt -> overflowInvocation[0]++);

        toolbar.openSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);

        Button sideButton = toolbar.findCommandComponent(side);
        assertNotNull(sideButton, "Side menu should create a button for the command");
        side.actionPerformed(new ActionEvent(side));
        flushSerialCalls();

        assertEquals(1, sideInvocation[0], "Side menu command should fire its listener");

        overflow.actionPerformed(new ActionEvent(overflow));
        flushSerialCalls();

        assertEquals(1, overflowInvocation[0], "Overflow command should be invoked");
    }

    /// Regression test for the JavaScript port "ghost side menu +
    /// previous preview visible as background" bug. closeLeftSideMenu
    /// used to synchronously detach the Toolbar's FormLayeredPane
    /// while the sidemenu dialog was still mid-animation, leaving the
    /// dialog's peer tree orphaned on the JS port. The fix defers the
    /// layered-pane detach until the dispose animation completes. The
    /// assertion here is the behaviour shift: immediately after
    /// closeLeftSideMenu returns (synchronously, before the dispose
    /// animation can have run to completion), the pane must still be
    /// attached. The old code detached synchronously.
    @FormTest
    void closeLeftSideMenuKeepsLayeredPaneAttachedDuringDispose() {
        implementation.setBuiltinSoundsEnabled(false);
        Toolbar.setOnTopSideMenu(true);

        Form form = Display.getInstance().getCurrent();
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        toolbar.addCommandToSideMenu("Entry", null, evt -> { });

        toolbar.openSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
        assertTrue(toolbar.isSideMenuShowing(), "Side menu should be showing after open");

        Container pane = form.getFormLayeredPane(Toolbar.class, false);
        assertNotNull(pane, "Toolbar layered pane must exist while menu is open");
        assertNotNull(pane.getParent(), "Layered pane must be attached to the form tree while menu is open");

        toolbar.closeLeftSideMenu();

        // Pane must still be attached — the detach is deferred to the
        // dispose onFinish so the dialog's peer teardown runs against
        // a still-attached parent. The old pre-fix code would have
        // nulled pane.getParent() before returning.
        assertNotNull(pane.getParent(),
                "Layered pane should stay attached while the dispose animation runs "
                        + "(deferred detach regression — see Toolbar.detachToolbarLayeredPane)");
    }
}
