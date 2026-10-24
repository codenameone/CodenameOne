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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.codename1.androidcompat.runtime.AndroidApp;
import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.androidcompat.runtime.CompatReport;
import com.codename1.androidcompat.runtime.MainThread;
import com.codename1.background.ForegroundService;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/// The services of this application: which are running, started, bound and
/// in the foreground. `Context.startService`, `bindService` and the rest come
/// here, as does `Service.stopSelf`.
///
/// A service is created with the `new` the build generated for its manifest
/// `<service>` (`AndroidApp.createService`), so an undeclared class is never
/// started -- as on Android, where the start answers null.
///
/// Every lifecycle callback is posted to the main thread, so `startService`
/// returns before `onCreate` runs, which is Android's order too.
public final class ActiveServices {

    private ActiveServices() {
    }

    static final class Record {
        final Class<?> type;
        final ComponentName name;
        Service service;
        boolean started;
        IBinder binder;
        boolean binderAsked;
        boolean rebindOnBind;
        final List<ServiceConnection> connections = new ArrayList<ServiceConnection>();
        int foregroundId;
        ForegroundService foreground;
        CountDownLatch foregroundDone;

        Record(Class<?> type, ComponentName name) {
            this.type = type;
            this.name = name;
        }
    }

    private static final Map<Class<?>, Record> RUNNING = new LinkedHashMap<Class<?>, Record>();
    private static final Map<ServiceConnection, Record> BINDINGS = new LinkedHashMap<ServiceConnection, Record>();

    // What the callers have asked for so far, kept at call time. The
    // lifecycle runs later, on the main thread, so a stop or unbind made in
    // the same frame as its start or bind is judged against these rather than
    // against a service that is not created yet.
    //
    // Callers may be on any thread (a worker calling stopService is
    // ordinary), so each is an immutable set swapped atomically.
    private static final CowSet<Class<?>> START_REQUESTED = new CowSet<Class<?>>();
    private static final CowSet<ServiceConnection> BIND_REQUESTED = new CowSet<ServiceConnection>();

    /// A copy-on-write set: every change publishes a new set with
    /// compareAndSet, so readers on any thread see a whole one.
    static final class CowSet<T> {
        private final java.util.concurrent.atomic.AtomicReference<java.util.Set<T>> ref =
                new java.util.concurrent.atomic.AtomicReference<java.util.Set<T>>(new java.util.HashSet<T>());

        boolean contains(T t) {
            return ref.get().contains(t);
        }

        void add(T t) {
            while (true) {
                java.util.Set<T> cur = ref.get();
                if (cur.contains(t)) {
                    return;
                }
                java.util.Set<T> next = new java.util.HashSet<T>(cur);
                next.add(t);
                if (ref.compareAndSet(cur, next)) {
                    return;
                }
            }
        }

        /// True when `t` was there.
        boolean remove(T t) {
            while (true) {
                java.util.Set<T> cur = ref.get();
                if (!cur.contains(t)) {
                    return false;
                }
                java.util.Set<T> next = new java.util.HashSet<T>(cur);
                next.remove(t);
                if (ref.compareAndSet(cur, next)) {
                    return true;
                }
            }
        }
    }

    // ------------------------------------------------------------ Context

    public static ComponentName startService(Context caller, final Intent intent) {
        final Class<?> type = resolve(intent);
        if (type == null) {
            return null;
        }
        final Intent copy = new Intent(intent);
        final ComponentName name = new ComponentName(caller, type);
        START_REQUESTED.add(type);
        MainThread.post(new Runnable() {
            @Override
            public void run() {
                Record r = ensureCreated(type, name);
                if (r == null) {
                    START_REQUESTED.remove(type);
                    return;
                }
                // Again here: a stopSelf that ran between the call and this
                // delivery cleared it, and this start is still a request.
                START_REQUESTED.add(type);
                r.started = true;
                r.service.onStartCommand(copy, 0, r.service.nextStartId());
            }
        });
        return name;
    }

    public static boolean stopService(Context caller, Intent intent) {
        final Class<?> type = resolve(intent);
        if (type == null) {
            return false;
        }
        if (!START_REQUESTED.remove(type)) {
            return false;
        }
        MainThread.post(new Runnable() {
            @Override
            public void run() {
                Record rec = RUNNING.get(type);
                if (rec != null) {
                    rec.started = false;
                    destroyIfIdle(rec);
                }
            }
        });
        return true;
    }

    public static boolean bindService(Context caller, Intent intent, final ServiceConnection conn, int flags) {
        if (conn == null) {
            throw new IllegalArgumentException("connection is null");
        }
        final Class<?> type = resolve(intent);
        if (type == null) {
            return false;
        }
        final boolean autoCreate = (flags & Context.BIND_AUTO_CREATE) != 0;
        if (!autoCreate && RUNNING.get(type) == null) {
            // Android records the binding and connects once something else
            // starts the service; a later start does not connect it here.
            CompatReport.unsupported("bindService", "binding without BIND_AUTO_CREATE to a stopped service");
            return false;
        }
        final Intent copy = new Intent(intent);
        final ComponentName name = new ComponentName(caller, type);
        BIND_REQUESTED.add(conn);
        MainThread.post(new Runnable() {
            @Override
            public void run() {
                if (!BIND_REQUESTED.contains(conn)) {
                    // Unbound before it connected: Android delivers nothing.
                    return;
                }
                Record r = ensureCreated(type, name);
                if (r == null) {
                    BIND_REQUESTED.remove(conn);
                    return;
                }
                if (BINDINGS.containsKey(conn)) {
                    return;
                }
                if (!r.binderAsked) {
                    r.binderAsked = true;
                    r.binder = r.service.onBind(copy);
                } else if (r.rebindOnBind && r.connections.isEmpty()) {
                    r.rebindOnBind = false;
                    r.service.onRebind(copy);
                }
                r.connections.add(conn);
                BINDINGS.put(conn, r);
                if (r.binder == null) {
                    conn.onNullBinding(r.name);
                } else {
                    conn.onServiceConnected(r.name, r.binder);
                }
            }
        });
        return true;
    }

