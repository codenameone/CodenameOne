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
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.impl.android.CodenameOneActivity;

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
/// below that and when the user disabled notifications. On Android 13 (API 33) and newer an
/// ongoing notification additionally needs the `POST_NOTIFICATIONS` runtime permission, which the
/// build declares for you: [#start(Context, String, Map)] raises the system prompt the first time
/// an app starts a live activity without it, at most twice across the install before `isSupported`
/// reports false. Updates re-render locally from the descriptor persisted at start time merged
/// with the latest state map (state-only updates per the SPI contract). Android 16 "Live Updates" / `ProgressStyle` is a possible future lowering.
public final class CN1LiveActivityManager {
    private static final String TAG = "CN1Surfaces";
    private static final String DEFAULT_CHANNEL = "cn1_live_activities";
    private static final String NOTIFICATION_TAG = "cn1la";
    /// Prompt attempts before live activities report unsupported; Android's own model auto-denies
    /// after two refusals, so a third attempt would never reach the user anyway.
    private static final int MAX_NOTIFICATION_PROMPTS = 2;
    /// Guards the permission request so concurrent starts raise one dialog and count one answer.
    private static final Object PERMISSION_LOCK = new Object();
    private static final int DECLARED_PRESENT = 1;
    private static final int DECLARED_MISSING = 2;
    /// Cached manifest verdict: 0 not looked up yet, otherwise one of the DECLARED_ constants.
    private static volatile int permissionDeclaredState;

    private CN1LiveActivityManager() {
    }

