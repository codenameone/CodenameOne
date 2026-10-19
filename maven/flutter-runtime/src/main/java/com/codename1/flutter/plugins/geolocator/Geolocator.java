/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.geolocator;

import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.ui.Display;

import dart.async.Completer;
import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;

/**
 * geolocator, over Codename One's LocationManager. The platform asks for the location
 * permission itself the first time a location is requested, so a running app reports
 * {@code whileInUse}; without a display there is no location service at all, so
 * permission is {@code denied} and a position request fails with
 * LocationServiceDisabledException, as geolocator does when services are off.
 */
public final class Geolocator {
    private static final double EARTH_RADIUS_M = 6378137.0;

    private Geolocator() {
    }

    private static LocationManager manager() {
        return Display.isInitialized() ? LocationManager.getLocationManager() : null;
    }

    public static Future<Boolean> isLocationServiceEnabled() {
        LocationManager lm = manager();
        boolean on = lm != null && (!lm.isGPSDetectionSupported() || lm.isGPSEnabled());
        return Future.value(Boolean.valueOf(on));
    }

    public static Future<LocationPermission> checkPermission() {
        return Future.value(manager() == null ? LocationPermission.denied : LocationPermission.whileInUse);
    }

    public static Future<LocationPermission> requestPermission() {
        return checkPermission();
    }

    public static Future<Position> getCurrentPosition(Object desiredAccuracy, Object forceAndroidLocationManager,
            Object timeLimit, LocationSettings locationSettings) {
        final LocationManager lm = manager();
        if (lm == null) {
            return Future.error(new LocationServiceDisabledException());
        }
        final Completer<Position> done = new Completer<Position>();
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                final Location l = lm.getCurrentLocationSync(30000);
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (l == null) {
                            done.completeError(new LocationServiceDisabledException());
                        } else {
                            done.complete(Position.of(l));
                        }
                    }
                });
            }
        });
        return done.future();
    }

    public static Future<Position> getLastKnownPosition(Object forceAndroidLocationManager) {
        LocationManager lm = manager();
        Location l = lm == null ? null : lm.getLastKnownLocation();
        return Future.value(l == null ? null : Position.of(l));
    }

    /** Positions as the device reports them, while anything listens. */
    public static Stream<Position> getPositionStream(LocationSettings locationSettings) {
        final StreamController<Position> out = StreamController.broadcast();
        LocationManager lm = manager();
        if (lm != null) {
            lm.setLocationListener(new LocationListener() {
                @Override
                public void locationUpdated(Location location) {
                    out.add(Position.of(location));
                }

                @Override
                public void providerStateChanged(int newState) {
                }
            });
        }
        return out.stream();
    }

    /** Codename One has no portable way to open the system settings; answers false. */
    public static Future<Boolean> openAppSettings() {
        return Future.value(Boolean.FALSE);
    }

    public static Future<Boolean> openLocationSettings() {
        return openAppSettings();
    }

    /** The great-circle distance in meters, by the haversine formula geolocator uses. */
    public static double distanceBetween(double startLatitude, double startLongitude,
            double endLatitude, double endLongitude) {
        double dLat = Math.toRadians(endLatitude - startLatitude);
        double dLon = Math.toRadians(endLongitude - startLongitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(startLatitude)) * Math.cos(Math.toRadians(endLatitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * com.codename1.util.MathUtil.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /** The initial bearing in degrees from the start to the end point. */
    public static double bearingBetween(double startLatitude, double startLongitude,
            double endLatitude, double endLongitude) {
        double lat1 = Math.toRadians(startLatitude);
        double lat2 = Math.toRadians(endLatitude);
        double dLon = Math.toRadians(endLongitude - startLongitude);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return Math.toDegrees(com.codename1.util.MathUtil.atan2(y, x));
    }
}
