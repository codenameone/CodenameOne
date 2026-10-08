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
package com.codename1.backend.security.oauth2.jwt;

import java.util.function.Supplier;

/// A decoder that is made when the first token arrives, and then kept.
///
/// Making a decoder from an issuer's address reads the issuer's metadata over
/// the network. Done at start-up, an issuer that is down stops the server from
/// starting; done here, the server starts and the first tokens fail until the
/// issuer is back.
///
/// ```java
/// JwtDecoder decoder = new SupplierJwtDecoder(() -> JwtDecoders.fromIssuerLocation(issuer));
/// ```
public final class SupplierJwtDecoder implements JwtDecoder {
    private final Supplier<JwtDecoder> supplier;
    private JwtDecoder delegate;

    public SupplierJwtDecoder(Supplier<JwtDecoder> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("supplier cannot be null");
        }
        this.supplier = supplier;
    }

    @Override
    public Jwt decode(String token) {
        JwtDecoder decoder;
        synchronized (this) {
            decoder = delegate;
        }
        if (decoder == null) {
            // Not under the lock: the supplier may be a request to another
            // server. Two first tokens at once each make one, and one is kept.
            try {
                decoder = supplier.get();
            } catch (JwtException err) {
                throw err;
            } catch (RuntimeException err) {
                throw new JwtException("Could not set up the token decoder: " + err.getMessage(),
                        err);
            }
            if (decoder == null) {
                throw new JwtException("Could not set up the token decoder");
            }
            synchronized (this) {
                if (delegate == null) {
                    delegate = decoder;
                }
                decoder = delegate;
            }
        }
        return decoder.decode(token);
    }
}
