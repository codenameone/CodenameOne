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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import com.codename1.push.PushCallback;
import com.codename1.ui.Display;

/**
 * This class implements a push notification fallback service for applications that require
 * push notification support but don't have Android Market installed
 *
 * @author Shai Almog
 */
public abstract class PushNotificationService extends Service implements PushCallback {

    private java.util.Map<String,String> properties = new java.util.HashMap<String,String>();
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
    }
    
    private String getProperty(String propertyName, String defaultValue) {
        String val = properties.get(propertyName);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }
    
    public void setProperty(String propertyName, String val) {
        properties.put(propertyName, val);
        
    }
    
    public static void startServiceIfRequired(Class service, Context ctx) {
        if(!AndroidImplementation.hasAndroidMarket(ctx)) {
            SharedPreferences sh = ctx.getSharedPreferences("C2DMNeeded", Context.MODE_PRIVATE);
            if(sh.getBoolean("C2DMNeeded", false)) {
                Intent i = new Intent();
                i.setAction(service.getClass().getName());
                ctx.startService(i);
            }
        }
    }
    
    public static void forceStartService(String service, Context ctx) {
        if(!AndroidImplementation.hasAndroidMarket(ctx)) {
            SharedPreferences sh = ctx.getSharedPreferences("C2DMNeeded", Context.MODE_PRIVATE);
            Editor editor = sh.edit();
            editor.putBoolean("C2DMNeeded", true);
            editor.commit();
            Intent i = new Intent();
            i.setAction(service);
            ctx.startService(i);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AndroidImplementation.registerPolling();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        AndroidImplementation.stopPollingLoop();
    }
    
    public abstract PushCallback getPushCallbackInstance();
    public abstract Class getStubClass();
    
    @Override
    public void push(final String value) {
        final PushCallback callback = getPushCallbackInstance();
        if(callback != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    callback.push(value);
                }
            });
        } else {
            NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            Intent newIntent = new Intent(this, getStubClass());
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);



            Notification.Builder builder = new Notification.Builder(this)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setTicker(value)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(value)

                    .setDefaults(Notification.DEFAULT_ALL);




            // The following section is commented out so that builds against SDKs below 26
            // won't fail.
            /*<SDK26>
            if(android.os.Build.VERSION.SDK_INT >= 21){
                builder.setCategory("Notification");
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                String id = getProperty("android.NotificationChannel.id", "cn1-channel");

                CharSequence name = getProperty("android.NotificationChannel.name", "Notifications");

                String description = getProperty("android.NotificationChannel.description", "Remote notifications");

                int importance = Integer.parseInt(getProperty("android.NotificationChannel.importance", ""+NotificationManager.IMPORTANCE_LOW));

                android.app.NotificationChannel mChannel = new android.app.NotificationChannel(id, name,importance);

                mChannel.setDescription(description);

                mChannel.enableLights(Boolean.parseBoolean(getProperty("android.NotificationChannel.enableLights", "true")));

                mChannel.setLightColor(Integer.parseInt(getProperty("android.NotificationChannel.lightColor", ""+android.graphics.Color.RED)));

                mChannel.enableVibration(Boolean.parseBoolean(getProperty("android.NotificationChannel.enableVibration", "false")));
                String vibrationPatternStr = getProperty("android.NotificationChannel.vibrationPattern", null);
                if (vibrationPatternStr != null) {
                    String[] parts = vibrationPatternStr.split(",");
                    int len = parts.length;
                    long[] pattern = new long[len];
                    for (int i=0; i<len; i++) {
                        pattern[i] = Long.parseLong(parts[i].trim());
                    }
                    mChannel.setVibrationPattern(pattern);
                }



                mNotificationManager.createNotificationChannel(mChannel);
                System.out.println("Setting push channel to "+id);
                builder.setChannelId(id);
            }
            </SDK26>*/

            Notification notif = builder.build();
            nm.notify((int)System.currentTimeMillis(), notif);
        }
    }

    @Override
    public void registeredForPush(String deviceId) {
    }

    @Override
    public void pushRegistrationError(String error, int errorCode) {
    }
}