    /// Returns true when live activities can be presented on this device, either right now or
    /// after the `POST_NOTIFICATIONS` prompt `start` raises on Android 13+. A pending permission
    /// counts as supported: reporting false there would make the app skip the very call that
    /// prompts, so a first-run install could never present an activity at all. It goes false once
    /// the permission is held but notifications are switched off, or the prompt has been refused
    /// as often as `start` will raise it.
    public static boolean isSupported(Context ctx) {
        if (ctx == null || Build.VERSION.SDK_INT < 24) {
            return false;
        }
        try {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
                return false;
            }
            if (nm.areNotificationsEnabled()) {
                // Observing the grant is what retires earlier refusals, not the path it arrived
                // by -- these prompts, push registration, Display.requestNotificationPermission
                // or the settings screen. Doing it here too means a grant the app only ever
                // observes through isSupported still resets the budget, so a later revoke starts
                // over with the full two attempts instead of a stale count.
                CN1SurfaceStore.clearNotificationPrompts(ctx);
                return true;
            }
            if (Build.VERSION.SDK_INT < 33) {
                // disabled notifications are a settled user choice, not a pending prompt
                return false;
            }
            // From API 33 notifications also read as disabled while POST_NOTIFICATIONS is merely
            // ungranted, which is the state of every fresh install. Granted-but-disabled is the
            // settled choice again; ungranted is supported while the permission is one the app
            // can actually ask for and a prompt attempt remains. The manifest test is what keeps
            // an app that publishes widgets but never set "liveActivities": true from reporting
            // supported forever: no declaration means no prompt, so no attempt is ever spent and
            // the count alone would never settle.
            return !hasPostNotificationsPermission(ctx)
                    && !isPermissionMissing(ctx)
                    && canPromptAgain(ctx);
        } catch (Throwable t) {
            return false;
        }
    }

    /// Starts a live activity from a serialized descriptor; returns its id or null on failure.
    /// Raises the `POST_NOTIFICATIONS` prompt first when Android 13+ needs it, blocking the
    /// calling thread until the user answers.
    public static String start(Context ctx, String descriptorJson, Map<String, byte[]> images) {
        // permission first: `isSupported` reports false once the prompt budget is spent, so
        // testing it first would short-circuit past the one place that logs *why* a start was
        // refused -- precisely the case a developer needs to see. Nothing is prompted that
        // `isSupported` would have rejected on capability grounds either, since the permission
        // only exists from API 33 and live activities need API 24.
        if (!ensureNotificationPermission(ctx) || !isSupported(ctx)) {
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

    // --- notification permission ----------------------------------------------

    /// Makes sure the ongoing notification a live activity lowers to can actually be posted:
    /// on Android 13+ that needs the `POST_NOTIFICATIONS` runtime permission the build declares
    /// but nobody has granted yet on a fresh install. Raises the standard Codename One permission
    /// request, which blocks the calling thread until the user answers in a way that keeps the
    /// EDT pumping when called from it.
    ///
    /// A request that comes back ungranted is counted rather than latched: the platform hands
    /// back one bare boolean for an explicit "Don't allow", a dialog the user dismissed without
    /// choosing and a request the system auto-denied without showing anything at all, so treating
    /// the first false as a permanent refusal would strand a user who only swiped the dialog
    /// away. Two attempts, matching Android's own two-strike model, then `isSupported` reports
    /// false. Nothing is consulted before the live permission state, so a grant that arrives from
    /// anywhere -- these prompts, push registration, `Display.requestNotificationPermission`, the
    /// system settings -- takes effect immediately and resets the count.
    ///
    /// Only `start` calls this. `update` and `end` act on an activity that is already running,
    /// so the permission was necessarily granted when it started.
    private static boolean ensureNotificationPermission(Context ctx) {
        if (Build.VERSION.SDK_INT < 33 || ctx == null) {
            return true;
        }
        if (hasPostNotificationsPermission(ctx)) {
            CN1SurfaceStore.clearNotificationPrompts(ctx);
            return true;
        }
        // `start` is callable from any thread and `checkForPermission` drives the activity's one
        // shared request flag and request code, so two concurrent starts would otherwise raise a
        // single dialog whose single outcome released both callers -- and then be counted twice,
        // spending the whole budget on one answer. One request at a time; whoever waited re-reads
        // the state the winner produced.
        //
        // This serializes surfaces against itself only. A camera or location request in flight
        // elsewhere still shares that same activity-wide flag and request code, and its callback
        // can release this one early -- an existing limitation of the shared permission machinery
        // rather than of this path, and one that needs per-request completion state in
        // `AndroidImplementation` to fix properly. The damage here is bounded: a spuriously
        // counted attempt costs the user one of two prompts, and any later grant, from any
        // source, clears the count.
        synchronized (PERMISSION_LOCK) {
            if (hasPostNotificationsPermission(ctx)) {
                CN1SurfaceStore.clearNotificationPrompts(ctx);
                return true;
            }
            if (!canPromptAgain(ctx)) {
                Log.w(TAG, "Live activities are unavailable: POST_NOTIFICATIONS was refused "
                        + "twice. LiveActivity.isSupported() reports false until the user enables "
                        + "notifications for this app in the system settings.");
                return false;
            }
            if (!hasForegroundActivity()) {
                // nothing to prompt from -- a live activity started from a background service, a
                // push, or with the app stopped. Not counted as an attempt, so the next start
                // with the app in front still asks.
                Log.w(TAG, "Cannot start a live activity: POST_NOTIFICATIONS has not been granted "
                        + "and the app is not in the foreground to request it. Start the first "
                        + "live activity while the app is visible.");
                return false;
            }
            if (isPermissionMissing(ctx)) {
                // requesting an undeclared permission is auto-denied without any UI; not counted
                // as an attempt either, so fixing the manifest is all it takes
                Log.e(TAG, "Cannot start a live activity: POST_NOTIFICATIONS is missing from the "
                        + "manifest. The build declares it for apps whose surfaces.json sets "
                        + "\"liveActivities\": true -- add that and rebuild.");
                return false;
            }
            boolean granted;
            try {
                granted = AndroidImplementation.checkForPermission(
                        "android.permission.POST_NOTIFICATIONS",
                        "This is required to show live activities", true);
            } catch (Throwable t) {
                // the request never completed, so it is not an attempt the user spent
                Log.w(TAG, "Failed to request the POST_NOTIFICATIONS permission", t);
                return false;
            }
            if (granted) {
                CN1SurfaceStore.clearNotificationPrompts(ctx);
                return true;
            }
            // counted only now that the request came back ungranted: a prompt the app died
            // during, or one that threw, is not an answer and must not spend part of the budget
            CN1SurfaceStore.recordNotificationPrompt(ctx);
            Log.w(TAG, "Live activities are unavailable for now: POST_NOTIFICATIONS was not "
                    + "granted (attempt " + CN1SurfaceStore.getNotificationPromptCount(ctx)
                    + " of " + MAX_NOTIFICATION_PROMPTS + ").");
            return false;
        }
    }

    /// True while a prompt attempt remains; see
    /// `CN1SurfaceStore#getNotificationPromptCount(Context)`.
    private static boolean canPromptAgain(Context ctx) {
        return CN1SurfaceStore.getNotificationPromptCount(ctx) < MAX_NOTIFICATION_PROMPTS;
    }

    /// True when the app has a visible activity to raise the system dialog from. The activity
    /// reference outlives `onStop`, so a non-null one proves nothing on its own -- a background
    /// fetch or a push handler running with the app stopped still sees it.
    private static boolean hasForegroundActivity() {
        android.app.Activity a = AndroidNativeUtil.getActivity();
        return a instanceof CodenameOneActivity && !((CodenameOneActivity) a).isBackground();
    }

    /// True only when the manifest was read successfully and `POST_NOTIFICATIONS` is definitely
    /// absent from it. "Definitely" is the point: a package that declares no permissions at all
    /// yields an empty or null array, which is a real answer and not a failed lookup, while a
    /// manifest that could not be read at all reports false so an unknown never blocks the prompt.
    /// Reading `PackageInfo` here rather than through
    /// `AndroidImplementation#getRequestedPermissions()` is what keeps those apart -- that helper
    /// flattens both a missing package and an unreadable one into the same empty list.
    private static boolean isPermissionMissing(Context ctx) {
        // `isSupported` consults this, and apps do call it per screen or per frame, so the binder
        // round trip is cached. A manifest cannot change under a live process -- an app update
        // kills it first -- and only a definite answer is cached, so a lookup that failed is
        // retried rather than frozen.
        int cached = permissionDeclaredState;
        if (cached != 0) {
            return cached == DECLARED_MISSING;
        }
        try {
            android.content.pm.PackageInfo info = ctx.getPackageManager().getPackageInfo(
                    ctx.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info == null) {
                return false;
            }
            String[] declared = info.requestedPermissions;
            boolean missing = true;
            if (declared != null) {
                for (String p : declared) {
                    if ("android.permission.POST_NOTIFICATIONS".equals(p)) {
                        missing = false;
                        break;
                    }
                }
            }
            permissionDeclaredState = missing ? DECLARED_MISSING : DECLARED_PRESENT;
            return missing;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasPostNotificationsPermission(Context ctx) {
        try {
            return ctx.getPackageManager().checkPermission("android.permission.POST_NOTIFICATIONS",
                    ctx.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
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
