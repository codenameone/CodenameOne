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

/// Observes every [ConnectionRequest] the [NetworkManager] runs, so a tracer can time
/// each one and put trace context on it.
///
/// Installed with [NetworkManager#setNetworkTracer(NetworkTracer)]. The one
/// implementation is `com.codename1.telemetry.Telemetry`'s, and it is reached only
/// from the bootstrap the build generates for a project that enables it -- so an app
/// that does not trace carries this interface and nothing else.
///
/// This is a separate slot from [NetworkGuard] on purpose. The guard is a security
/// decision: it seals on first install and can veto a request. A tracer observes and
/// decorates, never refuses, and must not have to compete with the guard for the one
/// slot the guard deliberately makes unreplaceable.
///
/// Every callback is guarded by the caller: an exception thrown from one is logged and
/// the request carries on untraced.
public interface NetworkTracer {
    /// A request is being queued, on the thread that queued it -- the EDT, for most
    /// requests. Whatever this returns is handed back to [#beforeRequest], which is
    /// how a request records the span that was current when the app ASKED for it,
    /// rather than whatever is current on the network thread later.
    ///
    /// #### Returns
    ///
    /// the parent context, or null
    Object requestQueued(ConnectionRequest request);

    /// An attempt is about to connect, on the network thread, before the request's
    /// headers are written. Headers added here with
    /// [ConnectionRequest#addRequestHeader(String, String)] are sent. Called again
    /// for every retry and redirect, each of which is its own attempt.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request
    ///
    /// - `parent`: what [#requestQueued] returned for it -- or, for a retry or
    ///   redirect of a request queued with no parent, the attempt before it (what
    ///   this method returned then), so that the attempts share one trace
    ///
    /// #### Returns
    ///
    /// the attempt's state, handed to [#afterRequest], or null to not trace it
    Object beforeRequest(ConnectionRequest request, Object parent);

    /// An attempt [#beforeRequest] started has ended, on the network thread.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request
    ///
    /// - `attempt`: what [#beforeRequest] returned
    ///
    /// - `responseCode`: the HTTP status, or a value below 100 when none arrived
    ///
    /// - `error`: what failed the attempt, or null
    void afterRequest(ConnectionRequest request, Object attempt, int responseCode, Throwable error);
}
