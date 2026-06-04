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
package com.codename1.ads;

/// Describes a failure delivered to [AdListener#onFailedToLoad(AdError)] or
/// [AdListener#onShowFailed(AdError)]. The numeric [#getCode()] and [#getDomain()]
/// are provider specific (e.g. the underlying SDK error code), while [#getMessage()]
/// is a human readable description.
///
/// @author Shai Almog
public class AdError {
    /// Error code used when no ad was available to fill the request.
    public static final int CODE_NO_FILL = 3;

    /// Error code used when the network was unavailable.
    public static final int CODE_NETWORK_ERROR = 2;

    /// Error code used when the request was rejected as invalid (e.g. a bad ad unit id).
    public static final int CODE_INVALID_REQUEST = 1;

    /// Error code used when no ad provider is installed or the format is unsupported.
    public static final int CODE_UNSUPPORTED = -1;

    /// Generic internal error code.
    public static final int CODE_INTERNAL = 0;

    private final int code;
    private final String domain;
    private final String message;

    /// Creates a new error.
    ///
    /// #### Parameters
    ///
    /// - `code`: the provider specific error code
    /// - `domain`: the provider specific error domain or null
    /// - `message`: a human readable description
    public AdError(int code, String domain, String message) {
        this.code = code;
        this.domain = domain;
        this.message = message;
    }

    /// The provider specific numeric error code.
    public int getCode() {
        return code;
    }

    /// The provider specific error domain, may be null.
    public String getDomain() {
        return domain;
    }

    /// A human readable description of the failure.
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "AdError{code=" + code + ", domain=" + domain + ", message=" + message + '}';
    }
}
