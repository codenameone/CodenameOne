package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.TableLayout;
import static com.codename1.ui.CN.*;

public class NestedTabsTest3023Test extends UITestBase {

    @FormTest
    public void testNestedTabs() {
        Form f = new Form("custom tabs", new BorderLayout());
        Tabs outerTabs = new Tabs();

        Tabs tabs = new Tabs();

        tabs.addTab("inner1", new Label("content1"));
        tabs.addTab("inner2", new Label("content2"));
        tabs.addTab("inner3", new Label("content3"));

        Container tabsCont = tabs.getTabsContainer();

        TableLayout newLayout = new TableLayout(1, 2);
        tabsCont.getComponentAt(0).setHidden(true);
        tabsCont.setLayout(newLayout);

        newLayout.addLayoutComponent(newLayout.cc(0,0).widthPercentage(50), tabsCont.getComponentAt(1), tabsCont);
        newLayout.addLayoutComponent(newLayout.cc(0,1).widthPercentage(50), tabsCont.getComponentAt(2), tabsCont);

        outerTabs.addTab("outer1", new Container());
        outerTabs.addTab("outer2", tabs);


        f.addComponent(BorderLayout.CENTER, outerTabs);
        f.show();
        // Smoke test to ensure no exceptions
    }
}
