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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;

/// An Android service, run in this process by [ActiveServices].
///
/// The lifecycle is Android's: `onCreate`, then `onStartCommand` for every
/// `startService`, `onBind`/`onUnbind` for bindings, and `onDestroy` once the
/// service is neither started nor bound. Every callback runs on the main
/// thread (Codename One's EDT), so long work belongs on a thread of the
/// service's own -- exactly as on Android.
///
/// [#startForeground(int, Notification)] runs the service under a Codename
/// One `ForegroundService`: a persistent notification on Android, a
/// background execution window and a notification on iOS. The service object
/// itself does not need the platform to restart it; `START_STICKY` and the
/// other return values are accepted and have no effect, since nothing kills a
/// service separately from the application here.
public abstract class Service extends ContextWrapper {

    public static final int START_STICKY_COMPATIBILITY = 0;
    public static final int START_STICKY = 1;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;
    public static final int START_FLAG_REDELIVERY = 1;
    public static final int START_FLAG_RETRY = 2;
    public static final int STOP_FOREGROUND_REMOVE = 1;
    public static final int STOP_FOREGROUND_DETACH = 2;
    public static final int STOP_FOREGROUND_LEGACY = 0;

    private Application application;
    // Read by stopSelfResult on the service's worker thread.
    private final java.util.concurrent.atomic.AtomicInteger lastStartId =
            new java.util.concurrent.atomic.AtomicInteger();

    public Service() {
        super(null);
    }

    final void attachForRuntime(Context base, Application app) {
        attachBaseContext(base);
        application = app;
    }

    public final Application getApplication() {
        return application;
    }

    public void onCreate() {
    }

    /// Deprecated on Android; still called by the default `onStartCommand`.
    public void onStart(Intent intent, int startId) {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_STICKY;
    }

    public abstract IBinder onBind(Intent intent);

    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void onRebind(Intent intent) {
    }

    public void onDestroy() {
    }

    public void onTaskRemoved(Intent rootIntent) {
    }

    public void onTimeout(int startId) {
    }

    public void onTimeout(int startId, int fgsType) {
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {
    }

    public final void stopSelf() {
        ActiveServices.stopSelf(this, -1);
    }

    public final void stopSelf(int startId) {
        stopSelfResult(startId);
    }

    /// Stops the service when `startId` is the most recent start, as
    /// Android does, so a start that arrived meanwhile keeps it running.
    public final boolean stopSelfResult(int startId) {
        return ActiveServices.stopSelf(this, startId);
    }

    public final void startForeground(int id, Notification notification) {
        ActiveServices.startForeground(this, id, notification);
    }

    public final void startForeground(int id, Notification notification, int foregroundServiceType) {
        startForeground(id, notification);
    }

    public final void stopForeground(boolean removeNotification) {
        ActiveServices.stopForeground(this);
    }

    public final void stopForeground(int notificationBehavior) {
        ActiveServices.stopForeground(this);
    }

    public final int getForegroundServiceType() {
        return 0;
    }

    int nextStartId() {
        return lastStartId.incrementAndGet();
    }

    int lastStartId() {
        return lastStartId.get();
    }
}
