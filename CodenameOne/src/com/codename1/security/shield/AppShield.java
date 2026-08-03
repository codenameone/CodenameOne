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
import com.codename1.io.NetworkGuard;
import com.codename1.io.NetworkManager;
import com.codename1.security.shield.spi.ShieldEngine;
import com.codename1.security.shield.spi.ShieldEngineRegistry;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import java.util.Enumeration;
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
    private static NetworkGuard guard;
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
            // A concurrent caller WAITS rather than returning early.
            //
            // `initialized` used to be published before the engine was initialized and
            // before the guard was installed, so a second init() returned as though
            // setup were complete -- and, worse, a ConnectionRequest starting in that
            // window found no network guard at all and sent a protected request with
            // neither a token nor a pin check, including for a host configured to fail
            // closed. The flag now means what its name says, and the window is closed by
            // making anyone who arrives during setup wait for it rather than by making
            // them guess.
            while (initializing) {
                try {
                    AppShield.class.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (initialized) {
                return;
            }
            config = cfg == null ? new ShieldConfig() : cfg;
            initializing = true;
        }
        try {
            // Installed FIRST, before the engine is given a chance to run.
            //
            // Publishing `initializing` is not enough on its own: a request that starts
            // while the engine is still initializing only reaches awaitInitialization()
            // if something routes it there, and the only thing that does is the guard.
            // With the install last, a concurrent ConnectionRequest found a null guard at
            // performOperationComplete(), skipped the shield entirely and opened the
            // connection -- so a fail-closed protected host could be called with neither a
            // token nor a pin check for as long as engine.initialize() took, which on a
            // cold start is exactly when it takes longest. The guard reads the
            // configuration live and every path through it waits for initialization, so
            // installing it before the engine makes that window a wait rather than a
            // bypass.
            installNetworkGuard();
            ShieldEngine engine = ShieldEngineRegistry.getEngine();
            try {
                engine.initialize(contextForEngine(), config);
                setStatus(engine.isAvailable() ? ShieldStatus.OK : ShieldStatus.UNPROTECTED);
            } catch (Throwable t) {
                // A failure inside the engine must not stop the app from starting.
                Log.e(t);
                setStatus(ShieldStatus.UNPROTECTED);
            }
        } finally {
            synchronized (AppShield.class) {
                initializing = false;
                initialized = true;
                AppShield.class.notifyAll();
            }
        }
    }

    /// Test hook: puts the shield back to its pre-`init()` state.
    ///
    /// `init()` is deliberately one-shot, so without this the ordering it guarantees can
    /// only be asserted once per JVM -- and the ordering is the thing that has been wrong
    /// twice. Matches the hooks
    /// [com.codename1.security.shield.spi.ShieldEngineRegistry] and
    /// [com.codename1.io.NetworkManager] already carry for the same reason.
    static void resetForTesting() {
        synchronized (AppShield.class) {
            config = null;
            initialized = false;
            initializing = false;
            guard = null;
            lastStatus = ShieldStatus.NOT_INITIALIZED;
            runtimeHosts.clear();
            listeners.removeAllElements();
            synchronized (attachedHeaderNames) {
                attachedHeaderNames.removeAllElements();
            }
            AppShield.class.notifyAll();
        }
    }

    /// True while [#init(ShieldConfig)] is between taking the job and finishing it.
    ///
    /// Separate from `initialized` because the two answer different questions, and
    /// conflating them is what let a caller act on a half-built shield.
    private static boolean initializing;

    /// Blocks until an initialization in progress has finished. Returns at once when
    /// none is, which is every call after startup.
    ///
    /// Safe from a network thread, which is where [#attach(ConnectionRequest)] runs by
    /// contract, and it waits on the same monitor `init()` notifies -- so the wait ends
    /// when setup does, including when the engine threw and the `finally` released it.
    /// False when the wait was cut short by an interrupt, which is NOT the same as the
    /// shield being up.
    ///
    /// Returning quietly made the interrupt look like "initialization finished": the
    /// caller then saw `initialized == false`, took its early return, and the request
    /// went out with no token and no pin check -- on a fail-closed host, which is the one
    /// request that must not. `ConnectionRequest` does not consult the interrupt flag
    /// either, so nothing further down stopped it. The interrupt status is preserved for
    /// whoever set it and the caller is told the shield could not be waited for.
    private static boolean awaitInitialization() {
        synchronized (AppShield.class) {
            while (initializing) {
                try {
                    AppShield.class.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return true;
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
            NetworkManager.setNetworkGuard(getNetworkGuard());
        } catch (IllegalStateException e) {
            // The slot seals after the first install. An app that installed its own guard
            // keeps it; say so rather than failing startup, and point at the composition
            // that gets everything back -- not just the token. Advising attach() alone
            // would restore header decoration while quietly dropping pin enforcement,
            // which is the half of the shield an app cannot notice is missing.
            Log.p("AppShield: a network guard is already installed, so protected hosts are "
                    + "not decorated or pinned automatically. Delegate to "
                    + "AppShield.getNetworkGuard() from your own guard to restore both; "
                    + "see the AppShield.getNetworkGuard() documentation.");
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// The shield's own [NetworkGuard], for an app that has to install a guard of its own.
    ///
    /// [NetworkManager] holds a single guard and seals the slot on first install, so an app
    /// with its own guard leaves no room for the shield's. Delegating to this one is the
    /// supported way to have both. Delegate every method, not only
    /// [NetworkGuard#beforeRequest(ConnectionRequest)]: attaching the token is the visible
    /// half of the shield, and the certificate callbacks are the half that enforces
    /// [HostPolicy#isEnforcePins()]. An app that forwards only `beforeRequest` gets tokens
    /// and no pinning, and nothing about its behaviour says so.
    ///
    /// ```java
    /// final NetworkGuard shield = AppShield.getNetworkGuard();
    /// NetworkManager.setNetworkGuard(new NetworkGuard() {
    ///     public void beforeRequest(ConnectionRequest r) throws IOException {
    ///         myOwnHeaders(r);
    ///         shield.beforeRequest(r);
    ///     }
    ///     public boolean isCertificateCheckRequired(String url) {
    ///         return myOwnCheckNeeded(url) || shield.isCertificateCheckRequired(url);
    ///     }
    ///     public void checkCertificates(ConnectionRequest r,
    ///             ConnectionRequest.SSLCertificate[] c) throws IOException {
    ///         myOwnCheck(r, c);
    ///         shield.checkCertificates(r, c);
    ///     }
    ///     public String[] interestingResponseHeaders() {
    ///         return concat(myOwnHeaderNames(), shield.interestingResponseHeaders());
    ///     }
    ///     public void afterResponse(ConnectionRequest r, int code, String[] headers) {
    ///         shield.afterResponse(r, code, headers);
    ///     }
    /// });
    /// AppShield.init(cfg);
    /// ```
    ///
    /// Note that `interestingResponseHeaders()` has to be the union of both guards' names, and
    /// that the `headers` array handed to `afterResponse` is positional against it -- so a
    /// composing guard must pass the shield the slice that corresponds to the shield's own
    /// names, in that order. Installing the shield's guard directly, by calling
    /// [#init(ShieldConfig)] before installing anything of your own, avoids the bookkeeping
    /// entirely and is what most apps should do.
    ///
    /// Safe to call before [#init(ShieldConfig)]; the returned guard reads the configuration
    /// live rather than capturing it.
    public static NetworkGuard getNetworkGuard() {
        synchronized (AppShield.class) {
            if (guard == null) {
                guard = new ShieldNetworkGuard();
            }
            return guard;
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
        final AsyncResource<ShieldToken> result = new TokenResource();
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                // The wait happens HERE, not before the task is scheduled.
                //
                // Same window attach() covers -- a caller racing startup must not be told
                // the shield was never initialized, which is a lie that lasts
                // milliseconds and an error the app cannot tell from the real one -- but
                // this method is the asynchronous one, and waiting for it in the caller
                // froze whatever thread asked. On the EDT that is a visible stall for the
                // length of a cold start, and if the engine's own initialization needs
                // anything dispatched to the EDT it is a deadlock: the EDT is parked
                // waiting for the initialization that is waiting for the EDT.
                if (!awaitInitialization()) {
                    result.error(new ShieldException(ShieldStatus.NOT_INITIALIZED,
                            "AppShield was still initializing and the wait was "
                            + "interrupted"));
                    return;
                }
                if (!initialized) {
                    result.error(new ShieldException(ShieldStatus.NOT_INITIALIZED,
                            "AppShield.init(...) has not been called"));
                    return;
                }
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
        if (request == null) {
            return;
        }
        // Waits for an initialization already under way rather than treating it as
        // "no shield". installNetworkGuard() necessarily runs before init() publishes
        // completion -- the guard has to exist before anything can claim to be
        // protected -- so a request that started concurrently reaches this method
        // through the freshly installed guard while the flag is still false. Returning
        // there sent a protected request untouched, including for a host configured to
        // fail closed, which is exactly the request that must not go out unprotected.
        boolean waited = awaitInitialization();
        String url = request.getUrl();
        String host = hostOf(url);
        HostPolicy policy = policyFor(host);
        // Always clear first, and clear EVERY name a token has been attached under.
        //
        // A redirect reuses this request object with its headers intact, so a protected
        // endpoint with an open redirect would otherwise hand a replayable token to
        // whatever host it points at. Removing only the currently configured name was
        // not enough for that: ShieldConfig is mutable and getConfig() hands out the live
        // instance, so an app that renames its token header between attempts leaves the
        // bearer token sitting in the request under the OLD name -- and the redirect
        // carries it to the new host. The set is tiny (one entry unless an app renames
        // the header) and only ever grows when a name is actually used.
        clearAttachedHeaders(request);
        if (!waited) {
            // Interrupted mid-wait. Nothing is known about the shield, so this is
            // routed through the host's failure mode exactly like an engine that
            // could not produce a token: fail-closed refuses, fail-open proceeds.
            failOrContinue(policy, new ShieldException(ShieldStatus.NOT_INITIALIZED,
                    "AppShield: interrupted while waiting for initialization, so no "
                    + "token could be attached for " + host));
            return;
        }
        if (!initialized) {
            return;
        }
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
                String header = getConfig().getTokenHeader();
                // Any spelling of it the app may already have set goes first. Header
                // names are case-insensitive, so adding ours beside an existing
                // "x-cn1-attest" leaves two fields on the wire and lets the backend or an
                // intermediary pick the stale one -- while attach() reports success.
                // Done HERE rather than in the general cleanup, which must not touch a
                // request the shield is not attaching to: this is the one request that is
                // about to receive a token under this name.
                request.removeRequestHeader(header);
                // Re-checked here, not only at configuration time: the cookie header name
                // is a runtime setting, so an app can rename it AFTER choosing a token
                // header and land on the same name. The cookie string is written after
                // the request's own headers, so the token would be overwritten and this
                // method would report success anyway.
                if (ShieldHosts.normalize(header).equals(
                        ShieldHosts.normalize(ConnectionRequest.getCookieHeader()))) {
                    Log.p("AppShield: the token header " + header + " is now this app's "
                            + "cookie header, so the token would be overwritten before "
                            + "the request goes out. Change one of the two.");
                    failOrContinue(policy, new ShieldException(ShieldStatus.REJECTED,
                            "AppShield: the token header collides with the cookie "
                            + "header, so no token can be attached for " + host));
                    return;
                }
                rememberAttachedHeader(request, header, token.getValue());
                request.addRequestHeader(header, token.getValue());
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

    /// The header name a token was attached under, and the token, per REQUEST.
    ///
    /// A single "current name" is not enough: [ShieldConfig] is mutable and
    /// [#getConfig()] hands out the live instance, so the name that has to be removed on
    /// a redirect is the one used when the header was set, which may no longer be
    /// configured. A process-wide list of every name ever used is too much: the shield
    /// would then strip that name from EVERY request, so an app whose token header is
    /// also a header some unprotected service legitimately expects -- `X-API-Key` is the
    /// obvious one -- would find the shield quietly deleting it on the way out, on a
    /// request the shield has nothing to do with.
    ///
    /// So the name is remembered against the request it was attached to. Weak keys,
    /// because a request that is dropped rather than redirected must not be held alive
    /// by this, and the dead entries are swept on every attach so the list cannot grow
    /// without bound in a long-lived app.
    private static final Vector attachedHeaderNames = new Vector();

    /// One remembered attachment: which request, the name used, and the token put there.
    ///
    /// The value is part of the identity of what has to be removed, not bookkeeping. The
    /// request is reused across a redirect, and `ConnectionRequest.performOperationComplete()`
    /// calls [ConnectionRequest#onRedirect(String)] in between -- the hook an app uses to
    /// set up the headers the redirect target needs. If the app installs its own credential
    /// under the same name there, the header at cleanup time is no longer the shield's, and
    /// removing it by name deletes the app's.
    private static final class AttachedHeader {

        private final java.lang.ref.WeakReference request;
        private final String name;
        private final String value;

        AttachedHeader(ConnectionRequest request, String name, String value) {
            this.request = new java.lang.ref.WeakReference(request);
            this.name = name;
            this.value = value;
        }

        ConnectionRequest get() {
            return (ConnectionRequest) request.get();
        }
    }

    private static void rememberAttachedHeader(ConnectionRequest request, String name,
            String value) {
        if (request == null || name == null || name.length() == 0) {
            return;
        }
        synchronized (attachedHeaderNames) {
            sweepAttachedHeaders();
            for (int i = attachedHeaderNames.size() - 1; i >= 0; i--) {
                AttachedHeader entry = (AttachedHeader) attachedHeaderNames.elementAt(i);
                if (entry.get() == request && name.equals(entry.name)) { // NOPMD identity
                    // Same request, same name, newer token: the entry has to carry the
                    // value that is actually on the request now, or the next cleanup
                    // compares against a token that was replaced and leaves this one on.
                    attachedHeaderNames.removeElementAt(i);
                }
            }
            attachedHeaderNames.addElement(new AttachedHeader(request, name, value));
        }
    }

    private static void clearAttachedHeaders(ConnectionRequest request) {
        // ONLY what this request was given, and only while it is still what was given.
        //
        // Clearing the configured name unconditionally was the same mistake one step
        // larger: it reached every request, so an app whose token header is also one an
        // unprotected service expects lost that service's header on a call the shield has
        // nothing to do with. Narrowing it to this request left the smaller version of it,
        // because the request is not untouched in between: onRedirect() runs between the
        // attachment and this cleanup, and an app whose redirect target needs its own key
        // under the same name installs it there. A header the shield did not attach is the
        // app's, whatever it is called -- including one that replaced the shield's own.
        //
        // The entry goes either way. Once the value has changed the header belongs to the
        // app, and there is nothing left here for the shield to take back.
        synchronized (attachedHeaderNames) {
            for (int i = attachedHeaderNames.size() - 1; i >= 0; i--) {
                AttachedHeader entry = (AttachedHeader) attachedHeaderNames.elementAt(i);
                ConnectionRequest owner = entry.get();
                if (owner == null) {
                    attachedHeaderNames.removeElementAt(i);
                } else if (owner == request) { // NOPMD identity: this request, not an equal one
                    if (entry.value == null) {
                        // Nothing to compare against, so the safe answer is the one that
                        // cannot leak a token: remove it.
                        request.removeRequestHeader(entry.name);
                    } else {
                        request.removeRequestHeaderIfUnchanged(entry.name, entry.value);
                    }
                    attachedHeaderNames.removeElementAt(i);
                }
            }
        }
    }

    /// Drops entries whose request has been collected. Called under the lock.
    private static void sweepAttachedHeaders() {
        for (int i = attachedHeaderNames.size() - 1; i >= 0; i--) {
            if (((AttachedHeader) attachedHeaderNames.elementAt(i)).get() == null) {
                attachedHeaderNames.removeElementAt(i);
            }
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
        if (url == null) {
            return out;
        }
        // Deliberately does NOT wait for initialization.
        //
        // This method is documented as never blocking and is called from the EDT --
        // BrowserComponent is its reason for existing -- so waiting here froze the UI for
        // the length of a cold start, and an engine whose initialization dispatches
        // anything to the EDT deadlocked: the EDT parked on the initialization that was
        // waiting for the EDT. A synchronous method that returns a map has no way to say
        // "later", so the only honest options are to answer now or to hang, and hanging
        // the UI is not an option.
        //
        // The cost is real and belongs in the log rather than in silence: a
        // BrowserComponent navigating a protected host during startup loads it without a
        // token, and that looks exactly like a page that loaded correctly. Apps that
        // navigate to a protected host at launch should call init() before doing so, or
        // use fetchToken(), which does wait -- on a background thread.
        // Read under the monitor, and only read -- no wait. Without it there is no
        // happens-before with init()'s writes, so a caller on another thread could go on
        // seeing `initialized == false` after startup finished and quietly navigate a
        // protected host without a token, indefinitely. The synchronized block costs an
        // uncontended lock and keeps the never-blocking contract, which is a different
        // promise from the never-synchronizing one nobody made.
        boolean ready;
        boolean starting;
        synchronized (AppShield.class) {
            ready = initialized;
            starting = initializing;
        }
        if (!ready) {
            if (starting) {
                Log.p("AppShield: headersFor(" + hostOf(url) + ") was called while "
                        + "initialization is still running, so no token is attached. Call "
                        + "AppShield.init(...) before navigating to a protected host.");
            }
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

    /// Every host currently protected: the ones [ShieldConfig] carried into
    /// [#init(ShieldConfig)], plus anything registered since through
    /// [#addProtectedHost(String, HostPolicy)].
    ///
    /// The config alone is not the answer to "what is protected", and treating it as such
    /// is a quiet way to lose the runtime registrations. An engine building a pin set from
    /// the config would leave a backend discovered at runtime with no pins, so
    /// [PinSet#isEnforcedFor] returns false for it and the certificate check is skipped
    /// entirely -- the host the app went out of its way to register is the one host not
    /// pinned.
    public static Enumeration protectedHosts() {
        Vector all = new Vector();
        Enumeration configured = getConfig().protectedHosts();
        while (configured.hasMoreElements()) {
            Object host = configured.nextElement();
            if (host != null && !all.contains(host)) {
                all.addElement(host);
            }
        }
        Enumeration runtime = runtimeHosts.keys();
        while (runtime.hasMoreElements()) {
            Object host = runtime.nextElement();
            if (host != null && !all.contains(host)) {
                all.addElement(host);
            }
        }
        return all.elements();
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

    /// Test hook: drives a status transition, so the ordering between the transition and
    /// the notification can be asserted from a test rather than only reasoned about.
    static void setStatusForTesting(ShieldStatus status) {
        setStatus(status);
    }

    private static void setStatus(ShieldStatus status) {
        if (status == null) {
            return;
        }
        // The transition happens under the lock; the dispatch is queued outside it.
        //
        // Both halves matter. Storing the status and enqueueing its notification as two
        // unsynchronized steps let two network threads interleave -- A stores, B stores
        // and enqueues, A enqueues -- and listeners then finished on A while getStatus()
        // already answered B, with nothing later to correct them. But holding the monitor
        // across callSerially is not the fix: callSerially runs the task INLINE when the
        // EDT is not up, so it would run application listeners under this class's monitor,
        // and a listener that touches the shield -- or waits on a thread that does --
        // deadlocks against attach(), which waits on the same monitor for initialization.
        //
        // So staleness is settled at delivery instead, exactly as ShieldSignals does it: a
        // dispatch that no longer describes the current status drops itself. A superseded
        // status was already wrong when it was queued.
        ShieldListener[] copy;
        synchronized (AppShield.class) {
            if (status.equals(lastStatus)) {
                return;
            }
            lastStatus = status;
            synchronized (listeners) {
                if (listeners.isEmpty()) {
                    return;
                }
                copy = new ShieldListener[listeners.size()];
                listeners.copyInto(copy);
            }
        }
        Display.getInstance().callSerially(new StatusDispatch(copy, status));
    }

    /// Whether this is still the status the shield holds.
    ///
    /// Read under the same monitor the transition is written under, so a dispatch either
    /// sees the value it was queued for or a newer one -- never a half-written state.
    static boolean isCurrentStatus(ShieldStatus status) {
        synchronized (AppShield.class) {
            return status.equals(lastStatus);
        }
    }

    /// The token handle handed back to callers, where exactly one of cancellation and
    /// delivery wins.
    ///
    /// [AsyncResource#complete(Object)] does not consult the cancelled flag: it stores the
    /// value, marks the resource done and runs the success callback regardless. So a
    /// caller that cancelled -- the screen was closed, the user backed out -- still had
    /// its `ready` callback invoked when the attestation round trip finished a moment
    /// later, and the error branches did the same through [AsyncResource#error(Throwable)].
    /// That contradicts the contract the rest of the framework is built on and tests, and
    /// this is the one place where a late callback fires against a screen that has gone.
    ///
    /// The claim is taken by whichever arrives first, and the loser does nothing. Both
    /// entry points are overridden rather than only the internal ones, because this object
    /// is handed to application code that can call either.
    private static final class TokenResource extends AsyncResource<ShieldToken> {
        private boolean claimed;

        private boolean claim() {
            synchronized (this) {
                if (claimed) {
                    return false;
                }
                claimed = true;
                return true;
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!claim()) {
                // Already delivered. The base class answers false for a resource that is
                // done, and so does this.
                return false;
            }
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public void complete(ShieldToken value) {
            if (claim()) {
                super.complete(value);
            }
        }

        @Override
        public void error(Throwable t) {
            if (claim()) {
                super.error(t);
            }
        }
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
            // Checked here rather than at enqueue time, because what matters is the state
            // at delivery: a transition superseded between being queued and arriving would
            // otherwise leave listeners holding a status the shield itself no longer has.
            if (!isCurrentStatus(status)) {
                return;
            }
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
