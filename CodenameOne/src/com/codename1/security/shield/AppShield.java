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
package com.codename1.security.shield;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.security.shield.spi.ShieldEngine;
import com.codename1.security.shield.spi.ShieldEngineRegistry;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import java.util.Hashtable;
import java.util.Vector;

/// API shielding: proves to your own backend that a request came from a genuine, unmodified build
/// of your app running on a device that has not been tampered with.
///
/// #### How it differs from [com.codename1.security.DeviceIntegrity]
///
/// `DeviceIntegrity` is the raw platform primitive -- it hands you a Play Integrity or App Attest
/// blob and leaves verification, policy and enforcement to you. `AppShield` is the managed layer
/// on top: the attestation is verified server side against Apple and Google, evaluated against a
/// policy you control, and turned into a short-lived signed token your backend can check with a
/// few lines of middleware. Both remain available and `DeviceIntegrity` keeps working unchanged;
/// an app that only needs the raw blob should keep using it.
///
/// #### The shape of the thing
///
/// ```java
/// AppShield.init(new ShieldConfig()
///     .protect("api.mybank.example", HostPolicy.PROTECTED));
///
/// // ...then just make requests. Protected hosts get the header and the pin check.
/// ConnectionRequest r = new ConnectionRequest("https://api.mybank.example/transfer", true);
/// NetworkManager.getInstance().addToQueueAndWait(r);
/// ```
///
/// Your backend rejects any request whose token is missing, expired or unsigned. That check is
/// where the security actually lives -- not in this class. A device the attacker fully controls
/// can always strip a header; what it cannot do is mint a token, because the token is signed by a
/// service the attacker does not control, on the strength of a statement from Apple or Google.
///
/// #### When the engine is absent
///
/// Builds without the enterprise attestation engine -- open-source builds, and any project not
/// entitled to it -- get a working, inert implementation. [#isProtected()] returns false,
/// [#fetchToken()] completes with [ShieldStatus#UNPROTECTED] rather than hanging, [#attach] does
/// nothing, and no request is ever blocked. The API is safe to call unconditionally; there is no
/// need to guard call sites.
///
/// #### Threading
///
/// [#fetchToken()] is asynchronous. [#attach(ConnectionRequest)] blocks and must not be called on
/// the EDT -- in normal use you never call it yourself, because a protected host is handled
/// automatically on the network thread.
public final class AppShield {

    private static ShieldConfig config;
    private static boolean initialized;
    private static ShieldStatus lastStatus = ShieldStatus.NOT_INITIALIZED;
    private static final Vector listeners = new Vector();
    private static final Hashtable runtimeHosts = new Hashtable();

    /// Response header a backend sets to say it rejected the *attestation token*, as opposed to
    /// the user's own credentials.
    ///
    /// Without it a 401 or 403 is ambiguous: protected APIs normally carry ordinary user
    /// authorization too, and treating every such response as an attestation rejection would make
    /// a client re-attest through an entire login failure. Emit it only when the token itself was
    /// the problem; the value is ignored.
    public static final String REJECT_HEADER = "X-CN1-Attest-Reject";

    private AppShield() {
    }

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------

    /// Initializes the shield. Call once during app startup, after `Display.init`.
    ///
    /// Safe to call in a build with no attestation engine: it logs one line and leaves the shield
    /// inert. Calling it twice is a no-op.
    public static void init(ShieldConfig cfg) {
        synchronized (AppShield.class) {
            if (initialized) {
                return;
            }
            config = cfg == null ? new ShieldConfig() : cfg;
            initialized = true;
        }
        ShieldEngine engine = ShieldEngineRegistry.getEngine();
        try {
            engine.initialize(contextForEngine(), config);
            setStatus(engine.isAvailable() ? ShieldStatus.OK : ShieldStatus.UNPROTECTED);
        } catch (Throwable t) {
            // A failure inside the engine must not stop the app from starting.
            Log.e(t);
            setStatus(ShieldStatus.UNPROTECTED);
        }
        installNetworkGuard();
    }

