package com.codename1.tools.skindesigner;

import com.codename1.system.NativeInterface;

/**
 * Reads the host-page theme that the website passes to the embedded Skin
 * Designer (the {@code ?theme=dark} / {@code ?theme=light} query on the iframe
 * URL), falling back to the OS {@code prefers-color-scheme}.
 *
 * <p>On the JavaScript port the translated application runs inside a Web Worker
 * with no access to {@code window.location} or {@code window.matchMedia}, so the
 * theme can only be read by a native interface whose JavaScript implementation
 * runs on the main-thread front end. The worker calls this synchronously and
 * receives the resolved value back. On every other platform the implementation
 * reports unsupported, and the caller falls back to {@code Display.isDarkMode()}.
 */
public interface WebsiteTheme extends NativeInterface {
    /**
     * @return {@code "dark"}, {@code "light"}, or {@code ""} when unknown.
     */
    String currentTheme();
}
