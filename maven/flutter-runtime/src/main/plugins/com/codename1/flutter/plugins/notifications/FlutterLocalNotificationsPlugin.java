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
 *
 * <p>{@code onDidReceiveBackgroundNotificationResponse} is called too, for a tap that
 * reaches the app while it is minimized or before its first form is shown (Codename One
 * has no background isolate, so it runs on the app's own thread, before the foreground
 * callback). {@code resolvePlatformSpecificImplementation} answers this object for every
 * platform type; {@code createNotificationChannel} records the channel's sound
 * ({@link RawResourceAndroidNotificationSound}) and a notification shown on that channel
 * plays it. Channel importance, lights and vibration are accepted and not applied:
 * LocalNotification has no channel of its own.
 */
public class FlutterLocalNotificationsPlugin {
    private static final String PREFIX = "cn1flutter-";
    private static final Map<String, String> PAYLOADS = new HashMap<String, String>();
    private static Funcs.VoidFunc1<NotificationResponse> onTap;
    private static Funcs.VoidFunc1<NotificationResponse> onBackgroundTap;
    private static final Map<String, String> CHANNEL_SOUNDS = new HashMap<String, String>();
    private static NotificationResponse launchResponse;
    private static int shown;

    public FlutterLocalNotificationsPlugin() {
    }

    public Future<Boolean> initialize(NotificationOptions settings,
            Funcs.VoidFunc1<NotificationResponse> onDidReceiveNotificationResponse,
            Funcs.VoidFunc1<NotificationResponse> onDidReceiveBackgroundNotificationResponse) {
        onTap = onDidReceiveNotificationResponse;
        onBackgroundTap = onDidReceiveBackgroundNotificationResponse;
        return Future.value(Boolean.TRUE);
    }

