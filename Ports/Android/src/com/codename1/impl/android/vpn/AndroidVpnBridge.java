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
import com.codename1.vpn.tunnel.TunnelStopReason;
import com.codename1.vpn.tunnel.Tunnels;

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

    /// The platform could not say what this app's profile is doing.
    private static final int PROFILE_STATE_UNKNOWN = Integer.MIN_VALUE;

    /// The platform says there is no provisioned profile at all.
    private static final int PROFILE_STATE_NONE = Integer.MIN_VALUE + 1;

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

    /// When that teardown was asked for, in wall-clock millis. See
    /// ownTunnelStillUp: it bounds how long an ambiguous answer may hold the
    /// status in DISCONNECTING.
    private volatile long stopRequestedAt;

    /// Counts the times something else settled who owns the tunnel.
    ///
    /// stopVpn writes its bookkeeping AFTER the platform call, deliberately:
    /// a refusal must not leave a teardown pending for a tunnel that is
    /// still up. That leaves a window the flags alone cannot describe. The
    /// transport loss can arrive while the thread is inside
    /// stopProvisionedVpnProfile; onTunnelTransport then clears both flags
    /// and publishes DISCONNECTED, and the returning thread writes
    /// stopRequested = true for a teardown that is already over. Nothing
    /// clears it afterwards -- reconciledStatus settles only from
    /// DISCONNECTING -- so setStatusListening(false) never unregisters the
    /// watcher, and the next VPN loss belonging to some other app is
    /// published as this profile's.
    ///
    /// Read before the platform call and compared after, so both the commit
    /// and the rollback apply only if nothing happened in between. That is
    /// what "after the platform accepted" was always reaching for; the flag
    /// it was written on cannot say it, because a stop is exactly the case
    /// where startRequested is legitimately false (the restart case), so
    /// "still ours" and "settled while we waited" look identical from there.
    ///
    /// Guarded by this instance's monitor, not volatile: it is read and
    /// written together with the flags it is about.
    private long ownershipEpoch;

    /// How long a pending teardown may go unconfirmed before it is taken as
    /// finished.
    ///
    /// Only reached where the platform cannot answer for this app's own
    /// profile AND some other app's VPN is up, which is the one combination
    /// in which nothing available says whether our tunnel is gone.
    /// stopProvisionedVpnProfile is a request Android acts on promptly, so
    /// after this long either it is down or nothing here will ever learn
    /// that it is. Settling is the recoverable error of the two: a status
    /// that says DISCONNECTED slightly early is corrected by the next
    /// transport event, while DISCONNECTING is a state nothing can leave.
    private static final long STOP_SETTLE_MILLIS = 8000L;

    /// A status written while an operation held the bridge, awaiting its
    /// release. Guarded by `this`, like the status it mirrors.
    private VpnStatus pendingPublication;

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
        // The port ships CN1VpnService, so this is true wherever the
        // manifest declares it -- which the builder does only for an app
        // that referenced com.codename1.vpn.tunnel. Tested rather than
        // assumed: an app carrying the class without the <service> cannot
        // run a tunnel, and saying it can would send it down a path that
        // fails at establish() with nothing to explain it.
        return declaresTunnelService();
    }

    /// Whether the manifest declares the tunnel service in a form Android
    /// will bind.
    ///
    /// The NAME is not the test, which is what this used to do while its own
    /// comment said otherwise. Android binds a VpnService only when the
    /// declaration also carries `android:permission="BIND_VPN_SERVICE"` --
    /// the service requiring that its binder be the system -- and it finds
    /// it by the `android.net.VpnService` action. A declaration missing
    /// either is refused at bind time, so establish() answers null on a
    /// build that otherwise looks complete, and saying the capability is
    /// there sends an app down exactly that path.
    ///
    /// The builder refuses to emit one, and refuses to stand aside for a
    /// project-supplied declaration that lacks either -- see
    /// VpnManifestFragments. This is the same question asked of the manifest
    /// that actually shipped, which is the one that can have come from a
    /// merge, an older build, or a cn1lib.
    private boolean declaresTunnelService() {
        String name = CN1VpnService.class.getName();
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ServiceInfo[] services = pm.getPackageInfo(
                    context.getPackageName(),
                    android.content.pm.PackageManager.GET_SERVICES).services;
            if (services == null) {
                return false;
            }
            boolean permitted = false;
            for (int i = 0; i < services.length && !permitted; i++) {
                permitted = name.equals(services[i].name)
                        && BIND_VPN_SERVICE.equals(services[i].permission);
            }
            if (!permitted) {
                return false;
            }
            // And that the system can FIND it. Starting is an explicit
            // intent and needs no filter, but a VpnService without the
            // action is not one as far as the platform is concerned -- it
            // never appears as an always-on VPN and the consent flow has
            // nothing to name.
            return resolvesTo(pm.queryIntentServices(
                    new Intent("android.net.VpnService"), 0), name);
        } catch (Exception missing) {
            // A package manager that cannot answer says nothing about the
            // manifest, and claiming the capability on a guess is the
            // failure this method exists to avoid.
            return false;
        }
    }

    /// Whether any of these resolutions names this service class.
    ///
    /// In its own method, outside any `try`, for the reason asIntent gives:
    /// reading a typed List inserts a checkcast on every get(), and under a
    /// handler that catches Exception that is a cast whose failure the
    /// handler appears to cover -- which ParparVM would not deliver.
    private static boolean resolvesTo(java.util.List<
            android.content.pm.ResolveInfo> matches, String name) {
        if (matches == null) {
            return false;
        }
        for (int i = 0; i < matches.size(); i++) {
            android.content.pm.ResolveInfo match = matches.get(i);
            if (match != null && match.serviceInfo != null
                    && name.equals(match.serviceInfo.name)) {
                return true;
            }
        }
        return false;
    }

    /// What the tunnel service requires of its binder, so only the system
    /// may. Mirrors VpnManifestFragments.BIND_VPN_SERVICE; the two are one
    /// contract read from either end.
    private static final String BIND_VPN_SERVICE =
            "android.permission.BIND_VPN_SERVICE";

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
        if (ownTunnelStillUp()) {
            synchronized (this) {
                return status;
            }
        }
        synchronized (this) {
            // Re-checked AND committed in one critical section. Split, a
            // start arriving between the check and the writes below took
            // CONNECTING and startRequested=true, and then this stale
            // settlement cleared the flag and overwrote the status with
            // DISCONNECTED -- after which the new tunnel's transport
            // callbacks were discarded as somebody else's. The three writes
            // are what the check was about, so they belong with it.
            if (status != VpnStatus.DISCONNECTING || !stopRequested) {
                return status;
            }
            stopRequested = false;
            startRequested = false;
            status = VpnStatus.DISCONNECTED;
            ownershipEpoch++;
        }
        // Only the NOTIFICATION is outside, which is the rule setStatus
        // exists to state: application code never runs under this monitor.
        publishStatus(VpnStatus.DISCONNECTED);
        return VpnStatus.DISCONNECTED;
    }

    /// Whether this app's own tunnel still appears to be up, for a teardown
    /// that has been asked for and not yet seen through.
    ///
    /// Three answers in descending order of authority, because the first two
    /// are not always available:
    ///
    /// 1. The platform's own view of THIS app's provisioned profile. Exact
    ///    when the device offers it, and the only source here that is about
    ///    our profile rather than about VPNs in general.
    /// 2. Otherwise, whether any VPN transport is present. Android will not
    ///    say WHICH VPN a transport belongs to, so another app's counts --
    ///    and on its own that was enough to hold this app in DISCONNECTING
    ///    for as long as a corporate VPN stayed connected, which is to say
    ///    indefinitely. It is evidence, not proof.
    /// 3. So the ambiguous answer expires. Past STOP_SETTLE_MILLIS the
    ///    teardown is taken as done whatever the transport says.
    ///
    /// A tunnel of our own that really is going down still settles the
    /// instant its transport is lost, through onTunnelTransport; none of
    /// this replaces that, it only covers the case where no loss is coming.
    private boolean ownTunnelStillUp() {
        int own = provisionedProfileState();
        if (own == Reflect.STATE_CONNECTED || own == Reflect.STATE_CONNECTING) {
            return true;
        }
        if (own != PROFILE_STATE_UNKNOWN) {
            // The platform answered, and it did not say up.
            return false;
        }
        if (!vpnTransportPresent()) {
            return false;
        }
        long since = System.currentTimeMillis() - stopRequestedAt;
        // Negative would mean the clock went backwards, which is a reason to
        // stop waiting rather than to wait for ever.
        return since >= 0 && since < STOP_SETTLE_MILLIS;
    }

    /// This app's provisioned profile state, or PROFILE_STATE_UNKNOWN.
    private int provisionedProfileState() {
        if (Reflect.PROFILE_STATE == null || Reflect.STATE_OF == null) {
            return PROFILE_STATE_UNKNOWN;
        }
        try {
            Object state = Reflect.PROFILE_STATE.invoke(Reflect.manager(context));
            if (state == null) {
                // No profile provisioned; nothing of ours is up.
                return PROFILE_STATE_NONE;
            }
            // Narrowed in asInt, outside this try, for the reason asIntent
            // gives: ParparVM does not check CHECKCAST, so a cast that fails
            // under a handler does not throw and cannot be caught --
            // scripts/check-cast-semantics.sh reports the shape whether or
            // not an instanceof sits beside it.
            return asInt(Reflect.STATE_OF.invoke(state),
                    PROFILE_STATE_UNKNOWN);
        } catch (Exception e) {
            // Including the SecurityException a platform throws with nothing
            // provisioned. Unknown rather than "down": the transport check
            // and the deadline below are what decide then.
            return PROFILE_STATE_UNKNOWN;
        }
    }

    /// Whether the platform has ANY VPN transport up right now.
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
        // On-demand is the same shape as the local identifier below: a
        // setting this platform cannot express, which the install would
        // otherwise acknowledge and then store as though it had taken.
        // load() reads the stored record back, so the app was told its
        // profile connects on demand while Android had installed a manually
        // started tunnel -- and getCapabilities has never claimed
        // CAPABILITY_ON_DEMAND, so the two disagreed about the same profile.
        if (p.isOnDemand()) {
            fail(requestId, VpnError.NOT_SUPPORTED,
                    "Android's managed VPN has no on-demand rules; install"
                    + " the profile without onDemand and start it when the"
                    + " app decides, or branch on Vpn.getCapabilities().");
            return;
        }
        // Ikev2VpnProfile.Builder cannot express a client IKE identity. Its
        // two-argument constructor takes the server address and the SERVER's
        // identity, and no setter carries the local one -- so a profile that
        // asked for one provisioned with the platform's default client
        // identity while load() went on reporting the identifier the app had
        // supplied. A gateway that authenticates on that identity then
        // refuses the tunnel, and nothing in the app or the profile says why.
        // Refused for the reason the credential pair above is: a materially
        // different configuration acknowledged as success is the worse
        // answer.
        if (p.getLocalIdentifier() != null
                && p.getLocalIdentifier().length() > 0) {
            fail(requestId, VpnError.NOT_SUPPORTED,
                    "Android's managed VPN cannot set a local IKE identifier;"
                    + " the platform chooses the client identity. Install the"
                    + " profile without one, or branch on"
                    + " Vpn.getCapabilities().");
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
                // The status is WRITTEN here and PUBLISHED by
                // endOperation; see setStatus. Written before the release
                // because a removal could otherwise claim the bridge in
                // between, delete the profile just provisioned and publish
                // NOT_CONFIGURED, which this thread would then overwrite --
                // both calls reporting success while load() finds no profile
                // and getStatus insists one is merely disconnected.
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
            if (channelBusy(currentActivity())) {
                // As the tunnel start does, and for the same reason: this
                // reservation covers VPN operations, not the activity result
                // channel every feature shares.
                endOperation(mine);
                fail(requestId, VpnError.UNKNOWN,
                        "Another dialog is already waiting for a result;"
                        + " install the profile once it closes");
                return;
            }
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
            // status is written while the reservation is still held so
            // nothing else can claim the profile in between, and published
            // by endOperation so the listener runs with the bridge free.
            // See setStatus.
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
            // The same argument, for the flag that says a teardown of ours is
            // still outstanding. remove() after an acknowledged stop() is the
            // ordinary way to retire a profile, and it leaves one pending by
            // construction -- stop() reports as soon as the platform accepts,
            // while the transport lives on for the teardown. Cleared only
            // here, the removal published NOT_CONFIGURED and the loss that
            // followed was still ours to hear, so it overwrote that with
            // DISCONNECTED and getStatus() went back to claiming a profile
            // this method had just deleted. Ownership of a teardown cannot
            // outlive the profile being torn down.
            stopRequested = false;
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
    /// Releases the reservation and publishes whatever the operation wrote.
    ///
    /// The publication belongs here rather than in setStatus; see the note
    /// there. It happens after the owner is cleared and outside the monitor,
    /// so a listener is free to start the very operation it was told about.
    ///
    /// Only the holder publishes. This is called again from every finally,
    /// and a second call finds the owner already null, so a status is never
    /// announced twice.
    private void endOperation(Object token) {
        VpnStatus publish;
        synchronized (this) {
            if (operationOwner != token) {
                return;
            }
            operationOwner = null;
            publish = pendingPublication;
            pendingPublication = null;
        }
        if (publish != null) {
            Vpn.deliverStatusChanged(publish.ordinal());
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
        // Sampled before the platform is asked; every write below is
        // conditional on it not having moved. See ownershipEpoch.
        long epoch;
        synchronized (this) {
            epoch = ownershipEpoch;
        }
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
            // Stamped before the flag, so a reader that sees the flag always
            // sees a time that is not left over from a previous stop.
            boolean settledMeanwhile;
            synchronized (this) {
                settledMeanwhile = ownershipEpoch != epoch;
                if (!settledMeanwhile) {
                    stopRequestedAt = System.currentTimeMillis();
                    stopRequested = true;
                }
            }
            if (settledMeanwhile) {
                // The loss arrived while the call was in flight, so the
                // teardown this method asked for is already over and the
                // flags belong to nobody. The DISCONNECTING written above
                // has to give way to what the callback concluded -- left
                // standing with stopRequested false, reconciledStatus can
                // never settle it and getStatus() reports a tunnel going
                // down for the life of the process.
                setStatus(VpnStatus.DISCONNECTED);
            }
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
            //
            // Under the same epoch guard as the success path, and for a
            // sharper reason: a refusal with a loss alongside it means the
            // user took the tunnel down from Settings while this call was
            // failing, so restoring startRequested would claim a tunnel that
            // is provably gone.
            boolean settledMeanwhile;
            synchronized (this) {
                settledMeanwhile = ownershipEpoch != epoch;
                if (!settledMeanwhile) {
                    startRequested = wasRequested && !absent;
                }
            }
            if (absent) {
                forgetInstalledProfile();
            }
            setStatus(absent ? VpnStatus.NOT_CONFIGURED
                    : settledMeanwhile ? VpnStatus.DISCONNECTED : before);
            endOperation(mine);
            fail(requestId, absent
                    ? VpnError.NOT_CONFIGURED : VpnError.UNKNOWN, describe(e));
        } finally {
            // See startVpn: released on every path.
            endOperation(mine);
        }
    }

    /// Whether the one activity-result listener is already taken.
    ///
    /// setIntentResultListener returns WITHOUT installing while another flow
    /// is waiting, so a prompt launched then has its result delivered to
    /// that flow and its own request is never answered.
    private static boolean channelBusy(Activity a) {
        return a instanceof com.codename1.impl.android.CodenameOneActivity
                && ((com.codename1.impl.android.CodenameOneActivity) a)
                        .isWaitingForResult();
    }

    /// Refuses a tunnel start, dropping the tunnel it registered.
    ///
    /// Every refusal goes through here rather than calling deliverAck
    /// directly: a start that never reaches the service has to release its
    /// entry, and a path that forgets leaks the application's tunnel object
    /// until the process ends.
    private static void failStart(int requestId, VpnError e, String message) {
        Tunnels.abandon(requestId);
        Tunnels.deliverAck(requestId, false, e.ordinal(), message);
    }

    /// Which tunnel start the bridge is on, bumped by every start and every
    /// stop.
    ///
    /// CN1VpnService has a generation of its own, and it cannot see this
    /// window: the consent prompt is open, no service exists yet, and
    /// Tunnels.stop() in the meantime starts the service with ACTION_STOP,
    /// acknowledges the stop and lets it go. An approval arriving after that
    /// launched the original tunnel anyway -- a VPN coming up seconds after
    /// the app had been told it was down, with the user having answered a
    /// prompt for a tunnel that no longer existed.
    ///
    /// Sampled by TunnelConsent when the prompt opens and compared when it
    /// answers. Guarded by this instance's monitor.
    private int tunnelGeneration;

    /// A new tunnel generation, invalidating any consent still on screen.
    private synchronized int nextTunnelGeneration() {
        return ++tunnelGeneration;
    }

    @Override
    public void startCustomTunnel(final int requestId, final String setupWire) {
        if (!isCustomTunnelSupported()) {
            failStart(requestId, VpnError.NOT_SUPPORTED,
                    "This build does not declare a VPN tunnel service;"
                    + " reference com.codename1.vpn.tunnel so the builder"
                    + " adds it");
            return;
        }
        // RESERVED, like every other path here that can open a prompt.
        // CodenameOneActivity keeps ONE result listener and
        // setIntentResultListener silently refuses to replace it while one
        // is waiting -- so a second consent launched over the first had its
        // result delivered to the first flow, left this request in
        // VpnRequests for ever, and overwrote the registered tunnel while it
        // was at it. The profile install guards this; the tunnel did not.
        Object mine = new Object();
        synchronized (this) {
            if (operationOwner != null) {
                failStart(requestId, VpnError.UNKNOWN,
                        "A VPN operation is in progress; wait for it to"
                        + " finish before starting a tunnel");
                return;
            }
            operationOwner = mine;
        }
        // This start's generation, taken before anything can be shown. A
        // stop -- or another start -- moves it on, and the consent below
        // finds out when it answers.
        final int generation = nextTunnelGeneration();
        // CONSENT first, and it is a prompt rather than a permission: an app
        // cannot hold BIND_VPN_SERVICE, it asks the user each time the grant
        // is not already in force. prepare() answers null when it is.
        Intent consent;
        try {
            consent = asIntent(android.net.VpnService.prepare(context));
        } catch (Exception refused) {
            endOperation(mine);
            failStart(requestId, VpnError.UNKNOWN, describe(refused));
            return;
        }
        if (consent == null) {
            endOperation(mine);
            launchTunnel(requestId, setupWire, generation);
            return;
        }
        Activity a = currentActivity();
        if (a == null) {
            endOperation(mine);
            failStart(requestId, VpnError.UNAUTHORIZED,
                    "Starting a VPN tunnel needs a foreground activity to"
                    + " show the consent prompt");
            return;
        }
        if (channelBusy(a)) {
            // The SHARED result channel; see CallScreeningRole for the same
            // guard. The reservation above serializes VPN operations against
            // each other and says nothing about a file chooser holding the
            // one listener CodenameOneActivity keeps.
            endOperation(mine);
            failStart(requestId, VpnError.UNKNOWN,
                    "Another dialog is already waiting for a result; start"
                    + " the tunnel once it closes");
            return;
        }
        try {
            com.codename1.impl.android.AndroidNativeUtil
                    .startActivityForResult(consent,
                            new TunnelConsent(this, requestId, setupWire,
                                    mine, generation));
            // From here the reservation belongs to TunnelConsent, exactly as
            // the install hands its own to Consent.
            return;
        } catch (RuntimeException launchFailed) {
            endOperation(mine);
            // The same case installProfile guards: a cached context that
            // still looks like an Activity after the app went to the
            // background. Answered rather than left pending.
            failStart(requestId, VpnError.UNAUTHORIZED,
                    "The VPN consent prompt could not be shown: "
                            + describe(launchFailed));
        }
    }

    /// Starts the service now that consent is in force, unless a stop has
    /// won in the meantime.
    ///
    /// The check is HERE rather than at the callers, because there are two
    /// of them and only one had it: an app whose consent was already granted
    /// takes the pre-authorized path, where prepare() answers null and this
    /// was reached without any generation test at all. A stop landing while
    /// prepare() was in flight was acknowledged, and then the tunnel came up
    /// anyway. One check in the one place every start passes through is the
    /// difference between a rule and a rule with an exception nobody
    /// remembered.
    ///
    /// @param generation the value sampled when this start began
    void launchTunnel(int requestId, String setupWire, int generation) {
        try {
            Intent i = new Intent(context, CN1VpnService.class);
            i.putExtra(CN1VpnService.EXTRA_SETUP, setupWire);
            i.putExtra(CN1VpnService.EXTRA_REQUEST, requestId);
            // The test and the send in ONE critical section, matched by the
            // bump-and-send in stopCustomTunnel. Split, a stop could move the
            // generation between them and its intent still reach the service
            // first, which is the ordering this is about.
            //
            // startForegroundService on Oreo and newer. This said
            // startService, justified as "a VpnService is exempt from the
            // background start restriction precisely because the user has
            // just consented to it, and asking for a foreground service would
            // demand a notification the tunnel does not need". Both halves
            // were wrong.
            //
            // The second half is refuted by this port's own code:
            // CN1VpnService.promote() builds a notification and calls
            // startForeground, because Android 8 shuts down an ordinary
            // started service. The notification is not a cost of asking for a
            // foreground service; it is already being paid.
            //
            // The first half rests on consent granted JUST NOW. A start where
            // the user has already authorised the app skips the dialog
            // entirely -- a reconnect, a retry after a network change, an app
            // resuming work in the background -- and then this is an ordinary
            // background startService subject to the Android 8 limits. It
            // throws, the catch below reports UNKNOWN, and a reconnect fails
            // for an app that still holds VPN authorisation. Rather than rest
            // on an exemption whose scope is not something this code can
            // establish, ask for what the service actually becomes.
            //
            // startForegroundService is a PROMISE to promote within a few
            // seconds, and the service now keeps it at the top of
            // onStartCommand rather than after establish() -- see the note
            // there, which is the other half of this fix and not optional.
            boolean sent;
            synchronized (this) {
                sent = generation == tunnelGeneration;
                if (sent) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(i);
                    } else {
                        context.startService(i);
                    }
                }
            }
            if (!sent) {
                failStart(requestId, VpnError.UNKNOWN,
                        "The tunnel start was superseded by a stop before the"
                        + " service was asked to run");
            }
        } catch (RuntimeException refused) {
            failStart(requestId, VpnError.UNKNOWN, describe(refused));
        }
    }

    @Override
    public void stopCustomTunnel(int requestId) {
        if (!isCustomTunnelSupported()) {
            Tunnels.deliverAck(requestId, false,
                    VpnError.NOT_SUPPORTED.ordinal(), null);
            return;
        }
        try {
            Intent i = new Intent(context, CN1VpnService.class);
            i.setAction(CN1VpnService.ACTION_STOP);
            i.putExtra(CN1VpnService.EXTRA_REQUEST, requestId);
            // Still startService, unlike the start above, and for a reason
            // rather than by omission. Either the tunnel is running -- in
            // which case this app owns a foreground service and the Android 8
            // background limits do not apply to it -- or it is not, in which
            // case the throw is caught below and answered as the success it
            // is. Asking for a foreground service here would instead promise
            // a promotion that ACTION_STOP has no intention of making, since
            // its whole job is to stop.
            //
            // The bump BEFORE the service is told, and in the same critical
            // section, so a start cannot read the old generation here and
            // still send its intent after this one. See tunnelGeneration and
            // the matching block in launchTunnel.
            synchronized (this) {
                tunnelGeneration++;
                context.startService(i);
            }
        } catch (RuntimeException refused) {
            // A service that is not running cannot be told to stop, and the
            // app asking for a stopped tunnel to stop has got what it asked
            // for -- so this is a success, not an error.
            CN1VpnService.stopTunnel(TunnelStopReason.REQUESTED);
            Tunnels.deliverAck(requestId, true, 0, null);
        }
    }

    /// Answers the tunnel start once the user has decided.
    ///
    /// A named class rather than an anonymous one so it carries no synthetic
    /// reference to the activity, which outlives the dialog.
    private static final class TunnelConsent
            implements com.codename1.impl.android.IntentResultListener {
        private final AndroidVpnBridge bridge;
        private final int requestId;
        private final String setupWire;
        private final Object token;
        private final int generation;

        TunnelConsent(AndroidVpnBridge bridge, int requestId,
                String setupWire, Object token, int generation) {
            this.bridge = bridge;
            this.requestId = requestId;
            this.setupWire = setupWire;
            this.token = token;
            this.generation = generation;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            // RELEASED before either answer, so a listener that starts
            // another operation from the callback is not refused by the
            // reservation this one was still holding.
            bridge.endOperation(token);
            if (resultCode == Activity.RESULT_OK) {
                // The generation goes to launchTunnel rather than being
                // tested here: a stop winning while this prompt was on
                // screen has already been acknowledged, and launching now
                // would bring a VPN up seconds after the app was told it was
                // down -- the service's own generation cannot refuse it,
                // because this start would be the newest thing it had ever
                // seen. Testing it there covers the pre-authorized path too,
                // which is the one this check originally missed.
                bridge.launchTunnel(requestId, setupWire, generation);
                return;
            }
            // USER_DECLINED rather than an error: refusing a VPN prompt is
            // an ordinary answer, and an app that treats it as a failure
            // shows the user a problem where they made a choice.
            failStart(requestId, VpnError.USER_DECLINED,
                    "The user declined the VPN consent prompt");
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
        // The tests and the writes in ONE critical section, which is the
        // rule reconciledStatus states for the same three fields: split,
        // a start arriving between them is read by the guard and then
        // overwritten by the clear. Only setStatus is outside, because it
        // reaches application code and this runs on a platform callback
        // thread.
        synchronized (this) {
            if (!startRequested && !stopRequested) {
                return;
            }
            if (available && !startRequested) {
                // A tunnel COMING UP while this app is tearing its own down
                // is not this app's; reporting CONNECTED there would undo
                // the stop the caller just asked for.
                return;
            }
            if (!available) {
                // The tunnel is actually gone, which is the point at which
                // this app stops owning one -- whether it asked to stop or
                // the user did it from Settings. stopVpn used to clear these
                // itself and publish DISCONNECTED before the teardown had
                // happened.
                //
                // The epoch is what tells a stopVpn still inside the
                // platform call that this happened; see its declaration.
                stopRequested = false;
                startRequested = false;
                ownershipEpoch++;
            }
        }
        setStatus(available ? VpnStatus.CONNECTED : VpnStatus.DISCONNECTED);
    }

    /// Moves to a new status and tells whoever is listening.
    ///
    /// The two halves are deliberately separated in time when an operation
    /// owns the bridge. Vpn.deliverStatusChanged runs the listener INLINE
    /// when the caller is already on the EDT, which is where an app calls
    /// Vpn.install() from -- so publishing from inside a reservation ran
    /// application code while the reservation was still held, and a listener
    /// that reacted to the new profile by calling Vpn.start() was refused
    /// with UNKNOWN every single time. Not a race: the ordinary path.
    ///
    /// Writing the status and releasing the reservation cannot simply be
    /// swapped, either, which is why this is not a one-line reordering.
    /// Releasing first lets a removal claim the bridge, delete the profile
    /// just provisioned and publish NOT_CONFIGURED, and then this thread
    /// overwrites that with a status describing a profile that no longer
    /// exists. So the WRITE stays inside the reservation and the
    /// PUBLICATION moves out of it, to endOperation.
    ///
    /// Deferring means an operation that writes a status it then rolls back
    /// -- stopVpn setting DISCONNECTING and restoring it when the platform
    /// refuses -- publishes only where it ended up, rather than announcing a
    /// state that never existed and then taking it back.
    /// Announces a status this class has already recorded.
    ///
    /// Split out so reconciledStatus can commit its transition under the
    /// monitor and announce afterwards; see setStatus for why the two halves
    /// are separated at all.
    private void publishStatus(VpnStatus s) {
        boolean publishNow;
        synchronized (this) {
            if (!listening) {
                pendingPublication = null;
                return;
            }
            if (operationOwner != null) {
                pendingPublication = s;
                return;
            }
            publishNow = true;
        }
        if (publishNow) {
            Vpn.deliverStatusChanged(s.ordinal());
        }
    }

    void setStatus(VpnStatus s) {
        boolean publishNow;
        synchronized (this) {
            this.status = s;
            if (!listening) {
                // Nothing to publish, and nothing to remember: a listener
                // registering later reads getStatus() rather than a backlog.
                pendingPublication = null;
                return;
            }
            if (operationOwner != null) {
                pendingPublication = s;
                return;
            }
            publishNow = true;
        }
        if (publishNow) {
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

    /// A reflective answer as an `int`, or the fallback when it is not one.
    ///
    /// In its own method, outside any `try`, for the reason asIntent gives.
    private static int asInt(Object o, int fallback) {
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        return fallback;
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
        /// getProvisionedVpnProfileState, or null where the platform has no
        /// such method. It arrived well after VpnManager itself, so it is
        /// looked up separately and its absence is not a failure to load --
        /// everything else here still works without it.
        private static final Method PROFILE_STATE;
        /// VpnProfileState.getState, paired with PROFILE_STATE.
        private static final Method STATE_OF;
        /// STATE_CONNECTED and STATE_CONNECTING, read from the class rather
        /// than written down here: a constant copied into this file would go
        /// on comparing equal after the platform renumbered it.
        private static final int STATE_CONNECTED;
        private static final int STATE_CONNECTING;
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

            Method profileState = null;
            Method stateOf = null;
            int connected = -1;
            int connecting = -2;
            try {
                if (manager != null) {
                    profileState =
                            manager.getMethod("getProvisionedVpnProfileState");
                    Class<?> state = Class.forName("android.net.VpnProfileState");
                    stateOf = state.getMethod("getState");
                    connected = state.getField("STATE_CONNECTED").getInt(null);
                    connecting = state.getField("STATE_CONNECTING").getInt(null);
                }
            } catch (Throwable t) {
                // A platform old enough to have VpnManager without this. The
                // callers fall back rather than failing; see
                // ownTunnelStillUp.
                profileState = null;
                stateOf = null;
            }
            PROFILE_STATE = profileState;
            STATE_OF = stateOf;
            STATE_CONNECTED = connected;
            STATE_CONNECTING = connecting;
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
            // CAPABILITY_ON_DEMAND -- and why installProfile refuses a
            // profile that asks for it rather than dropping the setting and
            // storing a record that says otherwise.
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
