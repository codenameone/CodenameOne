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
package com.codename1.crash;

import com.codename1.crash.CrashReportPayload.Frame;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Crash protection: captures unhandled exceptions, scrubs PII, persists
/// each crash to {@link Storage} immediately, and uploads to the
/// Codename One cloud crash service when enabled.
///
/// Quick start:
///
/// ```java
/// // In your start() method, after Display has initialised:
/// CrashProtection.install();
/// // Once your app has obtained user consent (e.g. from a settings
/// // screen) flip this on. The default is off so no data leaves the
/// // device until the developer explicitly enables uploads.
/// CrashProtection.setEnabled(true);
/// ```
///
/// Pluggable PII scrubbing: override {@link PiiScrubber} and register
/// your subclass via {@link #setScrubber(PiiScrubber)} to extend the
/// default redaction rules. The default scrubber masks the local part
/// of email addresses (keeping the first three characters and the full
/// domain) and replaces runs of six or more digits with `[num]`.
///
/// The endpoint URL is fixed and cannot be modified by the application.
/// Crash uploads are accepted only for Pro-tier (or higher) accounts
/// for builds produced by the Codename One cloud build server within
/// the last 30 days; out-of-tier or stale builds are silently dropped
/// by the server.
///
/// This runs in parallel with the legacy {@code Log.bindCrashProtection}
/// path; both can be installed in the same app without conflict.
public final class CrashProtection {

    /// Production crash-upload endpoint. Fixed; not configurable from
    /// application code by design (use {@link #setEndpointForTesting}
    /// from the framework's own unit tests).
    static final String DEFAULT_ENDPOINT =
            "https://cloud.codenameone.com/api/v2/crash/reports";

    static final String STORAGE_PREFIX = "CN1Crash__$";
    static final String PREF_ENABLED = "crashProtectionEnabled";
    static final int MAX_STORED = 100;

    private static String endpoint = DEFAULT_ENDPOINT;
    private static PiiScrubber scrubber = new PiiScrubber();
    private static boolean installed;
    private static boolean draining;

    private CrashProtection() {
    }