    /** Called by the app entry point when the user opens one of our notifications. */
    public static void notificationTapped(String notificationId) {
        if (notificationId == null || !notificationId.startsWith(PREFIX)) {
            return;
        }
        Long id = null;
        try {
            id = Long.valueOf(Long.parseLong(notificationId.substring(PREFIX.length())));
        } catch (NumberFormatException ignore) {
            // not one of ours after all
        }
        NotificationResponse response = new NotificationResponse(id, payload(notificationId));
        Funcs.VoidFunc1<NotificationResponse> cb = onTap;
        Funcs.VoidFunc1<NotificationResponse> bg = onBackgroundTap;
        boolean background = !Display.isInitialized() || Display.getInstance().getCurrent() == null
                || Display.getInstance().isMinimized();
        if (background && bg != null) {
            bg.call(response);
        }
        if (cb != null) {
            cb.call(response);
        } else {
            // A cold launch from a tap arrives before the app has run initialize(). As in
            // Flutter, that tap is reported by getNotificationAppLaunchDetails, not the
            // callback, so it is held for the app to ask.
            launchResponse = response;
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
        long interval;
        switch (repeat == null ? RepeatInterval.daily : repeat) {
            case everyMinute:
                r = LocalNotification.REPEAT_MINUTE;
                interval = 60000L;
                break;
            case hourly:
                r = LocalNotification.REPEAT_HOUR;
                interval = 3600000L;
                break;
            case weekly:
                r = LocalNotification.REPEAT_WEEK;
                interval = 7L * 86400000L;
                break;
            default:
                r = LocalNotification.REPEAT_DAY;
                interval = 86400000L;
                break;
        }
        // The plugin's contract: the first one arrives one interval after the call, not now.
        schedule(id, title, body, details, payload, System.currentTimeMillis() + interval, r);
        return Future.value(null);
    }

    /** {@code zonedSchedule}: the time is taken as the instant it names. */
    public Future<Object> zonedSchedule(long id, String title, String body, Object scheduledDate,
            NotificationOptions details, Object androidScheduleMode, String payload,
            Object matchDateTimeComponents, Object uiLocalNotificationDateInterpretation) {
        long at = scheduledDate instanceof DateTime
                ? ((DateTime) scheduledDate).millisecondsSinceEpoch() : System.currentTimeMillis() + 10;
        // matchDateTimeComponents makes the schedule recur: time is every day at that
        // time, dayOfWeekAndTime every week. LocalNotification has no monthly or yearly
        // interval, so dayOfMonthAndTime and dateAndTime fire once, at the first date.
        int repeat = LocalNotification.REPEAT_NONE;
        if (matchDateTimeComponents == DateTimeComponents.time) {
            repeat = LocalNotification.REPEAT_DAY;
        } else if (matchDateTimeComponents == DateTimeComponents.dayOfWeekAndTime) {
            repeat = LocalNotification.REPEAT_WEEK;
        }
        schedule(id, title, body, details, payload, at, repeat);
        return Future.value(null);
    }

    private static void schedule(long id, String title, String body, NotificationOptions details,
            String payload, long at, int repeat) {
        String key = PREFIX + id;
        putPayload(key, payload);
        putMeta(key, title, body, at, repeat);
        shown++;
        if (!Display.isInitialized()) {
            return;
        }
        LocalNotification n = new LocalNotification();
        n.setId(key);
        n.setAlertTitle(title == null ? "" : title);
        n.setAlertBody(body == null ? "" : body);
        String sound = soundOf(details);
        if (sound != null) {
            n.setAlertSound(sound);
        }
        Object badge = details == null ? null : details.find("badgeNumber");
        if (badge instanceof Number) {
            n.setBadgeNumber(((Number) badge).intValue());
        }
        n.setForeground(true);
        Display.getInstance().scheduleLocalNotification(n, at, repeat);
    }

    /// The sound a notification plays: its own, else its Android channel's.
    static String soundOf(NotificationOptions details) {
        if (details == null) {
            return null;
        }
        String own = soundPath(details.find("sound"));
        if (own != null) {
            return own;
        }
        Object android = details.get("android");
        Object channel = android instanceof NotificationOptions
                ? ((NotificationOptions) android).get("arg0") : details.get("arg0");
        return channel instanceof String ? CHANNEL_SOUNDS.get(channel) : null;
    }

    private static String soundPath(Object sound) {
        if (sound instanceof RawResourceAndroidNotificationSound) {
            return ((RawResourceAndroidNotificationSound) sound).alertSoundPath();
        }
        return sound instanceof String ? (String) sound : null;
    }

    /** The sound a notification with these details would play; null when none. */
    public static String alertSoundFor(NotificationOptions details) {
        return soundOf(details);
    }

    public Future<Object> cancel(long id, String tag) {
        String key = PREFIX + id;
        removePayload(key);
        if (Display.isInitialized()) {
            Display.getInstance().cancelLocalNotification(key);
        }
        return Future.value(null);
    }

    public Future<Object> cancelAll() {
        for (String key : payloadKeys()) {
            if (Display.isInitialized()) {
                Display.getInstance().cancelLocalNotification(key);
            }
            removePayload(key);
        }
        return Future.value(null);
    }

    // Payloads are persisted: a scheduled notification usually fires after the process
    // that scheduled it is gone, and the tap that restarts the app still needs the
    // payload to route. The map is this process's cache; Preferences outlives it.
    private static final String PAYLOAD_PREF = "cn1.flutter.notifications.payload.";
    private static final String KEYS_PREF = "cn1.flutter.notifications.keys";

    private static void putPayload(String key, String payload) {
        PAYLOADS.put(key, payload);
        if (Display.isInitialized()) {
            java.util.Set<String> keys = payloadKeys();
            keys.add(key);
            com.codename1.io.Preferences.set(KEYS_PREF, join(keys));
            if (payload == null) {
                com.codename1.io.Preferences.delete(PAYLOAD_PREF + key);
            } else {
                com.codename1.io.Preferences.set(PAYLOAD_PREF + key, payload);
            }
        }
    }

    private static String payload(String key) {
        if (PAYLOADS.containsKey(key)) {
            return PAYLOADS.get(key);
        }
        return Display.isInitialized() ? com.codename1.io.Preferences.get(PAYLOAD_PREF + key, (String) null) : null;
    }

    private static void removePayload(String key) {
        PAYLOADS.remove(key);
        removeMeta(key);
        if (Display.isInitialized()) {
            java.util.Set<String> keys = payloadKeys();
            keys.remove(key);
            com.codename1.io.Preferences.set(KEYS_PREF, join(keys));
            com.codename1.io.Preferences.delete(PAYLOAD_PREF + key);
        }
    }

    private static java.util.Set<String> payloadKeys() {
        java.util.Set<String> keys = new java.util.LinkedHashSet<String>(PAYLOADS.keySet());
        if (Display.isInitialized()) {
            String stored = com.codename1.io.Preferences.get(KEYS_PREF, "");
            for (String k : com.codename1.util.StringUtil.tokenize(stored, ',')) {
                if (k.length() > 0) {
                    keys.add(k);
                }
            }
        }
        return keys;
    }

    private static String join(java.util.Set<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            sb.append(sb.length() == 0 ? "" : ",").append(k);
        }
        return sb.toString();
    }

