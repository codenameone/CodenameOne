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
