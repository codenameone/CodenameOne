package com.codename1.examples.hellocodenameone.tests.accessibility;

public class AccessibilityNativeInterfaceImpl implements AccessibilityNativeInterface {
    public String getLastAccessibilityAnnouncement() {
        return com.codename1.impl.android.AndroidImplementation.testLastAccessibilityAnnouncement;
    }
    public boolean isSupported() {
        return true;
    }
}
