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

import java.util.Comparator;

/// Metadata for geofencing support that allows tracking user location in the background while the app
/// is inactive.
///
/// The sample below tracks location and posts a notification or shows a dialog based on the state of the app:
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
///
/// **NOTE:** For iOS you must include the `ios.background_modes` build hint with a value that includes "location" for geofencing to work.
///
/// Geofencing is not supported on all platforms, use `LocationManager#isGeofenceSupported()` to find out if the current
/// platform supports it at runtime.
///
/// The maximum number of simulataneous Geofences allowed will vary by platform.  iOS currently has a maximum of 20, and Android has a maximum of 100.  If you need to
/// track more than 20 at a time, consider using the `GeofenceManager` class to manage your Geofences, as it will allow you to
/// effectively track an unlimited number of regions.
/// @author Chen
///
/// #### See also
///
/// - LocationManager#isGeofenceSupported()
///
/// - LocationManager#addGeoFencing(java.lang.Class, com.codename1.location.Geofence)
///
/// - LocationManager#removeGeoFencing(java.lang.String)
///
/// - GeofenceListener
public class Geofence {

    private final String id;

    // Location of the geofence
    private final Location loc;

    // radius in metres.
    private final int radius;

    // Expiration in milliseconds .  Duration, not timestamp.  -1L to never expire.
    private final long expiration;

    /// Constructor
    ///
    /// #### Parameters
    ///
    /// - `id`: unique identifier
    ///
    /// - `loc`: the center location of this Geofence
    ///
    /// - `radius`: @param radius     the radius in meters. Note that the actual radius will vary
    /// on an actual device depending on the hardware and OS.  Typical android and iOS devices
    /// have a minimum radius of 100m.
    ///
    /// - `expiration`: the expiration time in milliseconds.  Note that this is a duration, not a timestamp.  Use -1 to never expire.
    public Geofence(String id, Location loc, int radius, long expiration) {
        this.id = id;
        this.loc = loc;
        this.radius = radius;
        this.expiration = expiration;
    }

    /// Creates a comparator for sorting Geofences from the current Geofence.
    public static Comparator<Geofence> createDistanceComparator(final Geofence refRegion) {
        return new Comparator<Geofence>() {

            @Override
            public int compare(Geofence o1, Geofence o2) {
                double d1 = refRegion.getDistanceTo(o1);
                double d2 = refRegion.getDistanceTo(o2);
                return d1 < d2 ? -1 : d2 < d1 ? 1 : 0;
            }

        };
    }

    /// Creates a comparator for sorting Geofences from the given reference point.
    public static Comparator<Geofence> createDistanceComparator(final Location refPoint) {
        return new Comparator<Geofence>() {

            @Override
            public int compare(Geofence o1, Geofence o2) {
                double d1 = Math.max(0, refPoint.getDistanceTo(o1.getLoc()) - o1.getRadius());
                double d2 = Math.max(0, refPoint.getDistanceTo(o2.getLoc()) - o2.getRadius());
                return d1 < d2 ? -1 : d2 < d1 ? 1 : 0;
            }

        };
    }

    /// Gets the Geofence ID.
    ///
    /// #### Returns
    ///
    /// the id
    public String getId() {
        return id;
    }

    /// Gets the location of the Geofence.
    ///
    /// #### Returns
    ///
    /// the center Location
    public Location getLoc() {
        return loc;
    }

    /// Gets the expiration duration (from now) of the Geofence in milliseconds.
    ///
    /// #### Returns
    ///
    /// the Geofence expiration
    public long getExpiration() {
        return expiration;
    }

    /// Gets the radius of the geofence in metres.  Note that the actual radius will vary
    /// on an actual device depending on the hardware and OS.  Typical android and iOS devices
    /// have a minimum radius of 100m.
    ///
    /// #### Returns
    ///
    /// Geofence radius
    public int getRadius() {
        return radius;
    }

    /// Gets the distance between the current region and the given region.
    ///
    /// #### Parameters
    ///
    /// - `gf`
    public double getDistanceTo(Geofence gf) {
        return Math.max(0, getLoc().getDistanceTo(gf.getLoc()) - gf.getRadius() - getRadius());
    }

    /// Geofences are equal if their id, radius, and expiration are the same, and the location latitude
    /// and longitude are the same.
    ///
    /// #### Parameters
    ///
    /// - `o`
    @Override
    public boolean equals(Object o) {
        if (o instanceof Geofence) {
            Geofence g = (Geofence) o;
            return eq(id, g.id) && g.radius == radius && eq(loc, g.loc) && g.expiration == expiration;
        }
        return false;
    }

    private boolean eq(Object o1, Object o2) {
        if (o1 != null) {
            return o1.equals(o2);
        }
        return o2 == null;
    }

    private boolean eq(Location l1, Location l2) {
        if (l1 != null && l2 != null) {
            return l1.equalsLatLng(l2);
        }
        return l1 == l2; // both null //NOPMD CompareObjectsWithEquals
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
        if (this.loc != null) {
            long temp = Double.doubleToLongBits(this.loc.getLatitude());
            hash = 53 * hash + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(this.loc.getLongitude());
            hash = 53 * hash + (int) (temp ^ (temp >>> 32));
        }
        hash = 53 * hash + this.radius;
        hash = 53 * hash + (int) (this.expiration ^ (this.expiration >>> 32));
        return hash;
    }
}
