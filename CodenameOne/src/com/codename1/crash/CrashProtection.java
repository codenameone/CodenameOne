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
import com.codename1.system.CrashReport;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Crash protection: captures unhandled exceptions, scrubs PII, persists
/// each crash to {@link Storage} immediately, and uploads to the
/// Codename One cloud crash service when enabled.
///
/// Quick start -- call from your `Lifecycle.init` (NOT from `start`,
/// which runs on every resume):
///
/// ```java
/// CrashProtection.install();
/// CrashProtection.setEnabled(true);
/// ```
///
/// `install()` is idempotent and wires the EDT error handler, the
/// platform native crash handler, and the native log capture. It also
/// replays any crash data persisted by the previous run (Java-side
/// failures buffered in {@link Storage}, plus native-layer crash
/// records written by the platform crash handler before the process
/// died).
///
/// `setEnabled` defaults to `false`, so no data leaves the device
/// until the developer explicitly enables uploads.
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
public final class CrashProtection {

    /// Production crash-upload endpoint. Fixed; not configurable from
    /// application code by design (use {@link #setEndpointForTesting}
    /// from the framework's own unit tests).
    static final String DEFAULT_ENDPOINT =
            "https://cloud.codenameone.com/api/v2/crash/reports";

    static final String STORAGE_PREFIX = "CN1Crash__$";
    static final String PREF_ENABLED = "crashProtectionEnabled";
    /// Hard cap on locally-buffered crashes. Crashes are rare events
    /// and the server dedups by fingerprint anyway, so the buffer is
    /// just protection against a runaway-throw loop offline. 5 is
    /// plenty; beyond that we evict the oldest to keep disk footprint
    /// negligible.
    static final int MAX_STORED = 5;

    private static String endpoint = DEFAULT_ENDPOINT;
    private static PiiScrubber scrubber = new PiiScrubber();
    private static boolean installed;
    private static boolean draining;

    private CrashProtection() {
    }

