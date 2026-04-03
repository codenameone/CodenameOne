/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface Location extends JSObject {
    @JSProperty String getHref();
    @JSProperty void setHref(String href);
    @JSProperty String getHash();
    @JSProperty void setHash(String hash);
    @JSProperty String getHost();
    @JSProperty String getHostname();
    @JSProperty String getOrigin();
    @JSProperty String getPathname();
    @JSProperty void setPathname(String pathname);
    @JSProperty String getPort();
    @JSProperty String getProtocol();
    @JSProperty String getSearch();
    @JSProperty void setSearch(String search);
    void reload();
    void assign(String url);
    void replace(String url);
}

public interface History extends JSObject {
    int getLength();
    void back();
    void forward();
    void go(int delta);
    void pushState(Object state, String title);
    void pushState(Object state, String title, String url);
    void replaceState(Object state, String title);
    void replaceState(Object state, String title, String url);
}

public interface Screen extends JSObject {
    @JSProperty int getWidth();
    @JSProperty int getHeight();
    @JSProperty int getAvailWidth();
    @JSProperty int getAvailHeight();
    @JSProperty int getColorDepth();
    @JSProperty int getPixelDepth();
}

public interface Navigator extends JSObject {
    @JSProperty String getUserAgent();
    @JSProperty String getAppName();
    @JSProperty String getAppVersion();
    @JSProperty String getPlatform();
    @JSProperty String getLanguage();
    @JSProperty boolean isOnLine();
    boolean javaEnabled();
}