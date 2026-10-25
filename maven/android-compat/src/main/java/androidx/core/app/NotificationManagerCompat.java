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
package androidx.core.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import java.util.List;

public final class NotificationManagerCompat {

    public static final int IMPORTANCE_UNSPECIFIED = NotificationManager.IMPORTANCE_UNSPECIFIED;
    public static final int IMPORTANCE_NONE = NotificationManager.IMPORTANCE_NONE;
    public static final int IMPORTANCE_MIN = NotificationManager.IMPORTANCE_MIN;
    public static final int IMPORTANCE_LOW = NotificationManager.IMPORTANCE_LOW;
    public static final int IMPORTANCE_DEFAULT = NotificationManager.IMPORTANCE_DEFAULT;
    public static final int IMPORTANCE_HIGH = NotificationManager.IMPORTANCE_HIGH;
    public static final int IMPORTANCE_MAX = NotificationManager.IMPORTANCE_MAX;

    private final NotificationManager manager;

    private NotificationManagerCompat(NotificationManager manager) {
        this.manager = manager;
    }

    public static NotificationManagerCompat from(Context context) {
        return new NotificationManagerCompat(NotificationManager.getInstance());
    }

    public boolean areNotificationsEnabled() {
        return manager.areNotificationsEnabled();
    }

    public void notify(int id, Notification notification) {
        manager.notify(id, notification);
    }

    public void notify(String tag, int id, Notification notification) {
        manager.notify(tag, id, notification);
    }

    public void cancel(int id) {
        manager.cancel(id);
    }

    public void cancel(String tag, int id) {
        manager.cancel(tag, id);
    }

    public void cancelAll() {
        manager.cancelAll();
    }

    public void createNotificationChannel(NotificationChannel channel) {
        manager.createNotificationChannel(channel);
    }

    public void createNotificationChannels(List<NotificationChannel> channels) {
        manager.createNotificationChannels(channels);
    }

    public NotificationChannel getNotificationChannel(String channelId) {
        return manager.getNotificationChannel(channelId);
    }

    public void deleteNotificationChannel(String channelId) {
        manager.deleteNotificationChannel(channelId);
    }
}
