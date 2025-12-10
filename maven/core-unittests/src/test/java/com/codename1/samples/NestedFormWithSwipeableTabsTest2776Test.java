package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import static com.codename1.ui.CN.*;

public class NestedFormWithSwipeableTabsTest2776Test extends UITestBase {

    @FormTest
    public void testNestedFormWithSwipeableTabs() {
        Toolbar.setGlobalToolbar(true);
        Form outer = new Form("", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_SCALE));
        outer.getToolbar().hideToolbar();
        Form inner = new Form("Tabs", new BorderLayout());
        Container layeredPane1 = inner.getLayeredPane(NestedFormWithSwipeableTabsTest2776Test.class, 1);
        layeredPane1.setLayout(new BorderLayout());
        Container layeredPane2 = inner.getLayeredPane(AnotherClass.class, 2);
        layeredPane2.setLayout(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));

        Tabs tab = new Tabs();
        tab.addTab("Tab1", new SpanLabel("Tab 1"));
        tab.addTab("Tab2", new SpanLabel("Tab 2"));

        inner.add(BorderLayout.CENTER, tab);
        outer.add(BorderLayout.CENTER, inner);
        outer.revalidate();
        outer.show();
        // Smoke test to ensure no exceptions
    }

    private static class AnotherClass {

    }
}