    /** How many notifications this process has shown or scheduled. */
    public static int shownCount() {
        return shown;
    }

    public Future<NotificationAppLaunchDetails> getNotificationAppLaunchDetails() {
        NotificationResponse r = launchResponse;
        return Future.value(new NotificationAppLaunchDetails(r != null, r));
    }

    /// Every scheduled notification that has not been cancelled and has not yet
    /// fired for the last time: a repeating one stays pending, a one-shot one until
    /// its time passes. Read from what schedule() persisted, so it survives a restart.
    public Future<DartList<PendingNotificationRequest>> pendingNotificationRequests() {
        DartList<PendingNotificationRequest> out = new DartList<PendingNotificationRequest>();
        long now = System.currentTimeMillis();
        for (String key : payloadKeys()) {
            String[] m = meta(key);
            if (m == null) {
                continue;
            }
            long at = parseLong(m[2]);
            int repeat = (int) parseLong(m[3]);
            if (repeat == LocalNotification.REPEAT_NONE && at <= now) {
                continue;
            }
            out.add(new PendingNotificationRequest(parseLong(key.substring(PREFIX.length())),
                    m[0], m[1], payload(key)));
        }
        return Future.value(out);
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Title, body, time and repeat of each scheduled notification, beside its payload.
    private static final Map<String, String[]> META = new HashMap<String, String[]>();
    private static final String META_PREF = "cn1.flutter.notifications.meta.";

    private static void putMeta(String key, String title, String body, long at, int repeat) {
        String[] m = {title, body, String.valueOf(at), String.valueOf(repeat)};
        META.put(key, m);
        if (Display.isInitialized()) {
            com.codename1.io.Preferences.set(META_PREF + key + ".title", title == null ? "" : title);
            com.codename1.io.Preferences.set(META_PREF + key + ".body", body == null ? "" : body);
            com.codename1.io.Preferences.set(META_PREF + key + ".at", at);
            com.codename1.io.Preferences.set(META_PREF + key + ".repeat", repeat);
        }
    }

    private static String[] meta(String key) {
        String[] m = META.get(key);
        if (m != null || !Display.isInitialized()) {
            return m;
        }
        long at = com.codename1.io.Preferences.get(META_PREF + key + ".at", -1L);
        if (at < 0) {
            return null;
        }
        return new String[] {com.codename1.io.Preferences.get(META_PREF + key + ".title", ""),
            com.codename1.io.Preferences.get(META_PREF + key + ".body", ""), String.valueOf(at),
            String.valueOf(com.codename1.io.Preferences.get(META_PREF + key + ".repeat", 0))};
    }

    private static void removeMeta(String key) {
        META.remove(key);
        if (Display.isInitialized()) {
            com.codename1.io.Preferences.delete(META_PREF + key + ".title");
            com.codename1.io.Preferences.delete(META_PREF + key + ".body");
            com.codename1.io.Preferences.delete(META_PREF + key + ".at");
            com.codename1.io.Preferences.delete(META_PREF + key + ".repeat");
        }
    }

    /**
     * The platform-specific plugin object ({@code AndroidFlutterLocalNotificationsPlugin}
     * and so on): this object, which implements all of them.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolvePlatformSpecificImplementation(Class<T> witness) {
        // Every platform type of the stub names this class, so this is the right object
        // whichever one the app asked for; no cast happens here (T is erased).
        return (T) this;
    }

    /**
     * The platform-specific permission requests. Reached only through the stub's
     * {@code Android/IOSFlutterLocalNotificationsPlugin} types, which map to this class.
     * Codename One asks for notification permission when it first shows one, so they
     * answer true.
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
        if (channel instanceof NotificationOptions) {
            NotificationOptions c = (NotificationOptions) channel;
            Object id = c.get("arg0");
            String sound = soundPath(c.get("sound"));
            if (id instanceof String && sound != null) {
                CHANNEL_SOUNDS.put((String) id, sound);
            }
        }
        return Future.value(null);
    }
}
