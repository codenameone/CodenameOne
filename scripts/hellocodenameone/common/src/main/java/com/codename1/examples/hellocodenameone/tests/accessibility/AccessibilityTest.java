package com.codename1.examples.hellocodenameone.tests.accessibility;

import com.codename1.ui.Display;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

public class AccessibilityTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        String expected = "Testing accessibility announcement";
        // Just verify that invoking this doesn't crash the app
        Display.getInstance().announceForAccessibility(expected);
        return true;
    }
}
