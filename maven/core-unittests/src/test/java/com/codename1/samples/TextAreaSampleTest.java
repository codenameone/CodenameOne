package com.codename1.samples;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TextAreaSampleTest extends UITestBase {

    @FormTest
    public void testTextArea() {
        Form hi = new Form("Hi World", new BorderLayout());
        TextArea ta = new TextArea();

        hi.add(BorderLayout.CENTER, ta);
        hi.show();

        Display.getInstance().callSerially(()->ta.setText("Some Text"));

        // Process serial calls
        com.codename1.ui.DisplayTest.flushEdt();

        assertEquals("Some Text", ta.getText());
    }
}
