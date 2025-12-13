package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SheetTest extends UITestBase {

    @FormTest
    void showRegistersCurrentSheet() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, "Primary");
        sheet.getContentPane().add(new Label("Content"));
        sheet.show(0);
        form.getAnimationManager().flush();
        flushSerialCalls();

        assertSame(sheet, Sheet.getCurrentSheet(), "Sheet should be current after showing");
    }

    @FormTest
    void parentChildNavigationUpdatesCurrentSheet() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet parent = new Sheet(null, "Parent");
        Label parentLabel = new Label("Parent content");
        parent.getContentPane().add(parentLabel);
        parent.show(0);
        form.getAnimationManager().flush();
        flushSerialCalls();

        Sheet child = new Sheet(parent, "Child");
        Label childLabel = new Label("Child content");
        child.getContentPane().add(childLabel);
        child.show(0);
        form.getAnimationManager().flush();
        flushSerialCalls();

        child.back(0);
        awaitAnimations(form);

        assertSame(parent, Sheet.getCurrentSheet(), "Parent sheet should be restored after child back()");
        assertSame(parent, Sheet.findContainingSheet(parentLabel), "findContainingSheet should locate parent sheet");
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
    void allowCloseFlagCanBeToggled() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, "Configurable");
        sheet.getContentPane().add(new Label("Content"));
        sheet.setAllowClose(false);
        assertFalse(sheet.isAllowClose(), "Sheet should report allowClose=false when disabled");

        sheet.setAllowClose(true);
        assertTrue(sheet.isAllowClose(), "Sheet should report allowClose=true when re-enabled");

        form.getAnimationManager().flush();
        assertNull(Sheet.getCurrentSheet(), "No sheet should remain after backing out");
    }
}
