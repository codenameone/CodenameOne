package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

public class ListThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "ListTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        List list = new List(new Object[]{
            "First item",
            "Second item",
            "Third item",
            "Fourth item",
            "Fifth item",
            "Sixth item",
            "Seventh item",
            "Eighth item"
        });
        list.setSelectedIndex(1);

        Container wrap = new Container(new BorderLayout());
        wrap.add(BorderLayout.CENTER, list);
        form.add(BorderLayout.CENTER, wrap);
    }
}
