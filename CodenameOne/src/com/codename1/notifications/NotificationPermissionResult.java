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
package com.codename1.notifications;

/// The result of a notification permission request delivered to a
/// `NotificationPermissionCallback`. It exposes the granular authorization level the
/// platform returned.
///
/// The levels mirror the iOS `UNAuthorizationStatus` values. On platforms with only a
/// binary granted/denied model (such as Android) the level is either `AuthorizationLevel#AUTHORIZED`
/// or `AuthorizationLevel#DENIED`.
///
/// #### See also
///
/// - NotificationPermissionCallback
///
/// - NotificationPermissionRequest
public class NotificationPermissionResult {

    /// The granular authorization level returned by the platform. The constants are
    /// ordered from least to most permissive, so any level after `#DENIED` means
    /// notifications are permitted.
    public enum AuthorizationLevel {
        /// The user has not yet made a choice regarding notification permission.
        NOT_DETERMINED,

        /// The user explicitly denied notification permission.
        DENIED,

        /// The user granted full notification permission.
        AUTHORIZED,

        /// Notifications were authorized provisionally (delivered quietly without an
        /// explicit prompt). Applies to iOS provisional authorization.
        PROVISIONAL,

        /// Notifications were authorized for a limited amount of time (App Clip style).
        EPHEMERAL
    }

    private final AuthorizationLevel authorizationLevel;

    /// Creates a new result.
    ///
    /// #### Parameters
    ///
    /// - `authorizationLevel`: the granular authorization level
    public NotificationPermissionResult(AuthorizationLevel authorizationLevel) {
        this.authorizationLevel = authorizationLevel;
    }

    /// Returns true if notifications are permitted. This is true for any level more
    /// permissive than `AuthorizationLevel#DENIED` (authorized, provisional or ephemeral).
    ///
    /// #### Returns
    ///
    /// true if notifications can be posted
    public boolean isGranted() {
        return authorizationLevel.compareTo(AuthorizationLevel.DENIED) > 0;
    }

    /// Returns the granular authorization level.
    ///
    /// #### Returns
    ///
    /// the authorization level
    public AuthorizationLevel getAuthorizationLevel() {
        return authorizationLevel;
    }

    /// Returns true if the authorization is provisional (quiet) rather than explicit.
    ///
    /// #### Returns
    ///
    /// true if the authorization level is `AuthorizationLevel#PROVISIONAL`
    public boolean isProvisional() {
        return authorizationLevel == AuthorizationLevel.PROVISIONAL;
    }
}
