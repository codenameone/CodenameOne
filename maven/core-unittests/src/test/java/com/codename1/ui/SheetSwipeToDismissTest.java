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
        // Sleep ensures elapsed > 0 for the velocity calc; the drag still
        // produces a velocity well above the (low) test-implementation flick
        // threshold (convertToPixels returns dipCount-as-pixels in tests),
        // so dismiss should fire via the velocity path despite the sub-
        // threshold absolute distance.
        sleepQuietly(20);
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

    /// Reproduction for issue #4899 - after a swipe-to-dismiss the underlying
    /// Form remains painted with the Sheet's dim overlay until the next user
    /// input wakes the EDT. The dismiss completes (`cnt.remove()` succeeds
    /// and the `Sheet` is gone), but the only repaint scheduled against the
    /// form is `revalidateLater()`, which only does its work if a *future*
    /// paint cycle calls `flushRevalidateQueue`. Nothing in the dismiss path
    /// puts the form into the impl's paint queue, so the EDT goes idle with
    /// stale dim pixels still on screen.
    ///
    /// We assert the loop precondition that, immediately after the dismiss,
    /// either a paint is pending against the form or another animation is
    /// going to run a paint cycle for us. Without the fix neither holds.
    @FormTest
    void swipeDismissTriggersFormRepaint() throws Exception {
        Form form = showFormWithSheet("Repaint After Swipe");
        Sheet sheet = Sheet.getCurrentSheet();
        assertNotNull(sheet);

        Container titleBar = getTitleBar(sheet);
        int x = titleBar.getAbsoluteX() + titleBar.getWidth() / 2;
        int startY = titleBar.getAbsoluteY() + titleBar.getHeight() / 2;
        int dragDistance = (int) (sheet.getHeight() * 0.6);

        dragSheet(x, startY, 0, dragDistance, 5);
        implementation.dispatchPointerRelease(x, startY + dragDistance);
        flushSerialCalls();

        // Drive any in-flight animation through to completion *and* clear the
        // paint queue between iterations, so by the time we inspect state the
        // queue reflects only what the dismiss path itself scheduled, not
        // leftovers from earlier animation frames.
        awaitAnimationsFlushingPaintQueue(form);

        assertNull(Sheet.getCurrentSheet(),
                "Sanity: sheet must be dismissed before checking repaint state");

        assertPaintScheduledOrAnimating(form,
                "After a swipe-to-dismiss the form must be queued for repaint "
                        + "(or another animation must keep the EDT awake) so "
                        + "the dim overlay is cleared. Otherwise the EDT "
                        + "idles with stale dim pixels until the user taps.");
    }

    /// Companion test: the same assertion holds when the user closes the
    /// sheet via the back/close button. This path goes through `Sheet#hide`
    /// and `Container#animateUnlayout`, which we expect to leave the form
    /// in a paintable state.
    @FormTest
    void backButtonDismissTriggersFormRepaint() throws Exception {
        Form form = showFormWithSheetDriven("Repaint After Back Button");
        Sheet sheet = Sheet.getCurrentSheet();
        assertNotNull(sheet);

        // Calling back() directly is what the back-button ActionListener
        // does. With no parent sheet this routes to hide(duration), which
        // is the path the user reports as "working fine" -- the baseline
        // we want to compare swipe-dismiss against.
        sheet.back(300);
        flushSerialCalls();
        assertTrue(form.getAnimationManager().isAnimating(),
                "Sanity: hide() must add an animation to the manager");

        awaitAnimationsFlushingPaintQueue(form);

        assertNull(Sheet.getCurrentSheet(),
                "Sanity: sheet must be dismissed before checking repaint state");
        assertPaintScheduledOrAnimating(form,
                "After a back-button dismiss the form must be queued for "
                        + "repaint so the dim overlay is cleared");
    }

    /// Variant of `showFormWithSheet` that drives the show animation through
    /// `updateAnimations` instead of `flush()`. flush() clears the anims
    /// queue but leaves the AnimationManager's `uiMutations` cache holding a
    /// reference to the (now drained) UIMutation, which then silently
    /// absorbs subsequent animations like `hide()` -- the new animation
    /// never reaches the anims queue and never runs. Routing the show
    /// through `updateAnimations` clears `uiMutations` properly so later
    /// `hide()` / `animateDismissFromDrag` calls land on a fresh queue.
    private Form showFormWithSheetDriven(String title) throws Exception {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, title);
        sheet.getContentPane().add(new Label("Content"));
        sheet.show(0);
        AnimationManager am = form.getAnimationManager();
        long deadline = System.currentTimeMillis() + 1000;
        while (am.isAnimating() && System.currentTimeMillis() < deadline) {
            am.updateAnimations();
            flushSerialCalls();
            sleepQuietly(5);
        }
        am.updateAnimations();
        flushSerialCalls();
        return form;
    }

    /// Drives the animation queue to completion the same way `awaitAnimations`
    /// does, but additionally clears the impl paint queue right before the
    /// final completion frame so that the only paints we observe afterwards
    /// are the ones scheduled by the dismissal's completion runnable. The
    /// MorphAnimation path runs `cnt.repaint()` on every tick - those are
    /// cancelled by `cnt.remove()` but not until inside the same
    /// updateAnimations call - and we want the test to be insensitive to
    /// any frame ordering that survives the cancellation.
    private void awaitAnimationsFlushingPaintQueue(Form form) throws Exception {
        AnimationManager am = form.getAnimationManager();
        long deadline = System.currentTimeMillis() + 3000;
        // Drain mid-animation paints by clearing on each tick *except* the
        // last one. We detect "last one" by sampling isAnimating just before
        // the next tick: when it flips false, the next updateAnimations
        // call is the one that runs the completion runnable.
        while (am.isAnimating() && System.currentTimeMillis() < deadline) {
            clearPaintQueue();
            am.updateAnimations();
            flushSerialCalls();
            sleepQuietly(10);
        }
        // One final tick (covers the case where a MorphAnimation finished
        // and the completion runnable has already executed inside the loop,
        // that runnable's repaint() lives in the paint queue and we don't
        // touch it from here on).
        am.updateAnimations();
        flushSerialCalls();
    }

    private void clearPaintQueue() throws Exception {
        Class<?> implClass = Class.forName("com.codename1.impl.CodenameOneImplementation");
        Field fillField = implClass.getDeclaredField("paintQueueFill");
        Field queueField = implClass.getDeclaredField("paintQueue");
        fillField.setAccessible(true);
        queueField.setAccessible(true);
        synchronized (implementation) {
            Object queue = queueField.get(implementation);
            int len = java.lang.reflect.Array.getLength(queue);
            for (int i = 0; i < len; i++) {
                java.lang.reflect.Array.set(queue, i, null);
            }
            fillField.setInt(implementation, 0);
        }
    }

    /// Verifies that the EDT will perform a paint that covers the form
    /// content. A repaint is "scheduled" if the impl paint queue holds the
    /// form, the content pane, or an ancestor of the content. Equivalently,
    /// if another animation is still running, a paint cycle will follow
    /// regardless and the assertion is satisfied.
    private void assertPaintScheduledOrAnimating(Form form, String message) throws Exception {
        AnimationManager am = form.getAnimationManager();
        if (am.isAnimating()) {
            return;
        }
        Class<?> implClass = Class.forName("com.codename1.impl.CodenameOneImplementation");
        Field fillField = implClass.getDeclaredField("paintQueueFill");
        Field queueField = implClass.getDeclaredField("paintQueue");
        fillField.setAccessible(true);
        queueField.setAccessible(true);
        int fill = fillField.getInt(implementation);
        Object queue = queueField.get(implementation);

        Component content = form.getContentPane();
        boolean covered = false;
        StringBuilder seen = new StringBuilder("[");
        for (int i = 0; i < fill; i++) {
            Object entry = java.lang.reflect.Array.get(queue, i);
            if (entry == null) {
                continue;
            }
            if (seen.length() > 1) {
                seen.append(", ");
            }
            seen.append(entry.getClass().getSimpleName());
            if (entry == form || entry == content) {
                covered = true;
            } else if (entry instanceof Container && ((Container) entry).contains(content)) {
                covered = true;
            }
        }
        seen.append("]");
        assertTrue(covered, message + ". paintQueue=" + seen);
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
