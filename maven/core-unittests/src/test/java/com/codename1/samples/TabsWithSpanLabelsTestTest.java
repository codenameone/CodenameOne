package com.codename1.samples;

import com.codename1.components.SpanButton;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TabsWithSpanLabelsTestTest extends UITestBase {

    @FormTest
    public void testTabsWithSpanLabels() {
        Form hi = new Form("Custom Tabs", new BorderLayout());
        Tabs tb = new Tabs(Component.TOP) {
            @Override
            protected Component createTab(String title, Image icon) {
                SpanButton custom = new SpanButton(title);
                custom.setName("SpanButton");
                custom.setIcon(icon);
                custom.setUIID("Container");
                custom.setTextUIID("mpiTabMetalUnSelected");
                custom.setIconPosition(BorderLayout.NORTH);
                return custom;
            }

            @Override
            public String getTabTitle(Component tab) {
                return ((SpanButton) tab).getText();
            }

            @Override
            protected void bindTabActionListener(Component tab, ActionListener l) {
                ((SpanButton) tab).addActionListener(l);
            }

            @Override
             protected void selectTab(Component tab) {
                 // Simplified logic for test
                 ((SpanButton)tab).setTextUIID("mpiTabMetalSelected");
             }
        };

        tb.setTabUIID(null);
        Button btn = new Button("Any Button Action");
        tb.addTab("Tab 1", btn);
        tb.addTab("Really long\ntext in tab", new Label("T2"));

        hi.add(BorderLayout.CENTER, tb);
        hi.show();

        assertEquals(2, tb.getTabCount());

        tb.setSelectedIndex(1);
        assertEquals(1, tb.getSelectedIndex());
    }
}
