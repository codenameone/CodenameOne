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

import com.codename1.notifications.LocalNotification;
import com.codename1.ui.Display;

import dart.async.Future;
import dart.core.DartList;
import dart.core.DateTime;
import dart.runtime.Funcs;

/**
 * flutter_local_notifications over Codename One's LocalNotification. A tap reaches the
 * {@code onDidReceiveNotificationResponse} callback through {@link #notificationTapped},
 * which the imported app's entry point calls from its LocalNotificationCallback.
 * Notifications are identified to the platform as {@code "cn1flutter-" + id}.
 */
public class FlutterLocalNotificationsPlugin {
    private static final String PREFIX = "cn1flutter-";
    private static final Map<String, String> PAYLOADS = new HashMap<String, String>();
    private static Funcs.VoidFunc1<NotificationResponse> onTap;
    private static int shown;

    public FlutterLocalNotificationsPlugin() {
    }

    public Future<Boolean> initialize(NotificationOptions settings,
            Funcs.VoidFunc1<NotificationResponse> onDidReceiveNotificationResponse,
            Object onDidReceiveBackgroundNotificationResponse) {
        onTap = onDidReceiveNotificationResponse;
        return Future.value(Boolean.TRUE);
    }

    /** Called by the app entry point when the user opens one of our notifications. */
    public static void notificationTapped(String notificationId) {
        if (notificationId == null || !notificationId.startsWith(PREFIX)) {
            return;
        }
        Funcs.VoidFunc1<NotificationResponse> cb = onTap;
        if (cb != null) {
            Long id = null;
            try {
                id = Long.valueOf(Long.parseLong(notificationId.substring(PREFIX.length())));
            } catch (NumberFormatException ignore) {
                // not one of ours after all
            }
            cb.call(new NotificationResponse(id, PAYLOADS.get(notificationId)));
        }
    }

    public Future<Object> show(long id, String title, String body, NotificationOptions details, String payload) {
        schedule(id, title, body, details, payload, System.currentTimeMillis() + 10,
                LocalNotification.REPEAT_NONE);
        return Future.value(null);
    }

    public Future<Object> periodicallyShow(long id, String title, String body, RepeatInterval repeat,
            NotificationOptions details, Object androidScheduleMode, String payload) {
        int r;
        switch (repeat == null ? RepeatInterval.daily : repeat) {
            case everyMinute:
                r = LocalNotification.REPEAT_MINUTE;
                break;
            case hourly:
                r = LocalNotification.REPEAT_HOUR;
                break;
            case weekly:
                r = LocalNotification.REPEAT_WEEK;
                break;
            default:
                r = LocalNotification.REPEAT_DAY;
                break;
        }
        schedule(id, title, body, details, payload, System.currentTimeMillis() + 10, r);
        return Future.value(null);
    }

    /** {@code zonedSchedule}: the time is taken as the instant it names. */
    public Future<Object> zonedSchedule(long id, String title, String body, Object scheduledDate,
            NotificationOptions details, Object androidScheduleMode, String payload,
            Object matchDateTimeComponents, Object uiLocalNotificationDateInterpretation) {
        long at = scheduledDate instanceof DateTime
                ? ((DateTime) scheduledDate).millisecondsSinceEpoch() : System.currentTimeMillis() + 10;
        schedule(id, title, body, details, payload, at, LocalNotification.REPEAT_NONE);
        return Future.value(null);
    }

    private static void schedule(long id, String title, String body, NotificationOptions details,
            String payload, long at, int repeat) {
        String key = PREFIX + id;
        PAYLOADS.put(key, payload);
        shown++;
        if (!Display.isInitialized()) {
            return;
        }
        LocalNotification n = new LocalNotification();
        n.setId(key);
        n.setAlertTitle(title == null ? "" : title);
        n.setAlertBody(body == null ? "" : body);
        Object sound = details == null ? null : details.find("sound");
        if (sound instanceof String) {
            n.setAlertSound((String) sound);
        }
        Object badge = details == null ? null : details.find("badgeNumber");
        if (badge instanceof Number) {
            n.setBadgeNumber(((Number) badge).intValue());
        }
        n.setForeground(true);
        Display.getInstance().scheduleLocalNotification(n, at, repeat);
    }

    public Future<Object> cancel(long id, String tag) {
        String key = PREFIX + id;
        PAYLOADS.remove(key);
        if (Display.isInitialized()) {
            Display.getInstance().cancelLocalNotification(key);
        }
        return Future.value(null);
    }

    public Future<Object> cancelAll() {
        for (String key : PAYLOADS.keySet().toArray(new String[0])) {
            if (Display.isInitialized()) {
                Display.getInstance().cancelLocalNotification(key);
            }
        }
        PAYLOADS.clear();
        return Future.value(null);
    }

    /** How many notifications this process has shown or scheduled. */
    public static int shownCount() {
        return shown;
    }

    public Future<Object> getNotificationAppLaunchDetails() {
        return Future.value(null);
    }

    public Future<DartList<Object>> pendingNotificationRequests() {
        return Future.value(new DartList<Object>());
    }

    /**
     * The platform-specific plugin object ({@code AndroidFlutterLocalNotificationsPlugin}
     * and so on). There is none here; callers use {@code ?.}, which skips the call.
     */
    public <T> T resolvePlatformSpecificImplementation(Class<T> witness) {
        return null;
    }

    /**
     * The platform-specific permission requests. Reached only through the stub's
     * {@code Android/IOSFlutterLocalNotificationsPlugin} types, which map to this class;
     * {@link #resolvePlatformSpecificImplementation} returns null, so they normally never
     * run. Codename One asks for notification permission when it first shows one.
     */
    public Future<Boolean> requestNotificationsPermission() {
        return Future.value(Boolean.TRUE);
    }

    public Future<Boolean> requestPermissions(Boolean alert, Boolean badge, Boolean sound, Boolean critical,
            Boolean provisional) {
        return Future.value(Boolean.TRUE);
    }

    public Future<Boolean> requestExactAlarmsPermission() {
        return Future.value(Boolean.TRUE);
    }

    public Future<Object> createNotificationChannel(Object channel) {
        return Future.value(null);
    }
}