    public static void unbindService(Context caller, final ServiceConnection conn) {
        if (!BIND_REQUESTED.remove(conn)) {
            throw new IllegalArgumentException("Service not registered: " + conn);
        }
        MainThread.post(new Runnable() {
            @Override
            public void run() {
                Record r = BINDINGS.remove(conn);
                if (r == null) {
                    return;
                }
                r.connections.remove(conn);
                // An unbind is not a disconnect: Android calls no callback.
                if (r.connections.isEmpty() && r.service != null) {
                    r.rebindOnBind = r.service.onUnbind(new Intent().setClassName(r.name.getPackageName(),
                            r.name.getClassName()));
                    destroyIfIdle(r);
                }
            }
        });
    }

    // ------------------------------------------------------------- Service

    /// May be called from the service's own worker thread (IntentService
    /// does), so the start id is compared on the main thread, in order with
    /// the starts: a start delivered between this call and the stop keeps the
    /// service running, as on Android. The answer is the call-time view.
    static boolean stopSelf(final Service s, final int startId) {
        MainThread.post(new Runnable() {
            @Override
            public void run() {
                Record r = recordOf(s);
                if (r == null || (startId >= 0 && startId != s.lastStartId())) {
                    return;
                }
                r.started = false;
                START_REQUESTED.remove(r.type);
                destroyIfIdle(r);
            }
        });
        return startId < 0 || startId == s.lastStartId();
    }

    static void startForeground(Service s, int id, Notification n) {
        Record r = recordOf(s);
        if (r == null || n == null) {
            return;
        }
        r.foregroundId = id;
        String title = text(n.title());
        String body = text(n.body());
        if (r.foreground != null) {
            r.foreground.updateNotification(title, body);
            return;
        }
        if (!Display.isInitialized()) {
            return;
        }
        // Codename One's foreground service lasts as long as its task: this
        // one parks a background thread until the service leaves the
        // foreground, which is when Android takes the notification away.
        final CountDownLatch done = new CountDownLatch(1);
        r.foregroundDone = done;
        String channel = n.getChannelId() == null ? NotificationChannel.DEFAULT_CHANNEL_ID : n.getChannelId();
        r.foreground = ForegroundService.start(channel, title, body, null, new ForegroundService.Task() {
            @Override
            public void run(ForegroundService service) {
                try {
                    done.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    static void stopForeground(Service s) {
        Record r = recordOf(s);
        if (r != null) {
            endForeground(r);
        }
    }

    /// `NotificationManager.notify` with the id of a running foreground
    /// service updates that service's notification.
    static boolean updateForegroundNotification(int id, Notification n) {
        for (Record r : RUNNING.values()) {
            if (r.foreground != null && r.foregroundId == id) {
                r.foreground.updateNotification(text(n.title()), text(n.body()));
                return true;
            }
        }
        return false;
    }

    /// Whether `type` is running; for tests and diagnostics.
    public static boolean isRunning(Class<?> type) {
        return RUNNING.containsKey(type);
    }

    // ------------------------------------------------------------- helpers

    private static String text(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    private static Record recordOf(Service s) {
        for (Record r : RUNNING.values()) {
            if (r.service == s) {
                return r;
            }
        }
        return null;
    }

    private static Class<?> resolve(Intent intent) {
        if (intent == null) {
            throw new NullPointerException("intent");
        }
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (rt == null) {
            throw new IllegalStateException("The Android runtime is not installed");
        }
        AndroidApp app = rt.getApp();
        Class<?> type = intent.getComponentClass();
        if (type == null && intent.getComponent() != null) {
            type = app.serviceClass(intent.getComponent().getClassName());
        }
        if (type == null) {
            // Android has required explicit service intents since API 21.
            throw new IllegalArgumentException("Service Intent must be explicit: " + intent);
        }
        if (!app.isService(type)) {
            CompatReport.unsupported("service", type.getName() + " (not declared in the manifest)");
            return null;
        }
        return type;
    }

    private static Record ensureCreated(Class<?> type, ComponentName name) {
        Record r = RUNNING.get(type);
        if (r != null) {
            return r;
        }
        AndroidRuntime rt = AndroidRuntime.getInstance();
        Service s = rt.getApp().createService(type);
        if (s == null) {
            CompatReport.unsupported("service", type.getName() + " (no generated factory entry)");
            return null;
        }
        r = new Record(type, name);
        r.service = s;
        RUNNING.put(type, r);
        s.attachForRuntime(rt.getApplication(), rt.getApplication());
        s.onCreate();
        return r;
    }

    private static void destroyIfIdle(Record r) {
        if (r.started || !r.connections.isEmpty() || RUNNING.get(r.type) != r) {
            return;
        }
        RUNNING.remove(r.type);
        endForeground(r);
        r.service.onDestroy();
    }

    private static void endForeground(Record r) {
        if (r.foregroundDone != null) {
            r.foregroundDone.countDown();
            r.foregroundDone = null;
        }
        if (r.foreground != null) {
            r.foreground.stop();
            r.foreground = null;
        }
    }
}
