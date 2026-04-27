package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.MultiButton;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class MultiButtonThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "MultiButtonTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(build("Title only", null, null, null, FontImage.MATERIAL_PERSON));
        form.add(build("First row", "Secondary line", null, null, FontImage.MATERIAL_EMAIL));
        form.add(build("Three lines", "Secondary line", "Tertiary line", null, FontImage.MATERIAL_PHONE));
        form.add(build("Four lines", "Secondary", "Tertiary", "Quaternary line", FontImage.MATERIAL_SCHEDULE));
    }

    private static MultiButton build(String l1, String l2, String l3, String l4, char icon) {
        MultiButton b = new MultiButton(l1);
        if (l2 != null) {
            b.setTextLine2(l2);
        }
        if (l3 != null) {
            b.setTextLine3(l3);
        }
        if (l4 != null) {
            b.setTextLine4(l4);
        }
        FontImage.setMaterialIcon(b, icon);
        return b;
    }
}
