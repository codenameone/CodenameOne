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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.spi.SurfaceBridge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// The Android implementation of the surfaces SPI. Published timelines are persisted through
/// `CN1SurfaceStore` (widgets render from broadcast receivers while the app process may be dead)
/// and the matching generated provider -- class-name convention
/// `com.codename1.impl.android.CN1Widget_<KindId>`, e.g. kind `delivery_status` maps to
/// `CN1Widget_DeliveryStatus` -- is poked with an `ACTION_APPWIDGET_UPDATE` broadcast. Live
/// activities lower to ongoing notifications through `CN1LiveActivityManager`.
///
/// Surface taps arrive through `CN1SurfaceActionActivity`, are queued here and drained into
/// `com.codename1.surfaces.Surfaces.dispatchAction` (which performs its own EDT marshaling and
/// cold-start queuing); `AndroidImplementation.deliverPendingSurfaceActions()` re-drains after
/// the app finishes starting as a safety net.
public class AndroidSurfaceBridge implements SurfaceBridge {
    private static final String TAG = "CN1Surfaces";
    private static final List<String[]> pendingActions = new ArrayList<String[]>();

    @Override
    public boolean areWidgetsSupported() {
        return context() != null;
    }

    @Override
    public boolean isLiveActivitySupported() {
        return CN1LiveActivityManager.isSupported(context());
    }

    @Override
    public void registerWidgetKind(String kindJson) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        try {
            JSONObject kind = new JSONObject(kindJson);
            String kindId = kind.optString("id", "");
            if (kindId.length() == 0) {
                return;
            }
            CN1SurfaceStore.rememberKind(ctx, kindId);
            // Validate the runtime registration against what was compiled into the app from
            // surfaces.json: the generated provider must be a manifest-declared receiver.
            ComponentName provider = providerComponent(ctx, kindId);
            try {
                ctx.getPackageManager().getReceiverInfo(provider, 0);
            } catch (Exception missing) {
                Log.e(TAG, "Widget kind '" + kindId + "' was registered at runtime but is not "
                        + "declared in surfaces.json; the build compiles widget kinds into the "
                        + "app, so this kind cannot appear in the widget gallery. Add it to "
                        + "surfaces.json and rebuild.");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to register widget kind", t);
        }
    }

    @Override
    public void publishWidgetTimeline(String kindId, String timelineJson,
            Map<String, byte[]> images) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        try {
            CN1SurfaceStore.writeWidgetTimeline(ctx, kindId, timelineJson, images);
            CN1SurfaceStore.rememberKind(ctx, kindId);
            // remember the app's BackgroundFetch listener (null when the app declares none)
            // so a widget rendering an exhausted timeline can pull fresh content itself
            CN1SurfaceStore.rememberBackgroundFetchClass(ctx,
                    AndroidImplementation.getBackgroundFetchListenerClassName());
            broadcastUpdate(ctx, kindId);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to publish the timeline of widget kind " + kindId, t);
        }
    }

    @Override
    public void reloadWidgets(String kindId) {
        Context ctx = context();
        if (ctx == null) {
            return;
        }
        if (kindId != null) {
            broadcastUpdate(ctx, kindId);
            return;
        }
        for (String kind : CN1SurfaceStore.getRememberedKinds(ctx)) {
            broadcastUpdate(ctx, kind);
        }
    }

    @Override
    public int getInstalledWidgetCount(String kindId) {
        Context ctx = context();
        if (ctx == null) {
            return 0;
        }
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(providerComponent(ctx, kindId));
            return ids == null ? 0 : ids.length;
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public String startLiveActivity(String descriptorJson, Map<String, byte[]> images) {
        return CN1LiveActivityManager.start(context(), descriptorJson, images);
    }

    @Override
    public void updateLiveActivity(String activityId, String stateJson) {
        CN1LiveActivityManager.update(context(), activityId, stateJson);
    }

    @Override
    public void endLiveActivity(String activityId, String finalStateJson,
            boolean dismissImmediately) {
        CN1LiveActivityManager.end(context(), activityId, finalStateJson, dismissImmediately);
    }

    // --- action delivery ------------------------------------------------------

    /// Queues a surface action decoded by the trampoline activity and drains the queue
    /// immediately; `Surfaces.dispatchAction` performs its own cold-start queuing so dispatching
    /// as soon as the classes are loadable is safe.
    public static void postAction(String source, String actionId, String paramsJson) {
        synchronized (pendingActions) {
            pendingActions.add(new String[]{source, actionId, paramsJson});
        }
        deliverPendingActions();
    }

    /// Drains queued surface actions into the framework dispatcher. Invoked from `postAction`
    /// and again from `AndroidImplementation.deliverPendingSurfaceActions()` once the app has
    /// finished starting.
    public static void deliverPendingActions() {
        List<String[]> drained;
        synchronized (pendingActions) {
            if (pendingActions.isEmpty()) {
                return;
            }
            drained = new ArrayList<String[]>(pendingActions);
            pendingActions.clear();
        }
        for (String[] action : drained) {
            try {
                Surfaces.dispatchAction(action[0], action[1], parseParams(action[2]));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to dispatch a surface action", t);
            }
        }
    }

    // --- helpers --------------------------------------------------------------

    /// Maps a widget kind id to the simple name suffix of its generated provider class:
    /// underscore-separated words become CamelCase (`delivery_status` -> `DeliveryStatus`).
    /// The identical logic lives in the Android builder's widget codegen; keep them in sync.
    static String toClassSuffix(String kindId) {
        StringBuilder sb = new StringBuilder(kindId.length());
        boolean upper = true;
        for (int i = 0; i < kindId.length(); i++) {
            char c = kindId.charAt(i);
            if (c == '_') {
                upper = true;
                continue;
            }
            if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static ComponentName providerComponent(Context ctx, String kindId) {
        return new ComponentName(ctx.getPackageName(),
                "com.codename1.impl.android.CN1Widget_" + toClassSuffix(kindId));
    }

    private static void broadcastUpdate(Context ctx, String kindId) {
        try {
            ComponentName provider = providerComponent(ctx, kindId);
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(provider);
            if (ids == null || ids.length == 0) {
                return;
            }
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.setComponent(provider);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            ctx.sendBroadcast(intent);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to broadcast a widget update for kind " + kindId, t);
        }
    }

    private static Map<String, Object> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.length() == 0) {
            return null;
        }
        try {
            return toMap(new JSONObject(paramsJson));
        } catch (Throwable t) {
            Log.w(TAG, "Failed to parse surface action parameters", t);
            return null;
        }
    }

    private static Map<String, Object> toMap(JSONObject o) {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<?> keys = o.keys();
        while (keys.hasNext()) {
            String key = String.valueOf(keys.next());
            map.put(key, convert(o.opt(key)));
        }
        return map;
    }

    private static Object convert(Object v) {
        if (v instanceof JSONObject) {
            return toMap((JSONObject) v);
        }
        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            List<Object> list = new ArrayList<Object>(a.length());
            for (int i = 0; i < a.length(); i++) {
                list.add(convert(a.opt(i)));
            }
            return list;
        }
        if (v == JSONObject.NULL) {
            return null;
        }
        return v;
    }

    private static Context context() {
        return AndroidNativeUtil.getContext();
    }
}
