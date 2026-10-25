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
package android.location;

/// A geographic fix, as Android describes one. Fixes reported by
/// [LocationManager] are converted from `com.codename1.location.Location`;
/// the `has*` flags say which of the optional values the platform supplied.
public class Location {

    private static final double EARTH_RADIUS_METERS = 6371008.8;

    private String provider;
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private float bearing;
    private float speed;
    private long time;
    private long elapsedRealtimeNanos;
    private boolean hasAltitude;
    private boolean hasAccuracy;
    private boolean hasBearing;
    private boolean hasSpeed;
    private android.os.Bundle extras;

    public Location(String provider) {
        this.provider = provider;
    }

    public Location(Location l) {
        set(l);
    }

    public void set(Location l) {
        provider = l.provider;
        latitude = l.latitude;
        longitude = l.longitude;
        altitude = l.altitude;
        accuracy = l.accuracy;
        bearing = l.bearing;
        speed = l.speed;
        time = l.time;
        elapsedRealtimeNanos = l.elapsedRealtimeNanos;
        hasAltitude = l.hasAltitude;
        hasAccuracy = l.hasAccuracy;
        hasBearing = l.hasBearing;
        hasSpeed = l.hasSpeed;
        extras = l.extras == null ? null : new android.os.Bundle(l.extras);
    }

    public void reset() {
        set(new Location((String) null));
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean hasAltitude() {
        return hasAltitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
        hasAltitude = true;
    }

    public void removeAltitude() {
        altitude = 0;
        hasAltitude = false;
    }

    public boolean hasAccuracy() {
        return hasAccuracy;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
        hasAccuracy = true;
    }

    public void removeAccuracy() {
        accuracy = 0;
        hasAccuracy = false;
    }

    public boolean hasBearing() {
        return hasBearing;
    }

    public float getBearing() {
        return bearing;
    }

    /// Normalized into [0, 360), as Android does.
    public void setBearing(float bearing) {
        float b = bearing % 360f;
        if (b < 0) {
            b += 360f;
        }
        this.bearing = b;
        hasBearing = true;
    }

    public void removeBearing() {
        bearing = 0;
        hasBearing = false;
    }

    public boolean hasSpeed() {
        return hasSpeed;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        hasSpeed = true;
    }

    public void removeSpeed() {
        speed = 0;
        hasSpeed = false;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getElapsedRealtimeNanos() {
        return elapsedRealtimeNanos;
    }

    public void setElapsedRealtimeNanos(long nanos) {
        elapsedRealtimeNanos = nanos;
    }

    public long getElapsedRealtimeMillis() {
        return elapsedRealtimeNanos / 1000000L;
    }

    public android.os.Bundle getExtras() {
        return extras;
    }

    public void setExtras(android.os.Bundle extras) {
        this.extras = extras;
    }

    /// No mock providers exist here.
    public boolean isMock() {
        return false;
    }

    public boolean isFromMockProvider() {
        return false;
    }

    public float distanceTo(Location dest) {
        float[] r = new float[2];
        computeDistanceAndBearing(latitude, longitude, dest.latitude, dest.longitude, r);
        return r[0];
    }

    public float bearingTo(Location dest) {
        float[] r = new float[2];
        computeDistanceAndBearing(latitude, longitude, dest.latitude, dest.longitude, r);
        return r[1];
    }

    /// `results[0]` receives the distance in meters, `results[1]` (when the
    /// array is long enough) the initial bearing and `results[2]` the final
    /// bearing, in degrees east of true north.
    ///
    /// Android solves Vincenty's inverse problem on the WGS84 ellipsoid; this
    /// uses the haversine formula on a sphere of the mean earth radius, which
    /// differs from it by up to about 0.5%. That is well inside GPS error for
    /// the distances a phone measures, but a survey-grade comparison against a
    /// value Android computed will not match to the meter.
    public static void distanceBetween(double startLatitude, double startLongitude,
            double endLatitude, double endLongitude, float[] results) {
        if (results == null || results.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        computeDistanceAndBearing(startLatitude, startLongitude, endLatitude, endLongitude, results);
    }

    private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2,
            float[] results) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = p2 - p1;
        double dl = Math.toRadians(lon2 - lon1);
        double sinDp = Math.sin(dp / 2);
        double sinDl = Math.sin(dl / 2);
        double a = sinDp * sinDp + Math.cos(p1) * Math.cos(p2) * sinDl * sinDl;
        double c = 2 * com.codename1.util.MathUtil.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        results[0] = (float) (EARTH_RADIUS_METERS * c);
        if (results.length > 1) {
            results[1] = initialBearing(p1, p2, dl);
        }
        if (results.length > 2) {
            // The final bearing is the reverse course's initial bearing, turned around.
            float back = initialBearing(p2, p1, -dl);
            results[2] = (float) ((back + 180.0) % 360.0);
        }
    }

    private static float initialBearing(double p1, double p2, double dl) {
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        double deg = Math.toDegrees(com.codename1.util.MathUtil.atan2(y, x));
        // Android reports bearings in (-180, 180].
        if (deg <= -180.0) {
            deg += 360.0;
        }
        return (float) deg;
    }

    @Override
    public String toString() {
        return "Location[" + provider + " " + latitude + "," + longitude
                + (hasAccuracy ? " hAcc=" + accuracy : "") + " et=" + getElapsedRealtimeMillis()
                + (hasAltitude ? " alt=" + altitude : "")
                + (hasSpeed ? " vel=" + speed : "")
                + (hasBearing ? " bear=" + bearing : "") + "]";
    }
}
