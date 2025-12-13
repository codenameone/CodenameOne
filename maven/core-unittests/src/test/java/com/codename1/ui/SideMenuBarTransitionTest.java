package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.plaf.UIManager;
import java.util.Hashtable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;

public class SideMenuBarTransitionTest extends UITestBase {

    public static class TestSideMenuBar extends SideMenuBar {
        @Override
        protected Button createOpenButton() {
            Button b = new Button();
            b.setUIID("MenuButton");
            return b;
        }
    }

    @Disabled("Fails due to NPE in headless environment")
    @FormTest
    public void testMenuTransition() throws Exception {
        Hashtable theme = new Hashtable();
        theme.put("sideMenuShadowBool", Boolean.FALSE);
        UIManager.getInstance().addThemeProps(theme);

        Form f1 = new Form("Source");
        f1.show();

        SideMenuBar bar = new TestSideMenuBar();
        bar.initMenuBar(f1);

        Command cmd = new Command("Test");
        cmd.putClientProperty(SideMenuBar.COMMAND_PLACEMENT_KEY, SideMenuBar.COMMAND_PLACEMENT_VALUE_TOP);
        bar.addCommand(cmd);

        bar.openMenu(SideMenuBar.COMMAND_PLACEMENT_VALUE_TOP);

        Transition t = f1.getTransitionOutAnimator();
        Assertions.assertNotNull(t);
        Assertions.assertTrue(t instanceof SideMenuBar.MenuTransition);
    }
}
