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
package com.codename1.impl.android.call;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import com.codename1.call.CallAvailability;
import com.codename1.call.CallError;
import com.codename1.call.CallHandle;
import com.codename1.call.CallHandleType;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.Calls;
import com.codename1.call.spi.CallBridge;
import com.codename1.impl.call.CallWire;
import com.codename1.ui.Display;

/// The Android half of `com.codename1.call`, on Telecom.
///
/// #### Everything here is guarded on API 26
///
/// A **self-managed** `ConnectionService` -- an app owning its own calls
/// rather than managing the SIM's -- arrives exactly at API 26, and below it
/// Telecom offers nothing to degrade to. So the port reports the capability
/// absent rather than the app's minimum being raised, and application code
/// branches on `Calls.isSupported()`.
///
/// #### Configuring is not optional and its absence is silent
///
/// `TelecomManager.addNewIncomingCall` **does nothing at all** if no
/// `PhoneAccount` has been registered, or if one was registered without
/// `CAPABILITY_SELF_MANAGED`: no exception, no log line, no call. That is why
/// [#configureProvider] exists as its own step and why every report checks it
/// first -- an answer of `CALL_REFUSED` is a great deal more use than the
/// silence the platform offers.
public class AndroidCallBridge implements CallBridge {

    private static final int MIN_SELF_MANAGED_SDK = 26;

    private final Context context;
    private PhoneAccountHandle handle;
    private boolean configured;

    public AndroidCallBridge(Context context) {
        this.context = context;
    }

