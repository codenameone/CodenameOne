/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

///
/// Immutable native registration produced by a push transport.
///
/// <p>The token is opaque provider data. Applications should send the complete
/// subscription to their server and must not parse, truncate, or use the token
/// as a user identity. Providers can rotate tokens, so a later registration for
/// the same installation replaces the earlier value.</p>
public final class PushSubscription {
    private final String transportId;
    private final String token;
    private final String platform;
    private final String installationId;
    private final long expiresAt;
    private final List<String> capabilities;

    ///
    /// Creates a subscription, primarily for custom {@link PushTransport}
    /// implementations.
    ///
    /// @param transportId stable provider identifier
    /// @param token opaque native provider token
    /// @param platform Codename One platform name
    /// @param installationId stable application installation identifier
    /// @param expiresAt expiration time in epoch milliseconds, or {@code 0} when
    ///                  the provider supplies no expiry
    /// @param capabilities immutable capability identifiers, or {@code null}
    /// @throws IllegalArgumentException if {@code transportId} or {@code token}
    ///                                  is null or blank
    public PushSubscription(String transportId, String token, String platform,
            String installationId, long expiresAt, List<String> capabilities) {
        if (transportId == null || transportId.trim().length() == 0
                || token == null || token.trim().length() == 0) {
            throw new IllegalArgumentException("transportId and token must not be blank");
        }
        this.transportId = transportId;
        this.token = token;
        this.platform = platform;
        this.installationId = installationId;
        this.expiresAt = expiresAt;
        this.capabilities = capabilities == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(capabilities));
    }

    ///
    /// Returns the provider identifier used for delivery routing.
    ///
    /// @return the transport identifier
    public String getTransportId() {
        return transportId;
    }

    ///
    /// Returns the opaque native provider token.
    ///
    /// @return the native token
    public String getToken() {
        return token;
    }

    ///
    /// Returns the Codename One platform name reported at registration.
    ///
    /// @return the platform name
    public String getPlatform() {
        return platform;
    }

    ///
    /// Returns the stable ID for this application installation.
    ///
    /// @return the installation identifier
    public String getInstallationId() {
        return installationId;
    }

    ///
    /// Returns the subscription expiry time.
    ///
    /// @return epoch milliseconds, or {@code 0} when no expiry is known
    public long getExpiresAt() {
        return expiresAt;
    }

    ///
    /// Returns transport capability identifiers.
    ///
    /// @return an immutable, possibly empty list
    public List<String> getCapabilities() {
        return capabilities;
    }
}