    /// Installs the crash protection hooks. Idempotent: calling more
    /// than once has no effect. Does nothing on the simulator (matches
    /// the legacy `Log.bindCrashProtection` behaviour) or when crash
    /// protection has been disabled for the current platform via the
    /// `codename1.crashProtection.<platform>.enabled` build property
    /// (see [#isPlatformDisabled]).
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
        if (isPlatformDisabled()) {
            // Developer has explicitly opted this platform out via the
            // build property. We mark `installed` so subsequent calls
            // also short-circuit; the rest of the crash machinery
            // stays inert (no error handler, no drain).
            installed = true;
            return;
        }
        // setCrashReporter is the right hook -- it fires unconditionally
        // in Display's EDT catch block before impl.handleEDTException
        // gets a chance to short-circuit (the legacy
        // AndroidImplementation.handleEDTException returns true after
        // showing its own AlertDialog and would otherwise eat the
        // exception). addEdtErrorHandler runs only when the impl
        // returns false, which leaves Android uncovered.
        Display.getInstance().setCrashReporter(new CrashReport() {
            @Override
            public void exception(Throwable t) {
                capture(t);
            }
        });
        // Wire the platform native crash handler so signals / Mach
        // exceptions / Objective-C NSException / JNI segfaults that
        // never reach the JVM error path still produce a record.
        Display.getInstance().installNativeCrashHandler();
        // Replay any native crash record the platform handler wrote
        // before the process died on a prior launch. The impl deletes
        // the record as it's read so we don't re-upload it.
        String pendingNative = Display.getInstance().consumePendingNativeCrash();
        if (pendingNative != null && pendingNative.length() > 0) {
            CrashReportPayload synthetic = new CrashReportPayload(
                    newEventId(),
                    "NativeCrash",
                    "Process terminated by native fault",
                    new ArrayList<Frame>(0),
                    null,
                    pendingNative);
            persistJson(synthetic.toJson());
        }
        installed = true;
        if (isEnabled()) {
            drainAsync();
        }
    }

    /// Returns `true` when the developer has explicitly opted out of
    /// crash protection for the current platform via
    /// `codename1.crashProtection.<platform>.enabled=false` in
    /// `codenameone_settings.properties`. The property is read at
    /// runtime via [com.codename1.ui.Display#getProperty(String,String)],
    /// which on each platform consumes the build-time settings file
    /// baked into the deliverable. Returns `false` when the property
    /// is unset or `true` (the default-on behaviour).
    ///
    /// Recognised platform names match {@link Display#getPlatformName()}:
    /// `and`, `ios`, `mac`, `linux`, `win`, `javascript`, `javase`.
    static boolean isPlatformDisabled() {
        String platform = Display.getInstance().getPlatformName();
        if (platform == null || platform.isEmpty()) {
            return false;
        }
        String key = "codename1.crashProtection." + platform.toLowerCase() + ".enabled";
        String v = Display.getInstance().getProperty(key, "");
        if (v == null || v.isEmpty()) {
            return false;
        }
        // Only an explicit `false` disables. Anything else (true / typo
        // / unrecognised) leaves the platform enabled -- safer to
        // upload than to silently swallow crashes.
        return "false".equalsIgnoreCase(v.trim());
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
        String nativeLog = safeNativeLog();
        return new CrashReportPayload(newEventId(), exClass, message,
                frames, nativeLog, null);
    }

    /// Pulls the platform log snapshot, swallowing any exception the
    /// platform implementation throws -- crash protection must never
    /// itself crash the host. Returns `null` on platforms without a
    /// readable process log or when the snapshot fails.
    private static String safeNativeLog() {
        try {
            return Display.getInstance().getNativeLogSnapshot();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /// Persists a JSON payload directly into the same drain queue
    /// used by Java-side captures. Used for synthetic native-crash
    /// records replayed at install time.
    private static void persistJson(String json) {
        if (json == null || json.length() == 0) {
            return;
        }
        if (countStored() >= MAX_STORED) {
            evictOldest();
        }
        String name = STORAGE_PREFIX + newEventId();
        // Manual close-in-finally rather than try-with-resources --
        // codenameone-core compiles with -source 1.5 for backward
        // compatibility, and try-with-resources is Java 7+. Calling
        // close() directly (rather than via Util.cleanup) lets PMD's
        // CloseResource analyser see the close path.
        OutputStream os = null;
        try {
            os = Storage.getInstance().createOutputStream(name);
            os.write(json.getBytes("UTF-8"));
            os.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {
                    /* best-effort */
                }
            }
        }
    }

    static List<Frame> extractFrames(Throwable t) {
        List<Frame> out = new ArrayList<Frame>();
        StackTraceElement[] elements = t.getStackTrace();
        // Throwable.getStackTrace contractually returns a non-null array
        // (empty when stack trace info is unavailable). SpotBugs flags
        // the redundant null check; trust the contract.
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
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {
                    /* best-effort */
                }
            }
        }
    }

    private static int countStored() {
        String[] all = Storage.getInstance().listEntries();
        if (all == null) {
            return 0;
        }
        int c = 0;
        for (String entry : all) {
            if (entry != null && entry.startsWith(STORAGE_PREFIX)) {
                c++;
            }
        }
        return c;
    }

    private static void evictOldest() {
        String[] all = Storage.getInstance().listEntries();
        if (all == null) {
            return;
        }
        String oldest = null;
        for (String n : all) {
            if (n == null || !n.startsWith(STORAGE_PREFIX)) {
                continue;
            }
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
            @Override
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
        for (String name : all) {
            if (name == null || !name.startsWith(STORAGE_PREFIX)) {
                continue;
            }
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
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                    /* best-effort */
                }
            }
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
            @Override
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

    private static final java.util.Random EVENT_ID_RNG = new java.util.Random();

    private static String newEventId() {
        char[] out = new char[32];
        for (int i = 0; i < 32; i++) {
            // Random.nextInt(16) avoids the float -> int dance that
            // Math.random() does and that SpotBugs flags as wasteful
            // (DM_NEXTINT_VIA_NEXTDOUBLE). Crypto-quality randomness
            // isn't needed -- the eventId is a dedup token, not a
            // secret. (The server also de-dups by fingerprint, so a
            // collision is recoverable.)
            int v = EVENT_ID_RNG.nextInt(16);
            out[i] = (char) (v < 10 ? '0' + v : 'a' + (v - 10));
        }
        return new String(out);
    }

    /// Framework-internal hook for unit tests; never call from app code.
    static void setEndpointForTesting(String url) {
        endpoint = url;
    }
}
