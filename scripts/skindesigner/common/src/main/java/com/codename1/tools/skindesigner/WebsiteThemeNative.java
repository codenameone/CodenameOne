package com.codename1.tools.skindesigner;

import com.codename1.system.NativeInterface;

public interface WebsiteThemeNative extends NativeInterface {
    boolean isDarkMode();
    void notifyUiReady();
}
