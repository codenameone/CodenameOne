/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.notifications;

import java.util.HashMap;
import java.util.Map;

/**
 * Every settings and details class of flutter_local_notifications --
 * {@code InitializationSettings}, {@code AndroidNotificationDetails},
 * {@code DarwinNotificationDetails} and the rest -- maps here. Codename One's local
 * notification has one title, body and sound, so the options are held by name and only
 * {@code sound} and {@code badgeNumber} are read when a notification is shown; the
 * positional values (an Android icon, channel id and name) are kept the same way.
 */
public class NotificationOptions {
    private final Map<String, Object> values = new HashMap<String, Object>();

    public NotificationOptions() {
    }

    public NotificationOptions(Object first) {
        put("arg0", first);
    }

    public NotificationOptions(Object first, Object second) {
        put("arg0", first);
        put("arg1", second);
    }

    public NotificationOptions(Object first, Object second, Object third) {
        put("arg0", first);
        put("arg1", second);
        put("arg2", third);
    }

    private void put(String name, Object v) {
        values.put(name, v);
    }

    Object get(String name) {
        return values.get(name);
    }

    /** A value held directly or by one of the platform options this one wraps. */
    Object find(String name) {
        Object v = values.get(name);
        if (v != null) {
            return v;
        }
        for (Object o : values.values()) {
            if (o instanceof NotificationOptions) {
                Object inner = ((NotificationOptions) o).values.get(name);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    public void android(Object v) {
        put("android", v);
    }

    public void iOS(Object v) {
        put("iOS", v);
    }

    public void macOS(Object v) {
        put("macOS", v);
    }

    public void linux(Object v) {
        put("linux", v);
    }

    public void windows(Object v) {
        put("windows", v);
    }

    public void importance(Object v) {
        put("importance", v);
    }

    public void priority(Object v) {
        put("priority", v);
    }

    public void channelDescription(Object v) {
        put("channelDescription", v);
    }

    public void icon(Object v) {
        put("icon", v);
    }

    public void playSound(Object v) {
        put("playSound", v);
    }

    public void enableVibration(Object v) {
        put("enableVibration", v);
    }

    public void sound(Object v) {
        put("sound", v);
    }

    public void ticker(Object v) {
        put("ticker", v);
    }

    public void styleInformation(Object v) {
        put("styleInformation", v);
    }

    public void color(Object v) {
        put("color", v);
    }

    public void largeIcon(Object v) {
        put("largeIcon", v);
    }

    public void showWhen(Object v) {
        put("showWhen", v);
    }

    public void autoCancel(Object v) {
        put("autoCancel", v);
    }

    public void ongoing(Object v) {
        put("ongoing", v);
    }

    public void category(Object v) {
        put("category", v);
    }

    public void groupKey(Object v) {
        put("groupKey", v);
    }

    public void channelShowBadge(Object v) {
        put("channelShowBadge", v);
    }

    public void visibility(Object v) {
        put("visibility", v);
    }

    public void fullScreenIntent(Object v) {
        put("fullScreenIntent", v);
    }

    public void presentAlert(Object v) {
        put("presentAlert", v);
    }

    public void presentBadge(Object v) {
        put("presentBadge", v);
    }

    public void presentSound(Object v) {
        put("presentSound", v);
    }

    public void presentBanner(Object v) {
        put("presentBanner", v);
    }

    public void presentList(Object v) {
        put("presentList", v);
    }

    public void badgeNumber(Object v) {
        put("badgeNumber", v);
    }

    public void subtitle(Object v) {
        put("subtitle", v);
    }

    public void threadIdentifier(Object v) {
        put("threadIdentifier", v);
    }

    public void attachments(Object v) {
        put("attachments", v);
    }

    public void requestAlertPermission(Object v) {
        put("requestAlertPermission", v);
    }

    public void requestBadgePermission(Object v) {
        put("requestBadgePermission", v);
    }

    public void requestSoundPermission(Object v) {
        put("requestSoundPermission", v);
    }

    public void defaultPresentAlert(Object v) {
        put("defaultPresentAlert", v);
    }

    public void defaultPresentBadge(Object v) {
        put("defaultPresentBadge", v);
    }

    public void defaultPresentSound(Object v) {
        put("defaultPresentSound", v);
    }

    public void defaultPresentBanner(Object v) {
        put("defaultPresentBanner", v);
    }

    public void defaultPresentList(Object v) {
        put("defaultPresentList", v);
    }

    public void notificationCategories(Object v) {
        put("notificationCategories", v);
    }

    public void enableLights(Object v) {
        put("enableLights", v);
    }

    public void ledColor(Object v) {
        put("ledColor", v);
    }

    public void timeoutAfter(Object v) {
        put("timeoutAfter", v);
    }

    public void onlyAlertOnce(Object v) {
        put("onlyAlertOnce", v);
    }

    public void actions(Object v) {
        put("actions", v);
    }

    public void number(Object v) {
        put("number", v);
    }

    public void tag(Object v) {
        put("tag", v);
    }
}
