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

    private CN1SurfaceStore() {
    }

    // --- widget timelines -----------------------------------------------------

    /// Returns the storage directory of a widget kind (also the image blob directory handed to
    /// the renderer). The directory is not created by this call.
    public static File kindDir(Context ctx, String kindId) {
        return new File(baseDir(ctx), sanitize(kindId));
    }

    /// Atomically replaces the persisted timeline of a widget kind.
    public static void writeWidgetTimeline(Context ctx, String kindId, String timelineJson,
            Map<String, byte[]> images) throws IOException {
        File dir = kindDir(ctx, kindId);
        mkdirs(dir);
        writeImages(dir, images);
        writeAtomic(new File(dir, "timeline.json"), utf8(timelineJson));
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
