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

import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Posts notifications as Codename One local notifications, delivered now.
///
/// A notification whose id belongs to a running foreground service updates
/// that service's notification instead, which is how Android code reports
/// progress from a service (`notify(FOREGROUND_ID, builder.build())`).
public class NotificationManager {

    public static final int IMPORTANCE_UNSPECIFIED = -1000;
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_MAX = 5;

    public static final String ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED =
            "android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED";

    private static NotificationManager instance;

    private final Map<String, NotificationChannel> channels = new LinkedHashMap<String, NotificationChannel>();

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    NotificationManager() {
    }

    public void createNotificationChannel(NotificationChannel channel) {
        channels.put(channel.getId(), channel);
        if (Display.isInitialized()) {
            Display.getInstance().registerNotificationChannel(channel.toBuilder());
        }
    }

    public void createNotificationChannels(List<NotificationChannel> list) {
        for (int i = 0; i < list.size(); i++) {
            createNotificationChannel(list.get(i));
        }
    }

    public NotificationChannel getNotificationChannel(String channelId) {
        return channels.get(channelId);
    }

    public List<NotificationChannel> getNotificationChannels() {
        return new ArrayList<NotificationChannel>(channels.values());
    }

    public void deleteNotificationChannel(String channelId) {
        channels.remove(channelId);
    }

    public boolean areNotificationsEnabled() {
        return true;
    }

    public int getImportance() {
        return IMPORTANCE_DEFAULT;
    }

    public void notify(int id, Notification notification) {
        notify(null, id, notification);
    }

    public void notify(String tag, int id, Notification notification) {
        if (tag == null && ActiveServices.updateForegroundNotification(id, notification)) {
            return;
        }
        if (Display.isInitialized()) {
            Display.getInstance().scheduleLocalNotification(notification.toLocal(key(tag, id)),
                    System.currentTimeMillis() + 1,
                    com.codename1.notifications.LocalNotification.REPEAT_NONE);
        }
    }

    public void cancel(int id) {
        cancel(null, id);
    }

    public void cancel(String tag, int id) {
        if (Display.isInitialized()) {
            Display.getInstance().cancelLocalNotification(key(tag, id));
        }
    }

    /// Codename One cancels local notifications one id at a time and does not
    /// list the ones already shown, so this cancels nothing it did not post
    /// in this run.
    public void cancelAll() {
        for (int i = 0; i < posted.size(); i++) {
            if (Display.isInitialized()) {
                Display.getInstance().cancelLocalNotification(posted.get(i));
            }
        }
        posted.clear();
    }

    private final List<String> posted = new ArrayList<String>();

    private String key(String tag, int id) {
        String k = tag == null ? "cn1compat:" + id : "cn1compat:" + tag + ":" + id;
        if (!posted.contains(k)) {
            posted.add(k);
        }
        return k;
    }
}
