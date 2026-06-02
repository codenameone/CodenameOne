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

import com.codename1.ui.Display;

/// Builds and registers a notification channel. Notification channels are an Android
/// concept (introduced in Android O) that let the user control the behavior of groups
/// of notifications: importance, sound, vibration, lights, lockscreen visibility and
/// whether a badge is shown. Once a channel is created its user-controllable settings
/// cannot be changed programmatically, so build it once at app startup.
///
/// On platforms without a channel concept (iOS, desktop) registering a channel is a
/// no-op, but the channel id you assign to a `LocalNotification` is still carried so the
/// notification behaves consistently.
///
/// Usage
/// ```java
/// new NotificationChannelBuilder("messages", "Messages")
///         .description("Incoming chat messages")
///         .importance(NotificationChannelBuilder.IMPORTANCE_HIGH)
///         .sound("/notification_sound_ping.mp3")
///         .enableVibration(true)
///         .register();
/// ```
///
/// #### See also
///
/// - LocalNotification#setChannelId(String)
public class NotificationChannelBuilder {

    /// Channel importance: a no-importance channel does not appear in the shade.
    public static final int IMPORTANCE_NONE = 0;

    /// Channel importance: shows nowhere, is not intrusive.
    public static final int IMPORTANCE_MIN = 1;

    /// Channel importance: shows in the shade and status bar but is not intrusive.
    public static final int IMPORTANCE_LOW = 2;

    /// Channel importance: shows everywhere, makes noise but does not visually intrude.
    public static final int IMPORTANCE_DEFAULT = 3;

    /// Channel importance: makes noise and shows as a heads-up notification.
    public static final int IMPORTANCE_HIGH = 4;

    /// Channel importance: the highest level (rarely needed).
    public static final int IMPORTANCE_MAX = 5;

    /// Lockscreen visibility: do not reveal any part of the notification on a secure
    /// lockscreen.
    public static final int VISIBILITY_SECRET = -1;

    /// Lockscreen visibility: show the notification but hide sensitive content on a
    /// secure lockscreen.
    public static final int VISIBILITY_PRIVATE = 0;

    /// Lockscreen visibility: show the notification in its entirety on the lockscreen.
    public static final int VISIBILITY_PUBLIC = 1;

    private final String id;
    private final String name;
    private String description;
    private int importance = IMPORTANCE_DEFAULT;
    private String sound;
    private boolean vibrationEnabled;
    private long[] vibrationPattern;
    private boolean lightsEnabled;
    private int lightColor;
    private int lockscreenVisibility = VISIBILITY_PRIVATE;
    private String group;
    private boolean showBadge = true;

