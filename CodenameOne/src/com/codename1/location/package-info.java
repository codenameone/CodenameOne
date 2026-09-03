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
/* 
    Document   : package
    Created on : Oct 11, 2007, 10:38:26 AM
    Author     : Shai Almog
*/

/// Abstraction of location services (GPS/Geofencing etc.) providing user global positioning and monitoring over
/// such changes both in the foreground and background.
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
/// public class MyListener implements LocationListener {
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
/// Geofencing allows tracking whether a user entered a specific region, this can work when the app is completely
/// in the background and is very efficient in terms of battery life:
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
/// Everything above holds a precise-location grant for as long as the app is installed, which is
/// what navigation, tracking and geofencing need. A one-time use -- "what is near me", an address
/// fill, a single share -- wants [LocationButton] instead: on platforms that draw a location button
/// of their own, a tap on it grants precise location for that session and no longer, and Google Play
/// requires that route for transactional use in apps targeting Android 17 and later.
///
/// ```java
/// LocationButton share = new LocationButton(LocationButton.TEXT_SHARE_PRECISE_LOCATION);
/// share.addLocationSharedListener(loc -> {
///     if (loc != null) {
///         status.setText(loc.getLatitude() + ", " + loc.getLongitude());
///         status.getParent().revalidate();
///     }
/// });
/// ```
package com.codename1.location;
