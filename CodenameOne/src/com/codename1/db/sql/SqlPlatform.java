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

import com.codename1.security.Hash;
import com.codename1.security.Pbkdf2;

import java.io.IOException;

/// What the wire clients need from the target they run on: a socket, random
/// bytes, and the exceptions a caller catches.
///
/// The backend and an application each install one before the first connection;
/// the wire clients never name either side's classes, which is what lets one copy
/// of them serve both.
@com.codename1.impl.SharedWithBackend
public abstract class SqlPlatform {
    private static SqlPlatform installed;

    /// Makes `platform` the one every connection opened from now on uses.
    public static void install(SqlPlatform platform) {
        installed = platform;
    }

    /// The installed platform.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: when none is installed, which is a defect in
    ///   the target, not something a caller recovers from
    public static SqlPlatform get() {
        SqlPlatform p = installed;
        if (p == null) {
            throw new IllegalStateException("No database platform is installed");
        }
        return p;
    }

    /// Opens a plain TCP connection.
    public abstract SqlSocket connect(String host, int port, int timeoutMillis)
            throws IOException;

    /// `length` bytes from a cryptographically secure source.
    public abstract byte[] randomBytes(int length) throws IOException;

    /// The exception for a transaction that can be run again.
    public IOException concurrencyFailure(String message) {
        return new SqlFailure(SqlFailure.CONCURRENCY, message);
    }

    /// The exception for a write a constraint refused.
    public IOException integrityViolation(String message) {
        return new SqlFailure(SqlFailure.INTEGRITY, message);
    }

    /// `failure` again with `message`, keeping what kind of failure it is.
    public IOException withMessage(IOException failure, String message) {
        if (failure instanceof SqlFailure) {
            return new SqlFailure(((SqlFailure) failure).getKind(), message);
        }
        return new IOException(message);
    }

    /// PBKDF2 over HMAC with `digest`, [Hash#SHA256] or [Hash#SHA512].
    public byte[] pbkdf2(String digest, byte[] password, byte[] salt, int iterations,
                         int length) throws IOException {
        return Hash.SHA512.equals(digest)
                ? Pbkdf2.hmacSha512(password, salt, iterations, length)
                : Pbkdf2.hmacSha256(password, salt, iterations, length);
    }
}
