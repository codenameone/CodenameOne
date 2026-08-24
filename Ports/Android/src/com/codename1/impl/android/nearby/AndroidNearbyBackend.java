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
package com.codename1.impl.android.nearby;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.companion.WifiDeviceFilter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.net.MacAddress;
import android.os.ParcelUuid;

import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.CodenameOneActivity;
import com.codename1.impl.android.IntentResultListener;
import com.codename1.nearby.NearbyAvailability;
import com.codename1.nearby.NearbyError;
import com.codename1.nearby.companion.CompanionDevices;
import com.codename1.nearby.spi.NearbyBridge;
import com.codename1.ui.Display;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/// The Android nearby implementation, compiled inside the generated app
/// rather than into the port jar.
///
/// It owns the companion-device half directly -- `CompanionDeviceManager` is
/// a framework class and needs no dependency, only an SDK newer than the one
/// the port jar is built against -- and reaches the other two halves
/// reflectively, for the same reason this class is itself reached
/// reflectively: an app that only associates accessories has neither
/// `androidx.core.uwb` nor `play-services-nearby` on its classpath, and the
/// builder has deleted the classes that would import them.
public class AndroidNearbyBackend implements NearbyBridge {

    /// Request code for the association chooser. Picked high to stay clear of
    /// the port's own IntentResultListener constants.
    private static final int ASSOCIATE_REQUEST = 0x4E42;

    /// The activity this backend was built with, used only when the port
    /// has no current one.
    ///
    /// WEAK. This backend is built once and cached for the life of the
    /// process, while Android destroys and recreates the activity freely --
    /// so a strong field here pinned the very first activity, its context
    /// and its whole view hierarchy in memory until the process died, for an
    /// app that associated one accessory at startup and never came back.
    /// Everything with a lifetime of its own uses appContext instead.
    private final WeakReference<Activity> initialActivity;

    /// The application context, which outlives every activity and leaks
    /// nothing by being held.
    private final Context appContext;
    private final NearbyBridge ranging;
    private final NearbyBridge transport;

    /// Guards pendingAssociateRequest.
    ///
    /// The chooser slot is a reservation, and a reservation tested in one
    /// step and taken in another is not one: two callers both read it free
    /// and both took it, the second overwriting the first, and a refusal
    /// then cleared the slot while the first chooser was still open. The
    /// public API does not promise associate() is called from one thread.
    private final Object associateLock = new Object();

    private int pendingAssociateRequest;

    /// Takes the chooser slot for this request, if it is free.
    private boolean reserveAssociate(int requestId) {
        synchronized (associateLock) {
            if (pendingAssociateRequest != 0) {
                return false;
            }
            pendingAssociateRequest = requestId;
            return true;
        }
    }

    /// Gives the slot back, but only if this request still owns it.
    private void releaseAssociate(int requestId) {
        synchronized (associateLock) {
            if (pendingAssociateRequest == requestId) {
                pendingAssociateRequest = 0;
            }
        }
    }

    /// The request holding the slot, or 0.
    private int pendingAssociate() {
        synchronized (associateLock) {
            return pendingAssociateRequest;
        }
    }

    public AndroidNearbyBackend(Activity activity) {
        this.initialActivity = new WeakReference<Activity>(activity);
        // Not from the activity alone. The bridge can be built while the
        // port holds a SERVICE context and no activity at all -- which is
        // exactly the case companion presence creates -- and deriving the
        // application context only from the activity stored null there for
        // the life of the process: the optional backends were constructed
        // with nothing, companion support reported itself unavailable, and
        // a later activity change only rewires the chooser and never went
        // back to repair it.
        Context seed = activity != null ? (Context) activity
                : AndroidImplementation.getContext();
        Context app = seed == null ? null : seed.getApplicationContext();
        this.appContext = app != null ? app : seed;
        this.ranging = load("com.codename1.impl.android.nearby."
                + "AndroidUwbRanging");
        this.transport = load("com.codename1.impl.android.nearby."
                + "AndroidNearbyTransport");
        restorePresence();
    }

    /// Replays presence events that outlived the process they arrived in.
    ///
    /// The platform starts the companion service for a sighting and does not
    /// start the application, so the event lands in an in-memory backlog that
    /// dies with the process if the user never opens the app -- and the
    /// platform does not replay it. The service persists them; this is where
    /// they come back, which is the first thing an app touches on its way to
    /// registering a presence listener.
    private void restorePresence() {
        String[] rows = NearbyPresenceStore.takePersistedPresence(
                appContext);
        for (int i = 0; i < rows.length; i++) {
            // Through the store, so the presence cache is seeded with what
            // is being replayed. Delivering straight to CompanionDevices
            // left getAssociations answering "absent" for the very device
            // the listener had just been told had appeared.
            NearbyPresenceStore.deliverRestored(rows[i]);
        }
    }

    /// The activity the association's result listener is installed on, or
    /// null when none is.
    ///
    /// Weak for the reason initialActivity is, and cleared as soon as the
    /// result settles: an association that completed normally used to leave
    /// its host activity referenced here until the next association replaced
    /// it, which for most apps is never.
    private WeakReference<Activity> listeningOn;

    /// The activity listeningOn refers to, or null once it is gone.
    private Activity listeningActivity() {
        return listeningOn == null ? null : listeningOn.get();
    }

