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
package com.codename1.impl.android.vpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import com.codename1.impl.vpn.VpnWire;
import com.codename1.io.Preferences;
import com.codename1.vpn.VpnError;
import com.codename1.vpn.VpnProtocol;
import com.codename1.vpn.VpnStatus;
import com.codename1.vpn.profile.Vpn;
import com.codename1.vpn.profile.VpnProfile;
import com.codename1.vpn.spi.VpnBridge;

import java.lang.reflect.Method;

/// The Android half of `com.codename1.vpn`, on the platform's own IKEv2
/// client.
///
/// #### Everything here is reflective, and that is not an accident
///
/// `android.net.VpnManager` and `android.net.Ikev2VpnProfile` arrive in API
/// 30, and the port compiles against an older SDK. Naming them directly would
/// mean raising the SDK the whole port builds against, so they are reached
/// through reflection instead and the capability is simply reported absent
/// below 30 -- which an app branches on with `Vpn.isSupported()`.
///
/// The alternative, raising the app's `minSdkVersion` to 30 for a feature it
/// can degrade out of, would cost far more than the feature is worth.
///
/// #### What Android does not offer
///
/// There is no IPsec-with-pre-shared-key equivalent in the managed profile
/// API -- only IKEv2, with a username and password, a pre-shared key, or a
/// certificate. A profile asking for [VpnProtocol#IPSEC] is refused here
/// rather than quietly installed as something else.
public class AndroidVpnBridge implements VpnBridge {

    private static final int MIN_VPN_MANAGER_SDK = 30;

    /// Android's own request code for the consent dialog.
    private static final int PROVISION_REQUEST = 0x7654;

    private final Context context;
    private String installedWire;
    /// The tunnel state and whether anybody is being told about it.
    ///
    /// Guarded by `this`, like installedWire and the reservation, because
    /// TunnelWatcher writes them from Android's connectivity callback thread
    /// while application code reads getStatus() and registers listeners on
    /// another. Unguarded, getStatus() could sit at CONNECTING after the
    /// tunnel came up, and setStatus() could miss a listener that had just
    /// been installed and swallow the transition. Not volatile: setStatus
    /// reads one and writes the other, which wants a single critical
    /// section, and startRequested is volatile only because it is a lone
    /// flag with no such pairing.
    private VpnStatus status = VpnStatus.NOT_CONFIGURED;
    private boolean listening;

    /// Whether a listener is being fed. Guarded like the field it reads.
    private boolean isListening() {
        synchronized (this) {
            return listening;
        }
    }
    private ConnectivityManager.NetworkCallback networkCallback;

    /// Guards the watcher registration. Its own monitor rather than the
    /// instance's, so a status delivery running on a platform callback thread
    /// can never be waiting behind a registration that is itself calling into
    /// ConnectivityManager.
    private final Object watchLock = new Object();

    /// Whether this app has asked for its tunnel to be up.
    private volatile boolean startRequested;

    /// Whether this app has asked for its tunnel to come DOWN and has yet to
    /// see it go.
    ///
    /// startRequested cannot answer this. It records that THIS PROCESS
    /// started the tunnel, and a stop is exactly the case where that need not
    /// be true: Android keeps a provisioned profile running across a restart
    /// of the app that installed it, so a fresh process can own a live tunnel
    /// with startRequested false. Gating the teardown on it left stopVpn
    /// publishing DISCONNECTING with nothing that could ever move it -- the
    /// transport loss arrived and onTunnelTransport discarded it as some
    /// other app's, so getStatus() said the tunnel was going down for the
    /// life of the process. This flag is what makes the stop path observe the
    /// teardown on its own account.
    private volatile boolean stopRequested;

    /// Whether an install or removal owns the profile right now.
    ///
    /// Covers the WHOLE operation rather than just the consent dialog: the
    /// already-authorized install path never shows one, and two of those
    /// racing could provision in one order and persist in the other.
    /// Who holds the one-operation-at-a-time reservation, or null.
    ///
    /// An OWNER rather than a flag, because a release has to be able to ask
    /// "is this still mine". Acknowledgements settle inline when the
    /// operation was started on the EDT, so the app's callback runs before
    /// this method's finally does -- and if that callback starts an install,
    /// the reservation the finally then cleared belonged to the CONSENT
    /// DIALOG that install had just opened. A third operation could walk in
    /// while the user was still looking at the prompt. Releasing twice is
    /// only harmless while nobody has claimed it in between.
    private Object operationOwner;

    public AndroidVpnBridge(Context context) {
        this.context = context;
    }

    private static boolean available() {
        return Build.VERSION.SDK_INT >= MIN_VPN_MANAGER_SDK && Reflect.LOADED;
    }

    @Override
    public boolean isVpnSupported() {
        return available();
    }

    @Override
    public boolean isCustomTunnelSupported() {
        // A VpnService the app implements is a separate, much larger
        // commitment and is reported by the tunnel package's own bridge.
        return false;
    }

