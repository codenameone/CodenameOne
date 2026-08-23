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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/// The generic widget provider behind every Codename One widget kind. The build generates one
/// tiny subclass per kind declared in `surfaces.json` (class-name convention
/// `com.codename1.impl.android.CN1Widget_<KindId>`) whose only job is returning the kind id;
/// everything else -- reading the persisted timeline, picking the active entry, choosing a size
/// bucket, rendering, and scheduling entry flips -- lives here.
///
/// Entry flips are scheduled with an *inexact* `AlarmManager.setWindow` (30 second window): no
/// `SCHEDULE_EXACT_ALARM` permission is required and second-precision countdowns are covered by
/// the natively ticking `Chronometer`, not by re-renders. Apps that need to-the-second entry
/// flips can opt in with the `android.surfaces.exactAlarms=true` build hint (default `false`):
/// the build then declares `SCHEDULE_EXACT_ALARM` and records the choice in the
/// `com.codename1.surfaces.EXACT_ALARMS` application meta-data entry this provider reads. With
/// the hint on, flips use `setExactAndAllowWhileIdle` -- on Android 12+ (API 31) only while
/// `AlarmManager.canScheduleExactAlarms()` reports the special app access is still granted,
/// silently falling back to the inexact window when the user revokes it; below API 31 exact
/// alarms need no special access. When an `atEnd` timeline is exhausted
/// (or nothing was published yet) the last known content stays on screen while the widget pulls
/// the app: if the app declares `com.codename1.background.BackgroundFetch` its fetch service is
/// started -- throttled to once per 15 minutes per kind -- so it can fetch data and re-publish
/// without any UI running. Dark-mode colors resolve at render time, so a light/dark switch shows
/// up on the next update rather than instantly.
public abstract class CN1WidgetProvider extends AppWidgetProvider {
    /// Broadcast action used for self-scheduled timeline entry flips.
    public static final String ACTION_NEXT_ENTRY = "com.codename1.surfaces.NEXT_ENTRY";
    private static final String TAG = "CN1Surfaces";
    private static final int FLAG_IMMUTABLE = 0x04000000;
    private static final long FLIP_WINDOW_MILLIS = 30000;
    private static final long FETCH_THROTTLE_MILLIS = 15L * 60 * 1000;

    /// Returns the widget kind id this provider renders; implemented by the generated
    /// per-kind subclass.
    protected abstract String getKindId();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        renderAll(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_NEXT_ENTRY.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            renderAll(context, mgr, mgr.getAppWidgetIds(new ComponentName(context, getClass())));
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // re-render for the new size bucket
        renderAll(context, appWidgetManager, new int[]{appWidgetId});
    }

