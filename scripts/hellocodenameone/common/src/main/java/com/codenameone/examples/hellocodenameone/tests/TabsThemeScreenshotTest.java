package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

public class TabsThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "TabsTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected boolean useTexturedBackdrop() {
        // iOS liquid-glass Tabs group approximates frosted-glass with a
        // semi-opaque fill; paint against a colourful backdrop so the
        // translucency (once real backdrop-filter lands) is visible.
        return true;
    }

    @Override
    protected void populate(Form form, String suffix) {
        Tabs tabs = new Tabs();
        Container first = new Container(new BorderLayout());
        first.add(BorderLayout.CENTER, new Label("First tab content"));
        Container second = new Container(new BorderLayout());
        second.add(BorderLayout.CENTER, new Button("Second tab button"));
        Container third = new Container(new BorderLayout());
        third.add(BorderLayout.CENTER, new Label("Third tab content"));

        tabs.addTab("Home", FontImage.MATERIAL_HOME, 8, first);
        tabs.addTab("Search", FontImage.MATERIAL_SEARCH, 8, second);
        tabs.addTab("Info", FontImage.MATERIAL_INFO, 8, third);
        tabs.setSelectedIndex(0);

        form.add(BorderLayout.CENTER, tabs);
    }
}
