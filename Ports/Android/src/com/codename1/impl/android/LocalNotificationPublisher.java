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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.codename1.background.BackgroundFetch;
import com.codename1.impl.android.compat.app.NotificationCompatWrapper;
import com.codename1.impl.android.compat.app.RemoteInputWrapper;
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
    public static String NOTIFICATION_CONTENT_TEMPLATE = "notification-content-template";

    public void onReceive(Context context, Intent intent) {
        //Fire the notification to the display
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Bundle extras=intent.getExtras();
        if(extras == null) {
            return;
        }
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
            Intent contentTemplate = extras.getParcelable(NOTIFICATION_CONTENT_TEMPLATE);
            Notification notification = createAndroidNotification(context, notif, content, contentTemplate);
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

    private Notification createAndroidNotification(Context context, LocalNotification localNotif, PendingIntent content, Intent contentTemplate) {
        Context ctx = context;
        int smallIcon = ctx.getResources().getIdentifier("ic_stat_notify", "drawable", ctx.getApplicationInfo().packageName);
        int icon = ctx.getResources().getIdentifier("icon", "drawable", ctx.getApplicationInfo().packageName);
        
        if (smallIcon == 0) {
            smallIcon = icon;
        } else {
            icon = smallIcon;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        builder.setContentTitle(localNotif.getAlertTitle());
        builder.setContentText(localNotif.getAlertBody());
        builder.setAutoCancel(true);
        if (localNotif.getBadgeNumber() >= 0) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                builder.setNumber(localNotif.getBadgeNumber());
            }
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
                builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(im));
            } catch (IOException ex) {
                Logger.getLogger(LocalNotificationPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        builder.setSmallIcon(smallIcon);
        builder.setContentIntent(content);

        // grouping
        if (localNotif.getGroupId() != null) {
            builder.setGroup(localNotif.getGroupId());
            builder.setGroupSummary(localNotif.isGroupSummary());
        }
        // ongoing (cannot be dismissed)
        if (localNotif.isOngoing()) {
            builder.setOngoing(true);
        }
        // progress bar
        if (localNotif.getProgressMax() > 0 || localNotif.isProgressIndeterminate()) {
            builder.setProgress(Math.max(1, localNotif.getProgressMax()), localNotif.getProgress(), localNotif.isProgressIndeterminate());
        }
        // full screen intent (high priority interruptions)
        if (localNotif.isFullScreenIntent() && content != null) {
            builder.setFullScreenIntent(content, true);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        // time sensitive: Android has no exact equivalent, approximate with category + priority
        if (localNotif.isTimeSensitive()) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        // messaging (conversation) style
        LocalNotification.MessagingStyle ms = localNotif.getMessagingStyle();
        if (ms != null && !ms.getMessages().isEmpty()) {
            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(
                    ms.getSelfDisplayName() == null ? "" : ms.getSelfDisplayName());
            if (ms.getConversationTitle() != null) {
                style.setConversationTitle(ms.getConversationTitle());
            }
            for (LocalNotification.MessagingStyle.Message m : ms.getMessages()) {
                CharSequence sender = m.getSenderName();
                style.addMessage(m.getText(), m.getTimestamp(), sender);
            }
            builder.setStyle(style);
        }
        // explicit channel id on the notification (Android O+), via reflection to stay
        // compatible with the support library baseline
        if (localNotif.getChannelId() != null) {
            try {
                builder.getClass().getMethod("setChannelId", String.class).invoke(builder, localNotif.getChannelId());
            } catch (Throwable ignore) {
            }
        }

        // action buttons (with optional inline quick-reply text input)
        if (!localNotif.getActions().isEmpty() && contentTemplate != null) {
            int requestCode = 1;
            for (LocalNotification.Action a : localNotif.getActions()) {
                Intent actionIntent = (Intent) contentTemplate.clone();
                actionIntent.putExtra("LocalNotificationActionId", a.getId());
                actionIntent.putExtra("LocalNotificationActionTitle", a.getTitle() == null ? "" : a.getTitle());
                // make the per-action intent unique so the PendingIntents don't collide
                actionIntent.setData(android.net.Uri.parse("http://codenameone.com/a?LocalNotificationID="
                        + android.net.Uri.encode(localNotif.getId()) + "&action=" + android.net.Uri.encode(a.getId())));
                PendingIntent actionPending = AndroidImplementation.createMutablePendingIntent(context, requestCode++, actionIntent);
                // The action icon is a drawable resource NAME (Codename One has no R.drawable
                // int constants). Resolve it by name against the generated res/drawable; an
                // unknown / null name yields 0 (no icon), which the action button tolerates.
                int iconId = 0;
                String iconName = a.getIcon();
                if (iconName != null && iconName.length() > 0) {
                    if (iconName.startsWith("/")) {
                        iconName = iconName.substring(1);
                    }
                    int dot = iconName.lastIndexOf('.');
                    if (dot > 0) {
                        iconName = iconName.substring(0, dot);
                    }
                    iconId = ctx.getResources().getIdentifier(iconName, "drawable", ctx.getApplicationInfo().packageName);
                }
                try {
                    if (NotificationCompatWrapper.ActionWrapper.BuilderWrapper.isSupported()) {
                        NotificationCompatWrapper.ActionWrapper.BuilderWrapper actionBuilder =
                                new NotificationCompatWrapper.ActionWrapper.BuilderWrapper(iconId, a.getTitle(), actionPending);
                        if (a.isTextInput() && RemoteInputWrapper.isSupported()) {
                            RemoteInputWrapper.BuilderWrapper remoteInputBuilder =
                                    new RemoteInputWrapper.BuilderWrapper(a.getId() + "$Result");
                            if (a.getTextInputPlaceholder() != null) {
                                remoteInputBuilder.setLabel(a.getTextInputPlaceholder());
                            }
                            actionBuilder.addRemoteInput(remoteInputBuilder.build());
                        }
                        new NotificationCompatWrapper.BuilderWrapper(builder).addAction(actionBuilder.build());
                    } else {
                        builder.addAction(iconId, a.getTitle(), actionPending);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        AndroidImplementation.setNotificationChannel((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE), builder, context);
        String sound = localNotif.getAlertSound();
        if (sound != null && sound.length() > 0) {
            sound = sound.toLowerCase();
            builder.setSound(android.net.Uri.parse("android.resource://"+ctx.getApplicationInfo().packageName+"/raw"+sound.substring(0, sound.indexOf("."))));
        }
        Notification n = builder.build();
        n.icon = icon;
        if (sound == null || sound.length() == 0) {
            n.defaults |= Notification.DEFAULT_SOUND;
        }
        return n;
    }

}
