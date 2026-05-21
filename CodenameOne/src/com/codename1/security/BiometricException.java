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
package com.codename1.security;

/// Thrown via the failure path of an `AsyncResource` returned by [Biometrics]
/// or [SecureStorage] when the underlying biometric or keychain operation
/// fails. [#getError()] returns a typed [BiometricError] code so callers can
/// react without string-matching.
public class BiometricException extends Exception {

    private final BiometricError error;

    public BiometricException(BiometricError error) {
        super(error == null ? "UNKNOWN" : error.name());
        this.error = error == null ? BiometricError.UNKNOWN : error;
    }

    public BiometricException(BiometricError error, String message) {
        super(message);
        this.error = error == null ? BiometricError.UNKNOWN : error;
    }

    public BiometricException(BiometricError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error == null ? BiometricError.UNKNOWN : error;
    }

    /// Typed error code describing the failure. Never `null`.
    public BiometricError getError() {
        return error;
    }
}
