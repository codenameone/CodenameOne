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
package com.codename1.analytics;

import android.content.Context;
import android.os.Bundle;
import com.codename1.impl.android.AndroidNativeUtil;
import java.lang.reflect.Method;
import java.util.Iterator;
import org.json.JSONObject;

/**
 * Android implementation of {@link NativeFirebaseAnalytics}, delegating to the
 * Firebase Analytics SDK.
 *
 * <p>The SDK is invoked reflectively -- the same approach the Android port
 * already uses for Firebase Cloud Messaging (see
 * {@code AndroidImplementation}'s {@code FirebaseMessaging} lookup). Reflection
 * keeps the Android port compilable without a {@code firebase-analytics}
 * dependency on its own classpath; the Gradle dependency is added to the
 * generated app by {@code AndroidGradleBuilder} when the
 * {@code android.firebaseAnalytics=true} build hint is set. If the SDK is not
 * present at runtime every method is a safe no-op and {@link #isSupported()}
 * returns false, so the {@link FirebaseAnalyticsProvider} degrades cleanly.
 */
public class NativeFirebaseAnalyticsImpl implements NativeFirebaseAnalytics {
    private Object firebaseAnalytics;
    private boolean resolved;

    private Object instance() {
        if (!resolved) {
            resolved = true;
            try {
                Context c = AndroidNativeUtil.getContext();
                if (c != null) {
                    Class<?> cls = Class.forName("com.google.firebase.analytics.FirebaseAnalytics");
                    Method getInstance = cls.getMethod("getInstance", Context.class);
                    firebaseAnalytics = getInstance.invoke(null, c);
                }
            } catch (Throwable t) {
                firebaseAnalytics = null;
            }
        }
        return firebaseAnalytics;
    }

    @Override
    public boolean isSupported() {
        return instance() != null;
    }

    @Override
    public void logEvent(String name, String paramsJson) {
        logEventBundle(sanitize(name), toBundle(paramsJson));
    }

    @Override
    public void logScreen(String screenName) {
        Bundle b = new Bundle();
        b.putString("screen_name", screenName);
        logEventBundle("screen_view", b);
    }

    @Override
    public void setUserId(String id) {
        Object fa = instance();
        if (fa == null) {
            return;
        }
        try {
            fa.getClass().getMethod("setUserId", String.class).invoke(fa, id);
        } catch (Throwable t) {
            // no-op
        }
    }

    @Override
    public void setUserProperty(String key, String value) {
        Object fa = instance();
        if (fa == null) {
            return;
        }
        try {
            fa.getClass().getMethod("setUserProperty", String.class, String.class).invoke(fa, key, value);
        } catch (Throwable t) {
            // no-op
        }
    }

    private void logEventBundle(String name, Bundle params) {
        Object fa = instance();
        if (fa == null) {
            return;
        }
        try {
            Method m = fa.getClass().getMethod("logEvent", String.class, Bundle.class);
            m.invoke(fa, name, params);
        } catch (Throwable t) {
            // no-op
        }
    }

    private static Bundle toBundle(String json) {
        Bundle b = new Bundle();
        if (json == null || json.length() == 0) {
            return b;
        }
        try {
            JSONObject o = new JSONObject(json);
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Object v = o.get(k);
                if (v instanceof Number) {
                    b.putDouble(k, ((Number) v).doubleValue());
                } else if (v instanceof Boolean) {
                    b.putString(k, v.toString());
                } else {
                    b.putString(k, String.valueOf(v));
                }
            }
        } catch (Throwable t) {
            // ignore malformed params; send the event without them
        }
        return b;
    }

    /** Firebase event / param names must be alphanumeric or underscore. */
    private static String sanitize(String name) {
        if (name == null || name.length() == 0) {
            return "event";
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
