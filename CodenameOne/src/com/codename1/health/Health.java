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
package com.codename1.health;

import com.codename1.health.sensors.HealthSensors;
import com.codename1.health.workout.WorkoutManager;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/// Entry point for the Codename One health API -- reading and writing
/// health data, watching it for changes, recording workouts, and streaming
/// from Bluetooth health sensors.
///
/// Obtain the platform implementation via [#getInstance()]; the returned
/// object is owned by the active port and is never `null`.
///
/// The API is split by role:
/// - [#getStore()] -- the platform health store: samples, aggregates,
///   writes and change subscriptions.
/// - [#getWorkouts()] -- live and recorded workout sessions.
/// - [#getSensors()] -- standard Bluetooth GATT health sensors: heart-rate
///   straps, power meters, scales, blood-pressure cuffs, glucose meters.
///
/// #### Quick start
///
/// ```java
/// Health health = Health.getInstance();
/// if (health.getAvailability() != HealthAvailability.AVAILABLE) {
///     health.openProviderSetup();
///     return;
/// }
/// HealthStore store = health.getStore();
/// store.requestAuthorization(HealthAccess.read(HealthDataType.STEPS))
///      .onResult((asked, err) -> {
///          if (err == null) {
///              readTodaysSteps(store);
///          }
///      });
/// ```
///
/// #### Two things that will surprise you
///
/// **You cannot ask whether you may read.** HealthKit deliberately refuses
/// to disclose read authorization -- a denied read looks exactly like an
/// empty result. So `requestAuthorization` resolving `true` means the user
/// was asked, not that they agreed, and an empty query result means
/// "denied or no data" with no way to tell which. Never render that to a
/// user as "you denied access"; say "no data available". See
/// [HealthStore#getReadAuthorizationStatus(HealthDataType)].
///
/// **Android never wakes your app for new data.** Health Connect has no
/// push mechanism, so subscriptions there are drained when your app runs.
/// Check [HealthSubscription#isPushDelivery()] rather than assuming.
///
/// #### Threading
///
/// Every method may be called from the EDT and returns immediately.
///
/// Change deliveries -- [HealthChangeListener] and
/// [HealthBackgroundListener] -- always arrive on the EDT. A background
/// delivery may run with no visible UI, after the OS relaunched your app.
///
/// **Results of the operations you start do not carry that guarantee.**
/// They arrive on whichever thread produced the answer: the platform SDK's
/// callback thread on iOS and Android, and on desktop, the simulator and
/// JavaScript -- where the store is local rather than platform-backed --
/// the very thread that made the call. So a query started off the EDT can
/// resolve off the EDT. Touch components from those callbacks through
/// `Display.getInstance().callSerially(...)`, or start the operation from
/// the EDT in the first place, which is the usual case and where the
/// distinction never arises.
///
/// #### Platform support
///
/// - **iOS / watchOS** -- HealthKit. Requires the HealthKit entitlement
///   and privacy usage strings; the build fails with an actionable message
///   if they are missing.
/// - **Android** -- Health Connect. Requires the provider app, a
///   privacy-policy declaration and per-type permissions declared through
///   build hints.
///
/// Two capabilities are deliberately **not** claimed by either mobile
/// store in this release, and both stores answer `false` rather than
/// pretending:
///
/// - **Background delivery.** Nothing relaunches or wakes your app for
///   new data on either platform, so subscriptions deliver when you call
///   [HealthStore#drainChanges()] and at no other time. Wire that into
///   your foreground path, or into `BackgroundFetch`. Ask
///   [HealthStore#isBackgroundDeliverySupported()] rather than assuming;
///   HealthKit's `HKObserverQuery` would change this answer on iOS, and
///   nothing here registers one yet.
/// - **Live workout sessions.** Workouts are recorded rather than
///   OS-owned everywhere: your app feeds samples in and the session is
///   written on `end()`. This is what Google documents for Android
///   phones, and it is what iOS does here too even though `HKWorkoutSession`
///   exists on watchOS and iOS 26. Check
///   [com.codename1.health.workout.WorkoutManager#isLiveSessionSupported()]
///   and
///   [com.codename1.health.workout.WorkoutManager#isSensorCollectionSupported()]
///   before designing around either.
/// - **JavaSE simulator** -- a scriptable virtual health store
///   (Simulate -> Health Simulation) with synthetic data, permission
///   scripting and fault injection.
/// - **Desktop and JavaScript** -- no platform store exists, so these
///   report [HealthAvailability#LOCAL_ONLY] and keep data locally. The
///   sensor API works fully wherever Bluetooth LE does.
/// - **tvOS and all other ports** -- this base class is returned as-is and
///   reports health as unsupported; operations fail fast with
///   [HealthError#NOT_SUPPORTED].
public class Health {