    @Override
    public int getVpnCapabilities() {
        if (!available()) {
            return 0;
        }
        // IKEv2 and nothing else, deliberately.
        //
        // No CAPABILITY_IPSEC: the managed profile API is IKEv2 only.
        // No CAPABILITY_PER_APP: per-application routing needs a VpnService
        // the app implements, not a managed profile.
        // No CAPABILITY_ON_DEMAND: Ikev2VpnProfile has no on-demand rules,
        // so an app that checked the bit and set VpnProfile.onDemand would
        // have got an ordinary manually started tunnel and no way to tell.
        // No CAPABILITY_ALWAYS_ON: that is a Settings toggle or a
        // device-owner API here, not something an app may request.
        return CAPABILITY_IKEV2;
    }

    @Override
    public int getVpnStatus() {
        return reconciledStatus().ordinal();
    }

    /// The status field, corrected for the profile that outlives the field.
    ///
    /// A provisioned profile survives a process restart and the field does
    /// not, so a fresh process reported NOT_CONFIGURED for a VPN Android
    /// still held -- and registering a listener could not repair it, because
    /// the transport callback is ignored until this process asks for a
    /// tunnel. DISCONNECTED is the honest reading: configured, and not
    /// started by us.
    ///
    /// It stops at DISCONNECTED rather than inspecting the live transports,
    /// because Android gives an app no way to tell its own tunnel from
    /// another app's -- the reason onTunnelTransport is gated the way it is.
    private VpnStatus reconciledStatus() {
        boolean settle;
        synchronized (this) {
            // storedWire() takes this same monitor; it is reentrant.
            if (status == VpnStatus.NOT_CONFIGURED && storedWire() != null) {
                status = VpnStatus.DISCONNECTED;
            }
            settle = status == VpnStatus.DISCONNECTING && stopRequested;
            if (!settle) {
                return status;
            }
        }
        // A pending teardown with no VPN transport left is a teardown that
        // has finished. The transport callback is the usual way this is
        // learned, but it cannot be the only one: stopping a profile that was
        // provisioned and NOT running produces no loss to hear, and a stop
        // issued before any listener registered may have had nothing watching
        // when the loss went past. Both left DISCONNECTING permanent.
        //
        // Deliberately outside the monitor -- ConnectivityManager is asked
        // here, and setStatus below reaches application code.
        if (vpnTransportPresent()) {
            synchronized (this) {
                return status;
            }
        }
        synchronized (this) {
            // Re-checked: a transport loss may have settled it already while
            // the query above was in flight, and re-announcing an end that
            // was announced is a status change the app never had.
            if (status != VpnStatus.DISCONNECTING || !stopRequested) {
                return status;
            }
        }
        stopRequested = false;
        startRequested = false;
        setStatus(VpnStatus.DISCONNECTED);
        return VpnStatus.DISCONNECTED;
    }

