package com.codename1.samples;

import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SpanLabelIconAlignmentTestTest extends UITestBase {

    private static final String[] text = new String[]{
        "Connect with your clients",
        "View their activities and Saved searches",
        "One Dashboard for important updates on properties",
        "Receive feedback from clients on their favourite properties"
    };

    @FormTest
    public void testSpanLabelIconAlignment() {
        Form hi = new Form("Hi World", BoxLayout.y());
        // Using createMaterial with a dummy style if needed, but since we don't have theme loaded, we use createMaterial with defaults or plain image
        // FontImage needs a Font to be set or available. In unit tests fonts might be tricky.
        // We will mock the icon with a simple image to avoid font issues if FontImage fails without proper fonts.
        Image icon = Image.createImage(30, 30, 0);

        for (String str : text) {
            SpanLabel l = new SpanLabel(str);
            // Skipping complex styling that depends on loaded theme/fonts for simplicity in unit test
            l.setIcon(icon);
            hi.add(l);
        }
        for (String str : text) {
            SpanButton l = new SpanButton(str);
            l.setIcon(icon);
            hi.add(l);
        }

        Image iconSmall = Image.createImage(4, 4, 0);
        for (String str : text) {
            SpanLabel l = new SpanLabel(str);
            l.setIcon(iconSmall);
            hi.add(l);
        }
        for (String str : text) {
            SpanLabel l = new SpanLabel();
            l.setText(str);
            l.setIcon(iconSmall);
            hi.add(l);
        }
        hi.add(new Label("Hi World"));
        hi.show();

        // Verify components are added
        // 4 text items * 4 loops + 1 label = 17 components
        assertEquals(17, hi.getContentPane().getComponentCount());
    }
}
