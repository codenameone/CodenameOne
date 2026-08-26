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
package com.codename1.vpn;

/// Typed error codes carried by [VpnException]. Branch on these rather than
/// on the message.
public enum VpnError {
    /// VPN management is not available on this port or this OS version.
    /// Android needs API 30 for the managed profile API; the desktop and
    /// browser ports have no equivalent at all.
    NOT_SUPPORTED,

    /// The user declined the system prompt that has to precede installing a
    /// configuration. Both platforms show one, and neither lets an app
    /// install a VPN without it.
    USER_DECLINED,

    /// The configuration was rejected: a malformed server address, an
    /// authentication method the platform does not offer, or a certificate
    /// it could not read.
    INVALID_CONFIGURATION,

    /// The credentials were refused by the server.
    AUTHENTICATION_FAILED,

    /// The tunnel could not be established -- unreachable server, no
    /// network, or a negotiation failure.
    CONNECTION_FAILED,

    /// The app is missing the entitlement or permission the platform
    /// requires. On iOS this usually means the Personal VPN capability is
    /// not enabled on the App ID.
    UNAUTHORIZED,

    /// No configuration is installed, so there was nothing to act on.
    NOT_CONFIGURED,

    /// The platform never answered within the safety timeout.
    TIMEOUT,

    /// Nothing more specific is known.
    UNKNOWN
}
