package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Command;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

public class ToolbarThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "ToolbarTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Toolbar tb = form.getToolbar();
        if (tb == null) {
            tb = new Toolbar();
            form.setToolbar(tb);
        }
        tb.setTitle("Theme Gallery");
        tb.addMaterialCommandToLeftBar("Menu", FontImage.MATERIAL_MENU,
                (ActionEvent e) -> { /* no-op */ });
        tb.addMaterialCommandToRightBar("Search", FontImage.MATERIAL_SEARCH,
                (ActionEvent e) -> { /* no-op */ });
        Command moreCmd = new Command("More") {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        tb.addCommandToOverflowMenu(moreCmd);

        form.add(new Label("Body content under the Toolbar."));
    }
}
