/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.util.MathUtil;

import java.util.Comparator;

/// Represents a position and possible velocity returned from positioning API's. This class is used both by
/// foreground and background location for the purposes of both conveying the users location and conveying a
/// desired location e.g. in the case of geofencing where we can define a location that would trigger the callback.
///
/// Trivial one time usage of location data can look like this sample:
///
/// ```java
/// Location position = LocationManager.getLocationManager().getCurrentLocationSync();
/// ```
///
/// You can also track location in the foreground using API calls like this:
///
/// ```java
/// public MyListener implements LocationListener {
///     public void locationUpdated(Location location) {
///         // update UI etc.
///     }
///
///     public void providerStateChanged(int newState) {
///         // handle status changes/errors appropriately
///     }
/// }
/// LocationManager.getLocationManager().setLocationListener(new MyListener());
/// ```
///
/// The sample below demonstrates the usage of the background geofencing API:
///
/// ```java
/// // File: GeofenceListenerImpl.java
/// public class GeofenceListenerImpl implements GeofenceListener {
/// @Override
///     public void onExit(String id) {
///     }
/// @Override
///     public void onEntered(String id) {
///         if(!Display.getInstance().isMinimized()) {
///             Display.getInstance().callSerially(() -> {
///                 Dialog.show("Welcome", "Thanks for arriving", "OK", null);
///             });
///         } else {
///             LocalNotification ln = new LocalNotification();
///             ln.setId("LnMessage");
///             ln.setAlertTitle("Welcome");
///             ln.setAlertBody("Thanks for arriving!");
///             Display.getInstance().scheduleLocalNotification(ln, System.currentTimeMillis() + 10, LocalNotification.REPEAT_NONE);
///         }
///     }
/// }
/// ```
///
/// ```java
/// // File: GeofenceSample.java
/// Geofence gf = new Geofence("test", loc, 100, 100000);
/// LocationManager.getLocationManager().addGeoFencing(GeofenceListenerImpl.class, gf);
/// ```
public class Location {

    private int status;

    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private float direction;
    private float velocity;
    private long timeStamp;

    public Location() {
    }

    public Location(double latitude, double longitude, double altitude, float direction) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.direction = direction;
    }


    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = 6372.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = MathUtil.pow(Math.sin(dLat / 2), 2) + MathUtil.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * MathUtil.asin(Math.sqrt(a));
        return r * c;
    }

    /// Returns the horizontal accuracy of the location in meters
    ///
    /// #### Returns
    ///
    /// the accuracy if exists or 0.0 if not.
    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        if (Float.isNaN(accuracy)) {
            this.accuracy = accuracy;
        }
    }

    /// Returns the altitude of this Location
    ///
    /// #### Returns
    ///
    /// altitude
    public double getAltitude() {
        return altitude;
    }

    /// Sets the altitude of this Location
    ///
    /// #### Parameters
    ///
    /// - `altitude`
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    /// Returns the direction of this Location in degress 0-360
    ///
    /// #### Returns
    ///
    /// direction in degrees
    public float getDirection() {
        return direction;
    }

    /// Sets the direction of this Location
    ///
    /// #### Parameters
    ///
    /// - `direction`
    public void setDirection(float direction) {
        this.direction = direction;
    }

    /// Returns the latitude of this Location
    ///
    /// #### Returns
    ///
    /// latitude
    public double getLatitude() {
        return latitude;
    }

    /// Sets the latitude of this Location
    ///
    /// #### Parameters
    ///
    /// - `latitude`
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /// Returns the longitude of this Location
    ///
    /// #### Returns
    ///
    /// longitude
    public double getLongitude() {
        return longitude;
    }

    /// Sets the longitude of this Location
    ///
    /// #### Parameters
    ///
    /// - `longitude`
    public void setLongitude(double longtitude) {
        this.longitude = longtitude;
    }

    /// Returns the longitude of this Location
    ///
    /// #### Returns
    ///
    /// longitude
    ///
    /// #### Deprecated
    ///
    /// use getLongitude
    public double getLongtitude() {
        return longitude;
    }

    /// Sets the longitude of this Location
    ///
    /// #### Parameters
    ///
    /// - `longitude`
    ///
    /// #### Deprecated
    ///
    /// use setLongitude
    public void setLongtitude(double longtitude) {
        this.longitude = longtitude;
    }

    /// The status of the location one of: LocationManager.AVAILABLE,
    /// LocationManager.OUT_OF_SERVICE or LocationManager.TEMPORARILY_UNAVAILABLE:
    public int getStatus() {
        return status;
    }

    /// The status of the location one of: LocationProvider.AVAILABLE,
    /// LocationProvider.OUT_OF_SERVICE or LocationProvider.TEMPORARILY_UNAVAILABLE:
    public void setStatus(int status) {
        this.status = status;
    }

    /// Returns the timestamp of this Location
    ///
    /// #### Returns
    ///
    /// timestamp
    public long getTimeStamp() {
        return timeStamp;
    }

    /// Sets the timeStamp of this Location
    ///
    /// #### Parameters
    ///
    /// - `timeStamp`
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /// Returns the velocity of this Location in meters per second (m/s)
    ///
    /// #### Returns
    ///
    /// velocity
    public float getVelocity() {
        return velocity;
    }

    /// Sets the velocity of this Location
    ///
    /// #### Parameters
    ///
    /// - `velocity`
    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    /// Gets the distance in metres from the current location to another location.
    ///
    /// #### Parameters
    ///
    /// - `l2`: The location to measure distance to.
    ///
    /// #### Returns
    ///
    /// The number of metres between the current location and l2.
    public double getDistanceTo(Location l2) {
        return haversine(getLatitude(), getLongitude(), l2.getLatitude(), l2.getLongitude()) * 1000;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "altitude = " + altitude
                + "\nlatitude" + latitude
                + "\nlongtitude" + longitude
                + "\ndirection" + direction
                + "\ntimeStamp" + timeStamp
                + "\nvelocity" + velocity;

    }

    /// Creates a comparator for sorting locations in order of increasing distance from the current
    /// location.
    public Comparator<Location> createDistanceCompartor() {
        return new Comparator<Location>() {

            @Override
            public int compare(Location o1, Location o2) {
                double d1 = Location.this.getDistanceTo(o1);
                double d2 = Location.this.getDistanceTo(o2);
                return d1 < d2 ? -1 : d2 < d1 ? 1 : 0;
            }

        };
    }

    /// Checks if the latitude and longitude of this location is the same as the provided location.
    /// Null values for l are safe.
    ///
    /// #### Parameters
    ///
    /// - `l`
    ///
    /// #### Returns
    ///
    /// True if l has same latitude and longitude as this location.
    boolean equalsLatLng(Location l) {

        return l != null && MathUtil.compare(l.latitude, latitude) == 0 && MathUtil.compare(l.longitude, longitude) == 0;

    }
}
