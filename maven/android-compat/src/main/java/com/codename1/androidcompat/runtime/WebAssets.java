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
package com.codename1.androidcompat.runtime;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Serves `file:///android_asset/` URLs to the native browser. Assets ship
/// inside the application, where a browser cannot read them, so the first
/// such URL copies the asset tree into the application's home directory and
/// is loaded from there; relative links between assets keep working.
public final class WebAssets {

    public static final String ASSET_PREFIX = "file:///android_asset/";

    private static boolean extracted;

    private WebAssets() {
    }

    private static String root() {
        String home = FileSystemStorage.getInstance().getAppHomePath();
        if (!home.endsWith("/")) {
            home = home + "/";
        }
        return home + "android_asset/";
    }

    /// A URL the native browser can load for `url`: asset URLs are mapped
    /// onto the extracted copy, anything else is returned unchanged.
    public static String toLoadableUrl(String url) {
        if (url == null || !url.startsWith(ASSET_PREFIX)) {
            return url;
        }
        if (!extracted) {
            extracted = true;
            extract("");
        }
        return root() + url.substring(ASSET_PREFIX.length());
    }

    /// The inverse of [#toLoadableUrl(String)], so the application sees the
    /// asset URLs it asked for in callbacks and `getUrl()`.
    public static String toAndroidUrl(String url) {
        if (url == null || !extracted) {
            return url;
        }
        String root = root();
        if (url.startsWith(root)) {
            return ASSET_PREFIX + url.substring(root.length());
        }
        return url;
    }

    private static void extract(String dir) {
        String[] names = Assets.list(dir);
        FileSystemStorage fs = FileSystemStorage.getInstance();
        for (String name : names) {
            String path = dir + name;
            String[] children = Assets.list(path);
            if (children.length > 0) {
                fs.mkdir(root() + path);
                extract(path + "/");
            } else {
                copy(path, root() + path);
            }
        }
    }

    private static void copy(String asset, String dest) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        int slash = dest.lastIndexOf('/');
        if (slash > 0) {
            String parent = dest.substring(0, slash);
            if (!fs.exists(parent)) {
                fs.mkdir(parent);
            }
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = Assets.open(asset);
            out = fs.openOutputStream(dest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            Log.e(e);
        } finally {
            close(in);
            close(out);
        }
    }

    private static void close(Object c) {
        try {
            if (c instanceof InputStream) {
                ((InputStream) c).close();
            } else if (c instanceof OutputStream) {
                ((OutputStream) c).close();
            }
        } catch (IOException e) {
            Log.e(e);
        }
    }
}
