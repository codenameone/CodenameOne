/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.js.browser;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;

/**
 * Interface for the JavaScript Location object.
 * https://developer.mozilla.org/en-US/docs/Web/API/Location
 */
public interface Location extends JSObject {
    @JSProperty String getHref();
    @JSProperty void setHref(String href);
    @JSProperty String getHash();
    @JSProperty String getHost();
    @JSProperty String getHostname();
    @JSProperty String getOrigin();
    @JSProperty String getPathname();
    @JSProperty String getPort();
    @JSProperty String getProtocol();
    @JSProperty String getSearch();
    String getFullURL();
    void setFullURL(String url);
    void assign(String url);
    void replace(String url);
    void reload(boolean forceGet);
}