    private TelecomManager telecom() {
        return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    private static boolean selfManagedAvailable() {
        return Build.VERSION.SDK_INT >= MIN_SELF_MANAGED_SDK;
    }

    @Override
    public boolean isCallSupported() {
        return selfManagedAvailable() && telecom() != null;
    }

    @Override
    public boolean isVoipPushSupported() {
        // FALSE, deliberately, and it is not a gap.
        //
        // com.codename1.call.voip exists for one reason: iOS reports a pushed
        // call to the system BEFORE any of the app's code runs, and kills the
        // app when it does not, so the framework has to own that path.
        // Android imposes no such deadline -- a high-priority FCM message
        // arrives in Java like any other -- so the natural Android answer is
        // for the app to call Calls.reportIncoming from its own push
        // callback, which works today and needs nothing from this facade.
        //
        // Saying true here handed apps a register() that resolved with an
        // empty token and a listener nothing ever called. Reporting the truth
        // sends them to the path that works.
        return false;
    }

    @Override
    public boolean isDirectorySupported() {
        // CallScreeningService is API 24, but the role that makes it fire is
        // API 29; below that an app can be granted screening only through a
        // dialog this API does not expose.
        return Build.VERSION.SDK_INT >= 29;
    }

    @Override
    public int getCallCapabilities() {
        if (!isCallSupported()) {
            return 0;
        }
        int caps = CAPABILITY_SYSTEM_UI | CAPABILITY_OUTGOING | CAPABILITY_HOLD
                | CAPABILITY_MUTE | CAPABILITY_DTMF | CAPABILITY_VIDEO;
        if (isDirectorySupported()) {
            // SCREENING only, not DIRECTORY. A CallScreeningService may allow,
            // reject or silence a call; Android offers a third-party app no
            // way to put a LABEL on an incoming call, which is what
            // CAPABILITY_DIRECTORY promises. Blocking entries work here and
            // labels are ignored, so claiming identification meant a
            // label-only list was accepted and then did nothing at all.
            caps |= CAPABILITY_SCREENING;
        }
        // No CAPABILITY_VOIP_PUSH: see isVoipPushSupported.
        // Deliberately no CAPABILITY_GROUPING or CAPABILITY_ROUTE_PICKER:
        // Telecom conferences self-managed calls only through a
        // ConnectionService conference this port does not build, and there is
        // no system route picker to show.
        return caps;
    }

    @Override
    public int getCallAvailability() {
        if (!isCallSupported()) {
            return CallAvailability.UNSUPPORTED.ordinal();
        }
        if ((getGrantedPermissions() & PERMISSION_MANAGE_CALLS) == 0) {
            return CallAvailability.NOT_PERMITTED.ordinal();
        }
        TelecomManager tm = telecom();
        if (tm != null && Build.VERSION.SDK_INT >= 26 && tm.isInCall()
                && !CN1ConnectionService.hasOwnCalls()) {
            // isInCall() is true for THIS app's own self-managed call too,
            // and OTHER_APP_IN_CALL means another application -- so reporting
            // it while the app was in its own call told that app not to
            // report a second one, which Telecom would have accepted from the
            // same account. Checking our own connections is the only way to
            // tell the two apart; with none of ours up, somebody else's is
            // the honest reading.
            return CallAvailability.OTHER_APP_IN_CALL.ordinal();
        }
        return CallAvailability.AVAILABLE.ordinal();
    }

    @Override
    public int getGrantedPermissions() {
        int mask = 0;
        if (granted("android.permission.MANAGE_OWN_CALLS")) {
            mask |= PERMISSION_MANAGE_CALLS;
        }
        if (granted(Manifest.permission.RECORD_AUDIO)) {
            mask |= PERMISSION_MICROPHONE;
        }
        if (granted(Manifest.permission.CAMERA)) {
            mask |= PERMISSION_CAMERA;
        }
        if (Build.VERSION.SDK_INT < 33
                || granted("android.permission.POST_NOTIFICATIONS")) {
            mask |= PERMISSION_NOTIFICATIONS;
        }
        return mask;
    }

    private boolean granted(String permission) {
        return context.checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestPermissions(final int requestId, final int permissionBits) {
        // Actually asks. Reporting the current mask and stopping there meant
        // an app calling the method whose contract says it REQUESTS the bits
        // always saw a denial, and had to reach for another permission API to
        // get call audio at all.
        //
        // MANAGE_OWN_CALLS is exempt: it is a normal permission granted at
        // install time, so there is nothing to prompt for and asking would
        // show the user a dialog that cannot change anything.
        //
        // Off the EDT because checkForPermission blocks on the dialog, and
        // the answer is delivered through the facade, which marshals back.
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                // One sequence at a time. checkForPermission blocks on a
                // dialog behind an activity-wide flag and request code, so two
                // workers running at once share them: the first result clears
                // the flag for both, and the second returned the mask as it
                // stood with its OWN dialog still on screen -- reporting a
                // denial the user was in the middle of granting.
                synchronized (PERMISSION_LOCK) {
                    requestPermissionsLocked(permissionBits, requestId);
                }
            }
        });
    }

    /// The permission sequence itself; the caller holds PERMISSION_LOCK.
    private void requestPermissionsLocked(int permissionBits, int requestId) {
        if ((permissionBits & PERMISSION_MICROPHONE) != 0) {
            com.codename1.impl.android.AndroidImplementation
                    .checkForPermission(Manifest.permission.RECORD_AUDIO,
                            "This is required to carry the audio of a call");
        }
        if ((permissionBits & PERMISSION_CAMERA) != 0) {
            com.codename1.impl.android.AndroidImplementation
                    .checkForPermission(Manifest.permission.CAMERA,
                            "This is required for video calls");
        }
        if ((permissionBits & PERMISSION_NOTIFICATIONS) != 0
                && Build.VERSION.SDK_INT >= 33) {
            com.codename1.impl.android.AndroidImplementation
                    .checkForPermission("android.permission.POST_NOTIFICATIONS",
                            "This is required to show an incoming call");
        }
        Calls.deliverPermissionResult(requestId, getGrantedPermissions());
    }

    /// Serialises the permission sequence; see requestPermissions.
    private static final Object PERMISSION_LOCK = new Object();

    @Override
    public void configureProvider(int requestId, String configWire) {
        if (!isCallSupported()) {
            Calls.deliverAck(requestId, false,
                    CallError.NOT_SUPPORTED.ordinal(),
                    "Self-managed calls need Android 8.0 or newer");
            return;
        }
        String[] f = CallWire.split(configWire);
        String label = CallWire.field(f, 0);
        if (label.length() == 0) {
            label = context.getApplicationInfo()
                    .loadLabel(context.getPackageManager()).toString();
        }
        boolean video = CallWire.flag(f, 1);
        try {
            handle = new PhoneAccountHandle(
                    new ComponentName(context, CN1ConnectionService.class), "cn1");
            int caps = PhoneAccount.CAPABILITY_SELF_MANAGED;
            if (video) {
                caps |= PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING
                        | PhoneAccount.CAPABILITY_VIDEO_CALLING;
            }
            PhoneAccount account = PhoneAccount.builder(handle, label)
                    .setCapabilities(caps)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
                    .build();
            telecom().registerPhoneAccount(account);
            configured = true;
            Calls.deliverAck(requestId, true, 0, null);
        } catch (SecurityException e) {
            configured = false;
            Calls.deliverAck(requestId, false, CallError.UNAUTHORIZED.ordinal(),
                    "MANAGE_OWN_CALLS is required to own calls: " + e.getMessage());
        }
    }

    @Override
    public void reportIncomingCall(int requestId, String callId,
            String handleWire, String displayName, int capabilityBits,
            boolean hasVideo) {
        if (!ready(requestId)) {
            return;
        }
        Bundle extras = extrasFor(callId, handleWire, displayName, hasVideo);
        CN1ConnectionService.expectReport(requestId, callId);
        try {
            telecom().addNewIncomingCall(handle, extras);
        } catch (SecurityException e) {
            CN1ConnectionService.failParkedReport(callId,
                    CallError.UNAUTHORIZED.ordinal(), e.getMessage());
        }
    }

    @Override
    public void reportOutgoingCall(int requestId, String callId,
            String handleWire, String displayName, int capabilityBits,
            boolean hasVideo) {
        if (!ready(requestId)) {
            return;
        }
        Bundle extras = extrasFor(callId, handleWire, displayName, hasVideo);
        Bundle outer = new Bundle();
        outer.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        outer.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
        CN1ConnectionService.expectReport(requestId, callId);
        try {
            telecom().placeCall(uriFor(handleWire), outer);
        } catch (SecurityException e) {
            CN1ConnectionService.failParkedReport(callId,
                    CallError.UNAUTHORIZED.ordinal(), e.getMessage());
        }
    }

    /// Whether a report can proceed, answering the request when it cannot.
    private boolean ready(int requestId) {
        if (!isCallSupported()) {
            Calls.deliverAck(requestId, false, CallError.NOT_SUPPORTED.ordinal(),
                    "Self-managed calls need Android 8.0 or newer");
            return false;
        }
        if (!configured || handle == null) {
            // The silent case, made loud. Without this Telecom drops the call
            // and says nothing at all.
            Calls.deliverAck(requestId, false, CallError.CALL_REFUSED.ordinal(),
                    "Calls.configure() must run before a call is reported:"
                    + " Telecom ignores calls from an unregistered account");
            return false;
        }
        return true;
    }

    private Bundle extrasFor(String callId, String handleWire, String name,
            boolean hasVideo) {
        Bundle b = new Bundle();
        b.putString(CN1ConnectionService.EXTRA_CALL_ID, callId);
        b.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                uriFor(handleWire));
        if (name != null) {
            b.putString(TelecomManager.EXTRA_CALL_SUBJECT, name);
        }
        // Carried to the connection, which is the only place it can be
        // applied: dropping it here left Telecom treating every call as
        // audio-only while the bridge advertised CAPABILITY_VIDEO.
        b.putBoolean(CN1ConnectionService.EXTRA_VIDEO, hasVideo);
        return b;
    }

    /// The address Telecom shows and dials.
    ///
    /// The scheme matters: a `tel:` address is matched against the address
    /// book and can be called back from Recents, while a username sent as
    /// `tel:` produces a call log entry the user cannot use.
    private static Uri uriFor(String handleWire) {
        CallHandle h = CallWire.decodeHandle(handleWire);
        if (h == null) {
            return Uri.fromParts(PhoneAccount.SCHEME_TEL, "", null);
        }
        String scheme = h.getType() == CallHandleType.PHONE_NUMBER
                ? PhoneAccount.SCHEME_TEL : PhoneAccount.SCHEME_SIP;
        return Uri.fromParts(scheme, h.getValue(), null);
    }

    @Override
    public void reportOutgoingStartedConnecting(String callId, long timestampMs) {
        CN1Connection c = CN1ConnectionService.find(callId);
        if (c != null) {
            c.setDialing();
        }
    }

    @Override
    public void reportOutgoingConnected(String callId, long timestampMs) {
        activate(callId);
    }

    @Override
    public void reportIncomingConnected(String callId, long timestampMs) {
        activate(callId);
    }

    private static void activate(String callId) {
        CN1Connection c = CN1ConnectionService.find(callId);
        if (c != null) {
            c.setActive();
        }
    }

    @Override
    public void updateCall(String callId, String handleWire, String displayName,
            int capabilityBits, boolean hasVideo) {
        CN1Connection c = CN1ConnectionService.find(callId);
        if (c == null) {
            return;
        }
        if (handleWire != null && handleWire.length() > 0) {
            c.setAddress(uriFor(handleWire), TelecomManager.PRESENTATION_ALLOWED);
        }
        if (displayName != null) {
            c.setCallerDisplayName(displayName,
                    TelecomManager.PRESENTATION_ALLOWED);
        }
        // Only when the update actually carries capability information.
        // CallSession.update() has no video parameter and passes -1 and
        // false, so applying the flag here turned every rename of a video
        // call into a downgrade -- and onAnswer(videoState) then skipped the
        // state the user had answered with, leaving Telecom showing the
        // original bidirectional video. iOS never had this: CXCallUpdate
        // leaves the fields an update does not set alone.
        if (capabilityBits >= 0) {
            c.setVideo(hasVideo);
        }
    }

    @Override
    public void reportCallEnded(String callId, int endReasonOrdinal,
            long timestampMs) {
        CN1Connection c = CN1ConnectionService.find(callId);
        if (c != null) {
            c.finish(CallWire.endReason(endReasonOrdinal));
            CN1ConnectionService.forget(callId);
        }
    }

    @Override
    public void endCall(int requestId, String callId, int endReasonOrdinal) {
        CN1Connection c = CN1ConnectionService.find(callId);
        if (c == null) {
            Calls.deliverAck(requestId, false, CallError.INVALID_ID.ordinal(),
                    "No such call: " + callId);
            return;
        }
        c.finish(CallWire.endReason(endReasonOrdinal));
        CN1ConnectionService.forget(callId);
        Calls.deliverAck(requestId, true, 0, null);
    }

    @Override
    public void setHeld(int requestId, String callId, boolean held) {
        CN1Connection c = CN1ConnectionService.find(callId);
        if (c == null) {
            Calls.deliverAck(requestId, false, CallError.INVALID_ID.ordinal(),
                    "No such call: " + callId);
            return;
        }
        if (held) {
            c.setOnHold();
        } else {
            c.setActive();
        }
        Calls.deliverAck(requestId, true, 0, null);
    }

    @Override
    public void setMuted(int requestId, String callId, boolean muted) {
        // Telecom owns the mute control for a self-managed call and does not
        // take instruction about it from the app; the app mutes its own
        // media. Answering true rather than failing keeps a portable app from
        // treating a platform difference as an error.
        Calls.deliverAck(requestId,
                CN1ConnectionService.find(callId) != null,
                CallError.INVALID_ID.ordinal(), "No such call: " + callId);
    }

    @Override
    public void sendDtmf(int requestId, String callId, String digits) {
        // Outbound DTMF is carried by the app's own media, not by Telecom:
        // there is no self-managed API that emits a tone.
        //
        // Acknowledging and stopping there was still wrong. On iOS the same
        // call submits a CXPlayDTMFCallAction and CallKit hands it straight
        // back through dtmfRequested, which is where an app puts the tone
        // into its media -- so an app written once did nothing at all here.
        // The round trip is synthesized instead, the way this port
        // synthesizes audioSessionActivated.
        if (CN1ConnectionService.find(callId) == null) {
            Calls.deliverAck(requestId, false,
                    CallError.INVALID_ID.ordinal(), "No such call: " + callId);
            return;
        }
        Calls.deliverAck(requestId, true, 0, null);
        Calls.deliverDtmfPlayed(callId, digits);
    }

    @Override
    public void setCallGroup(int requestId, String callId, String otherCallId) {
        Calls.deliverAck(requestId, false, CallError.NOT_SUPPORTED.ordinal(),
                "Telecom does not conference self-managed calls");
    }

    @Override
    public int getAudioRoute() {
        return CN1ConnectionService.getRoute();
    }

    @Override
    public void setAudioRoute(int requestId, int routeOrdinal) {
        CN1Connection c = null;
        for (com.codename1.call.session.CallSession s : Calls.getSessions()) {
            c = CN1ConnectionService.find(s.getCallId());
            if (c != null) {
                break;
            }
        }
        if (c == null) {
            Calls.deliverAck(requestId, false, CallError.INVALID_ID.ordinal(),
                    "There is no call to route audio for");
            return;
        }
        // Asked whether Telecom will take it. setAudioRoute answers nothing,
        // so caching the REQUESTED ordinal and acknowledging meant asking for
        // BLUETOOTH with no device paired reported success -- and
        // getAudioRoute() then claimed Bluetooth while audio stayed on the
        // earpiece. This is the same correction the iOS route setter needed.
        int androidRoute = androidRouteOf(routeOrdinal);
        if (!c.routeIsAvailable(androidRoute)) {
            Calls.deliverAck(requestId, false,
                    CallError.NOT_SUPPORTED.ordinal(),
                    "That audio route is not available for this call");
            return;
        }
        c.setAudioRoute(androidRoute);
        // The route itself is recorded by onCallAudioStateChanged when
        // Telecom actually moves it, which is the only report that means it
        // happened.
        Calls.deliverAck(requestId, true, 0, null);
    }

    /// The activity a prompt can be shown from, or null when there is none.
    ///
    /// The cached context is for system work and may be a Service; only the
    /// prompt paths need an activity, and only at the moment they show one.
    private Activity currentActivity() {
        Activity current = com.codename1.impl.android.AndroidImplementation
                .getActivity();
        if (current != null) {
            return current;
        }
        return context instanceof Activity ? (Activity) context : null;
    }

    private static int androidRouteOf(int ordinal) {
        CallAudioRoute[] values = CallAudioRoute.values();
        CallAudioRoute r = ordinal < 0 || ordinal >= values.length
                ? CallAudioRoute.UNKNOWN : values[ordinal];
        switch (r) {
            case SPEAKER:
                return android.telecom.CallAudioState.ROUTE_SPEAKER;
            case BLUETOOTH:
                return android.telecom.CallAudioState.ROUTE_BLUETOOTH;
            case WIRED_HEADSET:
                return android.telecom.CallAudioState.ROUTE_WIRED_HEADSET;
            default:
                return android.telecom.CallAudioState.ROUTE_EARPIECE;
        }
    }

    @Override
    public void showAudioRoutePicker(int requestId, String callId) {
        Calls.deliverAck(requestId, false, CallError.NOT_SUPPORTED.ordinal(),
                "Android has no system audio route picker to show");
    }

    @Override
    public void completeAction(long actionToken, boolean fulfilled) {
        CN1ConnectionService.completeAction(actionToken, fulfilled);
    }

    @Override
    public void registerVoipPush(int requestId) {
        // The wake-up is an ordinary high-priority FCM message, so the token
        // is the app's existing push registration and there is nothing
        // separate to register for. Answering with an empty token rather than
        // failing keeps portable code from treating this as an error.
        com.codename1.call.voip.VoipPush.deliverToken(requestId, "");
    }

    @Override
    public void unregisterVoipPush(int requestId) {
    }

    @Override
    public void setJavaReady(boolean ready) {
        // Nothing to hold. Unlike iOS, Android never reports a call to the
        // system before this app's code has run -- the FCM message arrives in
        // Java first -- so there is no queue to drain.
    }

    @Override
    public void drainPendingCalls(int requestId) {
        com.codename1.call.voip.VoipPush.deliverPendingCallsDrained(requestId, 0);
    }

    @Override
    public void setDirectorySource(int requestId, String filePath) {
        if (!isDirectorySupported()) {
            Calls.deliverAck(requestId, false,
                    CallError.NOT_SUPPORTED.ordinal(),
                    "Call screening needs Android 10 or newer");
            return;
        }
        // The screening service reads the path CallDirectory wrote, so
        // nothing needs copying here -- but its cache has to be dropped or
        // the new list is ignored until the process dies.
        CN1CallScreeningService.invalidate();
        Calls.deliverAck(requestId, true, 0, null);
    }

    @Override
    public void reloadDirectory(int requestId) {
        if (!isDirectorySupported()) {
            Calls.deliverAck(requestId, false,
                    CallError.NOT_SUPPORTED.ordinal(),
                    "Call screening needs Android 10 or newer");
            return;
        }
        // The screening service caches the file the first time it screens a
        // call, and it lives in a process this one does not control. Without
        // this, setEntries replaced the file and every later call was still
        // screened against the list loaded at startup.
        CN1CallScreeningService.invalidate();
        Calls.deliverAck(requestId, true, 0, null);
    }

    @Override
    public void getDirectoryStatus(int requestId) {
        // Asks the platform rather than trusting the static flag: the role
        // may have been granted in a previous process, or from Settings, and
        // the flag defaults to false either way -- so status reported
        // "disabled" while Android was actively binding the service.
        com.codename1.call.directory.CallDirectory.deliverStatus(requestId,
                CallWire.join(new String[]{
                    CallWire.flagOf(CN1CallScreeningService.isRoleHeld(context)),
                    "-1", "android"}));
    }

    @Override
    public void requestScreeningRole(int requestId) {
        // Asked for NOW rather than taken from the cached context. That
        // context is deliberately allowed to be a Service -- a push can wake
        // this process with no activity at all -- and testing it here meant a
        // bridge first obtained from that service could never prompt again,
        // even once the app was in the foreground.
        Activity a = currentActivity();
        if (a == null || !isDirectorySupported()) {
            Calls.deliverAck(requestId, false, CallError.NOT_SUPPORTED.ordinal(),
                    "Call screening needs Android 10 or newer and a foreground"
                    + " activity");
            return;
        }
        CN1CallScreeningService.requestRole(a, requestId);
    }

    /// Clears every call this port knows about, for a provider reset.
    public static void resetProvider() {
        CN1ConnectionService.reset();
        Calls.deliverProviderReset();
    }
}