    /// Hooks the shield into the network stack, which is what makes
    /// [ShieldConfig#protect(String, HostPolicy)] take effect on ordinary requests. Without it a
    /// registered host would carry a policy nothing consults.
    ///
    /// Installed even when no engine is present: the guard is inert in that case (the default
    /// engine issues no tokens and enforces no pins) and installing unconditionally keeps the
    /// behaviour identical whether or not the enterprise engine was injected.
    private static void installNetworkGuard() {
        try {
            NetworkManager.setNetworkGuard(new ShieldNetworkGuard());
        } catch (IllegalStateException e) {
            // The slot seals after the first install. An app that installed its
            // own guard keeps it; say so rather than failing startup, because the
            // consequence is that protected hosts are not decorated automatically
            // and that is worth knowing about.
            Log.p("AppShield: a network guard is already installed, so protected hosts will "
                    + "not be decorated automatically. Call AppShield.attach(request) from "
                    + "your own guard if you need both.");
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// True when a real attestation engine is present and available. False in an open-source or
    /// unentitled build, and in the simulator unless simulation is switched on.
    public static boolean isProtected() {
        // Guarded because the engine is pluggable and this is consulted on the failure
        // path: a partially initialized engine whose isAvailable() throws would turn a
        // fail-open host into a blocked request, which is the exact inversion the
        // degradation contract promises will not happen. Unanswerable means unprotected.
        try {
            return ShieldEngineRegistry.getEngine().isAvailable();
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// The active engine's name, for diagnostics and support logs.
    public static String getEngineName() {
        return ShieldEngineRegistry.getEngine().getName();
    }

    /// The configuration passed to [#init(ShieldConfig)], or defaults if it has not been called.
    public static ShieldConfig getConfig() {
        synchronized (AppShield.class) {
            if (config == null) {
                config = new ShieldConfig();
            }
            return config;
        }
    }

    // -----------------------------------------------------------------
    // Tokens
    // -----------------------------------------------------------------

    /// Fetches a time-limited token, reusing the cached one when it is still good.
    public static AsyncResource<ShieldToken> fetchToken() {
        return fetchToken(null);
    }

    /// Fetches a token bound to specific request data.
    ///
    /// Binding ties the token to one request, so a token lifted off a captured request cannot be
    /// replayed against a different one. Worth the extra round trip on the calls that matter --
    /// a transfer, a password change -- and not worth it on the rest, which should use the plain
    /// [#fetchToken()].
    ///
    /// @param bindingData the data to bind to, typically a digest of the request body
    public static AsyncResource<ShieldToken> fetchToken(final String bindingData) {
        final AsyncResource<ShieldToken> result = new AsyncResource<ShieldToken>();
        if (!initialized) {
            result.error(new ShieldException(ShieldStatus.NOT_INITIALIZED,
                    "AppShield.init(...) has not been called"));
            return result;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ShieldToken token = ShieldEngineRegistry.getEngine().fetchToken(bindingData);
                    setStatus(token.getStatus());
                    result.complete(token);
                } catch (ShieldException e) {
                    setStatus(e.getStatus());
                    result.error(e);
                } catch (Throwable t) {
                    setStatus(ShieldStatus.SERVICE_DOWN);
                    result.error(t);
                }
            }
        });
        return result;
    }

    /// Discards any cached token. Call this when your backend rejects a token, so the next request
    /// re-attests rather than replaying the token that was just refused.
    public static void invalidateToken() {
        try {
            ShieldEngineRegistry.getEngine().invalidate();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    // -----------------------------------------------------------------
    // Request binding
    // -----------------------------------------------------------------

    /// Attaches the attestation header to a request.
    ///
    /// **Blocks** while a token is fetched, so it must be called on a network thread. Requests to
    /// hosts registered via [ShieldConfig#protect(String, HostPolicy)] are handled automatically
    /// and do not need this; use it for a request built outside the normal path.
    ///
    /// Honours the host's [FailureMode]: under [FailureMode#OPEN] a token failure leaves the
    /// request untouched, under [FailureMode#CLOSED] it propagates.
    public static void attach(ConnectionRequest request) throws ShieldException {
        if (request == null || !initialized) {
            return;
        }
        String url = request.getUrl();
        String host = hostOf(url);
        HostPolicy policy = policyFor(host);
        // Always clear first. A redirect reuses this request object with its
        // headers intact, so a protected endpoint with an open redirect would
        // otherwise hand a replayable token to whatever host it points at.
        // Re-adding below is conditional on the *current* host's policy.
        request.removeRequestHeader(getConfig().getTokenHeader());
        if (!policy.isAttachToken()) {
            return;
        }
        if (!isSecure(url)) {
            // The token is a bearer credential. Sending it in plaintext -- after a
            // downgrade redirect, or a mistyped scheme -- hands it to anyone on the
            // path, and pinning cannot help because there is no certificate to pin.
            //
            // Routed through the failure mode rather than simply returning: a
            // fail-closed host promises to refuse requests that carry no valid token,
            // and silently sending the body over plaintext instead is the one outcome
            // that policy exists to rule out.
            Log.p("AppShield: refusing to attach a token to a plaintext URL for "
                    + host + ". Use https for protected hosts.");
            failOrContinue(policy, new ShieldException(ShieldStatus.REJECTED,
                    "AppShield: " + host + " is a protected host but the request is "
                    + "plaintext, so no token can be attached safely. Use https."));
            return;
        }
        try {
            ShieldToken token = ShieldEngineRegistry.getEngine().fetchToken(null);
            setStatus(token == null ? ShieldStatus.SERVICE_DOWN : token.getStatus());
            if (token != null && token.isValid()) {
                request.addRequestHeader(getConfig().getTokenHeader(), token.getValue());
                return;
            }
            failOrContinue(policy, new ShieldException(
                    token == null ? ShieldStatus.SERVICE_DOWN : token.getStatus(),
                    "No valid attestation token for " + host));
        } catch (ShieldException e) {
            setStatus(e.getStatus());
            failOrContinue(policy, e);
        } catch (Throwable t) {
            // An engine is pluggable code that can throw anything. Letting an
            // unchecked failure escape would block the request regardless of the
            // host's failure mode, which is the opposite of fail-open.
            Log.e(t);
            setStatus(ShieldStatus.SERVICE_DOWN);
            failOrContinue(policy, new ShieldException(ShieldStatus.SERVICE_DOWN,
                    "Attestation engine failed for " + host));
        }
    }

    /// True for an absolute https URL. Anything else -- http, or a relative URL we cannot
    /// classify -- is not somewhere a bearer token belongs.
    static boolean isSecure(String url) {
        return ShieldHosts.startsWithIgnoreCase(url, "https://");
    }

    private static void failOrContinue(HostPolicy policy, ShieldException e) throws ShieldException {
        // A build with no engine must never block a request: that is the
        // degradation contract this API documents, and a fail-closed host in an
        // open-source or unentitled build would otherwise break outright.
        // Registered, not available. An engine may legitimately report itself
        // unavailable -- an unsupported device, a failed initialization -- and that is
        // exactly when a fail-closed host must refuse, not the moment to stop
        // enforcing. Only a build with no engine at all is exempt, which is the
        // degradation contract the open-source path documents.
        boolean enginePresent = ShieldEngineRegistry.isEngineRegistered();
        if (policy.getFailureMode() == FailureMode.CLOSED && enginePresent) {
            throw e;
        }
        Log.p("AppShield: continuing without a token (" + e.getStatus().getId()
                + "); " + (enginePresent ? "host policy is fail-open."
                        : "no attestation engine is present, so nothing is enforced."));
    }

    /// The headers a protected URL should carry, for network paths that do not go through
    /// `ConnectionRequest` -- notably `BrowserComponent.setURL(url, headers)`.
    ///
    /// Returns an empty table when the host is unprotected or no token is available. Never blocks:
    /// it uses the cached token only, because the callers are typically on the EDT.
    ///
    /// Note this covers only the initial navigation. Requests the loaded page makes itself are not
    /// visible to the framework and cannot be given a token or pinned.
    public static Hashtable headersFor(String url) {
        Hashtable out = new Hashtable();
        if (!initialized || url == null) {
            return out;
        }
        if (!policyFor(hostOf(url)).isAttachToken()) {
            return out;
        }
        if (!isSecure(url)) {
            return out;
        }
        ShieldToken token = getCachedToken();
        // isBoundTo(null) as well as isValid(): a token minted for one specific
        // request must not be handed to an unrelated navigation, which is the
        // whole reason binding exists.
        if (token != null && token.isValid() && token.isBoundTo(null)) {
            out.put(getConfig().getTokenHeader(), token.getValue());
        }
        return out;
    }

    /// The cached token without triggering a fetch. May be null or lapsed. Never blocks.
    public static ShieldToken getCachedToken() {
        try {
            return ShieldEngineRegistry.getEngine().getCachedToken();
        } catch (Throwable t) {
            return null;
        }
    }

    // -----------------------------------------------------------------
    // Host policy
    // -----------------------------------------------------------------

    /// Registers a protected host after [#init(ShieldConfig)], for a backend discovered at
    /// runtime.
    public static void addProtectedHost(String host, HostPolicy policy) {
        if (host != null && host.length() > 0) {
            // Same rule as ShieldConfig.protect: an omitted policy has to pick up
            // the configured default failure mode, or setting a fail-closed
            // default silently does nothing on this path too.
            runtimeHosts.put(ShieldHosts.normalize(host),
                    policy == null ? implicitPolicy() : policy);
        }
    }

    /// Registers a host with the default policy, honouring
    /// [ShieldConfig#defaultFailureMode(FailureMode)].
    public static void addProtectedHost(String host) {
        addProtectedHost(host, null);
    }

    private static HostPolicy implicitPolicy() {
        FailureMode mode = getConfig().getDefaultFailureMode();
        if (mode == FailureMode.OPEN) {
            return HostPolicy.PROTECTED;
        }
        return new HostPolicy(true, true, mode);
    }

    /// The policy in force for a host. Returns [HostPolicy#UNPROTECTED] for anything not
    /// registered, which is the great majority of hosts an app talks to.
    public static HostPolicy policyFor(String host) {
        if (host == null) {
            return HostPolicy.UNPROTECTED;
        }
        Object runtime = runtimeHosts.get(ShieldHosts.normalize(host));
        if (runtime != null) {
            return (HostPolicy) runtime;
        }
        return getConfig().policyFor(host);
    }

    // -----------------------------------------------------------------
    // Pinning
    // -----------------------------------------------------------------

    /// The pin set currently in force. Never null; may be [PinSet#EMPTY].
    public static PinSet getPinSet() {
        try {
            PinSet set = ShieldEngineRegistry.getEngine().getPinSet();
            return set == null ? PinSet.EMPTY : set;
        } catch (Throwable t) {
            return PinSet.EMPTY;
        }
    }

    // -----------------------------------------------------------------
    // RASP
    // -----------------------------------------------------------------

    /// The runtime self-protection observations recorded so far, combining what the engine
    /// detected with anything the app or a library reported to [ShieldSignals].
    ///
    /// Informational. The attestation service applies the policy, and it may reach a different
    /// conclusion than a naive reading of this array -- an emulator signal, for instance, is
    /// normal on a developer's machine.
    public static ShieldSignal[] getSignals() {
        ShieldSignal[] fromEngine;
        try {
            fromEngine = ShieldEngineRegistry.getEngine().collectSignals();
        } catch (Throwable t) {
            fromEngine = new ShieldSignal[0];
        }
        if (fromEngine != null) {
            for (ShieldSignal s : fromEngine) {
                ShieldSignals.add(s);
            }
        }
        return ShieldSignals.snapshot();
    }

    /// The most recent token status. [ShieldStatus#NOT_INITIALIZED] before [#init(ShieldConfig)].
    public static ShieldStatus getStatus() {
        synchronized (AppShield.class) {
            return lastStatus;
        }
    }

    // -----------------------------------------------------------------
    // Observation
    // -----------------------------------------------------------------

    /// Registers a listener for status and signal changes. Callbacks arrive on the EDT.
    public static void addListener(ShieldListener l) {
        if (l == null) {
            return;
        }
        synchronized (listeners) {
            if (!listeners.contains(l)) {
                listeners.addElement(l);
            }
        }
        ShieldSignals.addListener(l);
    }

    public static void removeListener(ShieldListener l) {
        synchronized (listeners) {
            listeners.removeElement(l);
        }
        ShieldSignals.removeListener(l);
    }

    // -----------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------

    private static void setStatus(ShieldStatus status) {
        if (status == null) {
            return;
        }
        ShieldListener[] copy;
        synchronized (AppShield.class) {
            if (status.equals(lastStatus)) {
                return;
            }
            lastStatus = status;
        }
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            copy = new ShieldListener[listeners.size()];
            listeners.copyInto(copy);
        }
        Display.getInstance().callSerially(new StatusDispatch(copy, status));
    }

    private static final class StatusDispatch implements Runnable {
        private final ShieldListener[] targets;
        private final ShieldStatus status;

        StatusDispatch(ShieldListener[] targets, ShieldStatus status) {
            this.targets = targets;
            this.status = status;
        }

        @Override
        public void run() {
            for (ShieldListener target : targets) {
                target.statusChanged(status);
            }
        }
    }

    /// Extracts the host from a URL without pulling in a URL parser. Returns null when the URL is
    /// not absolute, in which case the host is treated as unprotected.
    static String hostOf(String url) {
        if (url == null) {
            return null;
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        int start = scheme + 3;
        int end = url.length();
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        String authority = url.substring(start, end);
        // Strip userinfo and port.
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            authority = authority.substring(at + 1);
        }
        int colon = authority.lastIndexOf(':');
        if (colon >= 0 && authority.indexOf(']') < colon) {
            authority = authority.substring(0, colon);
        }
        return authority.length() == 0 ? null : ShieldHosts.normalize(authority);
    }

    private static com.codename1.security.shield.spi.EngineContext contextForEngine() {
        return ShieldEngineRegistry.getDefaultContext();
    }
}
