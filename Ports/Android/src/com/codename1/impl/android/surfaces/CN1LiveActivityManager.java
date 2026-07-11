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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

/// Android lowering of live activities: an ongoing, silent, high-importance notification with
/// custom content views rendered by `CN1SurfaceRenderer` from the persisted descriptor. The
/// collapsed row approximates the Dynamic Island compact presentation (compactLeading +
/// compactTrailing composed into a row when the descriptor declares island regions, the full
/// content otherwise); the expanded notification shows the full content layout.
///
/// Requires API 24 (`Notification.Builder#setCustomContentView` and
/// `DecoratedCustomViewStyle`); `AndroidSurfaceBridge#isLiveActivitySupported()` reports false
/// below that and when the user disabled notifications. Updates re-render locally from the
/// descriptor persisted at start time merged with the latest state map (state-only updates per
/// the SPI contract). Android 16 "Live Updates" / `ProgressStyle` is a possible future lowering.
public final class CN1LiveActivityManager {
    private static final String TAG = "CN1Surfaces";
    private static final String DEFAULT_CHANNEL = "cn1_live_activities";
    private static final String NOTIFICATION_TAG = "cn1la";

    private CN1LiveActivityManager() {
    }

    /// Returns true when live activities can be presented on this device right now.
    public static boolean isSupported(Context ctx) {
        if (ctx == null || Build.VERSION.SDK_INT < 24) {
            return false;
        }
        try {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            return nm != null && nm.areNotificationsEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    /// Starts a live activity from a serialized descriptor; returns its id or null on failure.
    public static String start(Context ctx, String descriptorJson, Map<String, byte[]> images) {
        if (!isSupported(ctx)) {
            return null;
        }
        try {
            String id = CN1SurfaceStore.newActivityId(ctx);
            CN1SurfaceStore.writeLiveActivity(ctx, id, descriptorJson, images);
            notifyActivity(ctx, id, new JSONObject(descriptorJson), true, false);
            return id;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to start a live activity", t);
            return null;
        }
    }

    /// Re-renders a running live activity with a fresh state map replacing the previous one.
    public static void update(Context ctx, String activityId, String stateJson) {
        if (ctx == null || activityId == null) {
            return;
        }
        try {
            JSONObject doc = replaceState(ctx, activityId, stateJson);
            if (doc == null) {
                return;
            }
            CN1SurfaceStore.writeLiveActivity(ctx, activityId, doc.toString(), null);
            notifyActivity(ctx, activityId, doc, true, false);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to update live activity " + activityId, t);
        }
    }

    /// Ends a live activity, optionally leaving a dismissible final state on screen.
    public static void end(Context ctx, String activityId, String finalStateJson,
            boolean dismissImmediately) {
        if (ctx == null || activityId == null) {
            return;
        }
        try {
            if (finalStateJson != null && !dismissImmediately) {
                JSONObject doc = replaceState(ctx, activityId, finalStateJson);
                if (doc != null) {
                    notifyActivity(ctx, activityId, doc, false, true);
                }
            } else {
                NotificationManager nm =
                        (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    nm.cancel(NOTIFICATION_TAG, notificationId(activityId));
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to end live activity " + activityId, t);
        } finally {
            CN1SurfaceStore.deleteLiveActivity(ctx, activityId);
        }
    }

    // --- internals ------------------------------------------------------------

    private static JSONObject replaceState(Context ctx, String activityId, String stateJson)
            throws Exception {
        String persisted = CN1SurfaceStore.readLiveActivity(ctx, activityId);
        if (persisted == null) {
            Log.w(TAG, "Live activity " + activityId + " is not running");
            return null;
        }
        JSONObject doc = new JSONObject(persisted);
        if (stateJson != null) {
            // each update carries the complete fresh state: replace wholesale so
            // keys omitted by the app disappear, matching every other platform
            doc.put("state", new JSONObject(stateJson));
        }
        return doc;
    }

    private static void notifyActivity(Context ctx, String activityId, JSONObject doc,
            boolean ongoing, boolean autoCancel) {
        if (Build.VERSION.SDK_INT < 24) {
            return;
        }
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        String type = doc.optString("type", "activity");
        JSONObject state = doc.optJSONObject("state");
        JSONObject content = doc.optJSONObject("content");
        File imagesDir = CN1SurfaceStore.liveActivityImagesDir(ctx, activityId);

        RemoteViews big = content == null ? null
                : CN1SurfaceRenderer.render(ctx, content, state, type, imagesDir);
        JSONObject compactNode = buildCompactNode(doc);
        RemoteViews compact = compactNode == null ? big
                : CN1SurfaceRenderer.render(ctx, compactNode, state, type, imagesDir);
        if (big == null && compact == null) {
            Log.w(TAG, "Live activity " + activityId + " has no renderable content");
            return;
        }

        String channelId = channelId(doc);
        ensureChannel(ctx, nm, channelId);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(ctx, channelId);
        } else {
            builder = new Notification.Builder(ctx);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }
        builder.setSmallIcon(smallIcon(ctx));
        builder.setStyle(new Notification.DecoratedCustomViewStyle());
        builder.setCustomContentView(compact != null ? compact : big);
        if (big != null) {
            builder.setCustomBigContentView(big);
        }
        builder.setOngoing(ongoing);
        builder.setAutoCancel(autoCancel);
        builder.setOnlyAlertOnce(true);
        JSONObject tint = doc.optJSONObject("tint");
        if (tint != null && Build.VERSION.SDK_INT >= 21) {
            long l = tint.optLong("l", 0xff007aff);
            builder.setColor((int) l);
        }
        nm.notify(NOTIFICATION_TAG, notificationId(activityId), builder.build());
    }

    /// Composes the collapsed-row layout from the Dynamic Island compact regions when present:
    /// leading, an expanding spacer, trailing.
    private static JSONObject buildCompactNode(JSONObject doc) {
        try {
            JSONObject island = doc.optJSONObject("island");
            if (island == null) {
                return null;
            }
            JSONObject leading = island.optJSONObject("compactLeading");
            JSONObject trailing = island.optJSONObject("compactTrailing");
            if (leading == null && trailing == null) {
                return null;
            }
            JSONObject row = new JSONObject();
            row.put("t", "row");
            row.put("spacing", 8);
            JSONArray ch = new JSONArray();
            if (leading != null) {
                ch.put(leading);
            }
            JSONObject spacer = new JSONObject();
            spacer.put("t", "spacer");
            ch.put(spacer);
            if (trailing != null) {
                ch.put(trailing);
            }
            row.put("ch", ch);
            return row;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to compose the compact live activity row", t);
            return null;
        }
    }

    private static String channelId(JSONObject doc) {
        JSONObject android = doc.optJSONObject("android");
        if (android != null) {
            String channel = android.optString("channel", null);
            if (channel != null && channel.length() > 0) {
                return channel;
            }
        }
        return DEFAULT_CHANNEL;
    }

    private static void ensureChannel(Context ctx, NotificationManager nm, String channelId) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        try {
            NotificationChannel channel = new NotificationChannel(channelId, "Live updates",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.enableVibration(false);
            nm.createNotificationChannel(channel);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to create the live activity channel", t);
        }
    }

    /// Resolves the small icon the same way the port's notification publisher does.
    private static int smallIcon(Context ctx) {
        int smallIcon = ctx.getResources().getIdentifier("ic_stat_notify", "drawable",
                ctx.getApplicationInfo().packageName);
        if (smallIcon == 0) {
            smallIcon = ctx.getResources().getIdentifier("icon", "drawable",
                    ctx.getApplicationInfo().packageName);
        }
        if (smallIcon == 0) {
            smallIcon = ctx.getApplicationInfo().icon;
        }
        return smallIcon;
    }

    private static int notificationId(String activityId) {
        return activityId.hashCode();
    }
}
