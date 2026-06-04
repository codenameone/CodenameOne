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
package com.codename1.impl.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.codename1.background.ForegroundService;
import java.util.HashMap;
import java.util.Map;

/// Android foreground service backing `com.codename1.background.ForegroundService`. Shows
/// an ongoing notification and runs the supplied task on a background thread until the task
/// returns or `stop()` is called. Tasks are live Java objects, so they are held in a static
/// registry keyed by an integer token passed through the start intent.
public class CodenameOneForegroundService extends Service {

    static final String ACTION_START = "com.codename1.impl.android.FGS_START";
    static final String ACTION_UPDATE = "com.codename1.impl.android.FGS_UPDATE";
    static final String ACTION_STOP = "com.codename1.impl.android.FGS_STOP";
    static final String EXTRA_TOKEN = "token";
    static final String EXTRA_CHANNEL = "channel";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_BODY = "body";
    static final String EXTRA_ICON = "icon";

    private static int nextToken = 1;
    private static final Map<Integer, Registration> REGISTRATIONS = new HashMap<Integer, Registration>();

    private static class Registration {
        ForegroundService.Task task;
        ForegroundService handle;
        String channelId;
        String title;
        String body;
        String iconName;
        boolean started;
    }

    static synchronized int registerTask(ForegroundService.Task task, ForegroundService handle, String channelId, String title, String body, String iconName) {
        int token = nextToken++;
        Registration r = new Registration();
        r.task = task;
        r.handle = handle;
        r.channelId = channelId;
        r.title = title;
        r.body = body;
        r.iconName = iconName;
        REGISTRATIONS.put(token, r);
        return token;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        final int token = intent.getIntExtra(EXTRA_TOKEN, -1);
        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            REGISTRATIONS.remove(token);
            return START_NOT_STICKY;
        }
        if (ACTION_UPDATE.equals(action)) {
            String title = intent.getStringExtra(EXTRA_TITLE);
            String body = intent.getStringExtra(EXTRA_BODY);
            Registration r = REGISTRATIONS.get(token);
            String channel = r != null ? r.channelId : intent.getStringExtra(EXTRA_CHANNEL);
            String icon = r != null ? r.iconName : intent.getStringExtra(EXTRA_ICON);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(token, buildNotification(channel, title, body, icon));
            return START_NOT_STICKY;
        }

        // ACTION_START
        final Registration reg = REGISTRATIONS.get(token);
        String channel = reg != null ? reg.channelId : intent.getStringExtra(EXTRA_CHANNEL);
        String title = reg != null ? reg.title : intent.getStringExtra(EXTRA_TITLE);
        String body = reg != null ? reg.body : intent.getStringExtra(EXTRA_BODY);
        String icon = reg != null ? reg.iconName : intent.getStringExtra(EXTRA_ICON);
        startForeground(token, buildNotification(channel, title, body, icon));
        if (reg != null && reg.task != null && !reg.started) {
            reg.started = true;
            final ForegroundService.Task task = reg.task;
            final ForegroundService handle = reg.handle;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        task.run(handle);
                    } catch (Throwable t) {
                        com.codename1.io.Log.e(t);
                    } finally {
                        stopForeground(true);
                        stopSelf();
                        REGISTRATIONS.remove(token);
                    }
                }
            }, "cn1-foreground-service").start();
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String channelId, String title, String body, String iconName) {
        int smallIcon = getResources().getIdentifier(iconName != null ? iconName : "ic_stat_notify", "drawable", getApplicationInfo().packageName);
        if (smallIcon == 0) {
            smallIcon = getResources().getIdentifier("icon", "drawable", getApplicationInfo().packageName);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setOngoing(true);
        builder.setSmallIcon(smallIcon);
        if (channelId != null) {
            try {
                builder.getClass().getMethod("setChannelId", String.class).invoke(builder, channelId);
            } catch (Throwable ignore) {
            }
        }
        AndroidImplementation.setNotificationChannel((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE), builder, this);
        return builder.build();
    }
}