    /// Whether the platform has ANY VPN transport up right now.
    ///
    /// Android offers no way to ask whether a PARTICULAR profile's tunnel is
    /// up, so this answers the weaker question, and the weakness is bounded:
    /// it is consulted only while this app is waiting for its own teardown,
    /// where the sole cost of another app's VPN being up is that the settle
    /// waits for the next call rather than happening on this one.
    private boolean vpnTransportPresent() {
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        try {
            Network[] networks = cm.getAllNetworks();
            if (networks == null) {
                return false;
            }
            for (int i = 0; i < networks.length; i++) {
                NetworkCapabilities caps =
                        cm.getNetworkCapabilities(networks[i]);
                if (caps != null && caps.hasTransport(
                        NetworkCapabilities.TRANSPORT_VPN)) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            // A device that refuses the query has told us nothing, and the
            // two ways of reading nothing are not equally bad: "still up"
            // strands the status in DISCONNECTING for ever, which is the
            // exact failure this method exists to end, while "gone" settles a
            // teardown that had in fact been asked for. Take the recoverable
            // one.
            return false;
        }
        return false;
    }

    @Override
    public void installProfile(int requestId, String wire) {
        if (!available()) {
            fail(requestId, VpnError.NOT_SUPPORTED,
                    "Managed VPN profiles need Android 11 or newer");
            return;
        }
        VpnProfile p = VpnWire.decodeProfile(wire);
        if (p == null) {
            fail(requestId, VpnError.INVALID_CONFIGURATION,
                    "The profile has no server address");
            return;
        }
        // BOTH credentials is a combination this platform cannot express.
        // Ikev2VpnProfile.Builder's setAuthPsk and setAuthUsernamePassword
        // each SET the authentication method, so the second call replaces
        // the first and only one survives -- while iOS installs the shared
        // secret and turns extended authentication on as well, which is the
        // common arrangement for a gateway that wants both. Taking the PSK
        // and dropping the pair provisioned cleanly and then could not
        // connect, with nothing to see in either profile. Refused instead,
        // and named, because a materially different profile acknowledged as
        // success is the worse answer.
        if (p.getSharedSecret() != null && p.getSharedSecret().length() > 0
                && p.getUsername() != null && p.getUsername().length() > 0
                && p.isPasswordKnown()) {
            fail(requestId, VpnError.NOT_SUPPORTED,
                    "Android cannot combine a shared secret with a username"
                    + " and password in one managed profile; install one or"
                    + " the other");
            return;
        }
        if (p.getProtocol() == VpnProtocol.IPSEC) {
            // NOT_SUPPORTED, which is what VpnProtocol.IPSEC documents this
            // platform answering. The profile is not malformed -- it is
            // perfectly valid, and installs on iOS -- so calling it invalid
            // told an app that branches on the typed error to reject the
            // user's configuration instead of falling back to IKEv2 or
            // hiding the option.
            fail(requestId, VpnError.NOT_SUPPORTED,
                    "Android's managed VPN offers IKEv2 only; IPSEC is an iOS"
                    + " configuration");
            return;
        }
        // RESERVED BEFORE PROVISIONING, not just before the prompt. The
        // already-consented path returns null and used to skip the
        // reservation entirely, so two concurrent replacements could
        // provision A then B and persist B then A -- both acknowledged, with
        // load() describing A while Android ran B. The window is the whole
        // operation, not the dialog, so the reservation has to open here.
        String previous;
        Object mine = new Object();
        synchronized (this) {
            if (operationOwner != null) {
                fail(requestId, VpnError.UNKNOWN,
                        "Another VPN profile operation is still in progress;"
                        + " wait for it to finish before installing again");
                return;
            }
            operationOwner = mine;
            previous = storedWire();
        }
        boolean handedOff = false;
        try {
            Object profile = Reflect.buildIkev2(p);
            Object manager = Reflect.manager(context);
            // Tested with instanceof rather than cast: ParparVM does not
            // check CHECKCAST, so a cast that fails there does not throw and
            // cannot be caught.
            Object raw = Reflect.PROVISION.invoke(manager, profile);
            Intent consent = asIntent(raw);
            if (raw == null) {
                // Already consented; the profile is provisioned. Persisted
                // here as well as in the consent callback -- replacing a
                // profile a user had already approved takes this path, and
                // leaving it out meant load() described the previous profile
                // after a restart while Android ran the new one.
                // BOTH: the field is what storedWire() answers from for the
                // rest of this process, so persisting only to Preferences
                // left load() describing the profile this one replaced.
                synchronized (this) {
                    installedWire = wire;
                    Preferences.set(WIRE_PREF, strip(wire));
                }
                // Status FIRST, reservation after. Released here, a removal
                // could claim it, delete the profile just provisioned and
                // publish NOT_CONFIGURED -- and this thread would then
                // overwrite that with DISCONNECTED. Both calls report
                // success, load() finds no profile, and getStatus insists
                // one is merely disconnected. The reservation is not held
                // until the work is done; it is held until the last thing
                // the work PUBLISHES has been published.
                setStatus(VpnStatus.DISCONNECTED);
                endOperation(mine);
                Vpn.deliverAck(requestId, true, 0, null);
                return;
            }
            // Resolved now, not from the cached context: see
            // AndroidCallBridge.currentActivity. A bridge first obtained from
            // a service could otherwise never show a consent prompt again.
            if (currentActivity() == null || consent == null) {
                endOperation(mine);
                fail(requestId, VpnError.UNAUTHORIZED,
                        "Installing a VPN needs a foreground activity to show"
                        + " the consent prompt");
                return;
            }
            // The reservation opened above is HELD across the dialog and
            // released by Consent. One prompt at a time also matters in its
            // own right: setIntentResultListener refuses to replace a
            // listener whose result channel is busy -- silently -- so a
            // second install started while the first dialog was up left the
            // second request in VpnRequests for ever.
            //
            // installedWire is NOT published here. Android provisions nothing
            // until the user approves the prompt, so publishing the attempted
            // profile now had load() hand back a configuration the user had
            // not agreed to, and -- on a first install -- reconciledStatus()
            // read its mere presence as DISCONNECTED for a VPN that did not
            // exist. Consent carries the wire and publishes it on approval.
            try {
                com.codename1.impl.android.AndroidNativeUtil
                        .startActivityForResult(consent,
                                new Consent(this, requestId, previous,
                                        wire, mine));
                // From here the reservation belongs to Consent.
                handedOff = true;
            } catch (RuntimeException launchFailed) {
                // The cached context can still LOOK like an Activity after
                // the app is backgrounded, while the current activity that
                // startActivityForResult consults is gone. Without this the
                // prompt never opened and nothing ever cleared the flag, so
                // every later install was refused as though a dialog were
                // still up -- and the cached record described a profile that
                // was never installed.
                endOperation(mine);
                fail(requestId, VpnError.UNAUTHORIZED,
                        "The VPN consent prompt could not be shown: "
                                + describe(launchFailed));
            }
        } catch (Exception e) {
            endOperation(mine);
            fail(requestId, VpnError.INVALID_CONFIGURATION, describe(e));
        } finally {
            // Released on every path that did not hand it to Consent --
            // including the failures above, one of which used to leave it set
            // for ever so every later install was refused as though a dialog
            // were still up.
            if (!handedOff) {
                endOperation(mine);
            }
        }
    }

    /// Answers the install once the user has decided about the prompt.
    ///
    /// A named class rather than an anonymous one so it carries no synthetic
    /// reference to anything that outlives the dialog.
    private static final class Consent
            implements com.codename1.impl.android.IntentResultListener {
        private final AndroidVpnBridge bridge;
        private final int requestId;
        /// The record describing what was installed BEFORE this attempt.
        private final String previous;
        /// The profile this prompt is about, so the result does not have to
        /// re-read a field another install may have replaced.
        private final String attempted;

        /// The reservation this prompt was handed, so it releases the one
        /// it owns rather than whatever happens to be set when it finishes.
        private final Object token;

        Consent(AndroidVpnBridge bridge, int requestId, String previous,
                String attempted, Object token) {
            this.token = token;
            this.bridge = bridge;
            this.requestId = requestId;
            this.previous = previous;
            this.attempted = attempted;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            boolean approved = resultCode == Activity.RESULT_OK;
            // The reservation is held THROUGH this, and the wire this prompt
            // was about is carried rather than re-read. Clearing the flag
            // first let a new install overwrite installedWire while this
            // callback was still running, so the first consent persisted the
            // second profile and acknowledged itself as successful.
            synchronized (bridge) {
                if (approved) {
                    bridge.installedWire = attempted;
                    Preferences.set(WIRE_PREF, strip(attempted));
                } else {
                    // The PREVIOUS profile is untouched: Android installs
                    // nothing when the prompt is declined, so a replacement
                    // the user refused leaves whatever was already
                    // provisioned in place. Clearing the record made load()
                    // answer null and getStatus() NOT_CONFIGURED for a VPN
                    // Android was still holding.
                    bridge.installedWire = previous;
                    if (previous == null) {
                        Preferences.delete(WIRE_PREF);
                    } else {
                        // STRIPPED, like every other path that writes here.
                        // installedWire carries the whole profile while an
                        // install is in flight, so a restored `previous` can
                        // still hold the password or the pre-shared key --
                        // and this is ordinary preference storage.
                        Preferences.set(WIRE_PREF, strip(previous));
                    }
                }
            }
            // The same ordering as the already-authorized path above: the
            // status this install publishes has to land before anything else
            // may claim the profile.
            if (approved) {
                bridge.setStatus(VpnStatus.DISCONNECTED);
            }
            bridge.endOperation(token);
            if (approved) {
                Vpn.deliverAck(requestId, true, 0, null);
            } else {
                Vpn.deliverAck(requestId, false,
                        VpnError.USER_DECLINED.ordinal(),
                        "The user declined the VPN configuration prompt");
            }
        }
    }

    @Override
    public void removeProfile(int requestId) {
        if (!available()) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        // RESERVED for the whole removal, not just the delete. The monitor
        // was released before startRequested and the status were updated, so
        // an already-authorized install could provision and publish the
        // replacement in that gap and then have this removal overwrite its
        // status and tunnel ownership -- or a start could bring the
        // replacement up and have its callbacks ignored, because
        // startRequested was cleared after it.
        Object mine = new Object();
        synchronized (this) {
            if (operationOwner != null) {
                // An install owns the profile: a prompt is on screen, or an
                // already-authorized install is between provisioning and
                // persisting. Either way deleting now would race it.
                fail(requestId, VpnError.UNKNOWN,
                        "A VPN profile install is in progress; wait for it"
                        + " to finish before removing the profile");
                return;
            }
            operationOwner = mine;
        }
        try {
            synchronized (this) {
                Reflect.DELETE.invoke(Reflect.manager(context));
                installedWire = null;
                Preferences.delete(WIRE_PREF);
            }
            // This app no longer has a tunnel to own. Left set, the transport
            // callback stayed attributed to us and the onLost that deleting a
            // provisioned profile produces moved the status from
            // NOT_CONFIGURED to DISCONNECTED -- so getStatus() claimed a
            // configuration still existed after the removal it had just
            // reported as successful.
            startRequested = false;
            if (!isListening()) {
                stopWatchingTheTunnel();
            }
            setStatus(VpnStatus.NOT_CONFIGURED);
            endOperation(mine);
            Vpn.deliverAck(requestId, true, 0, null);
        } catch (Exception e) {
            endOperation(mine);
            fail(requestId, VpnError.UNKNOWN, describe(e));
        } finally {
            // Released on every path; see startVpn.
            endOperation(mine);
        }
    }

    @Override
    public void loadProfile(int requestId) {
        if (!available()) {
            // Guarded like every mutating operation, because the bridge is
            // supplied on EVERY Android version while available() is false
            // below API 30 and whenever the reflective VpnManager surface
            // will not load -- and Vpn.load() only checks that the bridge is
            // non-null. Without this, the one device that cannot hold a
            // profile was the one that answered load() successfully: with no
            // profile, or worse with a record restored from a backup taken on
            // a device that could. LocalVpnBridge and the iOS native both
            // already answer NOT_SUPPORTED here; this was the odd one out.
            //
            // Answered on the LOAD channel, not through fail(): fail() routes
            // to deliverAck, whose takeAck finds nothing for a request opened
            // as a string, and load() would then never be answered at all --
            // worse than any failure.
            Vpn.deliverProfileFailed(requestId,
                    VpnError.NOT_SUPPORTED.ordinal(), null);
            return;
        }
        // The platform keeps the profile and does not hand it back, so the
        // record this port stored is the only description available -- and
        // the secrets were never written into it in the first place.
        //
        // Read through storedWire rather than the field, because the field
        // does not survive the process restart that the provisioned profile
        // does; without this, load() reported no profile for one Android
        // still held.
        Vpn.deliverProfile(requestId, strip(storedWire()));
    }

    /// The description of the installed profile, from this process or the
    /// one before it.
    private String storedWire() {
        // Under the same monitor as every other access. SpotBugs caught this
        // one as inconsistent synchronisation, and it is the read the consent
        // result and the removal both depend on.
        synchronized (this) {
            if (installedWire != null) {
                return installedWire;
            }
            String saved = Preferences.get(WIRE_PREF, null);
            installedWire = saved;
            return saved;
        }
    }

    /// Where the profile description outlives the process.
    ///
    /// Only the description: strip() has already removed the password and the
    /// shared secret, and the platform keychain is the only thing that ever
    /// held those.
    private static final String WIRE_PREF = "cn1.vpn.profile";

    /// Removes the password and shared secret from a stored record.
    private static String strip(String wire) {
        if (wire == null || wire.length() == 0) {
            return "";
        }
        String[] f = com.codename1.impl.call.CallWire.split(wire);
        if (f.length > 5) {
            f[5] = "";
        }
        if (f.length > 6) {
            f[6] = "";
        }
        return com.codename1.impl.call.CallWire.join(f);
    }

    @Override
    public void startVpn(int requestId) {
        if (!available()) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        // Deliberately NOT gated on installedWire. Android keeps the
        // provisioned profile across a process restart, and the field does
        // not survive one -- so gating here answered NOT_CONFIGURED for a
        // profile the platform still holds. The platform's own refusal is
        // the authority, and it is mapped below.
        // The SAME reservation install and removal take. Without it a start
        // could bring up the OLD profile while an install was building or
        // waiting on the consent prompt -- after which the replacement is
        // provisioned and the caller is left with a running tunnel that is
        // not the one it asked for, both requests having reported success --
        // or be accepted a moment before a removal deleted the profile.
        // RESERVED, not merely tested. Reading the flag under the monitor and
        // releasing it before the platform call left the whole start outside
        // the reservation: an install could claim it in that gap and be
        // provisioning a replacement while this brought the old profile up.
        // A check and an act that are not one critical section are not a
        // guard at all.
        Object mine = new Object();
        synchronized (this) {
            if (operationOwner != null) {
                fail(requestId, VpnError.UNKNOWN,
                        "A VPN profile operation is in progress; wait for it"
                        + " to finish before starting the tunnel");
                return;
            }
            operationOwner = mine;
        }
        try {
            startRequested = true;
            // A start supersedes a teardown this app was still waiting on;
            // left set, the first loss after the new tunnel came up would be
            // read as the OLD stop completing.
            stopRequested = false;
            setStatus(VpnStatus.CONNECTING);
            Reflect.START.invoke(Reflect.manager(context));
            // Watching starts HERE as well as in setStatusListening. The
            // transport callback is the only signal Android gives that a
            // tunnel came up, so an app that starts a VPN without registering
            // a listener first sat at CONNECTING for ever and getStatus()
            // could never report otherwise.
            startWatchingTheTunnel();
            // The platform reports no completion, so the tunnel is CONNECTING
            // until the network callback says otherwise. Answering the request
            // now is the honest thing: "the platform accepted the request" is
            // all that is actually known.
            endOperation(mine);
            Vpn.deliverAck(requestId, true, 0, null);
        } catch (Exception e) {
            startRequested = false;
            // A SecurityException here is the platform saying no profile is
            // provisioned, which is a different answer from a tunnel that
            // could not be established -- and the STATUS has to say the same
            // thing as the failure. Reporting DISCONNECTED for a refusal that
            // said NOT_CONFIGURED had getStatus() contradict the error the
            // caller had just been handed.
            boolean absent = isNotProvisioned(e);
            if (absent) {
                // The platform is authoritative here, and it says there is no
                // profile; the record this port kept is stale.
                forgetInstalledProfile();
            }
            setStatus(absent ? VpnStatus.NOT_CONFIGURED
                    : VpnStatus.DISCONNECTED);
            endOperation(mine);
            fail(requestId, absent
                    ? VpnError.NOT_CONFIGURED : VpnError.CONNECTION_FAILED,
                    describe(e));
        } finally {
            // Released on EVERY path, success or failure. Left set, the next
            // install would be refused for ever as though an operation were
            // still running.
            endOperation(mine);
        }
    }

    /// Drops the record of an installed profile, cached and persisted.
    ///
    /// Called only where the PLATFORM has said there is no profile. Setting
    /// NOT_CONFIGURED without this achieved nothing: reconciledStatus() sees
    /// the record storedWire() still answers from and moves the status
    /// straight back to DISCONNECTED, while load() keeps describing a profile
    /// Android does not have -- after restored app data, or after the user
    /// removed the profile from Settings.
    /// Ends the reservation, before control goes back to application code.
    ///
    /// The rule the success path of installProfile already states -- held
    /// until the last thing the work PUBLISHES has been published -- has a
    /// second half: the acknowledgement IS that last publication, and it
    /// hands control BACK. VpnRequests settles its EdtResult inline when the
    /// operation was started on the EDT, so `Vpn.stop().ready(v -> remove())`
    /// runs the app's next call inside deliverAck, while a `finally` further
    /// down still had the reservation set. The follow-up was then refused
    /// with UNKNOWN for an operation that had just reported success.
    ///
    /// So: every state change first, then this, then the ack or the failure.
    /// The finallys are kept -- they still cover the paths that return before
    /// publishing anything -- and they pass the same token, so a finally that
    /// runs after the acknowledgement handed the reservation to somebody else
    /// leaves it alone.
    private void endOperation(Object token) {
        synchronized (this) {
            if (operationOwner == token) {
                operationOwner = null;
            }
        }
    }

    private void forgetInstalledProfile() {
        synchronized (this) {
            installedWire = null;
            Preferences.delete(WIRE_PREF);
        }
    }

    @Override
    public void stopVpn(int requestId) {
        if (!available()) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        // Not gated on installedWire; see startVpn.
        // The same reservation, for the same reason as startVpn: stopping
        // a tunnel whose profile is being replaced acts on the one the
        // install is about to remove.
        // Reserved through the platform call; see startVpn.
        Object mine = new Object();
        synchronized (this) {
            if (operationOwner != null) {
                fail(requestId, VpnError.UNKNOWN,
                        "A VPN profile operation is in progress; wait for it"
                        + " to finish before stopping the tunnel");
                return;
            }
            operationOwner = mine;
        }
        VpnStatus before = reconciledStatus();
        boolean wasRequested = startRequested;
        try {
            setStatus(VpnStatus.DISCONNECTING);
            Reflect.STOP.invoke(Reflect.manager(context));
            // DISCONNECTING is where this STAYS. stopProvisionedVpnProfile
            // only submits the request; Android tears the tunnel down after
            // it returns, and traffic can still be on it. Publishing
            // DISCONNECTED here announced an end that had not happened, and
            // clearing startRequested first meant the transport callback --
            // the only thing that knows when it really did -- was then
            // ignored as somebody else's tunnel. The watcher owns the
            // transition now, which is what it is for.
            //
            // startRequested is cleared by onTunnelTransport when the
            // transport goes, or by the failure path below if the platform
            // refuses.
            //
            // Set AFTER the platform accepted: a refusal must not leave a
            // teardown pending for a tunnel that is still up.
            stopRequested = true;
            // And something has to be WATCHING. startVpn arms the watcher,
            // but a process that did not start this tunnel never ran it --
            // the restart case -- so a stop issued there had no callback
            // registered and no way to learn the teardown had happened.
            startWatchingTheTunnel();
            endOperation(mine);
            // Settles now when there is no tunnel left to lose, which is what
            // stopping an already-idle profile looks like; otherwise this
            // leaves DISCONNECTING for the transport loss to finish.
            reconciledStatus();
            Vpn.deliverAck(requestId, true, 0, null);
        } catch (Exception e) {
            // DISCONNECTING was set before the platform was asked, so a
            // refusal left the bridge saying a tunnel was going down for ever
            // -- with no tunnel and, in the SecurityException case, no
            // profile either. The state goes back to what it was, or to
            // NOT_CONFIGURED when the platform has just said there is
            // nothing provisioned.
            boolean absent = isNotProvisioned(e);
            // startRequested goes back too, and for the same reason the
            // status does: a refused stop means the tunnel this app asked for
            // is still up. Left false, setStatusListening(false) would then
            // unregister the transport watcher on a live tunnel -- the very
            // case the watcher exists for -- and a disconnect from Settings
            // would go unseen for ever. Only a platform saying there is no
            // profile at all justifies keeping it clear.
            startRequested = wasRequested && !absent;
            if (absent) {
                forgetInstalledProfile();
            }
            setStatus(absent ? VpnStatus.NOT_CONFIGURED : before);
            endOperation(mine);
            fail(requestId, absent
                    ? VpnError.NOT_CONFIGURED : VpnError.UNKNOWN, describe(e));
        } finally {
            // See startVpn: released on every path.
            endOperation(mine);
        }
    }

    @Override
    public void setStatusListening(boolean value) {
        synchronized (this) {
            this.listening = value;
        }
        if (value) {
            // Before the first callback, so the baseline a new listener sees
            // is the profile Android actually holds.
            reconciledStatus();
            startWatchingTheTunnel();
        } else if (!startRequested && !stopRequested) {
            // Only when this app has no tunnel of its own up. The transport
            // callback is the sole signal Android gives that a tunnel went
            // away, so unregistering it because the last LISTENER left meant
            // a disconnect from Settings was never seen and getStatus() said
            // CONNECTED for ever. Delivery stops; observation does not.
            stopWatchingTheTunnel();
        }
    }

    /// Watches the real tunnel rather than this class's own guesses.
    ///
    /// Without this the status was whatever the last call to setStatus left
    /// behind: startVpn set CONNECTING and nothing ever moved it, because
    /// VpnManager reports no completion. A VPN transport arriving or leaving
    /// is the only signal Android gives an app that its tunnel came up or
    /// went away, including when the user disconnects it from Settings.
    private void startWatchingTheTunnel() {
        // The test, the assignment and the registration under ONE ordering.
        // Vpn.start() and the first status listener both call this, and split,
        // both could read null, build a watcher and register it -- after
        // which only the last one written to the field is reachable. The
        // other stayed registered for the life of the process, delivering
        // duplicate status changes that no unregister could ever stop.
        synchronized (watchLock) {
            if (networkCallback != null || Build.VERSION.SDK_INT < 21) {
                return;
            }
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return;
            }
            ConnectivityManager.NetworkCallback cb = new TunnelWatcher(this);
            try {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                        // A VPN is not "internet capable" by the default
                        // filter's reckoning, so the default capabilities
                        // would never match one.
                        .removeCapability(
                                NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                        .build();
                cm.registerNetworkCallback(request, cb);
                // Published only once the platform has accepted it, so a
                // refusal leaves nothing registered and nothing in the field.
                networkCallback = cb;
            } catch (RuntimeException e) {
                // Some devices refuse the registration; the API then reports
                // whatever setStatus last set, which is what it did before.
                networkCallback = null;
            }
        }
    }

    private void stopWatchingTheTunnel() {
        synchronized (watchLock) {
            if (networkCallback == null) {
                return;
            }
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                try {
                    cm.unregisterNetworkCallback(networkCallback);
                } catch (RuntimeException ignored) {
                    // Already gone.
                }
            }
            networkCallback = null;
        }
    }

