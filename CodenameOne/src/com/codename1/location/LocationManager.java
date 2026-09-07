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

import com.codename1.io.Log;
import com.codename1.ui.Display;

import java.io.IOException;

/// The LocationManager is the main entry to retrieveLocation or to bind  a LocationListener,
/// **important:** in order to use location on iOS you will need to define the build
/// argument `ios.locationUsageDescription`.
/// This build argument should be used to describe to Apple & the users why you need to use the location
/// functionality.
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
public abstract class LocationManager {

    public static final int AVAILABLE = 0;
    public static final int OUT_OF_SERVICE = 1;
    public static final int TEMPORARILY_UNAVAILABLE = 2;
    private static final Object LISTENER_LOCK = new Object();
    private static LocationListener listener;

    /// The installed listener whose timed wait expired, or null.
    ///
    /// Routing treats it as absent; the slot still holds it so the next
    /// install still tears the platform subscription down. See markTimedOut.
    private static LocationListener timedOut;
    private static Class backgroundlistener;
    private LocationRequest request;
    private int status = TEMPORARILY_UNAVAILABLE;

    /// Gets the LocationManager instance
    public static LocationManager getLocationManager() {
        return Display.getInstance().getLocationManager();
    }

    /// Gets the Manager status: AVAILABLE, OUT_OF_SERVICE or TEMPORARILY_UNAVAILABLE
    ///
    /// #### Returns
    ///
    /// the status of the LoactionManager
    public int getStatus() {
        return status;
    }

    /// Allows the implementation to set the status of the location
    ///
    /// #### Parameters
    ///
    /// - `status`: the new status
    protected void setStatus(int status) {
        this.status = status;
    }

    /// Gets the current Location of the device, in most cases this uses the GPS. Notice! This method
    /// will only return a valid value after the location listener callback returns
    ///
    /// #### Returns
    ///
    /// a Location Object
    ///
    /// #### Throws
    ///
    /// - `IOException`: if Location cannot be retrieve from the device
    public abstract Location getCurrentLocation() throws IOException;

    protected final LocationListener getListener() {
        return listener;
    }

    /// Returns the current location synchronously, this is useful if you just want
    /// to know the location NOW and don't care about tracking location. Notice that
    /// this method will block until a result is returned so you might want to use something
    /// like InfiniteProgress while this is running
    ///
    /// #### Returns
    ///
    /// the current location or null in case of an error
    public Location getCurrentLocationSync() {
        return getCurrentLocationSync(-1);
    }

