package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

public class GradientSampleTest extends UITestBase {

    @FormTest
    public void testGradient() {
        Form hi = new Form("Hi World", BoxLayout.y());
        hi.getContentPane().setUIID("Gradient");
        hi.add(new Label("Hi World"));
        hi.show();

        assertEquals("Hi World", hi.getTitle());
        assertEquals("Gradient", hi.getContentPane().getUIID());
        assertEquals(1, hi.getContentPane().getComponentCount());
        assertTrue(hi.getContentPane().getComponentAt(0) instanceof Label);
        assertEquals("Hi World", ((Label)hi.getContentPane().getComponentAt(0)).getText());
    }
}
