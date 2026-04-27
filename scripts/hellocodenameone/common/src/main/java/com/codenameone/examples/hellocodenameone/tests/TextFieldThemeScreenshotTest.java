package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class TextFieldThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "TextFieldTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        TextField filled = new TextField("Hello theme");
        form.add(new Label("TextField"));
        form.add(filled);

        TextField empty = new TextField();
        empty.setHint("Type here");
        form.add(new Label("TextField (hint)"));
        form.add(empty);

        TextField disabled = new TextField("Disabled");
        disabled.setEnabled(false);
        form.add(new Label("TextField disabled"));
        form.add(disabled);

        TextArea area = new TextArea("Multi-line text\nacross several lines.", 3, 20);
        form.add(new Label("TextArea"));
        form.add(area);
    }
}
