package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TabsAnimationSample2957Test extends UITestBase {

    @FormTest
    public void testTabsAnimation() {
        Form hi = new Form("Tabs", new BorderLayout());
        Tabs t = new Tabs();
        SpanLabel t1 = new SpanLabel("Blue");
        t1.getAllStyles().setBgColor(0xff);
        t1.getAllStyles().setBgTransparency(255);
        Container cont1 = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        cont1.add(t1);
        SpanLabel t2 = new SpanLabel("Green");
        t2.getAllStyles().setBgColor(0xff00);
        t2.getAllStyles().setBgTransparency(255);
        Container cont2 = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        cont2.add(t2);
        SpanLabel t3 = new SpanLabel("Red");
        t3.getAllStyles().setBgColor(0xff0000);
        t3.getAllStyles().setBgTransparency(255);
        Container cont3 = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        cont3.add(t3);
        t.addTab("Blue", cont1);
        t.addTab("Green", cont2);
        t.addTab("Red", cont3);

        hi.add(BorderLayout.CENTER, t);
        hi.show();

        // Simulate timer logic
        int idx = t.getSelectedIndex() + 1;
        if (idx >= t.getTabCount()) {
            idx = 0;
        }
        t.setSelectedIndex(idx);

        assertEquals(1, t.getSelectedIndex());

        idx = t.getSelectedIndex() + 1;
        t.setSelectedIndex(idx);
        assertEquals(2, t.getSelectedIndex());

        idx = t.getSelectedIndex() + 1;
        if (idx >= t.getTabCount()) idx = 0;
        t.setSelectedIndex(idx);
        assertEquals(0, t.getSelectedIndex());
    }
}
