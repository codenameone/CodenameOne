package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SheetSwipeToDismissTest extends UITestBase {

    @FormTest
    void swipeToDismissEnabledByDefault() {
        Sheet sheet = new Sheet(null, "Test");
        assertTrue(sheet.isSwipeToDismissEnabled(),
                "Swipe-to-dismiss should be enabled by default");
    }

    @FormTest
    void canToggleSwipeToDismiss() {
        Sheet sheet = new Sheet(null, "Test");
        sheet.setSwipeToDismissEnabled(false);
        assertFalse(sheet.isSwipeToDismissEnabled(),
                "Swipe-to-dismiss should be disabled after setSwipeToDismissEnabled(false)");
        sheet.setSwipeToDismissEnabled(true);
        assertTrue(sheet.isSwipeToDismissEnabled(),
                "Swipe-to-dismiss should be re-enabled after setSwipeToDismissEnabled(true)");
    }

    @FormTest
    void swipeDownPastThresholdDismissesSheet() throws Exception {
        Form form = showFormWithSheet("Drag To Dismiss");
        Sheet sheet = Sheet.getCurrentSheet();
        assertNotNull(sheet, "Sheet should be visible before drag");

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        int dragDistance = (int) (sheet.getHeight() * 0.6);

        dragSheet(x, startY, 0, dragDistance, 5);
        implementation.dispatchPointerRelease(x, startY + dragDistance);
        flushSerialCalls();

        awaitAnimations(form);

        assertNull(Sheet.getCurrentSheet(),
                "Sheet should be dismissed when dragged past the dismiss threshold");
    }

    @FormTest
    void fastFlickDismissesSheetEvenBelowDistanceThreshold() throws Exception {
        Form form = showFormWithSheet("Flick Dismiss");
        Sheet sheet = Sheet.getCurrentSheet();
        assertNotNull(sheet);

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        // Sub-threshold distance, but a fast last-frame movement so velocity
        // (~ pixels per second) sails past the flick threshold.
        int finalY = startY + Math.max(2, sheet.getHeight() / 8);

        implementation.dispatchPointerPress(x, startY);
        implementation.setHasDragStarted(true);
        flushSerialCalls();
        // Single very fast drag to maximise velocity at release time. No sleep,
        // so dragVelocity comes out enormous and clears the flick threshold
        // even though the absolute distance is below 1/3 of the sheet height.
        implementation.dispatchPointerDrag(x, finalY);
        implementation.dispatchPointerRelease(x, finalY);
        flushSerialCalls();

        awaitAnimations(form);

        assertNull(Sheet.getCurrentSheet(),
                "A fast flick should dismiss the sheet via the velocity threshold");
    }

    @FormTest
    void smallSwipeSnapsBackInsteadOfDismissing() throws Exception {
        Form form = showFormWithSheet("Snap Back");
        Sheet sheet = Sheet.getCurrentSheet();
        int restingY = sheet.getY();

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        // Drag well below the 1/3 dismiss threshold.
        int dragDistance = Math.max(1, sheet.getHeight() / 10);

        dragSheet(x, startY, 0, dragDistance, 3);
        implementation.dispatchPointerRelease(x, startY + dragDistance);
        flushSerialCalls();

        awaitAnimations(form);

        assertSame(sheet, Sheet.getCurrentSheet(),
                "Sheet should still be visible after a sub-threshold drag");
        assertEquals(restingY, sheet.getY(),
                "Sheet should snap back to its original Y after a sub-threshold drag");
    }

    @FormTest
    void dragInWrongDirectionDoesNotDismissSouthSheet() throws Exception {
        Form form = showFormWithSheet("Wrong Direction");
        Sheet sheet = Sheet.getCurrentSheet();
        int restingY = sheet.getY();

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        // Upward drag on a SOUTH sheet should not dismiss it.
        int dragDistance = sheet.getHeight();

        dragSheet(x, startY, 0, -dragDistance, 5);
        implementation.dispatchPointerRelease(x, startY - dragDistance);
        flushSerialCalls();

        awaitAnimations(form);

        assertSame(sheet, Sheet.getCurrentSheet(),
                "Upward drag on a SOUTH sheet must not dismiss it");
        assertEquals(restingY, sheet.getY(),
                "Sheet Y should not move when dragging in the wrong direction");
    }

    @FormTest
    void disabledSwipeToDismissIgnoresDrag() throws Exception {
        Form form = showFormWithSheet("Disabled Swipe");
        Sheet sheet = Sheet.getCurrentSheet();
        sheet.setSwipeToDismissEnabled(false);
        int restingY = sheet.getY();

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        int dragDistance = (int) (sheet.getHeight() * 0.8);

        dragSheet(x, startY, 0, dragDistance, 5);
        implementation.dispatchPointerRelease(x, startY + dragDistance);
        flushSerialCalls();

        awaitAnimations(form);

        assertSame(sheet, Sheet.getCurrentSheet(),
                "Sheet must not be dismissed when swipeToDismiss is disabled");
        assertEquals(restingY, sheet.getY(),
                "Sheet Y must not move when swipeToDismiss is disabled");
    }

    @FormTest
    void allowCloseFalseIgnoresSwipe() throws Exception {
        Form form = showFormWithSheet("Locked");
        Sheet sheet = Sheet.getCurrentSheet();
        sheet.setAllowClose(false);
        int restingY = sheet.getY();

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        int dragDistance = (int) (sheet.getHeight() * 0.8);

        dragSheet(x, startY, 0, dragDistance, 5);
        implementation.dispatchPointerRelease(x, startY + dragDistance);
        flushSerialCalls();

        awaitAnimations(form);

        assertSame(sheet, Sheet.getCurrentSheet(),
                "Sheet must not be dismissed via swipe when allowClose is false");
        assertEquals(restingY, sheet.getY(),
                "Sheet Y must not move when allowClose is false");
    }

    @FormTest
    void dragOnContentAreaDoesNotInitiateDismiss() throws Exception {
        Form form = showFormWithSheet("Content Drag");
        Sheet sheet = Sheet.getCurrentSheet();
        int restingY = sheet.getY();

        Container content = sheet.getContentPane();
        int x = content.getAbsoluteX() + content.getWidth() / 2;
        int startY = content.getAbsoluteY() + content.getHeight() / 2;
        int dragDistance = sheet.getHeight();

        dragSheet(x, startY, 0, dragDistance, 5);
        implementation.dispatchPointerRelease(x, startY + dragDistance);
        flushSerialCalls();

        awaitAnimations(form);

        assertSame(sheet, Sheet.getCurrentSheet(),
                "Drag starting in the content pane must not dismiss the sheet");
        assertEquals(restingY, sheet.getY(),
                "Sheet Y must not move when drag starts outside the title bar");
    }

    private Form showFormWithSheet(String title) {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, title);
        sheet.getContentPane().add(new Label("Content"));
        sheet.show(0);
        form.getAnimationManager().flush();
        flushSerialCalls();
        return form;
    }

    private void dragSheet(int startX, int startY, int dx, int dy, int steps) {
        implementation.dispatchPointerPress(startX, startY);
        implementation.setHasDragStarted(true);
        flushSerialCalls();
        for (int i = 1; i <= steps; i++) {
            sleepQuietly(20);
            int px = startX + (dx * i / steps);
            int py = startY + (dy * i / steps);
            implementation.dispatchPointerDrag(px, py);
        }
        // Send a final drag at the same final coordinate after a longer pause
        // so the recorded velocity decays to zero — keeps the snap-back / no-op
        // tests deterministic regardless of how fast the previous drags ran.
        sleepQuietly(60);
        implementation.dispatchPointerDrag(startX + dx, startY + dy);
        flushSerialCalls();
    }

    private void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitAnimations(Form form) {
        AnimationManager am = form.getAnimationManager();
        // Drive any in-flight animation through to completion. flush()
        // discards rather than runs, so manually step updateAnimations()
        // (which advances motions and triggers completion callbacks) until
        // the manager reports nothing in progress or we hit the cap.
        long deadline = System.currentTimeMillis() + 3000;
        while (am.isAnimating() && System.currentTimeMillis() < deadline) {
            am.updateAnimations();
            flushSerialCalls();
            sleepQuietly(10);
        }
        // Drain any pending postAnimations runnables that flushAnimation queued.
        am.updateAnimations();
        flushSerialCalls();
        CountDownLatch latch = new CountDownLatch(1);
        am.flushAnimation(latch::countDown);
        try {
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for animations");
        }
        flushSerialCalls();
    }

    private static Container getTitleBar(Sheet sheet) throws Exception {
        Field f = Sheet.class.getDeclaredField("titleBar");
        f.setAccessible(true);
        return (Container) f.get(sheet);
    }
}
