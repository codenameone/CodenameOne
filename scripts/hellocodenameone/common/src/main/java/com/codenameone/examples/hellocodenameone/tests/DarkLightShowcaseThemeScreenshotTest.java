package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/**
 * Mixed-components showcase: one screen with Button / RaisedButton /
 * TextField / CheckBox / RadioButton / SpanLabel stacked together, to
 * catch regressions where the light and dark palettes diverge in contrast
 * across a realistic form mix.
 */
public class DarkLightShowcaseThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "ShowcaseTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("Showcase " + suffix));

        Container row = new Container(BoxLayout.x());
        row.add(new Button("Default"));
        Button raised = new Button("Raised");
        raised.setUIID("RaisedButton");
        row.add(raised);
        form.add(row);

        TextField tf = new TextField("hello@example.com");
        form.add(tf);

        Container toggles = new Container(BoxLayout.x());
        CheckBox cb = new CheckBox("Remember me");
        cb.setSelected(true);
        toggles.add(cb);
        RadioButton rb = new RadioButton("Agree");
        rb.setSelected(true);
        toggles.add(rb);
        form.add(toggles);

        SpanLabel body = new SpanLabel(
                "Body copy using the theme's default SpanLabel styling. This "
                + "should be clearly legible against the form background in "
                + "both light and dark appearances.");
        form.add(body);
    }
}
