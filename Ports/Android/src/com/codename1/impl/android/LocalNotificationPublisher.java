/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import static com.codename1.impl.android.AndroidImplementation.activity;
import com.codename1.notifications.LocalNotification;
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

    public void onReceive(Context context, Intent intent) {
        //Fire the notification to the display
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Bundle extras=intent.getExtras();
        PendingIntent content = extras.getParcelable(NOTIFICATION_INTENT);
        Bundle b = extras.getParcelable(NOTIFICATION);
        LocalNotification notif = AndroidImplementation.createNotificationFromBundle(b);

        Notification notification = createAndroidNotification(context, notif, content);
        notification.when = System.currentTimeMillis();
        notificationManager.notify(notif.getId(), 0, notification);
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
