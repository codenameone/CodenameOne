package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.Switch;
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
        form.add(new Label("Switch off"));
        Switch off = new Switch();
        off.setValue(false);
        form.add(off);
        annotateComponent(off, "Switch track: pill + 1.4x thumb scale (iOS) / 1.5x (Material)");

        form.add(new Label("Switch on"));
        Switch on = new Switch();
        on.setValue(true);
        form.add(on);

        form.add(new Label("Disabled switch"));
        Switch disabled = new Switch();
        disabled.setValue(true);
        disabled.setEnabled(false);
        form.add(disabled);
    }
}
