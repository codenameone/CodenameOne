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
        // The name the build gave THIS kind, from the map it wrote. Trying candidates instead
        // would be wrong here for the same reason it is wrong for the widget provider: the plain
        // name may well exist and belong to a different kind.
        String suffix = AndroidSurfaceBridge.classSuffix(ctx, kindId);
        requestComplicationUpdate(ctx, "com.codename1.impl.android.CN1Complication_" + suffix);
        requestTileUpdate(ctx, "com.codename1.impl.android.CN1Tile_" + suffix);
    }

    /**
     * Asks the PHONE to publish this kind again, for a watch that has no content of its own.
     *
     * <p>A mirrored kind's descriptors are produced on the phone and sent down, so a watch asking
     * itself for fresh content asks the wrong device -- and it has no background-fetch listener
     * recorded anyway, that preference being written by the publish path the watch never runs.
     * The request goes back over the Data Layer the descriptor came down.</p>
     *
     * <p>Reflective for the same reason the update requesters are: CN1WearableBridge is injected
     * by the build and is simply absent from a project that declares no wearable link, where
     * there is no phone half to ask.</p>
     *
     * @param ctx any context
     * @param kindId the kind wanting fresh content
     */
    static void requestPhoneReload(Context ctx, String kindId) {
        try {
            Class<?> bridge = Class.forName("com.codename1.impl.android.CN1WearableBridge");
            bridge.getMethod("requestSurfaceReload", Context.class, String.class)
                    .invoke(null, ctx, kindId);
        } catch (ClassNotFoundException expected) {
            // No wearable link in this build, so there is no phone half to ask.
        } catch (NoSuchMethodException expected) {
            // An older injected bridge. The watch keeps what it has, as it did before.
        } catch (Throwable t) {
            Log.w(TAG, "Could not ask the phone to republish " + kindId, t);
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
