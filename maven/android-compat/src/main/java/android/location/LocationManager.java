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

import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import com.codename1.androidcompat.runtime.CompatReport;
import com.codename1.androidcompat.runtime.MainThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/// Android's location service over `com.codename1.location.LocationManager`.
///
/// Codename One has one location stream per application and one listener on
/// it, where Android has any number of registrations, each with its own
/// provider, interval and distance filter. This class owns the single
/// Codename One listener while at least one registration exists and fans each
/// fix out to every registration whose `minTimeMs` and `minDistanceM` it
/// passes. The stream is requested at the shortest interval and the finest
/// accuracy any registration asked for.
///
/// Providers are names only: there is one platform stream, and `gps`,
/// `network` and `fused` registrations all receive it (with the accuracy the
/// platform reports). `passive` registrations receive it too, which is what
/// Android's passive provider does when another app is not involved.
///
/// The `PendingIntent` overloads are not provided. Background delivery to a
/// killed application is a platform feature; on iOS keeping the stream alive
/// in the background needs the `ios.background_modes=location` build hint,
/// and a foreground [android.app.Service] keeps the application running on
/// Android.
///
/// Registrations are kept on the main thread, which is Codename One's EDT; a
/// call from another thread is posted there rather than synchronized.
public class LocationManager {

    public static final String GPS_PROVIDER = "gps";
    public static final String NETWORK_PROVIDER = "network";
    public static final String PASSIVE_PROVIDER = "passive";
    public static final String FUSED_PROVIDER = "fused";
    public static final String KEY_LOCATION_CHANGED = "location";
    public static final String KEY_PROVIDER_ENABLED = "providerEnabled";
    public static final String KEY_STATUS_CHANGED = "status";
    public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";
    public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";

    /// How long [#getCurrentLocation] waits for a fix before answering null,
    /// as Android's does after roughly thirty seconds.
    static final long CURRENT_LOCATION_TIMEOUT_MS = 30000L;

    /// The Codename One manager to use instead of the platform's; set by tests.
    static com.codename1.location.LocationManager override;

    private static LocationManager instance;

    private final List<Registration> registrations = new ArrayList<Registration>();
    private boolean listening;
    private long requestedInterval = -1;
    private int requestedPriority = -1;

    private final com.codename1.location.LocationListener bridge = new com.codename1.location.LocationListener() {
        @Override
        public void locationUpdated(com.codename1.location.Location location) {
            deliver(location);
        }

        @Override
        public void providerStateChanged(int newState) {
            providerState(newState);
        }
    };

    /// Context.getSystemService(LOCATION_SERVICE) answers one manager per
    /// application, as Android does.
    public static LocationManager getInstance() {
        if (instance == null) {
            instance = new LocationManager();
        }
        return instance;
    }

    LocationManager() {
    }

    static com.codename1.location.LocationManager platform() {
        if (override != null) {
            return override;
        }
        return com.codename1.location.LocationManager.getLocationManager();
    }

    // ------------------------------------------------------------- providers

    public List<String> getAllProviders() {
        List<String> out = new ArrayList<String>();
        out.add(PASSIVE_PROVIDER);
        out.add(GPS_PROVIDER);
        out.add(NETWORK_PROVIDER);
        out.add(FUSED_PROVIDER);
        return out;
    }

    public List<String> getProviders(boolean enabledOnly) {
        List<String> out = new ArrayList<String>();
        for (String p : getAllProviders()) {
            if (!enabledOnly || isProviderEnabled(p)) {
                out.add(p);
            }
        }
        return out;
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        return getProviders(enabledOnly);
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        String best = criteria == null || criteria.wantsFine() ? GPS_PROVIDER : NETWORK_PROVIDER;
        if (enabledOnly && !isProviderEnabled(best)) {
            return isProviderEnabled(NETWORK_PROVIDER) ? NETWORK_PROVIDER : null;
        }
        return best;
    }