    /// Re-installs the association result listener on the activity that
    /// replaced the one it was on.
    ///
    /// Android delivers the chooser's result to whichever activity is alive
    /// when it closes, and the listener lives on the instance -- so a
    /// recreation mid-chooser sent the result somewhere the backend was not
    /// listening, leaving the association resource unsettled and
    /// pendingAssociateRequest set, which made every later association answer
    /// BUSY. Called from AndroidImplementation.init through
    /// AndroidNearbyBridge, the one place that knows the activity changed.
    public void onActivityChanged() {
        int outstanding = pendingAssociate();
        if (outstanding == 0) {
            return;
        }
        Activity current = currentActivity();
        if (current == null || current == listeningActivity()) {
            return;
        }
        CompanionDeviceManager cdm = manager();
        if (cdm == null || !listenForResult(outstanding, cdm)) {
            // Nothing can answer it now, so it is failed rather than left to
            // hang -- and the pending slot is released so the next
            // association is not refused as BUSY for a chooser nobody is
            // waiting on any more.
            int requestId = outstanding;
            releaseAssociate(requestId);
            listeningOn = null;
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.USER_CANCELED.ordinal(),
                    "the screen was recreated while the device chooser was"
                    + " open; associate again");
        }
    }

    /// The activity to launch from and ask permissions on, now.
    ///
    /// NOT the one this backend was constructed with. The bridge is cached
    /// for the life of the process while Android recreates the activity
    /// freely -- a configuration change, or "Don't keep activities" -- so a
    /// held activity is destroyed long before the app associates a device,
    /// and the chooser was launched on it while the result listener waited on
    /// a host nothing would ever deliver to.
    private Activity currentActivity() {
        Activity current = AndroidImplementation.getActivity();
        return current != null ? current : initialActivity.get();
    }

    /// The context the optional backends hold.
    ///
    /// The application context, not the activity: these live as long as the
    /// bridge does and use it only for package manager, permission and
    /// content-resolver lookups, so holding a destroyed activity would be a
    /// leak with no upside.
    private Context contextForBackends() {
        return appContext;
    }

    private NearbyBridge load(String className) {
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(className);
            instance = clazz.getConstructor(Context.class)
                    .newInstance(contextForBackends());
        } catch (Throwable t) {
            // The builder deletes the half an app did not reference, so this
            // is the ordinary path rather than an error.
            instance = null;
        }
        // Guarded with instanceof rather than cast inside the catch: a failed
        // cast does not throw under ParparVM, so catching one is a handler
        // that never runs.
        return instance instanceof NearbyBridge ? (NearbyBridge) instance
                : null;
    }

    // ------------------------------------------------------------------
    // Shared
    // ------------------------------------------------------------------

    public boolean isRangingSupported() {
        return ranging != null && ranging.isRangingSupported();
    }

    public boolean isTransportSupported() {
        return transport != null && transport.isTransportSupported();
    }

    public boolean isCompanionSupported() {
        return Build.VERSION.SDK_INT >= 26 && manager() != null;
    }

    public int getRangingAvailability() {
        return ranging == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : ranging.getRangingAvailability();
    }

    public int getTransportAvailability() {
        return transport == null ? NearbyAvailability.NOT_SUPPORTED.ordinal()
                : transport.getTransportAvailability();
    }

    public int getCompanionAvailability() {
        return isCompanionSupported()
                ? NearbyAvailability.AVAILABLE.ordinal()
                : NearbyAvailability.NOT_SUPPORTED.ordinal();
    }

    public void requestPermissions(int requestId, int permissionBits) {
        // Owned here rather than delegated to the two optional backends, for
        // two reasons that between them killed the previous arrangement.
        //
        // Delegating by "whichever half is loaded" handed an app that uses
        // both its discovery, advertise and connect bits to the UWB backend,
        // which knows only UWB_RANGING, ignores the rest and reports success
        // -- so the transport grants were never requested and the first
        // advertise failed for a permission the user never saw. Splitting the
        // request in two instead needs the two answers joined into the single
        // result the caller is waiting on, and neither backend can be asked
        // for a partial answer through an SPI whose only reply path is a
        // request id the caller owns.
        //
        // None of this needs an optional dependency: these are platform
        // permission strings and AndroidImplementation.checkForPermission is
        // in the always-compiled half of the port. So one list, one pass, one
        // answer.
        // The APPLICATION context, and checked for null before anything is
        // read off it. Everything below only needs package-manager and
        // permission lookups, which every Context answers, and a bridge
        // cached for the life of the process legitimately has no activity at
        // times -- during a recreation, or when reached from a service after
        // the weak initial activity was collected. Dereferencing one anyway
        // threw out of a method the facade had already registered an
        // EdtResult for, so the exception escaped synchronously and left
        // that permission request pending for good.
        Context ctx = contextForBackends();
        if (ctx == null) {
            com.codename1.nearby.ranging.Ranging
                    .deliverPermissionResult(requestId, false);
            return;
        }
        final ArrayList<String> perms = new ArrayList<String>();
        if ((permissionBits & NearbyBridge.PERMISSION_RANGING) != 0
                && Build.VERSION.SDK_INT >= 31) {
            add(perms, "android.permission.UWB_RANGING", ctx);
        }
        boolean transportBits = (permissionBits
                & (NearbyBridge.PERMISSION_DISCOVERY
                        | NearbyBridge.PERMISSION_ADVERTISE
                        | NearbyBridge.PERMISSION_CONNECT)) != 0;
        if (transportBits) {
            // Worked out by NearbyPermissions, which AndroidNearbyTransport
            // also uses to answer getTransportAvailability -- one list, so
            // the two cannot disagree about what "ready" means. It keys off
            // the app's TARGET as well as the device level, because Android's
            // Bluetooth permission model does: an app targeting 30 on Android
            // 12 uses the legacy permissions and location, and asking it for
            // BLUETOOTH_SCAN left the grant it needed unrequested.
            List<String> transport = NearbyPermissions.transportPermissions(
                    ctx, permissionBits);
            for (int i = 0; i < transport.size(); i++) {
                add(perms, transport.get(i), ctx);
            }
        }
        if (perms.isEmpty()) {
            // Nothing left to ask for -- everything is already granted, or the
            // request was for association, which needs no runtime permission
            // on any Android version because the chooser IS the consent.
            com.codename1.nearby.ranging.Ranging
                    .deliverPermissionResult(requestId, true);
            return;
        }
        // Asking for a grant DOES need an activity, and there may be none.
        // Answered false rather than thrown: the caller is waiting on a
        // result, and "not granted" is both true and something it can act on.
        Activity host = currentActivity();
        if (host == null) {
            com.codename1.nearby.ranging.Ranging
                    .deliverPermissionResult(requestId, false);
            return;
        }
        // checkForPermission blocks through invokeAndBlock and must run on the
        // EDT.
        Display.getInstance().callSerially(
                permissionRunnable(requestId, perms, host));
    }

    /// Adds a permission the app has not already been granted.
    ///
    /// Below API 23 nothing is ever outstanding: permissions are granted at
    /// install time, and Context.checkSelfPermission does not exist there --
    /// calling it threw NoSuchMethodError rather than answering, which a
    /// transport app on Android 5.0 or 5.1 can reach, since the transport's
    /// minimum is 21.
    private void add(ArrayList<String> perms, String permission,
            Context ctx) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (ctx.checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(permission);
        }
    }

    /// Static so the Runnable carries no synthetic outer reference, which
    /// SpotBugs reports as SIC_INNER_SHOULD_BE_STATIC_ANON.
    private static Runnable permissionRunnable(final int requestId,
            final ArrayList<String> perms, final Activity activity) {
        return new Runnable() {
            @Override
            public void run() {
                com.codename1.nearby.ranging.Ranging.deliverPermissionResult(
                        requestId, requestTogether(activity, perms));
            }
        };
    }

    /// Asks for every outstanding permission in ONE prompt.
    ///
    /// Not a loop over AndroidImplementation.checkForPermission: that issues a
    /// one-element requestPermissions, and from Android 12 fine and coarse
    /// location must be requested TOGETHER -- the system shows one dialog with
    /// a precise/approximate choice and rejects a request for fine on its own.
    /// Asked one at a time, the fine request was refused outright, the method
    /// answered false, and the transport could not become authorized without
    /// the app asking a second time.
    ///
    /// #### Parameters
    ///
    /// - `activity`: the foreground activity
    /// - `perms`: every permission the operation needs
    ///
    /// #### Returns
    ///
    /// true when all of them are granted once the prompt closes
    static boolean requestTogether(Activity activity, List<String> perms) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        if (activity == null) {
            return false;
        }
        List<String> missing = new ArrayList<String>();
        for (int i = 0; i < perms.size(); i++) {
            if (activity.checkSelfPermission(perms.get(i))
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(perms.get(i));
            }
        }
        if (missing.isEmpty()) {
            return true;
        }
        if (!(activity instanceof CodenameOneActivity)) {
            return false;
        }
        final CodenameOneActivity host = (CodenameOneActivity) activity;
        host.setRequestForPermission(true);
        host.setWaitingForPermissionResult(true);
        // Request code 1, the one CodenameOneActivity's own result handler
        // expects; it clears the flag whatever the code, but matching keeps
        // this indistinguishable from the port's other permission requests.
        activity.requestPermissions(
                missing.toArray(new String[missing.size()]), 1);
        final List<String> requested = missing;
        final Context checkAgainst = activity.getApplicationContext() != null
                ? activity.getApplicationContext() : (Context) activity;
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                // The flag is instance state, cleared by the activity the
                // result is delivered TO -- and Android delivers it to
                // whichever activity is alive when the dialog closes. So a
                // recreation while the dialog is open leaves THIS instance's
                // flag set for good, and waiting on it alone spun for the
                // life of the process, holding the invokeAndBlock worker and
                // leaving the request unresolved.
                //
                // The wait is handed to the replacement rather than
                // abandoned. Abandoning it answered "not granted" the moment
                // the new activity appeared -- while the dialog was still on
                // screen and the user had not touched it yet, so an app that
                // rotated its screen at the wrong moment was told the user
                // had refused.
                CodenameOneActivity waiting = host;
                long deadline = 0;
                while (waiting.isRequestForPermission()) {
                    // The grant itself, which any context can answer and
                    // which no recreation can hide. This is what ends the
                    // wait when the result reached the replacement before
                    // the swap was noticed and its flag could be set.
                    if (allGranted(checkAgainst, requested)) {
                        return;
                    }
                    Activity current = AndroidImplementation.getActivity();
                    if (current != waiting) {
                        if (!(current instanceof CodenameOneActivity)) {
                            // No CodenameOne activity at all: nothing will
                            // receive the result, so nothing will end this.
                            return;
                        }
                        waiting = (CodenameOneActivity) current;
                        waiting.setRequestForPermission(true);
                        waiting.setWaitingForPermissionResult(true);
                        // Bounded from the swap onwards. If the answer was
                        // delivered before the flag above was set, nothing
                        // will ever clear it -- and a denial is invisible to
                        // the grant check, so only a deadline ends that. It
                        // is generous because the person is being asked a
                        // question; the caller can ask again.
                        deadline = System.currentTimeMillis() + 120000L;
                    }
                    if (deadline != 0
                            && System.currentTimeMillis() > deadline) {
                        return;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        // Asked of the CURRENT context, not the activity captured above,
        // which may be the one that just went away. A grant belongs to the
        // application, so any live context answers for it.
        Activity current = AndroidImplementation.getActivity();
        Context ctx = current != null ? (Context) current
                : activity.getApplicationContext();
        if (ctx == null) {
            return false;
        }
        for (int i = 0; i < perms.size(); i++) {
            if (ctx.checkSelfPermission(perms.get(i))
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /// Whether every one of these permissions is granted right now.
    private static boolean allGranted(Context ctx, List<String> perms) {
        for (int i = 0; i < perms.size(); i++) {
            if (ctx.checkSelfPermission(perms.get(i))
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Ranging
    // ------------------------------------------------------------------

    public int getRangingCapabilities() {
        return ranging == null ? 0 : ranging.getRangingCapabilities();
    }

    public void prepareRangingSession(int requestId, int sessionHandle,
            boolean controller) {
        if (ranging == null) {
            failRanging(requestId);
            return;
        }
        ranging.prepareRangingSession(requestId, sessionHandle, controller);
    }

    public void startRanging(int requestId, int sessionHandle,
            byte[] peerToken) {
        if (ranging == null) {
            failRanging(requestId);
            return;
        }
        ranging.startRanging(requestId, sessionHandle, peerToken);
    }

    public void startAccessoryRanging(int requestId, int sessionHandle,
            byte[] accessoryData) {
        if (ranging == null) {
            failRanging(requestId);
            return;
        }
        ranging.startAccessoryRanging(requestId, sessionHandle, accessoryData);
    }

    public void stopRangingSession(int sessionHandle) {
        if (ranging != null) {
            ranging.stopRangingSession(sessionHandle);
        }
    }

    // ------------------------------------------------------------------
    // Companion
    // ------------------------------------------------------------------

    /// The system service, looked up on the APPLICATION context.
    ///
    /// Not on the current activity. CompanionDeviceManager is a system
    /// service like any other and every context answers for it -- but keying
    /// the lookup off an activity meant that during a recreation, or when
    /// this process-lived bridge is reached from a service after its weak
    /// activity reference was collected, isCompanionSupported reported
    /// false, getAssociations answered with an empty list, and disassociation
    /// and presence failed. All of it for a manager that was available the
    /// whole time. An activity is needed to LAUNCH the chooser, and that is
    /// where it is required.
    private CompanionDeviceManager manager() {
        if (Build.VERSION.SDK_INT < 26 || appContext == null) {
            return null;
        }
        try {
            return (CompanionDeviceManager) appContext.getSystemService(
                    Context.COMPANION_DEVICE_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    public void associate(final int requestId, int profile,
            boolean singleDevice, String[] filters) {
        final CompanionDeviceManager cdm = manager();
        if (cdm == null) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.NOT_SUPPORTED.ordinal(),
                    "companion association needs Android 8 or later");
            return;
        }
        // Reserved HERE, where it is tested. Everything between this and the
        // chooser opening gives it back on the way out.
        if (!reserveAssociate(requestId)) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.BUSY.ordinal(),
                    "an association chooser is already open");
            return;
        }
        AssociationRequest.Builder request = new AssociationRequest.Builder();
        request.setSingleDevice(singleDevice);
        // A profile this Android version does not have FAILS the request.
        //
        // profileFor returns null both for GENERIC, which wants no profile at
        // all, and for a profile that arrived too early -- WATCH below API 31,
        // COMPUTER below 33, GLASSES below 34. Treating the two alike
        // submitted a generic association for a caller that asked for an
        // elevated one, so the chooser succeeded WITHOUT the privileges
        // requested and handed back a device reporting GENERIC. A profile is
        // not a preference to drop quietly.
        if (profile != 0) {
            String deviceProfile = Build.VERSION.SDK_INT >= 31
                    ? profileFor(profile) : null;
            if (deviceProfile == null) {
                releaseAssociate(requestId);
                CompanionDevices.deliverRequestFailed(requestId,
                        NearbyError.NOT_SUPPORTED.ordinal(),
                        "this Android version has no companion profile "
                        + profile + "; associate with CompanionProfile.GENERIC"
                        + " or check CompanionDevices.isSupported first");
                return;
            }
            request.setDeviceProfile(deviceProfile);
        }
        // A supplied filter that cannot be installed FAILS the request. It
        // used to be ignored, which quietly turned "show me only devices
        // matching this" into "show me everything" -- and the user could then
        // associate the wrong accessory from a picker that was never supposed
        // to offer it. A malformed service UUID or name pattern is a mistake
        // worth reporting, not one worth widening.
        for (int i = 0; filters != null && i < filters.length; i++) {
            if (!addFilter(request, filters[i])) {
                releaseAssociate(requestId);
                CompanionDevices.deliverRequestFailed(requestId,
                        NearbyError.INVALID_TOKEN.ordinal(),
                        "this device filter could not be used: " + filters[i]);
                return;
            }
        }
        // No filter is added when the caller gave none. An empty
        // BluetoothLeDeviceFilter is NOT the neutral choice it looks like: a
        // request carrying one restricts the chooser to a BLE scan, so classic
        // Bluetooth and Wi-Fi companions vanish from the very case that asked
        // to see everything. A request with no filters at all is what makes
        // the platform scan all three transports, which is what the portable
        // API promises for an empty filter list.
        if (!listenForResult(requestId, cdm)) {
            // Nothing would ever answer this request, so it is refused now
            // rather than left pending while another flow takes its result.
            releaseAssociate(requestId);
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.BUSY.ordinal(),
                    "another activity result is outstanding; try again when"
                    + " it has finished");
            return;
        }
        // associate() can refuse SYNCHRONOUSLY -- a SecurityException for a
        // profile whose REQUEST_COMPANION_PROFILE_* permission the manifest
        // does not declare, which happens when the matching
        // android.nearby.*Profile hint was not set. Unguarded, that escaped
        // past this method with pendingAssociateRequest still set and the
        // result listener still installed: the AsyncResource never settled
        // and every later association answered BUSY.
        try {
            associateNow(cdm, request.build(), requestId);
        } catch (Throwable refused) {
            releaseAssociate(requestId);
            releaseResultListener();
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.NOT_SUPPORTED.ordinal(),
                    "the platform refused this association request: "
                    + refused.getMessage());
        }
    }

    /// The associate call itself, split out so the caller can catch a
    /// synchronous refusal without wrapping the callback wiring too.
    @SuppressLint("MissingPermission")
    private void associateNow(CompanionDeviceManager cdm,
            AssociationRequest request, final int requestId) {
        cdm.associate(request, new CompanionDeviceManager.Callback() {
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                launch(chooserLauncher, requestId);
            }

            @Override
            public void onFailure(CharSequence error) {
                // Still ours? The platform can answer long after an activity
                // recreation released this request and a new chooser took
                // the slot, and releaseResultListener is NOT owner-checked:
                // a stale failure tore down the live request's listener, so
                // its chooser result went nowhere and its resource never
                // settled. launch() checks the same thing for the same
                // reason.
                if (pendingAssociate() != requestId) {
                    return;
                }
                releaseAssociate(requestId);
                // The listener was installed before associate() was called,
                // and installing one marks CodenameOneActivity as waiting for
                // a result. Leaving it there when no chooser is ever launched
                // wedges the whole activity-result channel: the camera, the
                // scanner and every other startActivityForResult caller then
                // cannot install their own listener and their results arrive
                // here instead.
                releaseResultListener();
                CompanionDevices.deliverRequestFailed(requestId,
                        NearbyError.PEER_UNAVAILABLE.ordinal(),
                        error == null ? null : error.toString());
            }
        }, new Handler(Looper.getMainLooper()));
    }

    private void launch(IntentSender chooserLauncher, int requestId) {
        // Still ours? The platform keeps searching after associate() returns
        // and answers on a later main-looper turn, and an activity
        // recreation in between can fail this request and give the slot
        // back. Launching anyway put a chooser on screen for a resource that
        // had already failed, and sent its result to whatever result flow the
        // replacement activity had installed by then.
        if (pendingAssociate() != requestId) {
            return;
        }
        // This runs from the platform's callback, which is a main-looper hop
        // after the activity was checked -- long enough for it to have gone.
        // Failed rather than thrown, for the reason requestPermissions is.
        Activity host = currentActivity();
        if (host == null) {
            releaseAssociate(requestId);
            releaseResultListener();
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.USER_CANCELED.ordinal(),
                    "the screen went away before the device chooser could"
                    + " open; associate again");
            return;
        }
        try {
            host.startIntentSenderForResult(chooserLauncher,
                    ASSOCIATE_REQUEST, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            releaseAssociate(requestId);
            // Same as the onFailure path: nothing will come back through the
            // listener, so it must not stay installed.
            releaseResultListener();
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.UNKNOWN.ordinal(), e.getMessage());
        }
    }

    /// Hands the activity-result channel back, so the next
    /// startActivityForResult caller can install its own listener.
    private void releaseResultListener() {
        listeningOn = null;
        Activity current = currentActivity();
        if (current instanceof CodenameOneActivity) {
            ((CodenameOneActivity) current).restoreIntentResultListener();
        }
    }

    /// Installs the result listener for the association chooser.
    ///
    /// #### Returns
    ///
    /// true when the listener is in place, false when the activity-result
    /// channel could not take it -- in which case the chooser must not be
    /// launched at all
    private boolean listenForResult(final int requestId,
            final CompanionDeviceManager cdm) {
        Activity current = currentActivity();
        if (!(current instanceof CodenameOneActivity)) {
            return false;
        }
        // setIntentResultListener SILENTLY ignores a registration while
        // another activity-result flow is outstanding -- the camera, the
        // scanner, anything that called startActivityForResult. Launching
        // the chooser anyway sent its result to that other listener and left
        // this request's AsyncResource pending for good, so the caller is
        // told the truth instead.
        if (((CodenameOneActivity) current).isWaitingForResult()) {
            return false;
        }
        // Taken BEFORE the chooser opens, so the association it creates can be
        // told apart from the ones this app already had.
        final Set<String> before = associationKeys(cdm);
        final CodenameOneActivity host = (CodenameOneActivity) current;
        listeningOn = new WeakReference<Activity>(current);
        host.setIntentResultListener(new IntentResultListener() {
            public void onActivityResult(int requestCode, int resultCode,
                    Intent data) {
                if (requestCode != ASSOCIATE_REQUEST) {
                    return;
                }
                host.restoreIntentResultListener();
                // Dropped here, not only when the next association replaces
                // it: the flow this listener belongs to is over.
                listeningOn = null;
                releaseAssociate(requestId);
                if (resultCode != Activity.RESULT_OK) {
                    CompanionDevices.deliverRequestFailed(requestId,
                            NearbyError.USER_CANCELED.ordinal(),
                            "the user dismissed the chooser");
                    return;
                }
                String encoded = newestAssociation(cdm, data, before);
                if (encoded == null) {
                    CompanionDevices.deliverRequestFailed(requestId,
                            NearbyError.UNKNOWN.ordinal(),
                            "the chooser returned no device");
                } else {
                    CompanionDevices.deliverAssociated(requestId, encoded);
                }
            }
        });
        return true;
    }

    /// The association the chooser just created.
    ///
    /// Read back from the platform rather than from the returned intent
    /// wherever possible: on API 33 and later the association carries an id
    /// and a display name the intent extra does not, and that id is what
    /// `disassociate` and presence observation take.
    ///
    /// Identified three ways, in descending order of certainty, because
    /// "the last one in the list" is not one of them -- `getMyAssociations`
    /// documents no order, so an app that already held associations could be
    /// handed one the user did not pick:
    ///
    /// 1. `EXTRA_ASSOCIATION`, which API 33 puts in the result intent and
    ///    which names the association directly;
    /// 2. the one association missing from the snapshot taken before the
    ///    chooser opened;
    /// 3. the intent's device extra, which is all API 26 through 32 offer.
    ///
    /// #### Parameters
    ///
    /// - `cdm`: the platform manager
    /// - `data`: the chooser's result intent
    /// - `before`: the association keys this app held before the chooser ran
    @SuppressLint("MissingPermission")
    private String newestAssociation(CompanionDeviceManager cdm, Intent data,
            Set<String> before) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (data != null) {
                Object association = data.getParcelableExtra(
                        CompanionDeviceManager.EXTRA_ASSOCIATION);
                if (association instanceof AssociationInfo) {
                    return encode((AssociationInfo) association, true);
                }
            }
            List<AssociationInfo> all = cdm.getMyAssociations();
            AssociationInfo fresh = null;
            for (int i = 0; all != null && i < all.size(); i++) {
                if (!before.contains(idOf(all.get(i)))) {
                    if (fresh != null) {
                        // Two new ones means something else associated while
                        // the chooser was open; neither can be claimed as the
                        // user's pick, so fall through to the intent extra.
                        fresh = null;
                        break;
                    }
                    fresh = all.get(i);
                }
            }
            if (fresh != null) {
                return encode(fresh, true);
            }
        }
        if (data != null) {
            Object extra = data.getParcelableExtra(
                    CompanionDeviceManager.EXTRA_DEVICE);
            if (extra instanceof BluetoothDevice) {
                BluetoothDevice d = (BluetoothDevice) extra;
                return encodeLegacy(d.getAddress(), d.getAddress(), true);
            }
        }
        List<String> legacy = cdm.getAssociations();
        for (int i = 0; legacy != null && i < legacy.size(); i++) {
            if (!before.contains(legacy.get(i))) {
                String mac = legacy.get(i);
                return encodeLegacy(mac, mac, true);
            }
        }
        return null;
    }

    /// The keys of every association this app currently holds: the API 33 id
    /// where there is one, the MAC address below that.
    @SuppressLint("MissingPermission")
    private Set<String> associationKeys(CompanionDeviceManager cdm) {
        Set<String> out = new HashSet<String>();
        if (cdm == null) {
            return out;
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                List<AssociationInfo> all = cdm.getMyAssociations();
                for (int i = 0; all != null && i < all.size(); i++) {
                    out.add(idOf(all.get(i)));
                }
                return out;
            }
            List<String> legacy = cdm.getAssociations();
            for (int i = 0; legacy != null && i < legacy.size(); i++) {
                out.add(legacy.get(i));
            }
        } catch (Throwable notPermitted) {
            // Reading associations needs no permission, but a manufacturer
            // build that throws here must not take the association with it:
            // an empty snapshot only costs the fallback path.
        }
        return out;
    }

    @SuppressLint("MissingPermission")
    public String[] getAssociations() {
        CompanionDeviceManager cdm = manager();
        if (cdm == null) {
            return new String[0];
        }
        // With the presence each association was last reported with, not a
        // flat false. CompanionDevice.isPresent() tells the app to re-read
        // the association for a current answer, and re-reading turned a
        // device that had just appeared back into one that was not there.
        List<String> out = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= 33) {
            List<AssociationInfo> all = cdm.getMyAssociations();
            for (int i = 0; all != null && i < all.size(); i++) {
                out.add(encode(all.get(i),
                        NearbyPresenceStore.isPresent(idOf(all.get(i)))));
            }
        } else {
            List<String> legacy = cdm.getAssociations();
            for (int i = 0; legacy != null && i < legacy.size(); i++) {
                out.add(encodeLegacy(legacy.get(i), legacy.get(i),
                        NearbyPresenceStore.isPresent(legacy.get(i))));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    @SuppressLint("MissingPermission")
    public void disassociate(int requestId, String associationId) {
        CompanionDeviceManager cdm = manager();
        if (cdm == null || associationId == null) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.NOT_SUPPORTED.ordinal(), null);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                List<AssociationInfo> all = cdm.getMyAssociations();
                for (int i = 0; all != null && i < all.size(); i++) {
                    if (idOf(all.get(i)).equals(associationId)) {
                        cdm.disassociate(all.get(i).getId());
                        CompanionDevices.deliverDisassociated(requestId);
                        return;
                    }
                }
                CompanionDevices.deliverRequestFailed(requestId,
                        NearbyError.PEER_UNAVAILABLE.ordinal(),
                        "no such association");
                return;
            }
            cdm.disassociate(associationId);
            CompanionDevices.deliverDisassociated(requestId);
        } catch (Throwable t) {
            CompanionDevices.deliverRequestFailed(requestId,
                    NearbyError.UNKNOWN.ordinal(), t.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public boolean startObservingPresence(String associationId) {
        CompanionDeviceManager cdm = manager();
        if (cdm == null || associationId == null
                || Build.VERSION.SDK_INT < 31) {
            return false;
        }
        try {
            // An association with no MAC -- a Wi-Fi or self-managed companion
            // -- cannot use the address overload: addressOf falls back to the
            // numeric association id, which that overload rejects as not a
            // MAC address, and the exception was swallowed into a bare false.
            //
            // It was suggested the association-id overload arrived in API 33.
            // It did not: android.companion.ObservingDevicePresenceRequest,
            // and the startObservingDevicePresence(request) that takes it,
            // are API 36 -- javap over android-33 through android-35 shows
            // only the String overload. So this is the honest split: use the
            // request where it exists, and where it does not, say plainly
            // that the platform cannot observe this association rather than
            // failing with no reason.
            if (macOf(cdm, associationId) == null) {
                if (Build.VERSION.SDK_INT >= 36
                        && observeByAssociationId(cdm, associationId)) {
                    NearbyPresenceStore.register(associationId);
                    return true;
                }
                Log.w("CN1", "com.codename1.nearby.companion: this Android"
                        + " version can only observe an association that has"
                        + " a Bluetooth address, and association "
                        + associationId + " has none. Presence observation"
                        + " for it needs Android 16 or later.");
                return false;
            }
            cdm.startObservingDevicePresence(addressOf(cdm, associationId));
            NearbyPresenceStore.register(associationId);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /// Observes by association id, which only API 36 can do.
    ///
    /// Reached reflectively so the port still compiles against the SDK 33
    /// floor the rest of the nearby package needs; referencing
    /// ObservingDevicePresenceRequest directly would raise that floor to 36
    /// for every app that merely associates a device.
    ///
    /// #### Parameters
    ///
    /// - `cdm`: the platform manager
    /// - `associationId`: the association to watch
    ///
    /// #### Returns
    ///
    /// true when the platform accepted the request
    private static boolean observeByAssociationId(CompanionDeviceManager cdm,
            String associationId) {
        try {
            int numeric = Integer.parseInt(associationId);
            Class<?> builderClass = Class.forName(
                    "android.companion.ObservingDevicePresenceRequest$Builder");
            Object builder = builderClass.newInstance();
            builderClass.getMethod("setAssociationId", int.class)
                    .invoke(builder, Integer.valueOf(numeric));
            Object request = builderClass.getMethod("build").invoke(builder);
            Class<?> requestClass = Class.forName(
                    "android.companion.ObservingDevicePresenceRequest");
            CompanionDeviceManager.class
                    .getMethod("startObservingDevicePresence", requestClass)
                    .invoke(cdm, request);
            return true;
        } catch (Throwable notAvailable) {
            return false;
        }
    }

    /// The API 36 counterpart of observeByAssociationId.
    private static void stopObservingByAssociationId(
            CompanionDeviceManager cdm, String associationId) {
        try {
            int numeric = Integer.parseInt(associationId);
            Class<?> builderClass = Class.forName(
                    "android.companion.ObservingDevicePresenceRequest$Builder");
            Object builder = builderClass.newInstance();
            builderClass.getMethod("setAssociationId", int.class)
                    .invoke(builder, Integer.valueOf(numeric));
            Object request = builderClass.getMethod("build").invoke(builder);
            Class<?> requestClass = Class.forName(
                    "android.companion.ObservingDevicePresenceRequest");
            CompanionDeviceManager.class
                    .getMethod("stopObservingDevicePresence", requestClass)
                    .invoke(cdm, request);
        } catch (Throwable notAvailable) {
            // Stopping something the platform is not watching is not a
            // failure the caller can act on.
        }
    }

    /// The MAC of an association, or null when it has none.
    @SuppressLint("MissingPermission")
    private static String macOf(CompanionDeviceManager cdm,
            String associationId) {
        if (Build.VERSION.SDK_INT < 33) {
            // Below 33 the id IS the address; there is nothing else to hold.
            return associationId;
        }
        List<AssociationInfo> all = cdm.getMyAssociations();
        for (int i = 0; all != null && i < all.size(); i++) {
            if (idOf(all.get(i)).equals(associationId)) {
                return macOf(all.get(i));
            }
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public void stopObservingPresence(String associationId) {
        CompanionDeviceManager cdm = manager();
        if (cdm == null || associationId == null
                || Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            if (macOf(cdm, associationId) == null) {
                if (Build.VERSION.SDK_INT >= 36) {
                    stopObservingByAssociationId(cdm, associationId);
                }
                NearbyPresenceStore.unregister(associationId);
                return;
            }
            cdm.stopObservingDevicePresence(addressOf(cdm, associationId));
            NearbyPresenceStore.unregister(associationId);
        } catch (Throwable t) {
            // Nothing to report: the caller asked to stop and it is stopped
            // either way.
        }
    }

    // ------------------------------------------------------------------
    // Transport
    // ------------------------------------------------------------------

    public int getMaxPayloadSize() {
        return transport == null ? 0 : transport.getMaxPayloadSize();
    }

    public void startAdvertising(int requestId, String serviceId,
            String localName, int strategy) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.startAdvertising(requestId, serviceId, localName, strategy);
    }

    public void stopAdvertising() {
        if (transport != null) {
            transport.stopAdvertising();
        }
    }

    public void startDiscovery(int requestId, String serviceId, int strategy) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.startDiscovery(requestId, serviceId, strategy);
    }

    public void stopDiscovery() {
        if (transport != null) {
            transport.stopDiscovery();
        }
    }

    public void requestConnection(int requestId, String endpointId,
            String localName) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.requestConnection(requestId, endpointId, localName);
    }

    public void acceptConnection(int requestId, String endpointId) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.acceptConnection(requestId, endpointId);
    }

    public void rejectConnection(String endpointId) {
        if (transport != null) {
            transport.rejectConnection(endpointId);
        }
    }

    public void sendPayload(int requestId, String[] endpointIds, int payloadId,
            int payloadType, byte[] bytes, String path) {
        if (transport == null) {
            failTransport(requestId);
            return;
        }
        transport.sendPayload(requestId, endpointIds, payloadId, payloadType,
                bytes, path);
    }

    public void cancelPayload(int payloadId) {
        if (transport != null) {
            transport.cancelPayload(payloadId);
        }
    }

    public void disconnect(String endpointId) {
        if (transport != null) {
            transport.disconnect(endpointId);
        }
    }

    public void stopAllTransport() {
        if (transport != null) {
            transport.stopAllTransport();
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /// The platform profile role names, written out rather than referenced.
    ///
    /// AssociationRequest.DEVICE_PROFILE_GLASSES is API 34, and the nearby
    /// compile-SDK floor is 33 -- so naming that constant would fail to
    /// compile for an app built against exactly 33, which the builder allows.
    /// These are compile-time String constants in the platform too, and their
    /// values are stable role names, so the literal is what the constant
    /// would have inlined anyway and it also works where the constant does
    /// not exist yet.
    private static final String PROFILE_WATCH =
            "android.app.role.COMPANION_DEVICE_WATCH";
    private static final String PROFILE_GLASSES =
            "android.app.role.COMPANION_DEVICE_GLASSES";
    private static final String PROFILE_COMPUTER =
            "android.app.role.COMPANION_DEVICE_COMPUTER";

    private static String profileFor(int profile) {
        // The ordinals of com.codename1.nearby.companion.CompanionProfile.
        if (Build.VERSION.SDK_INT < 31) {
            return null;
        }
        switch (profile) {
            case 1:
                return PROFILE_WATCH;
            case 2:
                // GLASSES is API 34 and COMPUTER is 33 -- not the other way
                // round, which is the order the enum happens to declare them
                // in. Passing the platform a profile string it does not know
                // throws, so these two gates were checked against the SDK's
                // own api-versions.xml rather than guessed from the ordinal.
                return Build.VERSION.SDK_INT >= 34 ? PROFILE_GLASSES : null;
            case 3:
                return Build.VERSION.SDK_INT >= 33 ? PROFILE_COMPUTER : null;
            default:
                // GENERIC. Deliberately no profile at all rather than a
                // harmless-looking one: a profile is a request for elevated
                // privileges and shows the user a stronger prompt.
                return null;
        }
    }

    /// The CompanionProfile ordinal an association was made under.
    ///
    /// #### Parameters
    ///
    /// - `info`: the association
    ///
    /// #### Returns
    ///
    /// the ordinal, or 0 for GENERIC and for anything this API does not model
    static int profileOrdinalOf(AssociationInfo info) {
        if (info == null || Build.VERSION.SDK_INT < 33) {
            return 0;
        }
        String profile;
        try {
            profile = info.getDeviceProfile();
        } catch (Throwable unreadable) {
            return 0;
        }
        if (PROFILE_WATCH.equals(profile)) {
            return 1;
        }
        if (PROFILE_GLASSES.equals(profile)) {
            return 2;
        }
        if (PROFILE_COMPUTER.equals(profile)) {
            return 3;
        }
        // Null, or one of the profiles the portable API does not model --
        // app streaming, automotive projection. GENERIC is the honest answer
        // for both.
        return 0;
    }

    private static boolean addFilter(AssociationRequest.Builder request,
            String encoded) {
        String[] fields = encoded == null ? null : encoded.split("\t", -1);
        if (fields == null || fields.length < 2) {
            return false;
        }
        int kind;
        try {
            kind = Integer.parseInt(fields[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        String value = fields[1];
        // The kind constants of com.codename1.nearby.companion.DeviceFilter.
        if (kind == 0) {
            try {
                request.addDeviceFilter(new BluetoothLeDeviceFilter.Builder()
                        .setScanFilter(new android.bluetooth.le.ScanFilter
                                .Builder()
                                .setServiceUuid(ParcelUuid.fromString(
                                        expandUuid(value)))
                                .build())
                        .build());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        if (kind == 1) {
            try {
                request.addDeviceFilter(new BluetoothLeDeviceFilter.Builder()
                        .setNamePattern(Pattern.compile(value))
                        .build());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        if (kind == 2) {
            // Guarded like the two above. setAddress and build() throw
            // IllegalArgumentException for anything that is not a MAC, and
            // this was the one branch that let it escape -- past the caller,
            // out of the backend, and into application code, leaving the
            // AsyncResource that associate() had already registered orphaned
            // instead of failing with INVALID_TOKEN.
            try {
                request.addDeviceFilter(
                        new android.companion.BluetoothDeviceFilter.Builder()
                                .setAddress(value)
                                .build());
                return true;
            } catch (Throwable notAnAddress) {
                return false;
            }
        }
        if (kind == 3) {
            try {
                request.addDeviceFilter(new WifiDeviceFilter.Builder()
                        .setNamePattern(Pattern.compile(Pattern.quote(value)))
                        .build());
                return true;
            } catch (Throwable notUsable) {
                return false;
            }
        }
        return false;
    }

    /// Expands the 16-bit short form of a Bluetooth UUID into the full one,
    /// which is what `ParcelUuid` requires. `"180D"` and the spelled-out
    /// 128-bit form must both work, because the portable API documents both.
    private static String expandUuid(String uuid) {
        String u = uuid.trim();
        if (u.length() == 4) {
            return "0000" + u + "-0000-1000-8000-00805F9B34FB";
        }
        if (u.length() == 8) {
            return u + "-0000-1000-8000-00805F9B34FB";
        }
        return u;
    }

    /// The association's MAC address as a string, or null when it has none.
    ///
    /// `AssociationInfo.getDeviceMacAddress()` returns an `android.net
    /// .MacAddress`, not a string -- the string-returning form is a hidden
    /// API that a normal app cannot call. Its `toString()` is the
    /// colon-separated lowercase form, which is what
    /// `startObservingDevicePresence` and `disassociate` take.
    private static String macOf(AssociationInfo info) {
        MacAddress address = info.getDeviceMacAddress();
        return address == null ? null : address.toString();
    }

    /// The association's id: the platform's, not its MAC address.
    ///
    /// AssociationInfo exists only from API 33, and from there every
    /// association has an id of its own. The MAC does not: one device can
    /// hold SEVERAL associations, and giving them all the address they share
    /// meant getAssociations handed back duplicate ids, the newly created
    /// association could not be told from the ones already held, and
    /// disassociate removed whichever of them it met first.
    ///
    /// It also makes the API 36 presence calls work at all. They take an
    /// association id and parse this string to get one, so an id that was a
    /// MAC address threw every time and the whole path failed silently.
    ///
    /// Below 33 the address IS the id -- there is no AssociationInfo to take
    /// one from -- and that half is unchanged.
    private static String idOf(AssociationInfo info) {
        return Integer.toString(info.getId());
    }

    private static String encode(AssociationInfo info, boolean present) {
        String mac = macOf(info);
        CharSequence name = info.getDisplayName();
        return join(idOf(info), name == null ? "" : name.toString(),
                mac == null ? "" : mac, profileOrdinalOf(info), present);
    }

    private static String encodeLegacy(String id, String mac,
            boolean present) {
        // No AssociationInfo below API 33, so no profile to read either.
        return join(id, mac == null ? "" : mac, mac == null ? "" : mac,
                0, present);
    }

    /// Builds the record `com.codename1.impl.nearby.NearbyWire` decodes.
    ///
    /// The profile is read back from the association rather than hardcoded to
    /// GENERIC: AssociationInfo.getDeviceProfile reports the one the
    /// association was made under from API 33, and reporting GENERIC for a
    /// watch contradicted CompanionDevice.getProfile and left an app unable
    /// to tell its profile-specific companions apart.
    private static String join(String id, String name, String address,
            int profileOrdinal, boolean present) {
        return sanitize(id) + '\t' + sanitize(name) + '\t' + sanitize(address)
                + '\t' + profileOrdinal + '\t' + (present ? '1' : '0');
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    @SuppressLint("MissingPermission")
    private static String addressOf(CompanionDeviceManager cdm, String id) {
        if (Build.VERSION.SDK_INT >= 33) {
            List<AssociationInfo> all = cdm.getMyAssociations();
            for (int i = 0; all != null && i < all.size(); i++) {
                if (idOf(all.get(i)).equals(id)) {
                    String mac = macOf(all.get(i));
                    if (mac != null) {
                        return mac;
                    }
                }
            }
        }
        return id;
    }

    private static void failRanging(int requestId) {
        com.codename1.nearby.ranging.Ranging.deliverRequestFailed(requestId,
                NearbyError.NOT_SUPPORTED.ordinal(),
                "this build does not include precision ranging");
    }

    private static void failTransport(int requestId) {
        com.codename1.nearby.transport.NearbyTransport.deliverRequestFailed(
                requestId, NearbyError.NOT_SUPPORTED.ordinal(),
                "this build does not include the nearby transport");
    }
}
