package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class CheckBoxRadioThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "CheckBoxRadioTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("CheckBoxes"));
        form.add(new CheckBox("Unselected"));
        CheckBox selected = new CheckBox("Selected");
        selected.setSelected(true);
        form.add(selected);
        CheckBox disabled = new CheckBox("Disabled");
        disabled.setEnabled(false);
        form.add(disabled);

        form.add(new Label("RadioButtons"));
        form.add(new RadioButton("Unselected"));
        RadioButton rSel = new RadioButton("Selected");
        rSel.setSelected(true);
        form.add(rSel);
        RadioButton rDis = new RadioButton("Disabled");
        rDis.setEnabled(false);
        form.add(rDis);
    }
}
