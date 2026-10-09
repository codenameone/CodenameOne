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

import com.codename1.backend.security.oauth2.core.OAuth2Error;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/// A token whose signature verified and whose claims did not pass: expired,
/// not yet valid, from another issuer, meant for another audience.
public class JwtValidationException extends BadJwtException {
    private final ArrayList<OAuth2Error> errors;

    public JwtValidationException(String message, Collection<OAuth2Error> errors) {
        super(message);
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("errors cannot be empty");
        }
        this.errors = new ArrayList<OAuth2Error>(errors);
    }

    /// Everything the validators found.
    public List<OAuth2Error> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
