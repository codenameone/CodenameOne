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
package com.codename1.security;

/// Thrown by classes in this package when a cryptographic operation fails. This
/// is a runtime exception so callers are not forced to handle every operation,
/// but it can be caught explicitly when needed (e.g. a malformed key, an
/// authentication-tag mismatch, an algorithm that is not available on the
/// current platform, etc.).
public class CryptoException extends RuntimeException {

    /// Creates a new instance with the given message.
    ///
    /// #### Parameters
    ///
    /// - `message`: human readable description of the failure
    public CryptoException(String message) {
        super(message);
    }

    /// Creates a new instance wrapping an underlying cause.
    ///
    /// #### Parameters
    ///
    /// - `message`: human readable description of the failure
    ///
    /// - `cause`: underlying exception that triggered the failure
    public CryptoException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
