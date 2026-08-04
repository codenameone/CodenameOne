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
import java.util.Enumeration;
import java.util.Hashtable;

/// Configuration for [AppShield#init(ShieldConfig)]. Chainable.
///
/// ```java
/// AppShield.init(new ShieldConfig()
///     .protect("api.mybank.example", HostPolicy.PROTECTED)
///     .protect("*.mybank.example", HostPolicy.PROTECTED));
/// ```
///
/// The defaults are chosen so that adding the shield to an existing app changes nothing until
/// hosts are explicitly protected: no host is touched, failures are open, and signal collection is
/// on because reporting costs nothing and is what makes the service useful.
public final class ShieldConfig {

    /// Header that carries the attestation token.
    ///
    /// Deliberately not `Authorization`: that slot belongs to the app's own user authentication,
    /// and the two answer different questions -- who the user is, versus whether this is a genuine
    /// unmodified app on an uncompromised device. A backend needs both, so they must compose.
    public static final String DEFAULT_TOKEN_HEADER = "X-CN1-Attest";

    private static final String DEFAULT_ENDPOINT = "https://cloud.codenameone.com/api/v2/attest";

    private String endpoint = DEFAULT_ENDPOINT;
    private String tokenHeader = DEFAULT_TOKEN_HEADER;
    private FailureMode defaultFailureMode = FailureMode.OPEN;
    /// Hosts registered without an explicit policy. Their policy is resolved from the default at
    /// read time, so it does not depend on the order the builder was called in.
    private final java.util.Vector implicitHosts = new java.util.Vector();
    private int refreshThresholdPercent = 50;
    private boolean collectSignals = true;
    private final Hashtable hostPolicies = new Hashtable();

    /// Overrides the attestation service endpoint. Only needed for a private deployment or a test
    /// double.
    public ShieldConfig endpoint(String url) {
        if (url != null) {
            this.endpoint = url;
        }
        return this;
    }

    /// Overrides the header used to carry the token. Change this only if it collides with
    /// something already in use on your backend.
    ///
    /// `Content-Type` is refused. [com.codename1.io.ConnectionRequest#addRequestHeader]
    /// special-cases that name into the request's own content type rather than the header
    /// map, so the token would replace the request's media type and -- because the removal
    /// path only clears the map -- would survive a redirect to an unprotected host and be
    /// handed to it. A leak with no symptom on the way there.
    ///
    /// @throws IllegalArgumentException if the name cannot carry a token safely
    public ShieldConfig tokenHeader(String name) {
        if (name != null && name.length() > 0) {
            String normalized = ShieldHosts.normalize(name);
            // Before every reserved-name check, because folding case does not make a
            // malformed name comparable: "Cookie " normalizes to "cookie ", matches
            // nothing on any list below, and walks straight past the checks written for
            // exactly that name. The name then goes on the wire -- HttpURLConnection
            // accepts a trailing space -- where an intermediary is entitled to reject the
            // malformed field or read it as something else, so a fail-closed host ends up
            // with attach() reporting success and a backend that received no token.
            if (!isFieldNameToken(name)) {
                throw new IllegalArgumentException(name + " is not a legal HTTP header "
                        + "name, so it cannot carry the attestation token: field names are "
                        + "tokens, with no spaces and no separators. Whatever survives on "
                        + "the wire is not what was asked for. Use a header of your own, "
                        + "or leave the default " + DEFAULT_TOKEN_HEADER + ".");
            }
            if ("content-type".equals(normalized)) {
                throw new IllegalArgumentException("Content-Type cannot carry the "
                        + "attestation token: it is not stored as an ordinary header, so it "
                        + "cannot be cleared when a request redirects off a protected host, "
                        + "and the token would follow the redirect. Use a header of your "
                        + "own, or leave the default " + DEFAULT_TOKEN_HEADER + ".");
            }
            // The cookie header NAME is configurable at runtime
            // (ConnectionRequest.setCookieHeader), so a hard-coded "cookie" in the list
            // below is only the default. An app that renames it and then picks the same
            // name for the token loses the token to its own cookie string -- written
            // after the request's own headers, exactly as the default name is.
            if (ShieldHosts.normalize(ConnectionRequest.getCookieHeader())
                    .equals(normalized)) {
                throw new IllegalArgumentException(name + " cannot carry the "
                        + "attestation token: it is this app's cookie header, which "
                        + "ConnectionRequest writes from its own cookie store after the "
                        + "request's headers are in place -- so the token would be "
                        + "overwritten after attach() reported success. Use a header of "
                        + "your own, or leave the default " + DEFAULT_TOKEN_HEADER + ".");
            }
            for (String reserved : TRANSPORT_HEADERS) {
                if (reserved.equals(normalized)) {
                    throw new IllegalArgumentException(name + " cannot carry the "
                            + "attestation token: it is connection or framing metadata, "
                            + "which the HTTP transport owns. Depending on the platform it "
                            + "is overwritten, refused, or acted on -- so attach() would "
                            + "report success while the backend received a request with no "
                            + "token, or a malformed one. Use a header of your own, or "
                            + "leave the default " + DEFAULT_TOKEN_HEADER + ".");
                }
            }
            this.tokenHeader = name;
        }
        return this;
    }

