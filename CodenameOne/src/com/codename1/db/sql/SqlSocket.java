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
package com.codename1.db.sql;

import java.io.IOException;

/// A connection to a database server, as the wire clients in this package see it.
///
/// Each target opens one its own way -- the backend over its own sockets, an
/// application over the port's -- and hands it over through
/// [SqlPlatform#connect(String, int, int)]. A target that cannot run TLS refuses
/// the TLS calls: a client asked for TLS fails to connect, it never continues in
/// the clear.
@com.codename1.impl.SharedWithBackend
public abstract class SqlSocket {
    /// Reads at most `length` bytes, blocking until at least one arrives.
    ///
    /// #### Returns
    ///
    /// the number of bytes read, or -1 at the end of the stream
    public abstract int read(byte[] buffer, int offset, int length) throws IOException;

    /// Writes all `length` bytes.
    public abstract void write(byte[] buffer, int offset, int length) throws IOException;

    /// How long a read may block before it fails, in milliseconds; 0 waits forever.
    public abstract void setReadTimeout(int millis) throws IOException;

    /// Upgrades the connection to TLS in place, verifying the server's certificate
    /// against `caFile` (or the platform's trust roots when it is null) and, when
    /// `verifyHost` is set, its name against `host`. `alpn` is the protocol to
    /// offer, or null for none.
    public abstract void startTls(String host, String caFile, boolean verifyHost, String alpn)
            throws IOException;

    /// Upgrades to TLS with the handshake carried inside the protocol's own
    /// packets of type `packetType` (SQL Server's PRELOGIN); after the handshake
    /// the connection is ordinary TLS.
    public abstract void startTlsTunnelled(String host, String caFile, boolean verifyHost,
                                           int packetType) throws IOException;

    /// Upgrades to TLS with no protocol offered; see
    /// [#startTls(String, String, boolean, String)].
    public void startTls(String host, String caFile, boolean verifyHost) throws IOException {
        startTls(host, caFile, verifyHost, null);
    }

    /// Closes the connection; closing twice does nothing.
    public abstract void close();
}
