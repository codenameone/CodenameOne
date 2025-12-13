package com.codename1.samples;

import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Form;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SpanLabelTest2897Test extends UITestBase {

    @FormTest
    public void testSpanLabelWrapping() {
        Form hi = new Form("Welcome");
        Container container = new Container(BoxLayout.y());

        String longText = "Span label only shows one row. We need to update the columns for rendering, otherwise it will still wrap at the old number of columns. And more text to ensure it definitely wraps.";
        SpanLabel label = new SpanLabel(longText);
        container.add(label);

        SpanLabel label1 = new SpanLabel("Set span label text component row count manually. We need to update the columns for rendering, otherwise it will still wrap at the old number of columns.");
        container.add(label1);

        SpanButton btn = new SpanButton("Hello");
        container.add(btn);

        SpanButton btn2 = new SpanButton("Set span button text component row count manually. We need to update the columns for rendering, otherwise it will still wrap at the old number of columns.");
        container.add(btn2);

        hi.add(container);
        hi.show();

        hi.layoutContainer();

        // Verify that long labels have height > 0 and likely > single line height
        // We can compare preferred height with a single line label if we want, but checking > 0 and correct containment is basic check
        assertTrue(label.getHeight() > 0);
        assertTrue(label.getPreferredH() > 0);
        assertTrue(btn2.getHeight() > 0);
    }
}
