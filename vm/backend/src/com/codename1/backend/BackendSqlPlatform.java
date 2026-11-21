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
package com.codename1.backend;

import com.codename1.db.sql.SqlPlatform;
import com.codename1.db.sql.SqlSocket;

import java.io.IOException;

/// The server's [SqlPlatform]: connections over [Tcp], randomness and key
/// derivation from [Crypto], and the server's own exception types, so a
/// repository catches the same [ConcurrencyFailureException] and
/// [DataIntegrityViolationException] whichever engine raised them.
public final class BackendSqlPlatform extends SqlPlatform {
    private static final BackendSqlPlatform INSTANCE = new BackendSqlPlatform();

    private BackendSqlPlatform() {
    }

    /// Installs the server platform. [Database] does this when it is first used;
    /// code that opens a wire client directly calls it first.
    public static void install() {
        SqlPlatform.install(INSTANCE);
    }

    @Override
    public SqlSocket connect(String host, int port, int timeoutMillis) throws IOException {
        return new TcpSocket(Tcp.connect(host, port, timeoutMillis));
    }

    @Override
    public byte[] randomBytes(int length) throws IOException {
        return Crypto.randomBytes(length);
    }

    @Override
    public byte[] pbkdf2(String digest, byte[] password, byte[] salt, int iterations,
                         int length) throws IOException {
        return Crypto.pbkdf2(digest, password, salt, iterations, length);
    }

    @Override
    public IOException concurrencyFailure(String message) {
        return new ConcurrencyFailureException(message);
    }

    @Override
    public IOException integrityViolation(String message) {
        return new DataIntegrityViolationException(message);
    }

    @Override
    public IOException withMessage(IOException failure, String message) {
        if (failure instanceof ConcurrencyFailureException) {
            return new ConcurrencyFailureException(message);
        }
        if (failure instanceof DataIntegrityViolationException) {
            return new DataIntegrityViolationException(message);
        }
        return new IOException(message);
    }

    private static final class TcpSocket extends SqlSocket {
        private final Tcp tcp;

        TcpSocket(Tcp tcp) {
            this.tcp = tcp;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return tcp.read(buffer, offset, length);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            tcp.write(buffer, offset, length);
        }

        @Override
        public void setReadTimeout(int millis) throws IOException {
            tcp.setReadTimeout(millis);
        }

        @Override
        public void startTls(String host, String caFile, boolean verifyHost, String alpn)
                throws IOException {
            tcp.startTls(host, caFile, verifyHost, alpn);
        }

        @Override
        public void startTlsTunnelled(String host, String caFile, boolean verifyHost,
                                      int packetType) throws IOException {
            tcp.startTlsTunnelled(host, caFile, verifyHost, packetType);
        }

        @Override
        public void close() {
            tcp.close();
        }
    }
}
