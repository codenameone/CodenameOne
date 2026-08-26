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
package com.codename1.impl.android.call;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import android.support.v4.app.NotificationCompat;

import com.codename1.impl.android.AndroidImplementation;

import java.util.HashMap;
import java.util.Map;

/// The incoming-call screen Telecom does not draw.
///
/// A self-managed `PhoneAccount` gets no system call UI: Telecom arbitrates
/// with other calls, routes the audio and keeps the call log, but presenting
/// the call is the application's job, and `Connection.onShowIncomingCallUi`
/// is where it asks for it. Without an answer to that callback
/// `reportIncoming` produced a call that rang in Telecom's bookkeeping and
/// appeared nowhere -- so `CAPABILITY_SYSTEM_UI`, which promises the platform
/// draws a call UI, was not true on Android.
///
/// What it draws is a call-category notification with a full-screen intent,
/// which is what the platform turns into a full-screen ringing UI on a locked
/// device and a heads-up banner on an unlocked one, plus Answer and Decline
/// actions. That is the shape Android documents for self-managed calling
/// apps, and it is why the port asks for `POST_NOTIFICATIONS`.
///
/// The labels are service properties -- `android.call.incomingTitle`,
/// `android.call.answerLabel`, `android.call.declineLabel` -- because a
/// ringing screen is the last place to show an untranslated English string.
final class CN1CallNotifications {

    private static final String CHANNEL_ID = "cn1-incoming-call";
    private static final String ACTION_ANSWER =
            "com.codename1.call.NOTIFICATION_ANSWER";
    private static final String ACTION_DECLINE =
            "com.codename1.call.NOTIFICATION_DECLINE";
    private static final String EXTRA_CALL = "com.codename1.call.NOTIFICATION_ID";

    /// Notification ids by call, so the right one is cancelled.
    private static final Map<String, Integer> SHOWN =
            new HashMap<String, Integer>();

    private static int nextNotificationId = 0x1CA0;
    private static BroadcastReceiver actions;

    private CN1CallNotifications() {
    }

