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

/// Why something OAuth 2.0 was refused: a code from the specification that
/// defines it, and optionally a sentence for a person and an address to read
/// more at.
public class OAuth2Error implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String description;
    private final String uri;

    public OAuth2Error(String errorCode) {
        this(errorCode, null, null);
    }

    public OAuth2Error(String errorCode, String description, String uri) {
        if (errorCode == null || errorCode.length() == 0) {
            throw new IllegalArgumentException("errorCode cannot be empty");
        }
        this.errorCode = errorCode;
        this.description = description;
        this.uri = uri;
    }

    public final String getErrorCode() {
        return errorCode;
    }

    public final String getDescription() {
        return description;
    }

    public final String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + (description == null ? "" : description);
    }
}
