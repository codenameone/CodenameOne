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

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Asks a Wear OS watch face and tile carousel to re-read what this app just published.
 *
 * <p>Reflective on purpose. {@code ComplicationDataSourceUpdateRequester} and
 * {@code TileService.getUpdater} live in {@code androidx.wear}, which the Android port must not
 * depend on -- an app that publishes no complication should carry neither library. The generated
 * service classes it looks for are only present in a watch build that declared watch families,
 * so on a phone every lookup simply misses.</p>
 *
 * <p>Same shape as {@code AndroidWearableSupport}'s reflective bridge lookup: a miss is expected
 * and answered with silence, and a genuine failure is one warning rather than an exception into
 * a caller that has already done its real work.</p>
 */
public final class CN1WatchSurfaceNotifier {

    private static final String TAG = "CN1Surfaces";

    private CN1WatchSurfaceNotifier() {
    }

    /**
     * Requests a refresh of everything showing a kind.
     *
     * @param ctx any context
     * @param kindId the widget kind that was just published
     */
    public static void requestUpdate(Context ctx, String kindId) {
        if (ctx == null || kindId == null) {
            return;
        }
        // Every name the build could have given this kind. Both requesters treat a missing
        // class as "no such surface" and say nothing, so trying the candidates costs a failed
        // Class.forName in the rare disambiguated case and nothing at all in the usual one.
        for (String suffix : AndroidSurfaceBridge.classSuffixCandidates(kindId)) {
            requestComplicationUpdate(ctx, "com.codename1.impl.android.CN1Complication_" + suffix);
            requestTileUpdate(ctx, "com.codename1.impl.android.CN1Tile_" + suffix);
        }
    }

    private static void requestComplicationUpdate(Context ctx, String className) {
        try {
            Class<?> service = Class.forName(className);
            Class<?> requester = Class.forName("androidx.wear.watchface.complications.datasource."
                    + "ComplicationDataSourceUpdateRequester");
            Method create = requester.getMethod("create", Context.class, ComponentName.class);
            Object instance = create.invoke(null, ctx,
                    new ComponentName(ctx, service));
            requester.getMethod("requestUpdateAll").invoke(instance);
        } catch (ClassNotFoundException expected) {
            // No complication for this kind, or not a watch build. Nothing to say.
        } catch (Throwable t) {
            Log.w(TAG, "Could not request a complication update for " + className, t);
        }
    }

    private static void requestTileUpdate(Context ctx, String className) {
        try {
            Class<?> service = Class.forName(className);
            Class<?> tileService = Class.forName("androidx.wear.tiles.TileService");
            Method getUpdater = tileService.getMethod("getUpdater", Context.class);
            Object updater = getUpdater.invoke(null, ctx);
            updater.getClass().getMethod("requestUpdate", Class.class).invoke(updater, service);
        } catch (ClassNotFoundException expected) {
            // No Tile for this kind, or not a watch build.
        } catch (Throwable t) {
            Log.w(TAG, "Could not request a Tile update for " + className, t);
        }
    }

}
