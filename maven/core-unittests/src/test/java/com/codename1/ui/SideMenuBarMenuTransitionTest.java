package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.SideMenuBar.MenuTransition;
import org.junit.jupiter.api.Assertions;

public class SideMenuBarMenuTransitionTest extends UITestBase {

    @FormTest
    public void testMenuTransitionInstantiation() {
        SideMenuBar menuBar = new SideMenuBar();
        MenuTransition transition = menuBar.new MenuTransition(300, true, 0, SideMenuBar.COMMAND_PLACEMENT_VALUE_RIGHT);
        Assertions.assertNotNull(transition);
    }
}