    /// Turns VPN transport arrival and loss into a status change.
    ///
    /// A named static class rather than an anonymous one so it holds no
    /// synthetic reference to anything that outlives the registration.
    private static final class TunnelWatcher
            extends ConnectivityManager.NetworkCallback {
        private final AndroidVpnBridge bridge;

        TunnelWatcher(AndroidVpnBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public void onAvailable(Network network) {
            bridge.onTunnelTransport(true);
        }

        @Override
        public void onLost(Network network) {
            bridge.onTunnelTransport(false);
        }
    }

    /// A VPN transport appeared or went away.
    ///
    /// Attributed to this app's profile only when this app asked for one.
    /// Android gives an application no way to tell WHICH VPN a transport
    /// belongs to, so another app's tunnel matches the same callback -- and
    /// reporting that as CONNECTED had listeners and Vpn.getStatus()
    /// describing somebody else's VPN. Gating on our own start request is not
    /// perfect (a foreign tunnel coming up while ours is connecting is still
    /// indistinguishable) but it removes the case that matters: an app that
    /// never started a tunnel never hears that one is up.
    void onTunnelTransport(boolean available) {
        if (!startRequested && !stopRequested) {
            return;
        }
        if (available && !startRequested) {
            // A tunnel COMING UP while this app is tearing its own down is
            // not this app's; reporting CONNECTED there would undo the stop
            // the caller just asked for.
            return;
        }
        if (!available) {
            stopRequested = false;
            // The tunnel is actually gone, which is the point at which this
            // app stops owning one -- whether it asked to stop or the user
            // did it from Settings. stopVpn used to clear this itself and
            // publish DISCONNECTED before the teardown had happened.
            startRequested = false;
        }
        setStatus(available ? VpnStatus.CONNECTED : VpnStatus.DISCONNECTED);
    }

    void setStatus(VpnStatus s) {
        boolean notify;
        synchronized (this) {
            this.status = s;
            notify = listening;
        }
        if (notify) {
            // OUTSIDE the monitor: the delivery reaches application code,
            // which calls straight back in here.
            Vpn.deliverStatusChanged(s.ordinal());
        }
    }

    /// The activity a prompt can be shown from, or null when there is none.
    private Activity currentActivity() {
        Activity current = com.codename1.impl.android.AndroidImplementation
                .getActivity();
        if (current != null) {
            return current;
        }
        return context instanceof Activity ? (Activity) context : null;
    }

    /// A reflective answer as an `Intent`, or null when it is not one.
    ///
    /// In its own method, outside any `try`, for the reason
    /// `CN1CallScreeningService.asString` gives: ParparVM does not check
    /// CHECKCAST, so a cast that fails there does not throw.
    private static Intent asIntent(Object o) {
        if (o instanceof Intent) {
            return (Intent) o;
        }
        return null;
    }

    private static void fail(int requestId, VpnError e, String message) {
        Vpn.deliverAck(requestId, false, e.ordinal(), message);
    }

    /// Whether a platform refusal means "nothing is provisioned".
    ///
    /// VpnManager answers a start or stop with no provisioned profile by
    /// throwing SecurityException, which reaches here wrapped in an
    /// InvocationTargetException.
    private static boolean isNotProvisioned(Exception e) {
        Throwable t = e.getCause() != null ? e.getCause() : e;
        return t instanceof SecurityException;
    }

    /// What went wrong, without letting a reflective wrapper hide it.
    private static String describe(Exception e) {
        Throwable t = e.getCause() != null ? e.getCause() : e;
        String m = t.getMessage();
        return m == null ? t.getClass().getName() : m;
    }

    /// The API-30 surface, resolved once.
    ///
    /// Held in a nested class so the lookup happens on first use rather than
    /// when the port loads, and so a device without these classes pays
    /// nothing for their absence.
    private static final class Reflect {
        private static final Class<?> MANAGER;
        private static final Class<?> BUILDER;
        private static final Method PROVISION;
        private static final Method DELETE;
        private static final Method START;
        private static final Method STOP;
        private static final boolean LOADED;

        static {
            Class<?> manager = null;
            Class<?> builder = null;
            Method provision = null;
            Method delete = null;
            Method start = null;
            Method stop = null;
            boolean ok = false;
            try {
                manager = Class.forName("android.net.VpnManager");
                builder = Class.forName("android.net.Ikev2VpnProfile$Builder");
                Class<?> profile = Class.forName("android.net.PlatformVpnProfile");
                provision = manager.getMethod("provisionVpnProfile", profile);
                delete = manager.getMethod("deleteProvisionedVpnProfile");
                start = manager.getMethod("startProvisionedVpnProfile");
                stop = manager.getMethod("stopProvisionedVpnProfile");
                ok = true;
            } catch (Throwable t) {
                // Below API 30. Every entry point checks LOADED first and
                // reports NOT_SUPPORTED, so there is nothing to log here.
                ok = false;
            }
            MANAGER = manager;
            BUILDER = builder;
            PROVISION = provision;
            DELETE = delete;
            START = start;
            STOP = stop;
            LOADED = ok;
        }

        static Object manager(Context context) {
            return context.getSystemService(MANAGER);
        }

        /// Builds an `Ikev2VpnProfile` from a portable profile.
        static Object buildIkev2(VpnProfile p) throws Exception {
            Object b = BUILDER
                    .getConstructor(String.class, String.class)
                    .newInstance(p.getServerAddress(),
                            p.getRemoteIdentifier() == null
                                    ? p.getServerAddress()
                                    : p.getRemoteIdentifier());
            // NON-EMPTY, not merely non-null. A profile carrying a valid
            // username and password plus sharedSecret("") took this branch
            // with a zero-length key and threw the credentials away -- while
            // iOS reads the same empty PSK as absent and uses the pair. The
            // install gate lets that profile through precisely because the
            // pair is valid, so the two ports have to agree on what an empty
            // secret means.
            if (p.getSharedSecret() != null
                    && p.getSharedSecret().length() > 0) {
                BUILDER.getMethod("setAuthPsk", byte[].class)
                        .invoke(b, (Object) utf8Bytes(p.getSharedSecret()));
            } else {
                BUILDER.getMethod("setAuthUsernamePassword", String.class,
                        String.class, java.security.cert.X509Certificate.class)
                        .invoke(b, p.getUsername(), p.getPassword(), null);
            }
            // Nothing here reads VpnProfile.onDemand: Ikev2VpnProfile has no
            // on-demand rules, which is why getVpnCapabilities does not claim
            // CAPABILITY_ON_DEMAND.
            return BUILDER.getMethod("build").invoke(b);
        }

        /// UTF-8, matching what CN1Vpn.m stores for the same Java string.
        /// The previous cast of each UTF-16 unit to a byte truncated any
        /// non-ASCII character and mangled a surrogate pair outright, so a
        /// key that authenticated on iOS was rejected on Android with
        /// nothing to see in either profile.
        private static byte[] utf8Bytes(String v) {
            try {
                return v.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException never) {
                // Every JVM is required to support UTF-8.
                throw new IllegalStateException(never.toString());
            }
        }

        private Reflect() {
        }
    }
}
