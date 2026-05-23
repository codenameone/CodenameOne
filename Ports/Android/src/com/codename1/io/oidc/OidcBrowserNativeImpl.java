/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.browser.customtabs.CustomTabsIntent;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.LifecycleListener;

/**
 * Android implementation of {@link OidcBrowserNative} backed by
 * {@code androidx.browser.customtabs.CustomTabsIntent}.
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>{@link #startAuthorization(String, String)} is called from a worker thread.
 *   <li>We launch a Custom Tabs intent at the authorization URL.
 *   <li>The identity provider eventually redirects to a URL on the registered
 *       custom scheme (e.g. {@code com.example.app:/oauth2redirect?code=...}).
 *   <li>Android delivers that as an intent to {@code CodenameOneActivity}; we
 *       observe it via a {@link LifecycleListener#onResume()} callback.
 *   <li>The worker thread unblocks and returns the redirect URL to Java.
 * </ol>
 *
 * <p>The application must register an intent filter for the redirect URI
 * scheme in its {@code AndroidManifest.xml}. Add the {@code android.xintent_filter}
 * build hint pointing at your main activity:
 *
 * <pre>{@code
 * android.xintent_filter=<intent-filter>\n
 *   <action android:name="android.intent.action.VIEW"/>\n
 *   <category android:name="android.intent.category.DEFAULT"/>\n
 *   <category android:name="android.intent.category.BROWSABLE"/>\n
 *   <data android:scheme="com.example.app"/>\n
 * </intent-filter>
 * }</pre>
 */
public class OidcBrowserNativeImpl implements OidcBrowserNative {

    /** Guards {@link #pendingScheme} / {@link #resultUrl} and acts as a wait monitor. */
    private static final Object LOCK = new Object();

    /** Scheme half of the current flow's redirect URI, e.g. {@code "com.example.app"}. */
    private static String pendingScheme;

    /** Captured redirect URL once it arrives. */
    private static String resultUrl;

    /** Single shared lifecycle listener; installed lazily on first call. */
    private static LifecycleListener installedListener;

    @Override
    public boolean isSupported() {
        return AndroidNativeUtil.getActivity() != null;
    }

    @Override
    public String startAuthorization(final String authUrl, final String redirectScheme) {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            return null;
        }
        if (authUrl == null || redirectScheme == null) {
            return null;
        }

        installRedirectListenerOnce();

        synchronized (LOCK) {
            pendingScheme = redirectScheme;
            resultUrl = null;
        }

        // Open the Custom Tab on the UI thread; the user is sent away from the
        // app and will be brought back via the registered intent filter.
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    CustomTabsIntent customTabs =
                            new CustomTabsIntent.Builder().build();
                    customTabs.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    customTabs.launchUrl(activity, Uri.parse(authUrl));
                } catch (Throwable t) {
                    // Fallback to ACTION_VIEW if Custom Tabs is unavailable for
                    // any reason (e.g. no Chrome / Custom-Tabs-capable browser
                    // installed). Most users will still complete the flow.
                    Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(fallback);
                }
            }
        });

        // Block the calling worker thread until onResume captures the redirect
        // (or until an hour passes -- the cap is purely defensive).
        synchronized (LOCK) {
            long deadline = System.currentTimeMillis() + 3600 * 1000L;
            while (resultUrl == null) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    pendingScheme = null;
                    return null;
                }
                try {
                    LOCK.wait(remaining);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    pendingScheme = null;
                    return null;
                }
            }
            String r = resultUrl;
            resultUrl = null;
            pendingScheme = null;
            return r;
        }
    }

    private void installRedirectListenerOnce() {
        synchronized (LOCK) {
            if (installedListener != null) {
                return;
            }
            installedListener = new RedirectLifecycleListener();
            AndroidNativeUtil.addLifecycleListener(installedListener);
        }
    }

    /**
     * Listens for the activity returning to the foreground and snapshots the
     * intent if it carries a URL on our pending redirect scheme. Stays
     * installed for the lifetime of the process so multiple sign-in flows
     * over time all hit the same hook.
     */
    private static final class RedirectLifecycleListener implements LifecycleListener {
        public void onCreate(Bundle savedInstanceState) {}
        public void onPause() {}
        public void onDestroy() {}
        public void onSaveInstanceState(Bundle b) {}
        public void onLowMemory() {}

        public void onResume() {
            Activity act = AndroidNativeUtil.getActivity();
            if (act == null) return;
            Intent intent = act.getIntent();
            if (intent == null) return;
            Uri data = intent.getData();
            if (data == null) return;
            String scheme = data.getScheme();
            if (scheme == null) return;
            String full = data.toString();
            synchronized (LOCK) {
                if (pendingScheme == null) {
                    return;
                }
                // Match either the literal scheme (`com.example.app`) or the
                // full URI prefix (`https://example.com/cb`).
                boolean match = scheme.equalsIgnoreCase(pendingScheme)
                        || full.startsWith(pendingScheme);
                if (!match) {
                    return;
                }
                resultUrl = full;
                // Reset the intent data so a subsequent resume after rotation
                // does not re-trigger.
                intent.setData(null);
                LOCK.notifyAll();
            }
        }
    }
}
