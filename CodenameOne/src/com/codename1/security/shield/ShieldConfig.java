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
    public ShieldConfig tokenHeader(String name) {
        if (name != null && name.length() > 0) {
            this.tokenHeader = name;
        }
        return this;
    }

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
            hostPolicies.put(ShieldHosts.normalize(hostPattern),
                    policy == null ? implicitPolicy() : policy);
        }
        return this;
    }

    /// Registers a host with the default policy, which honours
    /// [#defaultFailureMode(FailureMode)].
    public ShieldConfig protect(String hostPattern) {
        return protect(hostPattern, implicitPolicy());
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
        String h = ShieldHosts.normalize(host);
        Object exact = hostPolicies.get(h);
        if (exact != null) {
            return (HostPolicy) exact;
        }
        int dot = h.indexOf('.');
        while (dot >= 0 && dot < h.length() - 1) {
            Object wild = hostPolicies.get("*." + h.substring(dot + 1));
            if (wild != null) {
                return (HostPolicy) wild;
            }
            dot = h.indexOf('.', dot + 1);
        }
        return HostPolicy.UNPROTECTED;
    }

    /// True when at least one host is registered, so callers can skip work entirely.
    public boolean hasProtectedHosts() {
        return !hostPolicies.isEmpty();
    }

    /// The registered host patterns.
    public Enumeration protectedHosts() {
        return hostPolicies.keys();
    }
}
