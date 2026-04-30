package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.FloatingActionButton;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

public class FloatingActionButtonThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "FloatingActionButtonTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Container content = new Container(new BorderLayout());
        content.add(BorderLayout.CENTER, new Label("Body content"));
        FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
        form.add(BorderLayout.CENTER, fab.bindFabToContainer(content));
    }
}
