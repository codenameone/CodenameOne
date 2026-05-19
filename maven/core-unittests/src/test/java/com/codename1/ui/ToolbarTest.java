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
    void rightSideMenuCommandsDispatch() throws Exception {
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

        // Issue #4979: command dispatch from the side menu is deferred
        // to the dispose-animation onFinish so the dim layered pane is
        // fully detached before the command runs. The test EDT cannot
        // tick wall-clock animations while blocked on the test body,
        // so disable the dispose animation to make the deferred fire
        // synchronous.
        disableSideMenuAnimation(toolbar, "rightSidemenuDialog");

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

    private static void disableSideMenuAnimation(Toolbar toolbar, String dialogFieldName) throws Exception {
        java.lang.reflect.Field dialogField = Toolbar.class.getDeclaredField(dialogFieldName);
        dialogField.setAccessible(true);
        Object dialog = dialogField.get(toolbar);
        if (dialog == null) {
            return;
        }
        java.lang.reflect.Method setAnimateShow = dialog.getClass().getMethod("setAnimateShow", boolean.class);
        setAnimateShow.invoke(dialog, false);
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

    /// Regression test for issue #4912: after closing the hamburger
    /// side menu the underlying Form remained "shaded" in the
    /// simulator and the JS port. The Toolbar.class layered pane
    /// carries a dim backdrop (bgTransparency over bgColor=0) that
    /// is painted over the form while the menu is open. The dispose
    /// onDisposed callback detached the pane and zeroed the tint,
    /// but did not queue a form-level revalidate; the user's
    /// confirmed workaround was to call form.revalidateLater() in
    /// response to the close, and that workaround was promoted into
    /// detachToolbarLayeredPane.
    ///
    /// The dispose animation is disabled via reflection so the
    /// detach runs synchronously inside closeLeftSideMenu, and the
    /// assertions check both that the pane is gone AND that the
    /// form has been added back to the revalidate queue so the
    /// next paint cycle overdraws any stale shaded pixels.
    @FormTest
    void closeLeftSideMenuClearsShadedBackdropAfterAnimation() throws Exception {
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
        int openTransparency = pane.getUnselectedStyle().getBgTransparency() & 0xff;
        assertTrue(openTransparency > 0,
                "Backdrop pane should be tinted while menu is open (was " + openTransparency + ")");

        // Disable the dispose animation so closeLeftSideMenu runs the
        // detach callback synchronously. The bug being tested is not
        // about animation timing -- it is about whether the form is
        // re-queued for layout/repaint once the dim pane is gone.
        java.lang.reflect.Field dialogField = Toolbar.class.getDeclaredField("sidemenuDialog");
        dialogField.setAccessible(true);
        Object dialog = dialogField.get(toolbar);
        java.lang.reflect.Method setAnimateShow = dialog.getClass().getMethod("setAnimateShow", boolean.class);
        setAnimateShow.invoke(dialog, false);

        // Drain any pending revalidate state so we can detect a fresh
        // revalidate request triggered specifically by close.
        flushSerialCalls();
        boolean revalidatePendingBeforeClose = isFormInRevalidateQueue(form);

        toolbar.closeLeftSideMenu();

        assertNull(pane.getParent(),
                "Toolbar layered pane should be detached once the synchronous dispose runs");
        assertEquals(0, pane.getUnselectedStyle().getBgTransparency() & 0xff,
                "Backdrop tint must be cleared so a stale reference cannot re-shade the form (issue #4912)");
        assertFalse(toolbar.isSideMenuShowing(),
                "Side menu should no longer be reported as showing after synchronous close");

        assertTrue(isFormInRevalidateQueue(form) && !revalidatePendingBeforeClose,
                "closeLeftSideMenu should queue a form revalidateLater after detaching the "
                        + "shaded backdrop pane (issue #4912 -- without this the form stays "
                        + "shaded until the user does something that forces a redraw)");
    }

    private static boolean isFormInRevalidateQueue(Form form) throws Exception {
        java.lang.reflect.Field f = Form.class.getDeclaredField("pendingRevalidateQueue");
        f.setAccessible(true);
        Object queue = f.get(form);
        if (queue instanceof java.util.Collection) {
            return ((java.util.Collection<?>) queue).contains(form);
        }
        return false;
    }

    /// Regression test for issue #4979: tapping a command in the
    /// on-top side menu used to run cmd.actionPerformed synchronously
    /// right after kicking off the side menu's async dispose
    /// animation. If the command then showed a modal Dialog (the
    /// archetype's default `hello()` does exactly this) the Dialog's
    /// event pump stole the EDT before the dispose animation could
    /// advance, the detachToolbarLayeredPane onFinish never fired,
    /// and the dim backdrop stayed visible after the Dialog was
    /// dismissed.
    ///
    /// The fix routes the command-fire through the new
    /// closeSideMenu(Runnable) onFinish, so the layered pane is
    /// guaranteed to be detached *before* the command runs. The
    /// assertion below checks that ordering directly: when the
    /// command's listener fires, the Toolbar layered pane must
    /// already be gone.
    @FormTest
    void sideMenuCommandFiresAfterLayeredPaneDetach() throws Exception {
        implementation.setBuiltinSoundsEnabled(false);
        Toolbar.setOnTopSideMenu(true);

        Form form = Display.getInstance().getCurrent();
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();

        // Capture the *specific* dim layered-pane instance via this
        // array so the listener can check that exact reference. We
        // cannot just call form.getFormLayeredPane(Toolbar.class,
        // false) inside the listener because that method is
        // get-or-create — once the original pane is removed it would
        // hand back a brand new attached Container.
        final Container[] capturedPane = new Container[1];
        final boolean[] paneAttachedWhenCommandFired = {true};
        final int[] invocation = {0};
        Command hello = toolbar.addCommandToSideMenu("Hello", null, evt -> {
            Container p = capturedPane[0];
            paneAttachedWhenCommandFired[0] = p != null && p.getParent() != null;
            invocation[0]++;
        });

        toolbar.openSideMenu();
        form.getAnimationManager().flush();
        flushSerialCalls();
        awaitAnimations(form);
        assertTrue(toolbar.isSideMenuShowing(), "Side menu should be showing after open");
        capturedPane[0] = form.getFormLayeredPane(Toolbar.class, false);
        assertNotNull(capturedPane[0], "Toolbar layered pane must exist while menu is open");
        assertNotNull(capturedPane[0].getParent(), "Layered pane must be attached while menu is open");

        // Make the dispose synchronous so we don't depend on the
        // animation thread inside the test; the bug we are pinning
        // down is about ordering between the dispose onFinish and the
        // command-fire, not about the animation duration.
        disableSideMenuAnimation(toolbar, "sidemenuDialog");

        // Dispatch through the same path the side-menu button click
        // would take — the CommandWrapper.actionPerformed branch that
        // does the close-then-fire dance.
        Button helloButton = toolbar.findCommandComponent(hello);
        assertNotNull(helloButton, "Side menu command should have a button while menu is open");
        int px = helloButton.getAbsoluteX() + helloButton.getWidth() / 2;
        int py = helloButton.getAbsoluteY() + helloButton.getHeight() / 2;
        helloButton.pointerPressed(px, py);
        helloButton.pointerReleased(px, py);
        flushSerialCalls();

        assertEquals(1, invocation[0], "Command listener should have fired exactly once");
        assertFalse(paneAttachedWhenCommandFired[0],
                "Issue #4979: command listener must run *after* the Toolbar layered pane "
                        + "is detached so a modal Dialog opened by the command cannot leave "
                        + "the dim backdrop visible behind it");
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
