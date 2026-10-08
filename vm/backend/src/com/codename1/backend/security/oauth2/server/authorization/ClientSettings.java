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
package com.codename1.backend.security.oauth2.server.authorization;

/// What is asked of one client beyond what OAuth2 asks of every client.
public final class ClientSettings {
    private final boolean requireProofKey;

    private ClientSettings(boolean requireProofKey) {
        this.requireProofKey = requireProofKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    /// Whether the client must use PKCE with the authorization code. A public
    /// client -- one with no secret -- must whatever this says.
    public boolean isRequireProofKey() {
        return requireProofKey;
    }

    /// Always false: this server has no consent page, and a client is
    /// authorized for the scopes it was registered with.
    public boolean isRequireAuthorizationConsent() {
        return false;
    }

    /// Builds a [ClientSettings].
    public static final class Builder {
        private boolean requireProofKey;

        Builder() {
        }

        public Builder requireProofKey(boolean requireProofKey) {
            this.requireProofKey = requireProofKey;
            return this;
        }

        /// Accepted as false only; see
        /// [ClientSettings#isRequireAuthorizationConsent].
        public Builder requireAuthorizationConsent(boolean requireAuthorizationConsent) {
            if (requireAuthorizationConsent) {
                throw new IllegalArgumentException("This authorization server has no consent "
                        + "page; requireAuthorizationConsent can only be false");
            }
            return this;
        }

        public ClientSettings build() {
            return new ClientSettings(requireProofKey);
        }
    }
}
