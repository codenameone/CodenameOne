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
package com.codename1.backend.security.oauth2.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/// What an [OAuth2TokenValidator] found: nothing, or the errors.
public final class OAuth2TokenValidatorResult {
    private static final OAuth2TokenValidatorResult NO_ERRORS =
            new OAuth2TokenValidatorResult(new ArrayList<OAuth2Error>());

    private final List<OAuth2Error> errors;

    private OAuth2TokenValidatorResult(List<OAuth2Error> errors) {
        this.errors = Collections.unmodifiableList(errors);
    }

    /// The token passed.
    public static OAuth2TokenValidatorResult success() {
        return NO_ERRORS;
    }

    /// The token failed, for these reasons.
    public static OAuth2TokenValidatorResult failure(OAuth2Error... errors) {
        List<OAuth2Error> list = new ArrayList<OAuth2Error>();
        for (OAuth2Error error : errors) {
            list.add(error);
        }
        return failure(list);
    }

    /// The token failed, for these reasons; passed, when there are none.
    public static OAuth2TokenValidatorResult failure(Collection<OAuth2Error> errors) {
        return errors.isEmpty() ? NO_ERRORS
                : new OAuth2TokenValidatorResult(new ArrayList<OAuth2Error>(errors));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<OAuth2Error> getErrors() {
        return errors;
    }
}
