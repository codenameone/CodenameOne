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
package com.codename1.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Display;

/// Delivers background location updates and geofence transitions requested by
/// [AndroidLocationManager] through the platform location service.
///
/// The counterpart of `BackgroundLocationBroadcastReceiver`, which reads Play
/// services results and is compiled only into applications that bundle the Play
/// services location library. This one names no Google type, so it ships in every
/// application and works on a device without Play services.
///
/// The listener is named by class in the intent and constructed here, because the
/// application may not be running: the location service starts the process for a
/// background update or a geofence transition.
public class FrameworkLocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String listenerClass = intent.getStringExtra(AndroidLocationManager.EXTRA_LISTENER_CLASS);
        if (listenerClass == null) {
            return;
        }
        String action = intent.getAction();
        if (AndroidLocationManager.ACTION_BACKGROUND.equals(action)) {
            Object value = intent.getExtras() == null ? null
                    : intent.getExtras().get(android.location.LocationManager.KEY_LOCATION_CHANGED);
            if (value instanceof android.location.Location) {
                deliverLocation(context, listenerClass,
                        AndroidLocationManager.convert((android.location.Location) value));
            }
        } else if (AndroidLocationManager.ACTION_GEOFENCE.equals(action)) {
            Uri data = intent.getData();
            if (data == null
                    || !intent.hasExtra(android.location.LocationManager.KEY_PROXIMITY_ENTERING)) {
                return;
            }
            deliverTransition(context, listenerClass, data.getSchemeSpecificPart(),
                    intent.getBooleanExtra(android.location.LocationManager.KEY_PROXIMITY_ENTERING, false));
        }
    }

    private static Object newListener(String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (Throwable err) {
            android.util.Log.e("Codename One", "Cannot create location listener " + className, err);
            return null;
        }
    }

    // The listener runs synchronously in onReceive, as BackgroundLocationBroadcastReceiver
    // runs it on the Play services path: the background listener contract is a short
    // callback, and both paths keep the same threading so an application sees one.
    private static void deliverLocation(Context context, String className, Location location) {
        boolean stopWhenDone = startIfNeeded(context);
        try {
            Object listener = newListener(className);
            if (listener instanceof LocationListener) {
                ((LocationListener) listener).locationUpdated(location);
            }
        } catch (Throwable err) {
            android.util.Log.e("Codename One", "background location error", err);
        } finally {
            if (stopWhenDone) {
                AndroidImplementation.stopContext(context);
            }
        }
    }

    private static void deliverTransition(Context context, String className, String id,
            boolean entering) {
        boolean stopWhenDone = startIfNeeded(context);
        try {
            Object listener = newListener(className);
            if (listener instanceof GeofenceListener) {
                if (entering) {
                    ((GeofenceListener) listener).onEntered(id);
                } else {
                    ((GeofenceListener) listener).onExit(id);
                }
            }
        } catch (Throwable err) {
            android.util.Log.e("Codename One", "geofence error", err);
        } finally {
            if (stopWhenDone) {
                AndroidImplementation.stopContext(context);
            }
        }
    }

    private static boolean startIfNeeded(Context context) {
        if (Display.isInitialized()) {
            return false;
        }
        AndroidImplementation.startContext(context);
        return true;
    }
}
