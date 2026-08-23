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
package com.codename1.nearby.transport;

import com.codename1.nearby.NearbyException;

/// Receives everything the nearby transport has to say. Every method is
/// called on the EDT.
///
/// Extend [TransportAdapter] rather than implementing all of this.
public interface TransportListener {

    /// A peer advertising the same service id came into view. Expect this
    /// repeatedly for the same endpoint across discovery sessions.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the peer that appeared
    void endpointFound(Endpoint endpoint);

    /// A discovered peer went away before any connection was made.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the peer that disappeared
    void endpointLost(Endpoint endpoint);

    /// A peer wants to connect. Call [ConnectionRequest#accept()] or
    /// [ConnectionRequest#reject()]; a request that is never answered times
    /// out on the far side.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request to answer
    void connectionRequested(ConnectionRequest request);

    /// A connection is open in both directions and payloads may be sent.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the connected peer
    void connected(Endpoint endpoint);

    /// A connection attempt failed, or was rejected by the far side.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the peer that did not connect
    /// - `error`: why
    void connectionFailed(Endpoint endpoint, NearbyException error);

    /// An open connection closed, whether deliberately or because the peer
    /// went out of range.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the peer that disconnected
    void disconnected(Endpoint endpoint);

    /// A complete payload arrived.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: who sent it
    /// - `payload`: what they sent
    void payloadReceived(Endpoint endpoint, Payload payload);

    /// Progress on a payload being sent or received.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the other end of the transfer
    /// - `update`: how far it has got
    void payloadProgress(Endpoint endpoint, PayloadTransferUpdate update);
}
