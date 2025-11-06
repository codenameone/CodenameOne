package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class SheetTest extends UITestBase {

    @FormTest
    void showAndHideByOutsideTap() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, "Primary");
        sheet.getContentPane().add(new Label("Content"));
        sheet.show(0);
        form.getAnimationManager().flush();
        form.revalidate();

        assertSame(sheet, Sheet.getCurrentSheet(), "Sheet should be current after showing");

        int outsideX = sheet.getAbsoluteX() + sheet.getWidth() / 2;
        int outsideY = Math.max(0, sheet.getAbsoluteY() - 20);
        form.pointerPressed(outsideX, outsideY);
        form.pointerReleased(outsideX, outsideY);
        form.getAnimationManager().flush();

        assertNull(Sheet.getCurrentSheet(), "Sheet should hide when tapping outside");
    }

    @FormTest
    void disallowClosePreventsOutsideDismiss() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, "Locked");
        sheet.setAllowClose(false);
        sheet.getContentPane().add(new Label("Content"));
        sheet.show(0);
        form.getAnimationManager().flush();
        form.revalidate();

        int outsideX = sheet.getAbsoluteX() + sheet.getWidth() / 2;
        int outsideY = Math.max(0, sheet.getAbsoluteY() - 20);
        form.pointerPressed(outsideX, outsideY);
        form.pointerReleased(outsideX, outsideY);
        form.getAnimationManager().flush();

        assertSame(sheet, Sheet.getCurrentSheet(), "Sheet should remain visible when closing disabled");

        sheet.back();
        form.getAnimationManager().flush();
    }

    @FormTest
    void navigatingBetweenSheetsTracksParent() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet parent = new Sheet(null, "Parent");
        Label parentLabel = new Label("Parent content");
        parent.getContentPane().add(parentLabel);
        parent.show(0);
        form.getAnimationManager().flush();

        Sheet child = new Sheet(parent, "Child");
        Label childContent = new Label("Child content");
        child.getContentPane().add(childContent);
        child.show(0);
        form.getAnimationManager().flush();

        assertSame(child, Sheet.getCurrentSheet(), "Child sheet should be current");
        assertSame(child, Sheet.findContainingSheet(childContent), "findContainingSheet should locate child");

        child.back();
        form.getAnimationManager().flush();

        assertSame(parent, Sheet.getCurrentSheet(), "Parent sheet should be restored after child back");
        assertSame(parent, Sheet.findContainingSheet(parentLabel), "findContainingSheet should locate parent");

        parent.back();
        form.getAnimationManager().flush();
        assertNull(Sheet.getCurrentSheet(), "No sheet should remain after backing out");
    }
}
