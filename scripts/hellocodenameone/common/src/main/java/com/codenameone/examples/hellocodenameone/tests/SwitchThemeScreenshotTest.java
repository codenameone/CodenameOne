package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.OnOffSwitch;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class SwitchThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "SwitchTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("On/Off Switch"));
        OnOffSwitch off = new OnOffSwitch();
        off.setValue(false);
        form.add(off);

        OnOffSwitch on = new OnOffSwitch();
        on.setValue(true);
        form.add(on);

        form.add(new Label("Disabled switch"));
        OnOffSwitch disabled = new OnOffSwitch();
        disabled.setValue(true);
        disabled.setEnabled(false);
        form.add(disabled);
    }
}
