package com.codename1.ui;

import com.codename1.junit.FormTest;
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
}
