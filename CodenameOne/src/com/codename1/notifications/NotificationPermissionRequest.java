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

/// Describes which notification capabilities an app wants to request when calling
/// `com.codename1.ui.Display#requestNotificationPermission(NotificationPermissionRequest, NotificationPermissionCallback)`.
///
/// By default `alert`, `sound` and `badge` are requested. Additional options such as
/// `provisional`, `critical`, `timeSensitive`, `carPlay` and `announcement` map to iOS
/// `UNAuthorizationOptions`. On Android only the existence of any requested capability
/// matters; the granular options that have no Android equivalent are ignored.
///
/// Usage
/// ```java
/// NotificationPermissionRequest req = new NotificationPermissionRequest()
///         .provisional(true)
///         .timeSensitive(true);
/// Display.getInstance().requestNotificationPermission(req, result -> {
///     if (result.isGranted()) {
///         // schedule notifications
///     }
/// });
/// ```
///
/// #### See also
///
/// - NotificationPermissionCallback
///
/// - NotificationPermissionResult
public class NotificationPermissionRequest {

    // Bit values match iOS UNAuthorizationOption constants.
    private static final int OPTION_BADGE = 1;
    private static final int OPTION_SOUND = 2;
    private static final int OPTION_ALERT = 4;
    private static final int OPTION_CAR_PLAY = 8;
    private static final int OPTION_CRITICAL_ALERT = 16;
    private static final int OPTION_PROVIDES_SETTINGS = 32;
    private static final int OPTION_PROVISIONAL = 64;
    private static final int OPTION_ANNOUNCEMENT = 128;

    private boolean alert = true;
    private boolean sound = true;
    private boolean badge = true;
    private boolean provisional;
    private boolean critical;
    private boolean timeSensitive;
    private boolean carPlay;
    private boolean announcement;
    private boolean providesAppSettings;

    /// Requests permission to display alerts. Enabled by default.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request alert permission
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest alert(boolean b) {
        this.alert = b;
        return this;
    }

    /// Requests permission to play notification sounds. Enabled by default.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request sound permission
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest sound(boolean b) {
        this.sound = b;
        return this;
    }

    /// Requests permission to update the app icon badge. Enabled by default.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request badge permission
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest badge(boolean b) {
        this.badge = b;
        return this;
    }

    /// Requests provisional authorization (iOS). Provisional notifications are delivered
    /// quietly to the notification center without an explicit prompt. Ignored on platforms
    /// that do not support it.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request provisional authorization
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest provisional(boolean b) {
        this.provisional = b;
        return this;
    }

    /// Requests permission to send critical alerts (iOS). Critical alerts bypass Do Not
    /// Disturb and the mute switch and require a special Apple entitlement. Without the
    /// entitlement the request is silently downgraded. Ignored on platforms that do not
    /// support it.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request critical alert permission
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest critical(boolean b) {
        this.critical = b;
        return this;
    }

    /// Requests the ability to mark notifications as time sensitive (iOS). Ignored on
    /// platforms that do not support it.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request the time sensitive interruption level
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest timeSensitive(boolean b) {
        this.timeSensitive = b;
        return this;
    }

    /// Requests permission to display notifications in CarPlay (iOS). Ignored elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request CarPlay permission
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest carPlay(boolean b) {
        this.carPlay = b;
        return this;
    }

    /// Requests permission for Siri to read out notifications (iOS). Ignored elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to request announcement permission
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest announcement(boolean b) {
        this.announcement = b;
        return this;
    }

    /// Requests that the app provide its own in-app notification settings (iOS). Ignored
    /// elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to advertise in-app notification settings
    ///
    /// #### Returns
    ///
    /// this request for chaining
    public NotificationPermissionRequest providesAppSettings(boolean b) {
        this.providesAppSettings = b;
        return this;
    }

    /// Returns true if alert permission is requested.
    ///
    /// #### Returns
    ///
    /// true if alerts are requested
    public boolean isAlert() {
        return alert;
    }

    /// Returns true if sound permission is requested.
    ///
    /// #### Returns
    ///
    /// true if sound is requested
    public boolean isSound() {
        return sound;
    }

    /// Returns true if badge permission is requested.
    ///
    /// #### Returns
    ///
    /// true if badge is requested
    public boolean isBadge() {
        return badge;
    }

    /// Returns true if provisional authorization is requested.
    ///
    /// #### Returns
    ///
    /// true if provisional authorization is requested
    public boolean isProvisional() {
        return provisional;
    }

    /// Returns true if critical alert permission is requested.
    ///
    /// #### Returns
    ///
    /// true if critical alerts are requested
    public boolean isCritical() {
        return critical;
    }

    /// Returns true if the time sensitive interruption level is requested.
    ///
    /// #### Returns
    ///
    /// true if time sensitive notifications are requested
    public boolean isTimeSensitive() {
        return timeSensitive;
    }

    /// Returns true if CarPlay permission is requested.
    ///
    /// #### Returns
    ///
    /// true if CarPlay is requested
    public boolean isCarPlay() {
        return carPlay;
    }

    /// Returns true if announcement permission is requested.
    ///
    /// #### Returns
    ///
    /// true if announcement is requested
    public boolean isAnnouncement() {
        return announcement;
    }

    /// Returns true if the app advertises in-app notification settings.
    ///
    /// #### Returns
    ///
    /// true if the app provides its own settings
    public boolean isProvidesAppSettings() {
        return providesAppSettings;
    }

    /// Converts this request into the bitmask consumed by the iOS native layer. The bit
    /// values match the iOS `UNAuthorizationOption` constants.
    ///
    /// #### Returns
    ///
    /// the authorization options bitmask
    public int toAuthorizationOptionsMask() {
        int mask = 0;
        if (alert) {
            mask |= OPTION_ALERT;
        }
        if (sound) {
            mask |= OPTION_SOUND;
        }
        if (badge) {
            mask |= OPTION_BADGE;
        }
        if (provisional) {
            mask |= OPTION_PROVISIONAL;
        }
        if (critical) {
            mask |= OPTION_CRITICAL_ALERT;
        }
        if (carPlay) {
            mask |= OPTION_CAR_PLAY;
        }
        if (announcement) {
            mask |= OPTION_ANNOUNCEMENT;
        }
        if (providesAppSettings) {
            mask |= OPTION_PROVIDES_SETTINGS;
        }
        return mask;
    }
}
