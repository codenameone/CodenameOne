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
package android.app;

/// A notification channel. [NotificationManager#createNotificationChannel]
/// registers it as a Codename One `NotificationChannelBuilder`, which is a real
/// channel on Android and a no-op elsewhere.
public final class NotificationChannel {

    public static final String DEFAULT_CHANNEL_ID = "miscellaneous";

    private final String id;
    private CharSequence name;
    private int importance;
    private String description;
    private String group;
    private boolean showBadge = true;
    private boolean lights;
    private int lightColor;
    private boolean vibration;
    private long[] vibrationPattern;
    private int lockscreenVisibility = Notification.VISIBILITY_PRIVATE;

    public NotificationChannel(String id, CharSequence name, int importance) {
        this.id = id;
        this.name = name;
        this.importance = importance;
    }

    public String getId() {
        return id;
    }

    public CharSequence getName() {
        return name;
    }

    public void setName(CharSequence name) {
        this.name = name;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String groupId) {
        group = groupId;
    }

    public boolean canShowBadge() {
        return showBadge;
    }

    public void setShowBadge(boolean showBadge) {
        this.showBadge = showBadge;
    }

    public boolean shouldShowLights() {
        return lights;
    }

    public void enableLights(boolean lights) {
        this.lights = lights;
    }

    public int getLightColor() {
        return lightColor;
    }

    public void setLightColor(int argb) {
        lightColor = argb;
    }

    public boolean shouldVibrate() {
        return vibration;
    }

    public void enableVibration(boolean vibration) {
        this.vibration = vibration;
    }

    public long[] getVibrationPattern() {
        return vibrationPattern;
    }

    public void setVibrationPattern(long[] pattern) {
        vibrationPattern = pattern;
        vibration = pattern != null && pattern.length > 0;
    }

    public int getLockscreenVisibility() {
        return lockscreenVisibility;
    }

    public void setLockscreenVisibility(int visibility) {
        lockscreenVisibility = visibility;
    }

    /// Sounds name an Android `Uri` and `AudioAttributes`; the channel keeps
    /// the platform's default sound.
    public void setSound(android.net.Uri sound, Object audioAttributes) {
    }

    public void setBypassDnd(boolean bypassDnd) {
    }

    com.codename1.notifications.NotificationChannelBuilder toBuilder() {
        com.codename1.notifications.NotificationChannelBuilder b =
                new com.codename1.notifications.NotificationChannelBuilder(id, name == null ? id : name.toString());
        // Android's IMPORTANCE_* and Codename One's share their numbering.
        b.importance(importance);
        if (description != null) {
            b.description(description);
        }
        if (group != null) {
            b.group(group);
        }
        b.showBadge(showBadge);
        b.enableLights(lights);
        if (lightColor != 0) {
            b.lightColor(lightColor);
        }
        b.enableVibration(vibration);
        if (vibrationPattern != null) {
            b.vibrationPattern(vibrationPattern);
        }
        b.lockscreenVisibility(lockscreenVisibility);
        return b;
    }
}