    /// Rings for a call, or does nothing when there is no context to ring in.
    static void showIncoming(String callId, String caller) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null || callId == null) {
            return;
        }
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        ensureChannel(ctx, nm);
        ensureReceiver(ctx);
        int id;
        synchronized (SHOWN) {
            Integer existing = SHOWN.get(callId);
            if (existing != null) {
                id = existing.intValue();
            } else {
                id = nextNotificationId++;
                SHOWN.put(callId, Integer.valueOf(id));
            }
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx,
                CHANNEL_ID)
                .setSmallIcon(smallIcon(ctx))
                .setContentTitle(caller == null || caller.length() == 0
                        ? label(ctx, "android.call.incomingTitle",
                                "Incoming call")
                        : caller)
                .setContentText(label(ctx, "android.call.incomingTitle",
                        "Incoming call"))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(0, label(ctx, "android.call.answerLabel", "Answer"),
                        actionIntent(ctx, ACTION_ANSWER, callId, id))
                .addAction(0, label(ctx, "android.call.declineLabel",
                        "Decline"), actionIntent(ctx, ACTION_DECLINE, callId,
                        id));
        PendingIntent full = launchIntent(ctx, callId, id);
        if (full != null) {
            // The second argument is what makes this a ringing screen on a
            // locked device rather than a banner behind the lock screen.
            b.setFullScreenIntent(full, true);
        }
        nm.notify(id, b.build());
    }

    /// Takes the call off the screen. Safe to call for a call never shown.
    static void dismiss(String callId) {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null || callId == null) {
            return;
        }
        Integer id;
        synchronized (SHOWN) {
            id = SHOWN.remove(callId);
        }
        if (id == null) {
            return;
        }
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(id.intValue());
        }
    }

    /// Clears every ringing notification, for a provider reset.
    static void dismissAll() {
        String[] ids;
        synchronized (SHOWN) {
            ids = SHOWN.keySet().toArray(new String[SHOWN.size()]);
        }
        for (int i = 0; i < ids.length; i++) {
            dismiss(ids[i]);
        }
    }

    private static String label(Context ctx, String key, String fallback) {
        String v = AndroidImplementation.getServiceProperty(key, fallback, ctx);
        return v == null || v.length() == 0 ? fallback : v;
    }

    private static int smallIcon(Context ctx) {
        int icon = ctx.getResources().getIdentifier("ic_stat_notify",
                "drawable", ctx.getApplicationInfo().packageName);
        return icon == 0 ? ctx.getApplicationInfo().icon : icon;
    }

    private static PendingIntent actionIntent(Context ctx, String action,
            String callId, int notificationId) {
        Intent i = new Intent(action);
        // Explicitly this app: an implicit broadcast would be both undelivered
        // on modern Android and answerable by anything else on the device.
        i.setPackage(ctx.getPackageName());
        i.putExtra(CN1ConnectionService.EXTRA_CALL_ID, callId);
        i.putExtra(EXTRA_CALL, notificationId);
        return PendingIntent.getBroadcast(ctx, notificationId + action.hashCode(),
                i, pendingFlags());
    }

    private static PendingIntent launchIntent(Context ctx, String callId,
            int notificationId) {
        Intent i = ctx.getPackageManager()
                .getLaunchIntentForPackage(ctx.getPackageName());
        if (i == null) {
            return null;
        }
        i.putExtra(CN1ConnectionService.EXTRA_CALL_ID, callId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, notificationId, i,
                pendingFlags());
    }

    /// `FLAG_IMMUTABLE` is mandatory from Android 12 and absent from the
    /// android.jar this port compiles against, so it is spelled out.
    private static int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= 0x04000000; // PendingIntent.FLAG_IMMUTABLE
        }
        return flags;
    }

    /// Registers the Answer/Decline receiver once per process.
    ///
    /// Dynamically rather than in the manifest, because the process is
    /// necessarily alive whenever one of these notifications is on screen --
    /// CN1ConnectionService is running -- and a manifest entry would be one
    /// more thing the builder has to inject and keep in step.
    private static void ensureReceiver(Context ctx) {
        synchronized (SHOWN) {
            if (actions != null) {
                return;
            }
            actions = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) {
                        return;
                    }
                    String callId = intent.getStringExtra(
                            CN1ConnectionService.EXTRA_CALL_ID);
                    CN1Connection c = CN1ConnectionService.find(callId);
                    if (c == null) {
                        dismiss(callId);
                        return;
                    }
                    // Neither branch dismisses. onAnswer() takes the
                    // notification down itself, and a rejection must keep it
                    // up until it is actually carried out: a listener that
                    // fails endRequested is saying it could not reject the
                    // call, Telecom then leaves it ringing, and taking the
                    // notification away here left that restored call with no
                    // answer surface. finish() clears it on the way out.
                    if (ACTION_ANSWER.equals(intent.getAction())) {
                        c.onAnswer();
                    } else {
                        c.onReject();
                    }
                }
            };
        }
        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_ANSWER);
        f.addAction(ACTION_DECLINE);
        registerNotExported(ctx, actions, f);
    }

    /// `Context.RECEIVER_NOT_EXPORTED` is required from Android 14 and is not
    /// in this port's android.jar, hence the reflective four-argument call
    /// with the two-argument one as the older path.
    private static void registerNotExported(Context ctx, BroadcastReceiver r,
            IntentFilter f) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Context.class.getMethod("registerReceiver",
                        BroadcastReceiver.class, IntentFilter.class, int.class)
                        .invoke(ctx, r, f, Integer.valueOf(4));
                return;
            } catch (Exception fallThrough) {
                // Older behaviour below; an exported receiver for an intent
                // that names this package is not a leak worth failing over.
            }
        }
        ctx.registerReceiver(r, f);
    }

    /// The high-importance channel a ringing call needs.
    ///
    /// Its own, not the app's push channel: a call has to ring, and putting
    /// calls on the channel used for notifications means silencing one
    /// silences the other.
    private static void ensureChannel(Context ctx, NotificationManager nm) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        try {
            Class<?> chan = Class.forName("android.app.NotificationChannel");
            Object c = chan.getConstructor(String.class, CharSequence.class,
                    int.class).newInstance(CHANNEL_ID,
                    label(ctx, "android.call.channelName", "Incoming calls"),
                    Integer.valueOf(4)); // IMPORTANCE_HIGH
            chan.getMethod("setDescription", String.class).invoke(c,
                    label(ctx, "android.call.channelDescription",
                            "Ringing for incoming calls"));
            chan.getMethod("enableVibration", boolean.class)
                    .invoke(c, Boolean.TRUE);
            chan.getMethod("setLockscreenVisibility", int.class)
                    .invoke(c, Integer.valueOf(Notification.VISIBILITY_PUBLIC));
            android.media.AudioAttributes attrs =
                    new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes
                            .CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes
                            .USAGE_NOTIFICATION_RINGTONE)
                    .build();
            chan.getMethod("setSound", android.net.Uri.class,
                    android.media.AudioAttributes.class).invoke(c,
                    android.media.RingtoneManager.getDefaultUri(
                            android.media.RingtoneManager.TYPE_RINGTONE),
                    attrs);
            NotificationManager.class.getMethod("createNotificationChannel",
                    chan).invoke(nm, c);
        } catch (Exception e) {
            // A channel this app could not create means the notification is
            // posted without one, which on 26+ means it is not shown. There
            // is nothing to fall back to and nothing the app can do, so this
            // is logged rather than thrown into Telecom's callback.
            com.codename1.io.Log.p("Could not create the incoming-call "
                    + "notification channel: " + e);
        }
    }
}
