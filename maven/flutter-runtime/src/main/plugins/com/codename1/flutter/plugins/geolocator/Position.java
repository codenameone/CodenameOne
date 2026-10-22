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

import dart.core.DartMap;
import dart.core.DateTime;

/** geolocator's {@code Position}. */
public class Position {
    private final double latitude;
    private final double longitude;
    private final DateTime timestamp;
    private final double accuracy;
    private final double altitude;
    private final double heading;
    private final double speed;

    public Position(double latitude, double longitude, DateTime timestamp, double accuracy,
            double altitude, double altitudeAccuracy, double heading, double headingAccuracy,
            double speed, double speedAccuracy, Object floor, Boolean isMocked) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.accuracy = accuracy;
        this.altitude = altitude;
        this.heading = heading;
        this.speed = speed;
    }

    static Position of(com.codename1.location.Location l) {
        return new Position(l.getLatitude(), l.getLongitude(),
                DateTime.fromMillisecondsSinceEpoch(l.getTimeStamp(), false), l.getAccuracy(),
                l.getAltitude(), 0, l.getDirection(), 0, l.getVelocity(), 0, null, Boolean.FALSE);
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public DateTime timestamp() {
        return timestamp;
    }

    public double accuracy() {
        return accuracy;
    }

    public double altitude() {
        return altitude;
    }

    public double altitudeAccuracy() {
        return 0;
    }

    public double heading() {
        return heading;
    }

    public double headingAccuracy() {
        return 0;
    }

    public double speed() {
        return speed;
    }

    public double speedAccuracy() {
        return 0;
    }

    public boolean isMocked() {
        return false;
    }

    public DartMap<String, Object> toJson() {
        return DartMap.of("latitude", Double.valueOf(latitude), "longitude", Double.valueOf(longitude),
                "timestamp", timestamp == null ? null : Long.valueOf(timestamp.millisecondsSinceEpoch()),
                "accuracy", Double.valueOf(accuracy), "altitude", Double.valueOf(altitude),
                "heading", Double.valueOf(heading), "speed", Double.valueOf(speed));
    }

    @Override
    public String toString() {
        return "Latitude: " + latitude + ", Longitude: " + longitude;
    }
}