    /// Creates a channel builder.
    ///
    /// #### Parameters
    ///
    /// - `id`: a stable channel id used when posting notifications
    ///
    /// - `name`: the user-visible channel name shown in the system settings
    public NotificationChannelBuilder(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /// Sets the user-visible channel description.
    ///
    /// #### Parameters
    ///
    /// - `d`: the description
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder description(String d) {
        this.description = d;
        return this;
    }

    /// Sets the channel importance, one of the `IMPORTANCE_` constants.
    ///
    /// #### Parameters
    ///
    /// - `imp`: the importance level
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder importance(int imp) {
        this.importance = imp;
        return this;
    }

    /// Sets the sound played for notifications on this channel. The file name must start
    /// with the "notification_sound" prefix and be bundled with the app.
    ///
    /// #### Parameters
    ///
    /// - `soundFile`: the sound file path
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder sound(String soundFile) {
        this.sound = soundFile;
        return this;
    }

    /// Enables or disables vibration for this channel.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to enable vibration
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder enableVibration(boolean b) {
        this.vibrationEnabled = b;
        return this;
    }

    /// Sets the vibration pattern (alternating off/on durations in milliseconds) and
    /// enables vibration.
    ///
    /// #### Parameters
    ///
    /// - `pattern`: the vibration pattern
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder vibrationPattern(long[] pattern) {
        this.vibrationPattern = pattern;
        this.vibrationEnabled = true;
        return this;
    }

    /// Enables or disables the notification light for this channel.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to enable lights
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder enableLights(boolean b) {
        this.lightsEnabled = b;
        return this;
    }

    /// Sets the notification light color (as an RGB integer) and enables lights.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the light color
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder lightColor(int rgb) {
        this.lightColor = rgb;
        this.lightsEnabled = true;
        return this;
    }

    /// Sets the lockscreen visibility, one of the `VISIBILITY_` constants.
    ///
    /// #### Parameters
    ///
    /// - `v`: the lockscreen visibility
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder lockscreenVisibility(int v) {
        this.lockscreenVisibility = v;
        return this;
    }

    /// Assigns this channel to a channel group. The group must be created with
    /// `#createChannelGroup(String, String)` before or after the channel is registered.
    ///
    /// #### Parameters
    ///
    /// - `groupId`: the channel group id
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder group(String groupId) {
        this.group = groupId;
        return this;
    }

    /// Controls whether notifications on this channel may show a launcher badge.
    ///
    /// #### Parameters
    ///
    /// - `b`: true to allow a badge
    ///
    /// #### Returns
    ///
    /// this builder for chaining
    public NotificationChannelBuilder showBadge(boolean b) {
        this.showBadge = b;
        return this;
    }

    /// Returns the channel id.
    ///
    /// #### Returns
    ///
    /// the channel id
    public String getId() {
        return id;
    }

    /// Returns the user-visible channel name.
    ///
    /// #### Returns
    ///
    /// the channel name
    public String getName() {
        return name;
    }

    /// Returns the channel description.
    ///
    /// #### Returns
    ///
    /// the description, or null
    public String getDescription() {
        return description;
    }

    /// Returns the channel importance.
    ///
    /// #### Returns
    ///
    /// the importance level
    public int getImportance() {
        return importance;
    }

    /// Returns the channel sound file path.
    ///
    /// #### Returns
    ///
    /// the sound file, or null
    public String getSound() {
        return sound;
    }

    /// Returns true if vibration is enabled.
    ///
    /// #### Returns
    ///
    /// true if vibration is enabled
    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    /// Returns the vibration pattern.
    ///
    /// #### Returns
    ///
    /// the vibration pattern, or null
    public long[] getVibrationPattern() {
        return vibrationPattern;
    }

    /// Returns true if lights are enabled.
    ///
    /// #### Returns
    ///
    /// true if lights are enabled
    public boolean isLightsEnabled() {
        return lightsEnabled;
    }

    /// Returns the light color.
    ///
    /// #### Returns
    ///
    /// the light color as an RGB integer
    public int getLightColor() {
        return lightColor;
    }

    /// Returns the lockscreen visibility.
    ///
    /// #### Returns
    ///
    /// the lockscreen visibility
    public int getLockscreenVisibility() {
        return lockscreenVisibility;
    }

    /// Returns the channel group id.
    ///
    /// #### Returns
    ///
    /// the group id, or null
    public String getGroup() {
        return group;
    }

    /// Returns true if a launcher badge is allowed.
    ///
    /// #### Returns
    ///
    /// true if a badge is allowed
    public boolean isShowBadge() {
        return showBadge;
    }

    /// Registers this channel with the platform. On platforms without channels this is a
    /// no-op.
    public void register() {
        Display.getInstance().registerNotificationChannel(this);
    }

    /// Deletes a previously registered channel. On platforms without channels this is a
    /// no-op.
    ///
    /// #### Parameters
    ///
    /// - `id`: the channel id to delete
    public static void deleteChannel(String id) {
        Display.getInstance().deleteNotificationChannel(id);
    }

    /// Creates a channel group, which visually groups channels in the system settings.
    /// On platforms without channels this is a no-op.
    ///
    /// #### Parameters
    ///
    /// - `groupId`: a stable group id
    ///
    /// - `groupName`: the user-visible group name
    public static void createChannelGroup(String groupId, String groupName) {
        Display.getInstance().createNotificationChannelGroup(groupId, groupName);
    }
}