    /// Installs the crash protection hooks. Idempotent: calling more
    /// than once has no effect. Does nothing on the simulator (matches
    /// the legacy {@code Log.bindCrashProtection} behaviour).
    ///
    /// Side effect: any crashes previously persisted to storage but not
    /// yet uploaded will be drained in the background if uploads are
    /// currently enabled.
    public static void install() {
        if (installed) {
            return;
        }
        if (Display.getInstance().isSimulator()) {
            installed = true;
            return;
        }
        Display.getInstance().addEdtErrorHandler(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Object src = evt.getSource();
                if (src instanceof Throwable) {
                    capture((Throwable) src);
                }
            }
        });
        installed = true;
        if (isEnabled()) {
            drainAsync();
        }
    }

    /// @return `true` if crash uploads are enabled. Default is `false`.
    public static boolean isEnabled() {
        return Preferences.get(PREF_ENABLED, false);
    }

    /// Enables or disables crash uploads. When transitioning from off
    /// to on, any crashes buffered in storage are drained in the
    /// background. The toggle persists across launches.
    public static void setEnabled(boolean enabled) {
        boolean was = isEnabled();
        Preferences.set(PREF_ENABLED, enabled);
        if (enabled && !was) {
            drainAsync();
        }
    }

    /// Replaces the active PII scrubber. Subclass {@link PiiScrubber}
    /// to extend or replace the default redaction rules.
    public static void setScrubber(PiiScrubber s) {
        if (s == null) {
            throw new IllegalArgumentException("scrubber");
        }
        scrubber = s;
    }

    public static PiiScrubber getScrubber() {
        return scrubber;
    }

    /// Manually report an exception. The crash is persisted to storage
    /// first and then uploaded if uploads are enabled. Stack frames
    /// are captured from the throwable.
    public static void capture(Throwable t) {
        if (t == null) {
            return;
        }
        if (Display.getInstance().isSimulator()) {
            return;
        }
        try {
            CrashReportPayload payload = build(t);
            String name = persist(payload);
            if (name != null && isEnabled()) {
                sendAsync(name, payload.toJson());
            }
        } catch (Throwable inner) {
            inner.printStackTrace();
        }
    }

    static CrashReportPayload build(Throwable t) {
        String exClass = t.getClass().getName();
        String message = scrubber.scrubMessage(t.getMessage());
        List<Frame> frames = extractFrames(t);
        return new CrashReportPayload(newEventId(), exClass, message, frames);
    }

    static List<Frame> extractFrames(Throwable t) {
        List<Frame> out = new ArrayList<Frame>();
        StackTraceElement[] elements = t.getStackTrace();
        if (elements == null) {
            return out;
        }
        int limit = elements.length < CrashReportPayload.MAX_FRAMES
                ? elements.length : CrashReportPayload.MAX_FRAMES;
        for (int i = 0; i < limit; i++) {
            StackTraceElement e = elements[i];
            String cls = e.getClassName();
            String method = scrubber.scrubFrame(cls, e.getMethodName());
            out.add(new Frame(cls, method, e.getFileName(),
                    e.getLineNumber(), e.isNativeMethod()));
        }
        return out;
    }

    private static String persist(CrashReportPayload payload) {
        if (countStored() >= MAX_STORED) {
            evictOldest();
        }
        String name = STORAGE_PREFIX + payload.eventId;
        OutputStream os = null;
        try {
            os = Storage.getInstance().createOutputStream(name);
            os.write(payload.toJson().getBytes("UTF-8"));
            os.flush();
            return name;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Util.cleanup(os);
        }
    }

    private static int countStored() {
        String[] all = Storage.getInstance().listEntries();
        if (all == null) return 0;
        int c = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i] != null && all[i].startsWith(STORAGE_PREFIX)) {
                c++;
            }
        }
        return c;
    }

    private static void evictOldest() {
        String[] all = Storage.getInstance().listEntries();
        if (all == null) return;
        String oldest = null;
        for (int i = 0; i < all.length; i++) {
            String n = all[i];
            if (n == null || !n.startsWith(STORAGE_PREFIX)) continue;
            if (oldest == null || n.compareTo(oldest) < 0) {
                oldest = n;
            }
        }
        if (oldest != null) {
            Storage.getInstance().deleteStorageFile(oldest);
        }
    }

    private static void drainAsync() {
        if (draining) {
            return;
        }
        draining = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    drain();
                } finally {
                    draining = false;
                }
            }
        }, "CrashProtectionDrain").start();
    }

    static void drain() {
        if (!isEnabled()) {
            return;
        }
        String[] all = Storage.getInstance().listEntries();
        if (all == null) {
            return;
        }
        for (int i = 0; i < all.length; i++) {
            String name = all[i];
            if (name == null || !name.startsWith(STORAGE_PREFIX)) continue;
            String json = readStored(name);
            if (json == null) {
                Storage.getInstance().deleteStorageFile(name);
                continue;
            }
            sendBlocking(name, json);
        }
    }

    private static String readStored(String name) {
        InputStream is = null;
        try {
            is = Storage.getInstance().createInputStream(name);
            if (is == null) {
                return null;
            }
            byte[] data = Util.readInputStream(is);
            return new String(data, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Util.cleanup(is);
        }
    }

    private static void sendAsync(final String storageName, final String json) {
        NetworkManager.getInstance().addToQueue(buildRequest(storageName, json));
    }

    private static void sendBlocking(String storageName, String json) {
        try {
            NetworkManager.getInstance().addToQueueAndWait(buildRequest(storageName, json));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static ConnectionRequest buildRequest(final String storageName, String json) {
        ConnectionRequest req = new ConnectionRequest() {
            protected void postResponse() {
                int code = getResponseCode();
                if (code >= 200 && code < 300) {
                    Storage.getInstance().deleteStorageFile(storageName);
                }
            }
        };
        req.setUrl(endpoint);
        req.setPost(true);
        req.setHttpMethod("POST");
        req.setContentType("application/json");
        req.setRequestBody(json);
        req.setFailSilently(true);
        return req;
    }

    private static String newEventId() {
        char[] out = new char[32];
        for (int i = 0; i < 32; i++) {
            int v = (int) (Math.random() * 16);
            out[i] = (char) (v < 10 ? '0' + v : 'a' + (v - 10));
        }
        return new String(out);
    }

    /// Framework-internal hook for unit tests; never call from app code.
    static void setEndpointForTesting(String url) {
        endpoint = url;
    }
}
