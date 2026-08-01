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
package com.codename1.impl.android.surfaces;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Persistence for published surfaces on Android. Surfaces render while the app process may be
/// dead (widget updates run in short-lived broadcast receivers), so everything published through
/// the bridge is written under `filesDir/cn1surfaces/`:
///
/// - `cn1surfaces/<kindId>/timeline.json` plus the `<name>.png` blobs of a widget kind
/// - `cn1surfaces/activities/<id>.json` (descriptor merged with the latest state) plus
///   `cn1surfaces/activities/<id>/<name>.png` blobs of a live activity
///
/// Writes are atomic (write to a temp file, then rename) because the widget provider may read
/// concurrently with a publish. A `SharedPreferences` file named `cn1surfaces` tracks the
/// registered kind ids (so `reloadWidgets(null)` can iterate them) and the live activity id
/// sequence. All helpers are static; there is no instance state.
public final class CN1SurfaceStore {
    private static final String TAG = "CN1Surfaces";
    private static final String PREFS = "cn1surfaces";
    private static final String KEY_KINDS = "kinds";
    private static final String KEY_ACTIVITY_SEQ = "laSeq";
    private static final String KEY_FETCH_CLASS = "bgFetchClass";
    private static final String KEY_FETCH_AT_PREFIX = "bgFetchAt_";
    private static final String KEY_NOTIFICATION_PROMPTS = "notificationPrompts";
    private static final String KEY_PROMPTS_INSTALL = "notificationPromptsInstall";
    /// Cached `firstInstallTime`; constant for the life of the process.
    private static volatile long installStamp;

    private CN1SurfaceStore() {
    }

    // --- widget timelines -----------------------------------------------------

    /// Returns the storage directory of a widget kind (also the image blob directory handed to
    /// the renderer). The directory is not created by this call.
    public static File kindDir(Context ctx, String kindId) {
        return new File(baseDir(ctx), sanitize(kindId));
    }

    /// Atomically replaces the persisted timeline of a widget kind and garbage collects image
    /// blobs the replacement timeline no longer references (the document's `images` list is the
    /// complete reference set; content-hash names would otherwise accumulate without bound).
    public static void writeWidgetTimeline(Context ctx, String kindId, String timelineJson,
            Map<String, byte[]> images) throws IOException {
        File dir = kindDir(ctx, kindId);
        mkdirs(dir);
        writeImages(dir, images);
        writeAtomic(new File(dir, "timeline.json"), utf8(timelineJson));
        deleteUnreferencedImages(dir, timelineJson);
    }

