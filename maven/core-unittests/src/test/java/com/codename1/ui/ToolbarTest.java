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
        sideButton.pointerPressed(sideButton.getAbsoluteX(), sideButton.getAbsoluteY());
        sideButton.pointerReleased(sideButton.getAbsoluteX(), sideButton.getAbsoluteY());
        flushSerialCalls();

        assertEquals(1, sideInvocation[0], "Side menu command should fire its listener");

        Button overflowButton = toolbar.findCommandComponent(overflow);
        assertNotNull(overflowButton, "Overflow menu should render a button");
        overflowButton.pointerPressed(overflowButton.getAbsoluteX(), overflowButton.getAbsoluteY());
        overflowButton.pointerReleased(overflowButton.getAbsoluteX(), overflowButton.getAbsoluteY());
        flushSerialCalls();

        assertEquals(1, overflowInvocation[0], "Overflow command should be invoked");
    }
}
