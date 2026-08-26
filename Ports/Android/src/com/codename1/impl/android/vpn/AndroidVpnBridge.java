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
    private VpnStatus status = VpnStatus.NOT_CONFIGURED;
    private boolean listening;
    private ConnectivityManager.NetworkCallback networkCallback;

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
        return status.ordinal();
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
        if (p.getProtocol() == VpnProtocol.IPSEC) {
            fail(requestId, VpnError.INVALID_CONFIGURATION,
                    "Android's managed VPN offers IKEv2 only; IPSEC is an iOS"
                    + " configuration");
            return;
        }
        try {
            Object profile = Reflect.buildIkev2(p);
            Object manager = Reflect.manager(context);
            // Tested with instanceof rather than cast: ParparVM does not
            // check CHECKCAST, so a cast that fails there does not throw and
            // cannot be caught.
            Object raw = Reflect.PROVISION.invoke(manager, profile);
            Intent consent = asIntent(raw);
            installedWire = wire;
            if (raw == null) {
                // Already consented; the profile is provisioned.
                setStatus(VpnStatus.DISCONNECTED);
                Vpn.deliverAck(requestId, true, 0, null);
                return;
            }
            if (!(context instanceof Activity) || consent == null) {
                fail(requestId, VpnError.UNAUTHORIZED,
                        "Installing a VPN needs a foreground activity to show"
                        + " the consent prompt");
                return;
            }
            com.codename1.impl.android.AndroidNativeUtil.startActivityForResult(
                    consent, new Consent(this, requestId));
        } catch (Exception e) {
            fail(requestId, VpnError.INVALID_CONFIGURATION, describe(e));
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

        Consent(AndroidVpnBridge bridge, int requestId) {
            this.bridge = bridge;
            this.requestId = requestId;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                Preferences.set(WIRE_PREF, strip(bridge.installedWire));
                bridge.setStatus(VpnStatus.DISCONNECTED);
                Vpn.deliverAck(requestId, true, 0, null);
            } else {
                bridge.installedWire = null;
                Preferences.delete(WIRE_PREF);
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
        try {
            Reflect.DELETE.invoke(Reflect.manager(context));
            installedWire = null;
            Preferences.delete(WIRE_PREF);
            setStatus(VpnStatus.NOT_CONFIGURED);
            Vpn.deliverAck(requestId, true, 0, null);
        } catch (Exception e) {
            fail(requestId, VpnError.UNKNOWN, describe(e));
        }
    }

    @Override
    public void loadProfile(int requestId) {
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
        if (installedWire != null) {
            return installedWire;
        }
        String saved = Preferences.get(WIRE_PREF, null);
        installedWire = saved;
        return saved;
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
        try {
            setStatus(VpnStatus.CONNECTING);
            Reflect.START.invoke(Reflect.manager(context));
            // The platform reports no completion, so the tunnel is CONNECTING
            // until the network callback says otherwise. Answering the request
            // now is the honest thing: "the platform accepted the request" is
            // all that is actually known.
            Vpn.deliverAck(requestId, true, 0, null);
        } catch (Exception e) {
            setStatus(VpnStatus.DISCONNECTED);
            // A SecurityException here is the platform saying no profile is
            // provisioned, which is a different answer from a tunnel that
            // could not be established.
            fail(requestId, isNotProvisioned(e)
                    ? VpnError.NOT_CONFIGURED : VpnError.CONNECTION_FAILED,
                    describe(e));
        }
    }

    @Override
    public void stopVpn(int requestId) {
        if (!available()) {
            fail(requestId, VpnError.NOT_SUPPORTED, null);
            return;
        }
        // Not gated on installedWire; see startVpn.
        try {
            setStatus(VpnStatus.DISCONNECTING);
            Reflect.STOP.invoke(Reflect.manager(context));
            setStatus(VpnStatus.DISCONNECTED);
            Vpn.deliverAck(requestId, true, 0, null);
        } catch (Exception e) {
            fail(requestId, isNotProvisioned(e)
                    ? VpnError.NOT_CONFIGURED : VpnError.UNKNOWN, describe(e));
        }
    }

    @Override
    public void setStatusListening(boolean value) {
        this.listening = value;
        if (value) {
            startWatchingTheTunnel();
        } else {
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
        if (networkCallback != null || Build.VERSION.SDK_INT < 21) {
            return;
        }
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return;
        }
        networkCallback = new TunnelWatcher(this);
        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    // A VPN is not "internet capable" by the default filter's
                    // reckoning, so the default capabilities would never
                    // match one.
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build();
            cm.registerNetworkCallback(request, networkCallback);
        } catch (RuntimeException e) {
            // Some devices refuse the registration; the API then reports
            // whatever setStatus last set, which is what it did before.
            networkCallback = null;
        }
    }

    private void stopWatchingTheTunnel() {
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
            bridge.setStatus(VpnStatus.CONNECTED);
        }

        @Override
        public void onLost(Network network) {
            bridge.setStatus(VpnStatus.DISCONNECTED);
        }
    }

    void setStatus(VpnStatus s) {
        this.status = s;
        if (listening) {
            Vpn.deliverStatusChanged(s.ordinal());
        }
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
            if (p.getSharedSecret() != null) {
                BUILDER.getMethod("setAuthPsk", byte[].class)
                        .invoke(b, (Object) asciiBytes(p.getSharedSecret()));
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

        private static byte[] asciiBytes(String v) {
            byte[] out = new byte[v.length()];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) v.charAt(i);
            }
            return out;
        }

        private Reflect() {
        }
    }
}
