package com.codename1.examples.hellocodenameone.tests.accessibility;

import com.codename1.ui.Display;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;
import com.codename1.system.NativeLookup;

public class AccessibilityTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        AccessibilityNativeInterface ni = NativeLookup.create(AccessibilityNativeInterface.class);
        if (ni == null) {
            System.out.println("AccessibilityNativeInterface not implemented on this platform");
            return true;
        }

        String expected = "Testing accessibility announcement";
        Display.getInstance().announceForAccessibility(expected);

        // Give it a moment to propagate if async
        Thread.sleep(100);

        String actual = ni.getLastAccessibilityAnnouncement();
        if (!expected.equals(actual)) {
            throw new RuntimeException("Accessibility announcement mismatch. Expected: " + expected + ", Actual: " + actual);
        }
        return true;
    }
}
