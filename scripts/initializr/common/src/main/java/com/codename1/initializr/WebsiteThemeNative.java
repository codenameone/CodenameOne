package com.codename1.initializr;

import com.codename1.system.NativeInterface;

public interface WebsiteThemeNative extends NativeInterface {
    boolean isDarkMode();
    void notifyUiReady();

    /// Horizontal clearance, in CSS pixels, that the host page's chat launcher
    /// (Crisp) currently needs at the bottom-right, or 0 when it is hidden. The
    /// generate button is shifted left by this amount so the launcher does not
    /// cover it.
    int chatLauncherClearance();
}
