package com.codename1.examples.hellocodenameone.tests.accessibility;

public class AccessibilityNativeInterfaceImpl implements AccessibilityNativeInterface {
    public native String getLastAccessibilityAnnouncement();
    public native boolean isSupported();
}
