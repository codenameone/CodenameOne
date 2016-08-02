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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import static com.codename1.impl.android.AndroidImplementation.activity;

import com.codename1.background.BackgroundFetch;
import com.codename1.notifications.LocalNotification;
import com.codename1.ui.Display;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen
 */
public class LocalNotificationPublisher extends BroadcastReceiver {

    public static String NOTIFICATION = "notification";
    public static String NOTIFICATION_INTENT = "notification-intent";
    public static String BACKGROUND_FETCH_INTENT = "background-fetch-intent";

    public void onReceive(Context context, Intent intent) {
        //Fire the notification to the display
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Bundle extras=intent.getExtras();
        PendingIntent content = extras.getParcelable(NOTIFICATION_INTENT);
        Bundle b = extras.getParcelable(NOTIFICATION);
        LocalNotification notif = AndroidImplementation.createNotificationFromBundle(b);

        if (AndroidImplementation.BACKGROUND_FETCH_NOTIFICATION_ID.equals(notif.getId())) {
            PendingIntent backgroundFetchIntent = extras.getParcelable(BACKGROUND_FETCH_INTENT);
            if (backgroundFetchIntent != null) {
                try {
                    backgroundFetchIntent.send();
                } catch (Exception ex) {
                    Log.e("Codename One", "Failed to send BackgroundFetchHandler intent", ex);
                }
            } else {
                Log.d("Codename One", "BackgroundFetch intent was null");
            }
        } else {
            Notification notification = createAndroidNotification(context, notif, content);
            notification.when = System.currentTimeMillis();
            try{
                int notifId = Integer.parseInt(notif.getId());
                notificationManager.notify("CN1", notifId, notification);                
            }catch(Exception e){
                //that was a mistake, the first param is the tag not the id
                notificationManager.notify(notif.getId(), 0, notification);
            }
        }
    }

    private Notification createAndroidNotification(Context context, LocalNotification localNotif, PendingIntent content) {
        int smallIcon = activity.getResources().getIdentifier("ic_stat_notify", "drawable", activity.getApplicationInfo().packageName);
        int icon = activity.getResources().getIdentifier("icon", "drawable", activity.getApplicationInfo().packageName);

        Notification.Builder builder = new Notification.Builder(activity);
        builder.setContentTitle(localNotif.getAlertTitle());
        builder.setContentText(localNotif.getAlertBody());
        builder.setAutoCancel(true);
        if (localNotif.getBadgeNumber() > 0) {
            builder.setNumber(localNotif.getBadgeNumber());
        }
        String image = localNotif.getAlertImage();
        if (image != null && image.length() > 0) {
            if (image.startsWith("/")) {
                image = image.substring(1);
            }
            InputStream in;
            try {
                in = context.getAssets().open(image);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap im = BitmapFactory.decodeStream(in, null, opts);
                builder.setStyle(new Notification.BigPictureStyle().bigPicture(im));
            } catch (IOException ex) {
                Logger.getLogger(LocalNotificationPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        builder.setSmallIcon(smallIcon);
        builder.setContentIntent(content);
        
        String sound = localNotif.getAlertSound();
        if (sound != null && sound.length() > 0) {
            sound = sound.toLowerCase();
            builder.setSound(android.net.Uri.parse("android.resource://"+activity.getApplicationInfo().packageName+"/raw"+sound.substring(0, sound.indexOf("."))));
        }
        Notification n = builder.build();
        n.icon = icon;
        n.defaults |= Notification.DEFAULT_SOUND;
        return n;
    }

}
