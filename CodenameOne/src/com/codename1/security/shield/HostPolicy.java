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

/// What the shield does for a given host: whether to attach an attestation token, whether to
/// enforce certificate pins, and what to do when a token cannot be obtained.
///
/// Hosts are opt-in. A host with no policy registered gets [#UNPROTECTED] and is left completely
/// alone -- no header, no pin check, no possibility of a blocked request. That default is what
/// lets an existing app adopt the shield on its own API without disturbing its analytics, CDN,
/// map-tile or ad traffic.
public final class HostPolicy {

    /// The policy for any host the app did not explicitly register. Does nothing at all.
    public static final HostPolicy UNPROTECTED = new HostPolicy(false, false, FailureMode.OPEN);

    /// Attach a token, enforce pins if the service has published any, and let the request through
    /// when no token is available. The sensible starting point for a protected host.
    public static final HostPolicy PROTECTED = new HostPolicy(true, true, FailureMode.OPEN);

    /// As [#PROTECTED] but refuses to send the request without a valid token. Adopt only after
    /// running with [#PROTECTED] long enough to know the real token-failure rate for your users.
    public static final HostPolicy ENFORCED = new HostPolicy(true, true, FailureMode.CLOSED);

    private final boolean attachToken;
    private final boolean enforcePins;
    private final FailureMode failureMode;

    public HostPolicy(boolean attachToken, boolean enforcePins, FailureMode failureMode) {
        this.attachToken = attachToken;
        this.enforcePins = enforcePins;
        this.failureMode = failureMode == null ? FailureMode.OPEN : failureMode;
    }

    /// True when requests to this host carry the attestation header.
    public boolean isAttachToken() {
        return attachToken;
    }

    /// True when the certificate chain for this host is checked against the published pin set.
    /// Note that enforcement still only happens if a pin set for the host actually exists; see
    /// [PinSet] for the never-brick rules.
    public boolean isEnforcePins() {
        return enforcePins;
    }

    /// What to do when no token could be obtained.
    public FailureMode getFailureMode() {
        return failureMode;
    }

    /// True when this policy does nothing, so callers can skip work entirely.
    public boolean isNoOp() {
        return !attachToken && !enforcePins;
    }

    @Override
    public String toString() {
        return "HostPolicy[token=" + attachToken + ", pins=" + enforcePins
                + ", onFailure=" + failureMode + "]";
    }
}
