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
import java.util.List;

/// Puts a token to several validators and reports everything any of them
/// found: a token that is both expired and from the wrong issuer says both.
public final class DelegatingOAuth2TokenValidator<T> implements OAuth2TokenValidator<T> {
    private final List<OAuth2TokenValidator<T>> validators;

    public DelegatingOAuth2TokenValidator(Collection<OAuth2TokenValidator<T>> validators) {
        this.validators = new ArrayList<OAuth2TokenValidator<T>>(validators);
        for (OAuth2TokenValidator<T> validator : this.validators) {
            if (validator == null) {
                throw new IllegalArgumentException("validators cannot contain null values");
            }
        }
    }

    @Override
    public OAuth2TokenValidatorResult validate(T token) {
        List<OAuth2Error> errors = new ArrayList<OAuth2Error>();
        for (OAuth2TokenValidator<T> validator : validators) {
            errors.addAll(validator.validate(token).getErrors());
        }
        return OAuth2TokenValidatorResult.failure(errors);
    }
}