    /// Whether this is a legal HTTP field name -- an RFC 9110 token, so no spaces, no
    /// separators, nothing outside printable ASCII.
    ///
    /// Rejecting rather than trimming, for the same reason the websocket handshake does:
    /// a caller that wrote a trailing space meant one header and a lenient server would
    /// read another, and quietly repairing the difference is how the two ends stop
    /// agreeing about what was sent.
    private static boolean isFieldNameToken(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c <= 0x20 || c >= 0x7f) {
                return false;
            }
            if (c == '(' || c == ')' || c == '<' || c == '>' || c == '@' || c == ','
                    || c == ';' || c == ':' || c == '\\' || c == '"' || c == '/'
                    || c == '[' || c == ']' || c == '?' || c == '=' || c == '{'
                    || c == '}') {
                return false;
            }
        }
        return true;
    }

    /// Header names the transport owns, so an attestation token put in one does not
    /// arrive as a header at all.
    ///
    /// Four families, all lower-cased for comparison because header names are
    /// case-insensitive:
    ///
    /// - framing (`content-length`, `transfer-encoding`) -- the transport computes these
    ///   from the body it is about to send, and a value that disagrees is either
    ///   discarded or produces a request the server rejects outright;
    /// - routing (`host`) -- this selects the virtual host, so overwriting it sends the
    ///   request somewhere else entirely;
    /// - hop-by-hop (`connection`, `keep-alive`, `proxy-connection`, `te`, `trailer`,
    ///   `upgrade`, `proxy-authorization`, `proxy-authenticate`) -- defined to be
    ///   consumed by the next hop and not forwarded, so the token would be stripped in
    ///   transit by a proxy that is behaving correctly;
    /// - written by a port on the way out (`accept-encoding`, `x-http-method-override`)
    ///   -- the Android implementation clears `Accept-Encoding` on a HEAD request to work
    ///   around a platform bug, and sets `X-HTTP-Method-Override` itself when its PATCH
    ///   fallback is in use. Both overwrite whatever was there, so a token in one of them
    ///   is replaced between `attach()` reporting success and the request going out, and
    ///   only on the platform and request shape that triggers it.
    ///
    /// Refused rather than warned about, because the failure has no symptom on the
    /// client: `attach()` returns having set the header, and the request reaches the
    /// backend without a usable token. The developer sees a working call and a backend
    /// that says they are unauthenticated.
    private static final String[] TRANSPORT_HEADERS = {
        "host", "content-length", "transfer-encoding", "connection",
        "keep-alive", "proxy-connection", "te", "trailer", "upgrade",
        // The two proxy-credential headers are hop-by-hop like the rest, and worth
        // naming because they do not LOOK like transport plumbing -- they look like a
        // place credentials belong, which is exactly why someone would pick one. A
        // forward proxy consumes them; the origin never sees the token, and the failure
        // appears only once a user is behind such a proxy, on a network nobody testing
        // this was on.
        "proxy-authorization", "proxy-authenticate",
        // Not transport metadata: these are written by a port. Android clears
        // Accept-Encoding on HEAD around a platform bug and sets X-HTTP-Method-Override
        // for its PATCH fallback, both after the request's own headers are in place. The
        // failure is worse than a plain collision because it is conditional -- one HTTP
        // method, one platform -- so it would pass every test that did not happen to use
        // that shape.
        "accept-encoding", "x-http-method-override",
        // And User-Agent, which JavaSEPort rewrites to a fixed BlackBerry string for any
        // URL containing facebook.com -- an old patch for getting a readable login page.
        // Conditional on the HOST, so a token there works everywhere until the one
        // request that matters.
        "user-agent",
        // Cookie is not transport framing, but it fails the same way and worse.
        // ConnectionRequest emits userHeaders first and THEN calls setHeader("Cookie",
        // ...) with the generated cookie string, so with cookie handling on and any
        // stored cookie the token is overwritten by the request itself -- after
        // attach() has reported success. A fail-closed host would then send a protected
        // request with no token and no indication anything went wrong.
        "cookie"
    };

    /// The failure mode applied to hosts registered without an explicit one.
    public ShieldConfig defaultFailureMode(FailureMode mode) {
        if (mode != null) {
            this.defaultFailureMode = mode;
        }
        return this;
    }

    /// How far through a token's lifetime to trigger a background refresh, as a percentage.
    /// Refreshing early is what stops a request ever having to wait on the network.
    public ShieldConfig refreshThresholdPercent(int percent) {
        if (percent > 0 && percent < 100) {
            this.refreshThresholdPercent = percent;
        }
        return this;
    }

    /// Whether to gather runtime self-protection observations. On by default; they ride along with
    /// the token fetch, so there is no extra request and no extra battery cost.
    public ShieldConfig collectSignals(boolean collect) {
        this.collectSignals = collect;
        return this;
    }

    /// Registers a host to protect. Accepts an exact host or a leading `*.` wildcard covering its
    /// subdomains. Hosts not registered here are never touched.
    public ShieldConfig protect(String hostPattern, HostPolicy policy) {
        if (hostPattern != null && hostPattern.length() > 0) {
            String key = ShieldHosts.normalize(hostPattern);
            if (policy == null) {
                // Recorded as implicit rather than resolved now. A builder is chained in
                // whatever order reads well, so `.protect(h).defaultFailureMode(CLOSED)`
                // must mean the same thing as the reverse -- snapshotting the default at
                // registration time left the host fail-open while the finished config
                // reported a closed default, which is the kind of disagreement nobody
                // finds until it matters.
                hostPolicies.remove(key);
                implicitHosts.addElement(key);
            } else {
                implicitHosts.removeElement(key);
                hostPolicies.put(key, policy);
            }
        }
        return this;
    }

    /// Registers a host with the default policy, which honours
    /// [#defaultFailureMode(FailureMode)].
    public ShieldConfig protect(String hostPattern) {
        return protect(hostPattern, null);
    }

    /// The policy used when a host is registered without an explicit one.
    ///
    /// Built from [#defaultFailureMode(FailureMode)] rather than returning the
    /// [HostPolicy#PROTECTED] constant, whose mode is always
    /// [FailureMode#OPEN] -- otherwise setting a fail-closed default would
    /// silently do nothing for every host registered the short way, which is
    /// most of them.
    private HostPolicy implicitPolicy() {
        if (defaultFailureMode == FailureMode.OPEN) {
            return HostPolicy.PROTECTED;
        }
        return new HostPolicy(true, true, defaultFailureMode);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getTokenHeader() {
        return tokenHeader;
    }

    public FailureMode getDefaultFailureMode() {
        return defaultFailureMode;
    }

    public int getRefreshThresholdPercent() {
        return refreshThresholdPercent;
    }

    public boolean isCollectSignals() {
        return collectSignals;
    }

    /// Resolves a host to its policy: exact match first, then the nearest `*.` wildcard, then
    /// [HostPolicy#UNPROTECTED]. Never returns null.
    public HostPolicy policyFor(String host) {
        if (host == null) {
            return HostPolicy.UNPROTECTED;
        }
        for (String key : ShieldHosts.lookupKeys(host)) {
            HostPolicy match = policyForKey(key);
            if (match != null) {
                return match;
            }
        }
        return HostPolicy.UNPROTECTED;
    }

    /// The policy registered for an exact key, or null. Implicit registrations resolve against the
    /// default as it stands now, not as it stood when they were registered.
    ///
    /// Reachable from [AppShield] so runtime registrations can be resolved against the
    /// configured ones key by key, most specific first, rather than one table entirely
    /// before the other.
    HostPolicy policyForKey(String key) {
        Object explicit = hostPolicies.get(key);
        if (explicit != null) {
            return (HostPolicy) explicit;
        }
        return implicitHosts.contains(key) ? implicitPolicy() : null;
    }

    /// True when at least one host is registered, so callers can skip work entirely.
    public boolean hasProtectedHosts() {
        return !hostPolicies.isEmpty() || !implicitHosts.isEmpty();
    }

    /// The registered host patterns, explicit and implicit alike.
    public Enumeration protectedHosts() {
        java.util.Vector all = new java.util.Vector();
        Enumeration keys = hostPolicies.keys();
        while (keys.hasMoreElements()) {
            all.addElement(keys.nextElement());
        }
        for (int i = 0; i < implicitHosts.size(); i++) {
            if (!all.contains(implicitHosts.elementAt(i))) {
                all.addElement(implicitHosts.elementAt(i));
            }
        }
        return all.elements();
    }
}
