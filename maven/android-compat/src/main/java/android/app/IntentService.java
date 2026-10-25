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

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/// Deprecated on Android, still common: each start is handled in turn on one
/// worker thread, and the service stops itself when the queue is empty.
public abstract class IntentService extends Service {

    private final String name;
    private Looper serviceLooper;
    private Handler serviceHandler;
    private boolean redelivery;

    public IntentService(String name) {
        this.name = name;
    }

    public void setIntentRedelivery(boolean enabled) {
        redelivery = enabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + name + "]");
        thread.start();
        serviceLooper = thread.getLooper();
        serviceHandler = new Handler(serviceLooper) {
            @Override
            public void handleMessage(Message msg) {
                onHandleIntent((Intent) msg.obj);
                stopSelf(msg.arg1);
            }
        };
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (serviceHandler == null) {
            // A subclass overrode onCreate without calling super, which
            // fails the same way on Android.
            throw new IllegalStateException(getClass().getName() + ".onCreate() did not call super.onCreate()");
        }
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        serviceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return redelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (serviceLooper != null) {
            serviceLooper.quit();
        }
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }

    protected abstract void onHandleIntent(Intent intent);
}
