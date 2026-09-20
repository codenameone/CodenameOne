package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/**
 * Captures how a theme styles the Toolbar, which means this test has to RENDER one.
 *
 * <p>It stopped doing that and nothing noticed. The suite sets
 * {@code desktop.titleBar=native}, and in that mode the Toolbar is deliberately never
 * attached to the form -- the title belongs to the OS window and the commands to the
 * platform menu bar. So on every desktop port this screen captured its one body Label on an
 * empty form: the committed Windows and macOS goldens are 99.8% and 99.7% a single colour,
 * and a test that photographs an empty screen cannot fail.
 *
 * <p>Hiding the Toolbar is correct for the application. It is wrong for the one test whose
 * subject IS the Toolbar, so this test asks for {@code toolbar} mode while it captures and
 * puts the setting back afterwards. Display.setProperty is what the ports actually read --
 * Display.getProperty consults localProperties before the implementation -- so this reaches
 * every port the suite runs on.
 */
public class ToolbarThemeScreenshotTest extends DualAppearanceBaseTest {
    private String priorTitleBarMode;
    private boolean titleBarModeOverridden;

    @Override
    public boolean runTest() {
        // Before super: the mode is read when the Toolbar is installed on the form, which
        // happens inside the first appearance pass.
        priorTitleBarMode = Display.getInstance().getProperty("desktop.titleBar", null);
        Display.getInstance().setProperty("desktop.titleBar", "toolbar");
        titleBarModeOverridden = true;
        return super.runTest();
    }

    @Override
    protected void restoreAfterCapture() {
        if (!titleBarModeOverridden) {
            return;
        }
        titleBarModeOverridden = false;
        // Null is not the same as "toolbar" here: it means the project asked for nothing and
        // the installed theme's own desktopTitleBarMode constant gets to answer. Writing
        // "toolbar" back would silence that for every test after this one.
        Display.getInstance().setProperty("desktop.titleBar", priorTitleBarMode);
    }

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
