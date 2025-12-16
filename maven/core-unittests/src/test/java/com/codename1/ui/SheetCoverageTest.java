package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Assertions;

public class SheetCoverageTest extends UITestBase {

    @FormTest
    public void testSheetStaticMethods() {
        // Setup a form with a Sheet
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Sheet sheet = new Sheet(null, "Test Sheet");
        sheet.show(0);

        // This should trigger getCurrentSheet() logic
        Sheet current = Sheet.getCurrentSheet();
        Assertions.assertEquals(sheet, current);

        // This should trigger findContainingSheet() logic
        Label content = new Label("Content");
        sheet.getContentPane().add(content);

        Sheet found = Sheet.findContainingSheet(content);
        Assertions.assertEquals(sheet, found);

        // Test null cases
        Assertions.assertNull(Sheet.findContainingSheet(form));
    }
}
