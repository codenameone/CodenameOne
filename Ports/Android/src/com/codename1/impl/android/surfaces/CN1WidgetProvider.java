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
/// the natively ticking `Chronometer`, not by re-renders. When an `atEnd` timeline is exhausted
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
    private static void requestAppRefresh(Context context, String kindId) {
        try {
            String listenerClass = CN1SurfaceStore.getBackgroundFetchClass(context);
            if (listenerClass == null) {
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
            if (Build.VERSION.SDK_INT >= 19) {
                am.setWindow(AlarmManager.RTC, next, FLIP_WINDOW_MILLIS, pi);
            } else {
                am.set(AlarmManager.RTC, next, pi);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to schedule the next timeline entry flip", t);
        }
    }
}