    private static void deleteUnreferencedImages(File dir, String timelineJson) {
        try {
            org.json.JSONObject doc = new org.json.JSONObject(timelineJson);
            org.json.JSONArray names = doc.optJSONArray("images");
            java.util.HashSet<String> referenced = new java.util.HashSet<String>();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    referenced.add(sanitize(names.optString(i)));
                }
            }
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                String name = f.getName();
                if (name.endsWith(".png")
                        && !referenced.contains(name.substring(0, name.length() - 4))) {
                    delete(f);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to garbage collect widget images in " + dir, t);
        }
    }

    /// Returns the persisted timeline JSON of a widget kind, or null when nothing was published.
    public static String readWidgetTimeline(Context ctx, String kindId) {
        return readText(new File(kindDir(ctx, kindId), "timeline.json"));
    }

    // --- live activities ------------------------------------------------------

    /// Allocates the next live activity id ("la1", "la2", ...).
    public static String newActivityId(Context ctx) {
        SharedPreferences prefs = prefs(ctx);
        int seq = prefs.getInt(KEY_ACTIVITY_SEQ, 0) + 1;
        prefs.edit().putInt(KEY_ACTIVITY_SEQ, seq).apply();
        return "la" + seq;
    }

    /// Atomically persists a live activity descriptor (with its state already merged in).
    public static void writeLiveActivity(Context ctx, String activityId, String descriptorJson,
            Map<String, byte[]> images) throws IOException {
        File dir = activitiesDir(ctx);
        mkdirs(dir);
        if (images != null && !images.isEmpty()) {
            File imgDir = liveActivityImagesDir(ctx, activityId);
            mkdirs(imgDir);
            writeImages(imgDir, images);
        }
        writeAtomic(new File(dir, sanitize(activityId) + ".json"), utf8(descriptorJson));
    }

    /// Returns the persisted descriptor of a live activity, or null when unknown.
    public static String readLiveActivity(Context ctx, String activityId) {
        return readText(new File(activitiesDir(ctx), sanitize(activityId) + ".json"));
    }

    /// Returns the image blob directory of a live activity.
    public static File liveActivityImagesDir(Context ctx, String activityId) {
        return new File(activitiesDir(ctx), sanitize(activityId));
    }

    /// Removes a live activity's descriptor and image blobs.
    public static void deleteLiveActivity(Context ctx, String activityId) {
        delete(new File(activitiesDir(ctx), sanitize(activityId) + ".json"));
        File imgDir = liveActivityImagesDir(ctx, activityId);
        File[] blobs = imgDir.listFiles();
        if (blobs != null) {
            for (File blob : blobs) {
                delete(blob);
            }
        }
        delete(imgDir);
    }

    // --- kind bookkeeping -----------------------------------------------------

    /// Records a widget kind id so `reloadWidgets(null)` can iterate every published kind.
    public static void rememberKind(Context ctx, String kindId) {
        SharedPreferences prefs = prefs(ctx);
        String joined = prefs.getString(KEY_KINDS, "");
        for (String existing : joined.split(",")) {
            if (existing.equals(kindId)) {
                return;
            }
        }
        String updated = joined.length() == 0 ? kindId : joined + "," + kindId;
        prefs.edit().putString(KEY_KINDS, updated).apply();
    }

    /// Returns the recorded widget kind ids, possibly empty.
    public static List<String> getRememberedKinds(Context ctx) {
        List<String> out = new ArrayList<String>();
        String joined = prefs(ctx).getString(KEY_KINDS, "");
        for (String kind : joined.split(",")) {
            if (kind.length() > 0) {
                out.add(kind);
            }
        }
        return out;
    }

    // --- widget-driven refresh ------------------------------------------------

    /// Records the class name of the app's `com.codename1.background.BackgroundFetch` listener
    /// so a widget rendering an exhausted timeline can start the fetch service while the app
    /// process is dead. Called by the bridge on every publish; a null name (the app declares
    /// no background fetch) is a no-op, keeping this zero-cost for apps without one.
    public static void rememberBackgroundFetchClass(Context ctx, String className) {
        if (className == null || className.length() == 0) {
            return;
        }
        SharedPreferences prefs = prefs(ctx);
        if (!className.equals(prefs.getString(KEY_FETCH_CLASS, null))) {
            prefs.edit().putString(KEY_FETCH_CLASS, className).apply();
        }
    }

    /// Returns the recorded `BackgroundFetch` listener class name, or null when the app never
    /// published while declaring background fetch.
    public static String getBackgroundFetchClass(Context ctx) {
        String name = prefs(ctx).getString(KEY_FETCH_CLASS, "");
        return name.length() == 0 ? null : name;
    }

    /// Claims a widget-driven background fetch slot for a kind: returns true (recording the
    /// attempt time) when no attempt happened within the throttle window, false to skip. A
    /// recorded time in the future (the clock jumped backwards) resets the window instead of
    /// blocking fetches until the clock catches up.
    public static boolean tryClaimBackgroundFetch(Context ctx, String kindId, long now,
            long throttleMillis) {
        SharedPreferences prefs = prefs(ctx);
        String key = KEY_FETCH_AT_PREFIX + sanitize(kindId);
        long last = prefs.getLong(key, 0);
        if (last <= now && now - last < throttleMillis) {
            return false;
        }
        prefs.edit().putLong(key, now).apply();
        return true;
    }

    // --- live activity notification permission --------------------------------

    /// How many times `LiveActivity.start()` has raised the `POST_NOTIFICATIONS` prompt on
    /// Android 13+ without ending up granted. `CN1LiveActivityManager` bounds the prompt at two
    /// attempts, mirroring Android's own two-strike model, so an outcome it cannot tell apart --
    /// an explicit "Don't allow", a dialog the user dismissed without choosing, or a request the
    /// system auto-denied without showing anything -- costs at most one more attempt instead of
    /// being locked in as a permanent refusal on the first one.
    ///
    /// The count is scoped to one installation. Codename One builds allow backup by default, so
    /// these preferences ride along to a reinstall or a new device, where a restored "2" would
    /// silently suppress the dialog forever on what the API documents as a fresh install --
    /// exactly the permanent silent failure this whole path exists to remove. Stamping the count
    /// with the install it was earned against costs one cached lookup and needs no build-side
    /// backup rules.
    public static int getNotificationPromptCount(Context ctx) {
        long stamp = installStamp(ctx);
        if (stamp == 0) {
            // the install could not be identified, so no stored count can be attributed to it.
            // Reading zero errs toward prompting, which is the safe direction: a lookup failure
            // of ours must never be what silently suppresses the dialog.
            return 0;
        }
        SharedPreferences prefs = prefs(ctx);
        if (prefs.getLong(KEY_PROMPTS_INSTALL, 0) != stamp) {
            return 0;
        }
        return prefs.getInt(KEY_NOTIFICATION_PROMPTS, 0);
    }

    /// Counts one raised prompt; see [#getNotificationPromptCount(Context)].
    public static void recordNotificationPrompt(Context ctx) {
        long stamp = installStamp(ctx);
        if (stamp == 0) {
            // nothing to attribute the attempt to. Persisting it under a zero stamp would let a
            // repeated lookup failure accumulate a budget that no install owns, and a later
            // successful lookup could not tell that count apart from a legitimately unstamped one
            return;
        }
        prefs(ctx).edit()
                .putInt(KEY_NOTIFICATION_PROMPTS, getNotificationPromptCount(ctx) + 1)
                .putLong(KEY_PROMPTS_INSTALL, stamp)
                .apply();
    }

    /// Forgets the prompt count once the permission is held, so a user who grants, later revokes
    /// in the system settings and comes back gets the same two attempts a fresh install does.
    public static void clearNotificationPrompts(Context ctx) {
        SharedPreferences prefs = prefs(ctx);
        if (prefs.contains(KEY_NOTIFICATION_PROMPTS) || prefs.contains(KEY_PROMPTS_INSTALL)) {
            prefs.edit().remove(KEY_NOTIFICATION_PROMPTS).remove(KEY_PROMPTS_INSTALL).apply();
        }
    }

    /// Identifies the current installation. `firstInstallTime` survives app *updates* -- which
    /// must not hand back a spent budget -- but changes on a genuine reinstall and on a restore
    /// to another device, which is precisely the line this needs to draw.
    private static long installStamp(Context ctx) {
        long stamp = installStamp;
        if (stamp != 0) {
            return stamp;
        }
        try {
            stamp = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).firstInstallTime;
        } catch (Throwable t) {
            // unknown: 0 matches the default of an unstamped preference, so a count written
            // under a known stamp is discarded rather than trusted. Deliberately not cached --
            // pinning a transient lookup failure would disable the per-install scoping for the
            // rest of the process and quietly hand every count back as zero.
            return 0;
        }
        installStamp = stamp;
        return stamp;
    }

    // --- internals ------------------------------------------------------------

    private static File baseDir(Context ctx) {
        return new File(ctx.getFilesDir(), "cn1surfaces");
    }

    private static File activitiesDir(Context ctx) {
        return new File(baseDir(ctx), "activities");
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void writeImages(File dir, Map<String, byte[]> images) throws IOException {
        if (images == null) {
            return;
        }
        for (Map.Entry<String, byte[]> e : images.entrySet()) {
            String name = sanitize(e.getKey());
            if (e.getValue() != null) {
                writeAtomic(new File(dir, name + ".png"), e.getValue());
            }
        }
    }

    private static void writeAtomic(File target, byte[] data) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        OutputStream os = new FileOutputStream(tmp);
        try {
            os.write(data);
        } finally {
            os.close();
        }
        if (!tmp.renameTo(target)) {
            // rename over an existing file is atomic on the filesystems Android uses, but be
            // defensive against exotic mounts
            delete(target);
            if (!tmp.renameTo(target)) {
                throw new IOException("Failed to move " + tmp + " to " + target);
            }
        }
    }

    private static String readText(File f) {
        if (!f.exists()) {
            return null;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                bo.write(buf, 0, r);
            }
            return new String(bo.toByteArray(), "UTF-8");
        } catch (IOException ex) {
            Log.w(TAG, "Failed to read " + f, ex);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // UTF-8 is guaranteed on Android; this cannot happen
            throw new IllegalStateException("UTF-8 unsupported", ex);
        }
    }

    private static void mkdirs(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create " + dir);
        }
    }

    private static void delete(File f) {
        if (f.exists() && !f.delete()) {
            Log.w(TAG, "Failed to delete " + f);
        }
    }

    private static String sanitize(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }
}
