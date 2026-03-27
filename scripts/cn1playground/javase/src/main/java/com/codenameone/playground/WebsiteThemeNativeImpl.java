package com.codenameone.playground;

public class WebsiteThemeNativeImpl implements WebsiteThemeNative {
    public boolean isDarkMode() {
        return true;
    }

    public boolean isSupported() {
        return true;
    }

    public void notifyUiReady() {
    }
}
