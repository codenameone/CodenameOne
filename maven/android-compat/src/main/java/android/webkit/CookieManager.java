/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package android.webkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The cookies web views send. Cookies set here are kept per host for the
/// application's lifetime and returned by [#getCookie(String)]; the native
/// browser keeps its own store, which this class does not read or write.
public class CookieManager {

    private static CookieManager sInstance;

    private final Map<String, List<String>> mCookies = new HashMap<String, List<String>>();
    private boolean mAccept = true;

    protected CookieManager() {
    }

    public static CookieManager getInstance() {
        if (sInstance == null) {
            sInstance = new CookieManager();
        }
        return sInstance;
    }

    public void setAcceptCookie(boolean accept) {
        mAccept = accept;
    }

    public boolean acceptCookie() {
        return mAccept;
    }

    public void setAcceptThirdPartyCookies(WebView webview, boolean accept) {
    }

    public boolean acceptThirdPartyCookies(WebView webview) {
        return false;
    }

    public static boolean allowFileSchemeCookies() {
        return false;
    }

    public static void setAcceptFileSchemeCookies(boolean accept) {
    }

    private static String host(String url) {
        if (url == null) {
            return "";
        }
        int start = url.indexOf("://");
        start = start < 0 ? 0 : start + 3;
        int end = start;
        while (end < url.length() && "/?#:".indexOf(url.charAt(end)) < 0) {
            end++;
        }
        return url.substring(start, end);
    }

    private static String name(String cookie) {
        int eq = cookie.indexOf('=');
        return (eq < 0 ? cookie : cookie.substring(0, eq)).trim();
    }

    public void setCookie(String url, String value) {
        if (!mAccept || value == null) {
            return;
        }
        int semi = value.indexOf(';');
        String pair = (semi < 0 ? value : value.substring(0, semi)).trim();
        String h = host(url);
        List<String> list = mCookies.get(h);
        if (list == null) {
            list = new ArrayList<String>();
            mCookies.put(h, list);
        }
        String n = name(pair);
        for (int i = 0; i < list.size(); i++) {
            if (name(list.get(i)).equals(n)) {
                list.remove(i);
                break;
            }
        }
        list.add(pair);
    }

    public void setCookie(String url, String value, ValueCallback<Boolean> callback) {
        setCookie(url, value);
        if (callback != null) {
            callback.onReceiveValue(Boolean.valueOf(mAccept));
        }
    }

    public String getCookie(String url) {
        List<String> list = mCookies.get(host(url));
        if (list == null || list.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public void removeSessionCookies(ValueCallback<Boolean> callback) {
        removeAllCookies(callback);
    }

    public void removeAllCookies(ValueCallback<Boolean> callback) {
        boolean had = !mCookies.isEmpty();
        mCookies.clear();
        if (callback != null) {
            callback.onReceiveValue(Boolean.valueOf(had));
        }
    }

    @Deprecated
    public void removeAllCookie() {
        mCookies.clear();
    }

    @Deprecated
    public void removeSessionCookie() {
        mCookies.clear();
    }

    public boolean hasCookies() {
        return !mCookies.isEmpty();
    }

    public void removeExpiredCookie() {
    }

    public void flush() {
    }
}
