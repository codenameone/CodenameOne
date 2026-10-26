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

import com.codename1.impl.CodenameOneImplementation;

/// The client's default [CryptoEngine]: PBKDF2 goes to the port's native
/// derivation ([CodenameOneImplementation#pbkdf2]) and everything else runs
/// in Java. A port with more native kernels subclasses it.
public class PortCryptoEngine extends CryptoEngine {
    private final CodenameOneImplementation impl;

    /// Creates the engine over a port.
    ///
    /// #### Parameters
    ///
    /// - `impl`: the port whose native derivation PBKDF2 uses
    public PortCryptoEngine(CodenameOneImplementation impl) {
        this.impl = impl;
    }

    @Override
    public boolean randomBytes(byte[] out) {
        SecureRandom.fill(out);
        return true;
    }

    @Override
    public byte[] pbkdf2(String hashAlgorithm, byte[] password, byte[] salt, int iterations, int length) {
        return impl.pbkdf2(hashAlgorithm, password, salt, iterations, length);
    }
}
