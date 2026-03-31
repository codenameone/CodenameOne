/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java runtime helpers extracted from {@link HTML5Implementation} so the
 * ParparVM JavaScript target can share path and proxy rules without depending
 * on TeaVM APIs.
 */
public final class JavaScriptRuntimeFacade {
    public static final String STORAGE_KEY_PREFIX = "storage/";
    public static final String FILE_SYSTEM_PREFIX = "cn1fs/";

    private JavaScriptRuntimeFacade() {
    }

    public interface UrlEncoder {
        String encode(String value);
    }

    public static String wrapStorageKey(String name) {
        return STORAGE_KEY_PREFIX + name;
    }

    public static String unwrapStorageKey(String name) {
        if (name != null && name.startsWith(STORAGE_KEY_PREFIX)) {
            return name.substring(STORAGE_KEY_PREFIX.length());
        }
        return null;
    }

    public static String[] unwrapStorageEntries(String[] keys) {
        List<String> out = new ArrayList<String>();
        if (keys == null) {
            return new String[0];
        }
        for (String key : keys) {
            String unwrapped = unwrapStorageKey(key);
            if (unwrapped != null) {
                out.add(unwrapped);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    public static String wrapFile(String path) {
        if (path == null) {
            return FILE_SYSTEM_PREFIX;
        }
        if (path.startsWith("file://")) {
            path = path.substring(7);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return FILE_SYSTEM_PREFIX + path;
    }

    public static String unwrapFile(String path) {
        return "file:///" + path.substring(FILE_SYSTEM_PREFIX.length());
    }

    public static String stripTrailingSlash(String path) {
        while (path != null && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static boolean isRootFile(String path) {
        return wrapFile(path).equals(wrapFile("/"));
    }

    public static boolean shouldProxyUrl(String url, String doNotProxyList, boolean useProxyForSameDomain, boolean sameDomain, String proxyUrl) {
        if (url == null || (!url.startsWith("http:") && !url.startsWith("https:"))) {
            return false;
        }
        if (isNoProxyUrl(url, doNotProxyList)) {
            return false;
        }
        if (!useProxyForSameDomain && sameDomain) {
            return false;
        }
        return proxyUrl != null && proxyUrl.length() > 0;
    }

    public static String proxifyUrl(String url, String doNotProxyList, boolean useProxyForSameDomain, boolean sameDomain, String proxyUrl, UrlEncoder encoder) {
        if (!shouldProxyUrl(url, doNotProxyList, useProxyForSameDomain, sameDomain, proxyUrl)) {
            return url;
        }
        return proxyUrl + encoder.encode(url);
    }

    private static boolean isNoProxyUrl(String url, String doNotProxyList) {
        if (doNotProxyList == null || doNotProxyList.trim().length() == 0) {
            return false;
        }
        for (String domain : doNotProxyList.trim().split("\\s+")) {
            if (domain.length() > 0 && url.startsWith(domain)) {
                return true;
            }
        }
        return false;
    }
}
