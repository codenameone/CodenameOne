package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class SpanLabelThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "SpanLabelTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("Single-line Label for reference"));

        SpanLabel shortSpan = new SpanLabel(
                "Short SpanLabel text that fits on one or two lines depending "
                + "on width.");
        form.add(shortSpan);

        SpanLabel longSpan = new SpanLabel(
                "Longer SpanLabel paragraph. SpanLabel wraps across lines "
                + "using the current theme's font settings. This lets us "
                + "verify that paragraph text spacing, line height, color, "
                + "and contrast all render correctly in both light and dark "
                + "appearances.");
        form.add(longSpan);

        SpanLabel secondary = new SpanLabel(
                "Secondary caption styled via the SecondaryLabel UIID.");
        secondary.setUIID("SecondaryLabel");
        form.add(secondary);
    }
}
