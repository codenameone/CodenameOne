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
package com.codename1.impl.android;

import android.content.pm.PackageInfo;
import android.util.Log;
import android.webkit.WebView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/// DIAGNOSTIC ONLY (diag branch, never merged).
final class WebViewNavDiag {
    static final boolean STORM = true;
    private static final String TAG = "CN1WVDIAG";
    private static boolean done;
    private static List<String> names;
    private static boolean[] before;
    private static Method findLoaded;
    private static ClassLoader cl;

    private WebViewNavDiag() {
    }

    static void beforeFirstNavigation(final WebView web) {
        if (done) {
            return;
        }
        done = true;
        try {
            Log.i(TAG, "debuggable=" + ((web.getContext().getApplicationInfo().flags
                    & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                    + " storm=" + STORM);
            cl = (ClassLoader) WebView.class.getMethod("getWebViewClassLoader").invoke(null);
            Log.i(TAG, "webview loader=" + cl);
            findLoaded = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            findLoaded.setAccessible(true);
            PackageInfo pi = (PackageInfo) WebView.class.getMethod("getCurrentWebViewPackage").invoke(null);
            List<String> paths = new ArrayList<String>();
            paths.add(pi.applicationInfo.sourceDir);
            if (pi.applicationInfo.splitSourceDirs != null) {
                for (String s : pi.applicationInfo.splitSourceDirs) {
                    paths.add(s);
                }
            }
            if (pi.applicationInfo.sharedLibraryFiles != null) {
                for (String s : pi.applicationInfo.sharedLibraryFiles) {
                    paths.add(s);
                }
            }
            names = new ArrayList<String>();
            for (String p : paths) {
                try {
                    dalvik.system.DexFile df = new dalvik.system.DexFile(p);
                    int c = 0;
                    for (Enumeration<String> e = df.entries(); e.hasMoreElements(); ) {
                        names.add(e.nextElement());
                        c++;
                    }
                    df.close();
                    Log.i(TAG, "dex " + p + " classes=" + c);
                } catch (Throwable t) {
                    Log.i(TAG, "dex " + p + " failed " + t);
                }
            }
            before = new boolean[names.size()];
            int loaded = 0;
            for (int i = 0; i < before.length; i++) {
                before[i] = isLoaded(names.get(i));
                if (before[i]) {
                    loaded++;
                }
            }
            Log.i(TAG, "before first navigation: " + loaded + " of " + before.length + " loaded");
        } catch (Throwable t) {
            Log.i(TAG, "enumeration failed", t);
        }
        if (STORM) {
            Thread storm = new Thread(new Runnable() {
                public void run() {
                    long end = System.currentTimeMillis() + 8000;
                    Object[] ring = new Object[8192];
                    int i = 0;
                    int gcs = 0;
                    while (System.currentTimeMillis() < end) {
                        ring[i & 8191] = new byte[64 + (i % 1024)];
                        i++;
                        if ((i & 0x3fff) == 0) {
                            Runtime.getRuntime().gc();
                            gcs++;
                        }
                    }
                    Log.i(TAG, "storm finished allocations=" + i + " gcs=" + gcs);
                }
            }, "cn1-gc-storm");
            storm.start();
        }
        web.postDelayed(new Runnable() {
            public void run() {
                after();
            }
        }, 4000);
    }

    private static boolean isLoaded(String n) {
        try {
            return findLoaded.invoke(cl, n) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void after() {
        if (names == null) {
            return;
        }
        int c = 0;
        for (int i = 0; i < before.length; i++) {
            if (!before[i] && isLoaded(names.get(i))) {
                Log.i(TAG, "NEW " + names.get(i));
                c++;
            }
        }
        Log.i(TAG, "loaded during first navigation: " + c);
        String[] dump = {
            "org.chromium.content_public.browser.NavigationHandle",
            "org.chromium.content.browser.webcontents.WebContentsObserverProxy",
            "org.chromium.content.browser.framehost.NavigationControllerImpl",
            "org.chromium.android_webview.AwWebContentsObserver",
            "org.chromium.android_webview.AwContentsClientBridge",
            "org.chromium.content_public.browser.LoadCommittedDetails",
            "org.chromium.content.browser.RenderFrameHostImpl"
        };
        for (String n : dump) {
            try {
                Class k = Class.forName(n, false, cl);
                for (Method m : k.getDeclaredMethods()) {
                    Log.i(TAG, "M " + m.toGenericString());
                }
            } catch (Throwable t) {
                Log.i(TAG, "M " + n + " " + t);
            }
        }
    }
}