    private void renderAll(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        String kindId = getKindId();
        String json = CN1SurfaceStore.readWidgetTimeline(context, kindId);
        if (json == null) {
            // nothing published yet; keep the initial placeholder layout but ask the app
            // (when it declares background fetch) to produce content
            requestAppRefresh(context, kindId);
            return;
        }
        try {
            JSONObject doc = new JSONObject(json);
            JSONObject layouts = doc.optJSONObject("layouts");
            JSONArray entries = doc.optJSONArray("entries");
            if (layouts == null || layouts.length() == 0) {
                return;
            }
            long now = System.currentTimeMillis();
            JSONObject active = pickActiveEntry(entries, now);
            JSONObject state = active == null ? null : active.optJSONObject("state");
            java.io.File imagesDir = CN1SurfaceStore.kindDir(context, kindId);
            for (int appWidgetId : appWidgetIds) {
                JSONObject layout = pickLayout(layouts, mgr, appWidgetId);
                if (layout == null) {
                    continue;
                }
                RemoteViews rv = CN1SurfaceRenderer.render(context, layout, state, kindId,
                        imagesDir);
                mgr.updateAppWidget(appWidgetId, rv);
            }
            long nextFlip = nextFlipDate(entries, now);
            scheduleNextFlip(context, nextFlip);
            if (nextFlip == 0 && "atEnd".equals(doc.optString("reload", "atEnd"))) {
                // reload=atEnd and the timeline is exhausted: the last entry stays on
                // screen while the app is asked (throttled) to republish fresh content
                requestAppRefresh(context, kindId);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to render widget kind " + kindId, t);
        }
    }

    /// Widget-driven refresh, the counterpart of the app-driven publish: starts the app's
    /// background fetch service so `com.codename1.background.BackgroundFetch` can fetch data
    /// and re-publish while no UI (and possibly no app process) exists. Only fires when the
    /// app actually declares background fetch -- the bridge records the listener class on
    /// publish -- and at most once per 15 minutes per kind. Failures are swallowed: modern
    /// Android may refuse a background service start, in which case the widget simply keeps
    /// showing the last entry until the app's own fetch schedule catches up.
    /// Package-private rather than private: a Tile reaching the end of its timeline needs the
    /// same throttled request, and reimplementing it there would give the two surfaces different
    /// refresh behaviour for one published document.
    /**
     * Asks for the same background fetch, but AT a stated moment rather than now.
     *
     * <p>A complication is handed its whole timeline once and the system swaps entries itself, so
     * nothing calls the provider when the last entry finally takes over -- which is exactly when
     * a reload-at-end timeline wants more content. Asking at build time instead can spend the
     * one throttled fetch hours early and republish over entries the user has not seen yet.</p>
     *
     * <p>An alarm carrying the same broadcast the immediate path sends. It targets
     * BackgroundFetchHandler, which every manifest that has background fetch already declares --
     * so this needs no new component -- and an alarm survives the process the way a posted
     * Runnable does not.</p>
     *
     * @param context any context
     * @param kindId the kind wanting fresh content
     * @param whenMillis when the timeline runs out
     */
    static void scheduleAppRefresh(Context context, String kindId, long whenMillis) {
        try {
            String listenerClass = CN1SurfaceStore.getBackgroundFetchClass(context);
            if (listenerClass == null) {
                // A mirrored kind on the watch; see requestAppRefresh. The phone is asked NOW
                // rather than at the timeline's end, because the alarm below needs a local
                // component to deliver to and this build has none -- the throttle is what keeps
                // that from being chatty. Asking early costs one phone-side publish; not asking
                // leaves the complication on its final entry.
                CN1WatchSurfaceNotifier.requestPhoneReload(context, kindId);
                return;
            }
            if (whenMillis <= System.currentTimeMillis()) {
                return;
            }
            // The cast sits INSIDE the instanceof branch, which is the shape the cast-semantics
            // verifier recognises -- and the reason for the rule is real here: a failed CHECKCAST
            // does not throw on ParparVM, so the catch below would never run for one.
            Object service = context.getSystemService(Context.ALARM_SERVICE);
            if (service instanceof AlarmManager) {
                scheduleFetchAlarm(context, (AlarmManager) service, kindId, listenerClass,
                        whenMillis);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not schedule the reload-at-end fetch for " + kindId, t);
        }
    }

    /// The alarm itself, once the manager is known to be one. Separate so the cast above is the
    /// last thing its own method does and no cast sits under the catch.
    private static void scheduleFetchAlarm(Context context, AlarmManager am, String kindId,
            String listenerClass, long whenMillis) {
        try {
            Intent intent = new Intent(context,
                    com.codename1.impl.android.BackgroundFetchHandler.class);
            intent.setData(android.net.Uri.parse("http://codenameone.com/a?" + listenerClass));
            // A SERVICE PendingIntent. BackgroundFetchHandler is an IntentService declared as a
            // <service>, so a broadcast one names a receiver that does not exist and the alarm
            // fires into nothing. The port's own helper is used rather than a hand-rolled call,
            // so the flags match what every other alarm-delivered start of this same handler
            // uses. An alarm briefly allowlists the app, which is what lets the service start
            // from here at all on API 26+.
            //
            // Keyed by kind so two kinds do not replace each other's wake-up, and distinct from
            // the flip alarm's own request code for the same reason.
            PendingIntent pi = com.codename1.impl.android.AndroidImplementation.getPendingIntent(
                    context, ("reloadAtEnd:" + kindId).hashCode(), intent);
            // INEXACT deliberately. This is "some time after the timeline runs out", not a
            // deadline, and an exact alarm costs the user a special permission for no benefit.
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC, whenMillis, pi);
            } else {
                am.set(AlarmManager.RTC, whenMillis, pi);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not schedule the reload-at-end fetch for " + kindId, t);
        }
    }

    static void requestAppRefresh(Context context, String kindId) {
        requestAppRefresh(context, kindId, true);
    }

    /**
     * As above, but able to refuse the peer fallback.
     *
     * <p>{@code mayAskPeer} is false when this IS the answer to a peer's request. Without that
     * the two devices bounce: a watch with no listener asks the phone, a phone with no listener
     * answers by asking the watch, and neither ever acquires one -- an unthrottled message loop
     * waking both processes until they disconnect. The device that was asked either has content
     * to produce or has nothing to say, and saying nothing is the end of it.</p>
     *
     * @param context any context
     * @param kindId the kind wanting fresh content
     * @param mayAskPeer whether a device with no listener of its own may ask the other one
     */
    static void requestAppRefresh(Context context, String kindId, boolean mayAskPeer) {
        try {
            String listenerClass = CN1SurfaceStore.getBackgroundFetchClass(context);
            if (listenerClass == null) {
                if (!mayAskPeer) {
                    // Answering a peer. It asked because it has nothing; this device has nothing
                    // either, so there is no one left to ask.
                    return;
                }
                // Nothing local to run. On a WATCH this is the normal case for a mirrored kind:
                // the preference is recorded by publishWidgetTimeline, which the watch never
                // runs -- its descriptors arrive through CN1SurfaceMirror.receive instead. The
                // content belongs to the phone, so the phone is who to ask, and the request goes
                // back over the same Data Layer the descriptor came down. A no-op everywhere
                // else, including a phone with no background fetch declared.
                CN1WatchSurfaceNotifier.requestPhoneReload(context, kindId);
                return;
            }
            if (!CN1SurfaceStore.tryClaimBackgroundFetch(context, kindId,
                    System.currentTimeMillis(), FETCH_THROTTLE_MILLIS)) {
                return;
            }
            Intent intent = new Intent(context,
                    com.codename1.impl.android.BackgroundFetchHandler.class);
            // same wire format as the alarm-driven fetch path: the listener class rides in
            // the data URI (an old putExtra bug workaround the handler still expects)
            intent.setData(android.net.Uri.parse("http://codenameone.com/a?" + listenerClass));
            // legal here: a broadcast receiver executing onReceive counts as foreground,
            // so the service start is exempt from background execution limits
            context.startService(intent);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to request a background refresh for widget kind " + kindId, t);
        }
    }

    /// Returns the latest entry whose date has passed, or the first entry when none has.
    private static JSONObject pickActiveEntry(JSONArray entries, long now) {
        if (entries == null || entries.length() == 0) {
            return null;
        }
        JSONObject active = entries.optJSONObject(0);
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.optJSONObject(i);
            if (e != null && e.optLong("date") <= now) {
                active = e;
            }
        }
        return active;
    }