    public boolean hasProvider(String provider) {
        return getAllProviders().contains(provider);
    }

    public boolean isProviderEnabled(String provider) {
        if (!hasProvider(provider)) {
            return false;
        }
        com.codename1.location.LocationManager lm = platform();
        if (lm == null) {
            return false;
        }
        if (GPS_PROVIDER.equals(provider) && lm.isGPSDetectionSupported()) {
            return lm.isGPSEnabled();
        }
        return lm.getStatus() != com.codename1.location.LocationManager.OUT_OF_SERVICE;
    }

    public boolean isLocationEnabled() {
        return isProviderEnabled(GPS_PROVIDER) || isProviderEnabled(NETWORK_PROVIDER);
    }

    // ------------------------------------------------------------ last fix

    public Location getLastKnownLocation(String provider) {
        if (!hasProvider(provider)) {
            throw new IllegalArgumentException("provider doesn't exist: " + provider);
        }
        com.codename1.location.LocationManager lm = platform();
        if (lm == null) {
            return null;
        }
        com.codename1.location.Location l = lm.getLastKnownLocation();
        return l == null ? null : convert(l, provider);
    }

    /// Answers the next fix, or null when none arrives within thirty
    /// seconds or `cancellationSignal` is cancelled first (then nothing is
    /// answered at all, as on Android).
    public void getCurrentLocation(String provider, CancellationSignal cancellationSignal,
            Executor executor, final Consumer<Location> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("consumer is null");
        }
        final Registration r = new Registration(provider, 0, 0, null, executor, null);
        r.consumer = consumer;
        r.oneShot = true;
        if (cancellationSignal != null) {
            cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                @Override
                public void onCancel() {
                    remove(r);
                }
            });
        }
        add(r);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (registrations.contains(r)) {
                    remove(r);
                    r.dispatch(null);
                }
            }
        }, CURRENT_LOCATION_TIMEOUT_MS);
    }

    public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
        Registration r = new Registration(provider, 0, 0, listener, null, looper);
        r.oneShot = true;
        add(r);
    }

    public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper) {
        requestSingleUpdate(getBestProvider(criteria, false), listener, looper);
    }

    // -------------------------------------------------------------- updates

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM,
            LocationListener listener) {
        requestLocationUpdates(provider, minTimeMs, minDistanceM, listener, (Looper) null);
    }

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM,
            LocationListener listener, Looper looper) {
        if (listener == null) {
            throw new IllegalArgumentException("invalid null listener");
        }
        if (!hasProvider(provider)) {
            throw new IllegalArgumentException("provider doesn't exist: " + provider);
        }
        replace(new Registration(provider, minTimeMs, minDistanceM, listener, null, looper));
    }

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM,
            Executor executor, LocationListener listener) {
        if (listener == null || executor == null) {
            throw new IllegalArgumentException("invalid null listener or executor");
        }
        if (!hasProvider(provider)) {
            throw new IllegalArgumentException("provider doesn't exist: " + provider);
        }
        replace(new Registration(provider, minTimeMs, minDistanceM, listener, executor, null));
    }

    public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria,
            LocationListener listener, Looper looper) {
        requestLocationUpdates(getBestProvider(criteria, false), minTimeMs, minDistanceM, listener, looper);
    }

    public void removeUpdates(final LocationListener listener) {
        onMain(new Runnable() {
            @Override
            public void run() {
                for (int i = registrations.size() - 1; i >= 0; i--) {
                    if (registrations.get(i).listener == listener) {
                        registrations.remove(i).removed.set(true);
                    }
                }
                reconfigure();
            }
        });
    }

    /// Codename One keeps no per-provider queue to flush; completion is
    /// reported straight away.
    public void requestFlush(String provider, final LocationListener listener, final int requestCode) {
        onMain(new ProviderCall(listener, null, requestCode));
    }

    /// One listener callback, posted where the registration wants it.
    private static final class ProviderCall implements Runnable {
        private final LocationListener listener;
        private final String provider;
        private final int code;

        /// `provider` null means a flush with request `code`; otherwise
        /// `code` is 1 for enabled and 0 for disabled.
        ProviderCall(LocationListener listener, String provider, int code) {
            this.listener = listener;
            this.provider = provider;
            this.code = code;
        }

        @Override
        public void run() {
            if (provider == null) {
                listener.onFlushComplete(code);
            } else if (code == 1) {
                listener.onProviderEnabled(provider);
            } else {
                listener.onProviderDisabled(provider);
            }
        }
    }

    /// Geofences through a `PendingIntent` have no counterpart that does not
    /// need reflection; use `com.codename1.location.GeofenceManager`.
    public void addProximityAlert(double latitude, double longitude, float radius, long expiration,
            Object intent) {
        CompatReport.unsupported("LocationManager", "addProximityAlert");
    }

    // -------------------------------------------------------- registrations

    private void replace(final Registration r) {
        onMain(new Runnable() {
            @Override
            public void run() {
                // Android replaces an existing registration of the same listener.
                for (int i = registrations.size() - 1; i >= 0; i--) {
                    if (registrations.get(i).listener == r.listener) {
                        registrations.remove(i).removed.set(true);
                    }
                }
                registrations.add(r);
                reconfigure();
            }
        });
    }

    private void add(final Registration r) {
        onMain(new Runnable() {
            @Override
            public void run() {
                registrations.add(r);
                reconfigure();
            }
        });
    }

    private void remove(final Registration r) {
        onMain(new Runnable() {
            @Override
            public void run() {
                registrations.remove(r);
                r.removed.set(true);
                reconfigure();
            }
        });
    }

    private static void onMain(Runnable r) {
        if (MainThread.isMainThread()) {
            r.run();
        } else {
            MainThread.post(r);
        }
    }

    /// Installs, retunes or removes the one Codename One listener so it
    /// matches what the registrations ask for.
    private void reconfigure() {
        com.codename1.location.LocationManager lm = platform();
        if (lm == null) {
            return;
        }
        if (registrations.isEmpty()) {
            if (listening) {
                lm.setLocationListener(null);
                listening = false;
                requestedInterval = -1;
                requestedPriority = -1;
            }
            return;
        }
        long interval = Long.MAX_VALUE;
        int priority = com.codename1.location.LocationRequest.PRIORITY_LOW_ACCUARCY;
        for (int i = 0; i < registrations.size(); i++) {
            Registration r = registrations.get(i);
            interval = Math.min(interval, r.minTime);
            priority = Math.min(priority, r.priority());
        }
        if (listening && interval == requestedInterval && priority == requestedPriority) {
            return;
        }
        // Codename One's request takes an int interval; a minTime of zero
        // asks for fixes as fast as the platform gives them.
        int cn1Interval = (int) Math.min(Integer.MAX_VALUE, Math.max(0, interval));
        lm.setLocationListener(bridge, new com.codename1.location.LocationRequest(priority, cn1Interval));
        listening = true;
        requestedInterval = interval;
        requestedPriority = priority;
    }

    void deliver(com.codename1.location.Location fix) {
        if (fix == null) {
            return;
        }
        // Copied first: a one-shot registration removes itself while the
        // list is being walked, and a listener may register or remove others.
        Registration[] snapshot = registrations.toArray(new Registration[registrations.size()]);
        boolean removed = false;
        for (int i = 0; i < snapshot.length; i++) {
            Registration r = snapshot[i];
            if (!registrations.contains(r)) {
                // Removed by an earlier listener's callback in this pass.
                continue;
            }
            Location l = convert(fix, r.provider);
            if (!r.accepts(l)) {
                continue;
            }
            if (r.oneShot) {
                // Its one fix is the one dispatched below, so it is not
                // flagged removed.
                registrations.remove(r);
                removed = true;
            }
            r.dispatch(l);
        }
        if (removed) {
            reconfigure();
        }
    }

    void providerState(int state) {
        Registration[] snapshot = registrations.toArray(new Registration[registrations.size()]);
        for (int i = 0; i < snapshot.length; i++) {
            final Registration r = snapshot[i];
            if (!registrations.contains(r)) {
                continue;
            }
            if (r.listener == null) {
                continue;
            }
            if (state == com.codename1.location.LocationManager.OUT_OF_SERVICE) {
                r.run(new ProviderCall(r.listener, r.provider, 0));
            } else if (state == com.codename1.location.LocationManager.AVAILABLE) {
                r.run(new ProviderCall(r.listener, r.provider, 1));
            }
        }
    }

    /// The Codename One fix as an Android one. Codename One reports zero for
    /// a value the platform did not supply, so a zero accuracy, speed or
    /// direction is left unset rather than reported as a measurement.
    static Location convert(com.codename1.location.Location src, String provider) {
        Location l = new Location(provider);
        l.setLatitude(src.getLatitude());
        l.setLongitude(src.getLongitude());
        if (src.getAltitude() != 0) {
            l.setAltitude(src.getAltitude());
        }
        if (src.getAccuracy() > 0) {
            l.setAccuracy(src.getAccuracy());
        }
        if (src.getVelocity() > 0) {
            l.setSpeed(src.getVelocity());
        }
        if (src.getDirection() > 0) {
            l.setBearing(src.getDirection());
        }
        long ts = src.getTimeStamp();
        l.setTime(ts > 0 ? ts : System.currentTimeMillis());
        l.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
        return l;
    }

    /// One `requestLocationUpdates` (or one-shot request).
    static final class Registration {
        /// Set when the registration is removed; a fix already posted to
        /// its looper or executor is then dropped, so a removed listener
        /// hears nothing more.
        final java.util.concurrent.atomic.AtomicBoolean removed = new java.util.concurrent.atomic.AtomicBoolean();
        final String provider;
        final long minTime;
        final float minDistance;
        final LocationListener listener;
        final Executor executor;
        final Looper looper;
        Consumer<Location> consumer;
        boolean oneShot;
        private Location last;

        Registration(String provider, long minTime, float minDistance, LocationListener listener,
                Executor executor, Looper looper) {
            this.provider = provider;
            this.minTime = Math.max(0, minTime);
            this.minDistance = Math.max(0, minDistance);
            this.listener = listener;
            this.executor = executor;
            this.looper = looper;
        }

        int priority() {
            if (NETWORK_PROVIDER.equals(provider)) {
                return com.codename1.location.LocationRequest.PRIORITY_MEDIUM_ACCUARCY;
            }
            if (PASSIVE_PROVIDER.equals(provider)) {
                return com.codename1.location.LocationRequest.PRIORITY_LOW_ACCUARCY;
            }
            return com.codename1.location.LocationRequest.PRIORITY_HIGH_ACCUARCY;
        }

        /// Android's filter: at least `minTime` since the last fix this
        /// registration was given, and at least `minDistance` from it.
        boolean accepts(Location l) {
            if (last == null) {
                last = l;
                return true;
            }
            if (l.getTime() - last.getTime() < minTime) {
                return false;
            }
            if (minDistance > 0 && last.distanceTo(l) < minDistance) {
                return false;
            }
            last = l;
            return true;
        }

        void dispatch(final Location l) {
            run(new Runnable() {
                @Override
                public void run() {
                    if (consumer == null && removed.get()) {
                        return;
                    }
                    if (consumer != null) {
                        consumer.accept(l);
                    } else if (l != null) {
                        listener.onLocationChanged(l);
                    }
                }
            });
        }

        void run(Runnable r) {
            if (executor != null) {
                executor.execute(r);
            } else if (looper != null && !looper.isCurrentThread()) {
                new Handler(looper).post(r);
            } else {
                r.run();
            }
        }
    }
}