    /// Returns the current location synchronously, this is useful if you just want
    /// to know the location NOW and don't care about tracking location. Notice that
    /// this method will block until a result is returned so you might want to use something
    /// like InfiniteProgress while this is running
    ///
    /// #### Parameters
    ///
    /// - `timeout`: timeout in milliseconds or -1 to never timeout
    ///
    /// #### Returns
    ///
    /// the current location or null in case of an error
    /// A fix taken after this call, for a caller whose permission is new.
    ///
    /// [#getCurrentLocationSync(long)] hands back the CACHED fix whenever a
    /// listener is installed, and that is right for an ordinary caller: the
    /// listener is keeping the cache current, and starting a second
    /// acquisition would replace the application's own listener -- `LL.bind`
    /// calls [#setLocationListener(LocationListener)] -- and silently end its
    /// tracking.
    ///
    /// It is wrong for the location button. The whole of that gesture is that
    /// the tap earns precise location for this session, and the cached fix was
    /// taken BEFORE it: on an application that until now held only the
    /// approximate grant, the cache holds an approximate fix and the tap
    /// reports it. The control appears to work and quietly answers with the
    /// accuracy the user just agreed to improve.
    ///
    /// So this waits for the next fix the installed listener delivers, which is
    /// the first one taken under the new grant, and recognises it by its
    /// timestamp. Nothing is registered or cleared, so the application's
    /// listener is untouched -- the constraint that makes the obvious fix
    /// (bind a fresh LL) unsafe.
    ///
    /// Every read happens inside the bounded wait, the baseline included: on
    /// Android with Play Services the "cached" read spins until the Google API
    /// client connects, with no bound, and doing that on the EDT freezes the
    /// application before the caller's timeout has begun to apply.
    ///
    /// Falls back to the cached fix rather than to null when none arrives in
    /// time. A listener registered at a slow interval may have nothing newer
    /// for minutes, and a slightly imprecise answer beats the empty one the
    /// caller would otherwise report -- so this is never worse than the
    /// method it wraps.
    ///
    /// Package private on purpose. It exists for `LocationButton`, which is in
    /// this package, and a public "give me a fix ignoring the cache" is a
    /// wider promise than this keeps.
    ///
    /// @param timeout how long to wait, in milliseconds, or -1 for the default
    /// @return a fix taken under the caller's new grant where one arrives, the
    ///         cached fix otherwise, or null if there is none at all
    Location freshLocationSync(long timeout) {
        // No listener of the application's to protect, so the ordinary path
        // already starts a fresh acquisition and this adds nothing.
        if (listener == null || listener == timedOut) { //NOPMD CompareObjectsWithEquals
            return getCurrentLocationSync(timeout);
        }
        final long wait = timeout > -1 ? timeout : FRESH_FIX_DEFAULT_MS;
        // [0] the fix taken after the grant, [1] the cache to fall back on.
        final Location[] answer = new Location[2];
        // EVERYTHING inside the block, the baseline read included.
        //
        // getCurrentLocation is not the cheap accessor its name suggests. On
        // Android with Play Services it reaches getLastKnownLocation, which
        // waits for the Google API client to connect -- so reading the
        // baseline on the EDT, which is where this is called from, freezes the
        // whole application for as long as that takes, and the caller's
        // timeout cannot be applied to a wait that has not started yet. It
        // also THROWS when there is no cached fix, and taking that as the
        // answer reported null to a caller whose next update was moments away.
        //
        // That port wait used to be unbounded, which put a ceiling on what
        // this loop could promise: the deadline governs the POLLING, and
        // nothing in core can interrupt a port that sleeps. It is bounded at
        // the source now -- see AndroidLocationPlayServiceManager
        // .getLastKnownLocation -- so a client that never connects ends as no
        // fix rather than as a tap that is never answered.
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                Location baseline = cachedFix();
                answer[1] = baseline;
                // No cache at all means the application has no fix yet, so
                // the first one to arrive was taken after the grant and is the
                // answer rather than a baseline to improve on.
                boolean compare = baseline != null;
                long was = compare ? baseline.getTimeStamp() : 0;
                while (System.currentTimeMillis() - start < wait) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException interrupted) {
                    }
                    Location now = cachedFix();
                    if (now == null) {
                        continue;
                    }
                    if (!compare || now.getTimeStamp() > was) {
                        answer[0] = now;
                        return;
                    }
                }
            }
        });
        return answer[0] != null ? answer[0] : answer[1];
    }

    /// The cached fix, or null where there is none.
    ///
    /// A port throws when it has nothing cached, which is a legitimate state
    /// and not a failure to report: the caller is waiting for the next fix and
    /// an empty cache simply means every fix from here is a new one.
    private Location cachedFix() {
        try {
            return getCurrentLocation();
        } catch (IOException noneYet) {
            return null;
        }
    }

    /// How long [#freshLocationSync(long)] waits when the caller names no
    /// deadline of its own.
    private static final long FRESH_FIX_DEFAULT_MS = 10000;

    public Location getCurrentLocationSync(long timeout) {
        try {
            // A timed-out listener routes as absent, and is deliberately
            // still INSTALLED. Nulling the field made this branch right and
            // broke the next setLocationListener, which only calls
            // clearListener() when it finds a listener to replace: the
            // platform subscription was then never torn down at all, and on
            // the JavaSE stub that is a non-daemon Timer still delivering to
            // an expired LL.
            if (listener == null || listener == timedOut) { //NOPMD CompareObjectsWithEquals
                LL l = new LL();
                l.timeout = timeout;
                l.bind();
                return l.result;
            }
            return getCurrentLocation();
        } catch (IOException err) {
            Log.e(err);
            return null;
        }
    }

    /// Gets the last known Location of the device.
    ///
    /// #### Returns
    ///
    /// a Location Object
    public abstract Location getLastKnownLocation();

    /// Sets a LocationListener on the device, use this method if you need to be
    /// updated on the device Locations rather then calling getCurrentLocation.
    ///
    /// #### Parameters
    ///
    /// - `l`: @param l   a LocationListener or null to stop the current listener
    /// from getting updates
    ///
    /// - `req`: @param req provide the settings in which we are interested to get updates
    /// to the Listener.
    public void setLocationListener(final LocationListener l, LocationRequest req) {
        setLocationListener(l);
        request = req;
    }

    /// Adds a geo fence listener to gets an event once the device is in/out of
    /// the Geofence range.
    /// The GeoFence events can arrive in the background therefore it is
    /// recommended to check the app state before deciding how to process this event.
    /// Use Display.isMinimized() to know if the app is currently running.
    /// if isGeofenceSupported() returns false this method does nothing
    ///
    /// **NOTE:** For iOS you must include the `ios.background_modes` build hint with a value that includes "location" for geofencing to work.
    ///
    /// #### Parameters
    ///
    /// - `geofenceListenerClass`: @param geofenceListenerClass a Class that implements the GeofenceListener interface
    /// this class must have an empty constructor
    ///
    /// - `gf`: a Geofence to track
    public void addGeoFencing(Class geofenceListenerClass, Geofence gf) {
    }

    /// Stop tracking a Geofence
    /// if isGeofenceSupported() returns false this method does nothing
    ///
    /// **NOTE:** For iOS you must include the `ios.background_modes` build hint with a value that includes "location" for geofencing to work.
    ///
    /// #### Parameters
    ///
    /// - `id`: a Geofence id to stop tracking
    public void removeGeoFencing(String id) {
    }

    /// Allows the implementation to notify the location listener of changes to location
    ///
    /// #### Returns
    ///
    /// location listener instance
    protected LocationListener getLocationListener() {
        return listener;
    }

    /// Sets a LocationListener on the device, use this method if you need to be
    /// updated on the device Locations rather then calling getCurrentLocation.
    ///
    /// #### Parameters
    ///
    /// - `l`: @param l a LocationListener or null to stop the current listener
    /// from getting updates
    public void setLocationListener(final LocationListener l) {
        synchronized (LISTENER_LOCK) {
            if (listener != null) {
                clearListener();
                request = null;
                status = TEMPORARILY_UNAVAILABLE;
            }
            listener = l;
            // Whatever timed out is gone now, whether it was replaced or
            // cleared: the marker only ever describes the current occupant.
            timedOut = null;
            if (l == null) {
                return;
            }
            bindListener();
        }
    }

    /// Gets the LocationRequest
    protected LocationRequest getRequest() {
        return request;
    }

    /// Gets the LocationListener class that handles background location updates.
    ///
    /// **NOTE:** For iOS you must include the
    /// `ios.background_modes` build hint with a value that includes
    /// "location" for background locations to work.
    protected Class getBackgroundLocationListener() {
        return backgroundlistener;
    }

    /// Use this method to track background location updates when the application
    /// is not running anymore.
    /// Do not perform long operations here, iOS wake-up time is very short(around 10 seconds).
    /// Notice this listener can sends events also when the app is in the foreground, therefore
    /// it is recommended to check the app state before deciding how to process this event.
    /// Use Display.isMinimized() to know if the app is currently running.
    ///
    /// #### Parameters
    ///
    /// - `locationListener`: @param locationListener a class that implements the LocationListener interface
    /// this class must have an empty constructor since the underlying implementation will
    /// try to create an instance and invoke the locationUpdated method
    public void setBackgroundLocationListener(Class locationListener) {
        synchronized (LISTENER_LOCK) {
            if (backgroundlistener != null) {
                clearBackgroundListener();
            }
            backgroundlistener = locationListener;
            if (locationListener == null) {
                return;
            }
            bindBackgroundListener();
        }
    }

    /// Removes `l` only if it is still the listener this manager holds.
    ///
    /// Under LISTENER_LOCK so the test and the removal cannot be split by the
    /// EDT installing a different listener in between -- which is the whole
    /// point: the only caller is a timed wait undoing ITS OWN subscription, and
    /// it must not touch one somebody else made while it was parked.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove if it is still current
    /// Marks `l` as having timed out, leaving it INSTALLED.
    ///
    /// Two things have to be true at once and neither may be traded for the
    /// other. `getCurrentLocationSync` must stop routing through it, or the
    /// next request takes `getCurrentLocation()` instead of starting a fresh
    /// timed one. And the platform subscription must still be torn down when
    /// something replaces it -- `setLocationListener` only calls
    /// `clearListener()` when it finds a listener there, so forgetting the
    /// field orphans the subscription for good.
    ///
    /// Marking does both: the retry's `setLocationListener` sees a listener,
    /// clears it and binds within that ONE port call, which is the ordering
    /// the port controls. Starting a clear here instead would be a separate
    /// asynchronous one on Android, free to arrive after the retry's bind.
    private void markTimedOut(LocationListener l) {
        synchronized (LISTENER_LOCK) {
            // Reference identity, for the reason clearListenerIfStill says.
            if (listener == l) { //NOPMD CompareObjectsWithEquals
                timedOut = l;
            }
        }
    }

    private void clearListenerIfStill(LocationListener l) {
        synchronized (LISTENER_LOCK) {
            // Reference identity is the question, not equality: "is the
            // manager still holding the very listener this wait installed".
            // A LocationListener is an application object with no equals
            // contract, and two distinct listeners that happened to compare
            // equal would be exactly the case this must not treat as the same.
            if (listener == l) { //NOPMD CompareObjectsWithEquals
                setLocationListener(null);
            }
        }
    }

    /// Bind the LocationListener to get events
    protected abstract void bindListener();

    /// Stop deliver events for the LocationListener
    protected abstract void clearListener();

    /// Bind the Background LocationListener to get events
    protected void bindBackgroundListener() {
    }

    /// Stop deliver events for the Background LocationListener
    protected void clearBackgroundListener() {
    }

    /// Returns true if the platform is able to detect if the GPS is on or off.
    /// see also isGPSEnabled()
    ///
    /// #### Returns
    ///
    /// true if platform is able to detect GPS on/off
    public boolean isGPSDetectionSupported() {
        return false;
    }

    /// Returns true if the platform is able to track background location.
    ///
    /// **NOTE:** For iOS you must include the `ios.background_modes` build hint with a value that includes "location" for background locations to work.
    ///
    /// #### Returns
    ///
    /// true if platform supports background location
    public boolean isBackgroundLocationSupported() {
        return false;
    }

    /// Returns true if the platform supports Geofence
    ///
    /// **NOTE:** For iOS you must include the `ios.background_modes` build hint with a value that includes "location" for geofencing to work.
    ///
    /// #### Returns
    ///
    /// true if platform supports Geofence
    public boolean isGeofenceSupported() {
        return false;
    }

    /// Returns GPS on/off state if isGPSDetectionSupported() returns true
    ///
    /// #### Returns
    ///
    /// true if GPS is on
    public boolean isGPSEnabled() {
        throw new RuntimeException("GPS Detection is not supported");
    }

    class LL implements Runnable, LocationListener {
        Location result;
        boolean finished;
        long timeout;

        public void bind() {
            setLocationListener(this);
            Display.getInstance().invokeAndBlock(this);
            if (!finished) {
                // The wait ended without a fix, which means run() hit the
                // timeout and broke out. Only the two callbacks below used to
                // clear the listener, so a timed-out request left this LL bound
                // as the manager's listener for the life of the process: the
                // platform kept delivering updates nothing consumed, and the
                // NEXT getCurrentLocationSync saw a non-null listener and took
                // the getCurrentLocation() path instead of starting a fresh
                // timed request.
                //
                // Latent until something actually passed a timeout -- the
                // no-argument getCurrentLocationSync passes -1 and waits
                // forever -- and LocationButton defaults to 30 seconds, which
                // is what makes this reachable in ordinary use.
                //
                // The CORE field only, and deliberately not the platform
                // subscription.
                //
                // What the leak actually broke is right here: getCurrentLocation
                // Sync branches on this field, so a timed-out LL left in it sent
                // the next request down getCurrentLocation() instead of starting
                // a fresh timed one. Releasing the field fixes that entirely.
                //
                // Calling setLocationListener(null) would fix it too and cost
                // more than it is worth. On Android with Play Services that
                // starts an ASYNCHRONOUS clearListener(), and a retry arriving
                // as connectivity returns can bind first -- the late clear then
                // removes the retry's subscription and the retry times out as
                // well. AndroidLocationPlayServiceManager.clearListener()
                // documents that ordering hazard; nothing in core can serialize
                // against it, and before this cleanup existed there was no
                // clear here to race at all.
                //
                // The platform subscription is not orphaned for long: the next
                // request's bind() calls setLocationListener(this), and a port
                // replaces a listener inside that ONE call rather than through
                // a separate clear that can overtake it.
                //
                // Only when this LL is STILL the manager's listener. The wait
                // runs through invokeAndBlock, so the EDT keeps pumping and the
                // application can call setLocationListener(other) while we are
                // parked here -- `finished` is false either way, so an
                // unconditional release would forget a listener the application
                // had just installed.
                markTimedOut(this);
            }
        }

        @Override
        public void locationUpdated(Location location) {
            result = location;
            finished = true;
            // Only while this LL is STILL the installed listener, for the same
            // reason the timeout path above says so. The platform can deliver
            // to a listener it captured before the wait gave up: the slot has
            // been released by then and the next request has installed its
            // own, and an unconditional clear here removed THAT one -- so a
            // fix arriving late for a request nobody is waiting on any more
            // made the request that is waiting time out instead.
            clearListenerIfStill(this);
        }

        @Override
        public void providerStateChanged(int newState) {
            if (newState == AVAILABLE) {
                try {
                    result = getCurrentLocation();
                } catch (IOException err) {
                    Log.e(err);
                    result = null;
                }
            } else {
                result = null;
            }
            finished = true;
            // Conditional for the reason locationUpdated above is: a late
            // state change belongs to the request that installed this LL, not
            // to whoever holds the slot now.
            clearListenerIfStill(this);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            while (!finished) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException er) {
                }
                if (timeout > -1 && System.currentTimeMillis() - start > timeout) {
                    break;
                }
            }
        }
    }

}
