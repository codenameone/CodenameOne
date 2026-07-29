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
package com.codename1.io;

import java.io.IOException;

/// Interception point for cross-cutting network policy: decorating outgoing requests, and vetting
/// the TLS certificate chain before a request body is written.
///
/// At most one guard is installed per app, via
/// [NetworkManager#setNetworkGuard(NetworkGuard)], and the slot seals after the first call.
/// [com.codename1.security.shield.AppShield] is the intended consumer, but the interface is
/// deliberately generic and carries no dependency on it.
///
/// #### This does not replace the per-request certificate hook
///
/// [ConnectionRequest#checkSSLCertificates(ConnectionRequest.SSLCertificate[])] remains the way an
/// individual request pins for itself, and it still runs first and unchanged. The guard runs
/// afterwards, so an app that already pins manually keeps working and simply gains a second,
/// app-wide layer.
public interface NetworkGuard {

    /// Called on the network thread immediately before the connection is opened, and again on each
    /// retry or redirect so a stale token is never reused.
    ///
    /// May block -- it is off the EDT and outside the network queue's lock. Add headers here.
    /// Throwing fails the request through the normal error path.
    void beforeRequest(ConnectionRequest request) throws IOException;

    /// Whether this URL's certificate chain needs to be inspected, i.e. whether the host has pins.
    ///
    /// Must be fast and purely local. On iOS this is consulted from the TLS delegate thread while
    /// the handshake is held open.
    ///
    /// Returning true has a cost beyond the check itself: the framework asks the platform for the
    /// richer certificate details, including public-key digests, which it does not otherwise
    /// collect. Return false for hosts with no pins.
    boolean isCertificateCheckRequired(String url);

    /// Vets the certificate chain, after the handshake and before any request body is written.
    ///
    /// Throwing an `IOException` aborts the request and surfaces through the request's normal
    /// error handling, rather than completing it with an empty response.
    ///
    /// Must be local and non-blocking, for the same delegate-thread reason as
    /// [#isCertificateCheckRequired(String)]. In particular it must not try to fetch a fresh pin
    /// set: use the last known one and let a mismatch stand.
    void checkCertificates(ConnectionRequest request,
            ConnectionRequest.SSLCertificate[] certificates) throws IOException;

    /// Called once the response code is known, including for failed requests.
    ///
    /// This is how a token layer learns its token was refused -- a 401 or 403 from a protected host
    /// usually means the cached token should be discarded before the next attempt. Must not throw;
    /// exceptions here are logged and swallowed so a telemetry problem cannot fail a request that
    /// otherwise succeeded.
    void afterResponse(ConnectionRequest request, int responseCode);
}
