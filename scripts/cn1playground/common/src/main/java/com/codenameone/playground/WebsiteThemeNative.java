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

    /// Inline mode (the Playground embedded under a documentation snippet): the
    /// code the host page most recently asked this frame to show, cleared by the
    /// read, or ``null`` when there is none. The page reuses one frame for every
    /// snippet it opens, so moving to another snippet costs a recompile, not a
    /// second VM boot.
    String takeInlineCode();

    /// Inline mode: tells the host page how the last run went. ``summary`` is the
    /// first error, or empty when ``ok``; ``sourceUrl`` is the full Playground
    /// carrying the reader's edits. The host page opens that one from its own
    /// button: a window opened from inside this frame, after the click has
    /// crossed to the EDT, is no longer a user gesture and gets popup-blocked.
    void notifyInlineResult(boolean ok, String summary, String sourceUrl);
}
