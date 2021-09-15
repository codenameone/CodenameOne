package com.codename1.ui;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
public class TestToolbar extends AbstractTest {
    public boolean runTest() throws Exception {
        Form f = new Form();
        Toolbar tb = f.getToolbar();
        tb.addComponentToSideMenu(new Label("Hello"));

        return true;

    }
}