/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.rpc;

import java.io.IOException;

/// The one HTTP exchange every chain client needs: send a body, read the reply. The clients in
/// this package are written against this interface alone, so the same [EvmRpc],
/// [EsploraClient] and [SolanaRpc] run in an application and on the backend:
///
/// - in an application, `NetworkTransport` sends through the `NetworkManager`;
/// - on the backend, `com.codename1.backend.WebRpcTransport` sends through its `WebClient`.
///
/// A test passes its own implementation that answers recorded replies.
///
/// Every call blocks until the reply arrives. On the EDT the application transport waits with
/// `invokeAndBlock`, as `NetworkManager.addToQueueAndWait` does, so the UI keeps painting.
@com.codename1.impl.SharedWithBackend
public interface RpcTransport {
    /// Sends one request and answers the response body.
    ///
    /// #### Parameters
    ///
    /// - `method`: `GET` or `POST`
    /// - `url`: the full URL
    /// - `contentType`: the body's media type, or `null` for a request without one
    /// - `body`: the body, or `null`
    ///
    /// #### Throws
    ///
    /// - `RpcException`: for a status outside 2xx, with the status and the body the server sent
    /// - `IOException`: when the request could not be made at all
    String send(String method, String url, String contentType, String body) throws IOException;
}
