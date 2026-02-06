/* 
    Document   : package
    Created on : Oct 11, 2007, 10:38:26 AM
    Author     : Shai Almog
*/

/// Abstraction of location services (GPS/Geofencing etc.) providing user global positioning and monitoring over
///     such changes both in the foreground and background.
///
///     Trivial one time usage of location data can look like this sample:
///
/// ```java
/// Location position = LocationManager.getLocationManager().getCurrentLocationSync();
/// ```
///
///     You can also track location in the foreground using API calls like this:
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
///     Geofencing allows tracking whether a user entered a specific region, this can work when the app is completely
///     in the background and is very efficient in terms of battery life:
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
package com.codename1.location;