    /// Ports construct subclasses. Application code obtains the active
    /// instance via [#getInstance()].
    protected Health() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// On ports without health support this returns a base [Health]
    /// instance that reports the feature as unsupported, so calling code
    /// never needs a `null` check or a platform-specific `if`.
    public static Health getInstance() {
        Health h = Display.getInstance().getHealth();
        return h != null ? h : DEFAULT;
    }

    private static final Health DEFAULT = new Health();

    /// `true` when this port exposes health data in any form. `false` on
    /// the fallback base class.
    public boolean isSupported() {
        return false;
    }

    /// Whether a health store is usable right now, and if not, why. Check
    /// this before anything else -- on Android the provider app may be
    /// missing or out of date, which the user can fix.
    public HealthAvailability getAvailability() {
        return HealthAvailability.NOT_SUPPORTED;
    }

    /// The platform health store. Never null: on ports without one this
    /// returns a no-op store whose every operation fails with
    /// [HealthError#NOT_SUPPORTED]. Branch on
    /// [HealthStore#isSupported()].
    public HealthStore getStore() {
        return DefaultStore.INSTANCE;
    }

    /// Workout recording. Never null.
    public WorkoutManager getWorkouts() {
        return DefaultWorkouts.INSTANCE;
    }

    /// Bluetooth health sensors. Never null, and -- unlike the other two
    /// -- not a no-op on ports without a health store: the sensor layer is
    /// built entirely on `com.codename1.bluetooth.le`, so it works
    /// anywhere Bluetooth LE does, including the desktop and JavaScript
    /// ports.
    public HealthSensors getSensors() {
        return SharedSensors.INSTANCE;
    }

    /// Opens the operating system screen where the user can change health
    /// permissions -- Settings on iOS, the Health Connect permission
    /// screen on Android. Resolves `false` where no such screen exists.
    ///
    /// This is the right response to a query that came back empty when you
    /// expected data, since on iOS you cannot tell denial from absence.
    public AsyncResource<Boolean> openHealthSettings() {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.complete(Boolean.FALSE);
        return out;
    }

    /// Sends the user to install or update the Health Connect provider.
    /// Meaningful when [#getAvailability()] is
    /// [HealthAvailability#PROVIDER_NOT_INSTALLED] or
    /// [HealthAvailability#PROVIDER_UPDATE_REQUIRED]; resolves `false`
    /// elsewhere.
    public AsyncResource<Boolean> openProviderSetup() {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.complete(Boolean.FALSE);
        return out;
    }

    /// Build-configuration problems detected at runtime -- a missing
    /// `ios.NSHealthShareUsageDescription` build hint, an absent Health
    /// Connect privacy-policy declaration. Empty when the app is
    /// configured correctly.
    ///
    /// Operations that need a missing entry throw
    /// [HealthConfigurationException] carrying the same text. This method
    /// reports the same diagnostics without throwing, so a diagnostics
    /// screen or a test can assert on them.
    public List<String> getConfigurationProblems() {
        return new ArrayList<String>();
    }

    /// Lazily allocated so a port that never touches the store does not
    /// pay for one.
    private static final class DefaultStore {
        static final HealthStore INSTANCE = new HealthStore();
    }

    private static final class DefaultWorkouts {
        static final WorkoutManager INSTANCE = new WorkoutManager();
    }

    private static final class SharedSensors {
        static final HealthSensors INSTANCE = new HealthSensors();
    }
}
