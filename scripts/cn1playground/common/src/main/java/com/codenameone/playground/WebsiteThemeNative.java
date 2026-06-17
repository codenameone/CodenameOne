package com.codenameone.playground;

import com.codename1.system.NativeInterface;

public interface WebsiteThemeNative extends NativeInterface {
    boolean isDarkMode();
    void notifyUiReady();

    /// The host-page URL (including query string) as seen by the browser. On the
    /// JavaScript port ``CN.getProperty("browser.window.location.href")`` is
    /// unreliable -- the call is inlined/devirtualised past
    /// ``HTML5Implementation.getProperty`` to the base implementation, which just
    /// returns the default -- so deep links (``?sample=`` / ``?code=`` / ``?css=``)
    /// were never seen. This bridges straight to the page (and its parent frame
    /// when embedded) where the real URL lives. Returns ``null`` off-browser.
    String locationHref();
}