    private static long nextFlipDate(JSONArray entries, long now) {
        long next = 0;
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject e = entries.optJSONObject(i);
                if (e == null) {
                    continue;
                }
                long date = e.optLong("date");
                if (date > now && (next == 0 || date < next)) {
                    next = date;
                }
            }
        }
        return next;
    }

    private static JSONObject pickLayout(JSONObject layouts, AppWidgetManager mgr,
            int appWidgetId) {
        String bucket = "medium";
        try {
            Bundle options = mgr.getAppWidgetOptions(appWidgetId);
            if (options != null) {
                int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
                int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
                if (minW >= 250 && minH >= 250) {
                    bucket = "large";
                } else if (minW > 0 && minW < 250) {
                    bucket = "small";
                }
            }
        } catch (Throwable ignore) {
        }
        JSONObject layout = layouts.optJSONObject(bucket);
        if (layout == null) {
            layout = layouts.optJSONObject("default");
        }
        if (layout == null) {
            String[] fallbacks = {"medium", "small", "large", "lockscreen"};
            for (String fallback : fallbacks) {
                layout = layouts.optJSONObject(fallback);
                if (layout != null) {
                    break;
                }
            }
        }
        return layout;
    }

    private void scheduleNextFlip(Context context, long next) {
        if (next <= 0) {
            return;
        }
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }
            Intent intent = new Intent(context, getClass());
            intent.setAction(ACTION_NEXT_ENTRY);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getBroadcast(context, getKindId().hashCode(),
                    intent, flags);
            if (exactAlarmsRequested(context) && canScheduleExactAlarms(am)) {
                if (Build.VERSION.SDK_INT >= 23) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC, next, pi);
                } else if (Build.VERSION.SDK_INT >= 19) {
                    am.setExact(AlarmManager.RTC, next, pi);
                } else {
                    am.set(AlarmManager.RTC, next, pi);
                }
            } else if (Build.VERSION.SDK_INT >= 19) {
                am.setWindow(AlarmManager.RTC, next, FLIP_WINDOW_MILLIS, pi);
            } else {
                am.set(AlarmManager.RTC, next, pi);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to schedule the next timeline entry flip", t);
        }
    }

    /// True when the build injected the `com.codename1.surfaces.EXACT_ALARMS` application
    /// meta-data entry, i.e. the app opted in with the `android.surfaces.exactAlarms` build
    /// hint. Any failure reads as "not requested" so the inexact default keeps working.
    private static boolean exactAlarmsRequested(Context context) {
        try {
            android.content.pm.ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            android.content.pm.PackageManager.GET_META_DATA);
            return ai != null && ai.metaData != null
                    && ai.metaData.getBoolean("com.codename1.surfaces.EXACT_ALARMS", false);
        } catch (Throwable t) {
            return false;
        }
    }

    /// On Android 12+ (API 31) `SCHEDULE_EXACT_ALARM` is special app access the user can
    /// revoke, so `AlarmManager.canScheduleExactAlarms()` gates every exact schedule; the
    /// method is invoked reflectively because the port compiles against an older SDK. Below
    /// API 31 exact alarms need no special access.
    private static boolean canScheduleExactAlarms(AlarmManager am) {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        try {
            Object can = am.getClass().getMethod("canScheduleExactAlarms").invoke(am);
            return Boolean.TRUE.equals(can);
        } catch (Throwable t) {
            return false;
        }
    }
}
