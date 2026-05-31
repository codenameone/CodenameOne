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
/// `NotificationPermissionCallback`. It exposes whether the user granted permission
/// and the granular authorization level that was returned by the platform.
///
/// The authorization levels mirror the iOS `UNAuthorizationStatus` values. On platforms
/// that only have a binary granted/denied model (such as Android) the level will be
/// either `#AUTH_AUTHORIZED` or `#AUTH_DENIED`.
///
/// #### See also
///
/// - NotificationPermissionCallback
///
/// - NotificationPermissionRequest
public class NotificationPermissionResult {

    /// The user has not yet made a choice regarding notification permission.
    public static final int AUTH_NOT_DETERMINED = 0;

    /// The user explicitly denied notification permission.
    public static final int AUTH_DENIED = 1;

    /// The user granted full notification permission.
    public static final int AUTH_AUTHORIZED = 2;

    /// Notifications were authorized provisionally (delivered quietly without an
    /// explicit prompt). Applies to iOS provisional authorization.
    public static final int AUTH_PROVISIONAL = 3;

    /// Notifications were authorized for a limited amount of time (App Clip style).
    public static final int AUTH_EPHEMERAL = 4;

    private final boolean granted;
    private final int authorizationLevel;

    /// Creates a new result.
    ///
    /// #### Parameters
    ///
    /// - `granted`: true if notifications are allowed (full or provisional)
    ///
    /// - `authorizationLevel`: one of the `AUTH_` constants
    public NotificationPermissionResult(boolean granted, int authorizationLevel) {
        this.granted = granted;
        this.authorizationLevel = authorizationLevel;
    }

    /// Returns true if notifications are permitted, including provisional authorization.
    ///
    /// #### Returns
    ///
    /// true if notifications can be posted
    public boolean isGranted() {
        return granted;
    }

    /// Returns the granular authorization level, one of the `AUTH_` constants.
    ///
    /// #### Returns
    ///
    /// the authorization level
    public int getAuthorizationLevel() {
        return authorizationLevel;
    }

    /// Returns true if the authorization is provisional (quiet) rather than explicit.
    ///
    /// #### Returns
    ///
    /// true if the authorization level is `#AUTH_PROVISIONAL`
    public boolean isProvisional() {
        return authorizationLevel == AUTH_PROVISIONAL;
    }
}
