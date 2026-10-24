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
import android.os.Binder;
import android.os.IBinder;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ServiceLifecycleTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private Context context;

    @Before
    public void setUp() {
        context = AndroidTestSupport.context();
        AndroidTestSupport.TestService.calls.clear();
    }

    @Test
    public void startAndStopRunTheLifecycleOnTheMainThread() {
        Intent i = new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 1);
        assertNotNull(context.startService(i));
        MainThreadRule.drain();
        context.startService(new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 2));
        MainThreadRule.drain();
        assertEquals("[create, start:1:1, start:2:2]", AndroidTestSupport.TestService.calls.toString());
        assertTrue(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
        assertTrue(context.stopService(new Intent(context, AndroidTestSupport.TestService.class)));
        MainThreadRule.drain();
        assertFalse(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
        assertEquals("destroy", AndroidTestSupport.TestService.calls.get(3));
    }

    @Test
    public void boundServiceStaysUntilTheLastUnbind() {
        final IBinder[] got = new IBinder[1];
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                got[0] = service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        assertTrue(context.bindService(new Intent(context, AndroidTestSupport.TestService.class), conn,
                Context.BIND_AUTO_CREATE));
        MainThreadRule.drain();
        assertNotNull(got[0]);
        assertSame(AndroidTestSupport.TestService.last,
                ((AndroidTestSupport.TestService.LocalBinder) got[0]).service());
        context.unbindService(conn);
        MainThreadRule.drain();
        assertFalse(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
        assertEquals("[create, bind, unbind, destroy]", AndroidTestSupport.TestService.calls.toString());
    }

    /// A stop in the same frame as its start stops the service; it used to
    /// be judged before the start ran, answer false and leave it running.
    @Test
    public void startThenStopInOneFrame() {
        context.startService(new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 1));
        assertTrue(context.stopService(new Intent(context, AndroidTestSupport.TestService.class)));
        MainThreadRule.drain();
        assertFalse(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
        assertEquals("[create, start:1:1, destroy]", AndroidTestSupport.TestService.calls.toString());
        assertFalse("nothing is left to stop",
                context.stopService(new Intent(context, AndroidTestSupport.TestService.class)));
    }

    /// An unbind in the same frame as its bind is accepted and nothing
    /// connects; it used to throw "Service not registered".
    @Test
    public void bindThenUnbindInOneFrame() {
        final boolean[] connected = new boolean[1];
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                connected[0] = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        assertTrue(context.bindService(new Intent(context, AndroidTestSupport.TestService.class), conn,
                Context.BIND_AUTO_CREATE));
        context.unbindService(conn);
        MainThreadRule.drain();
        assertFalse(connected[0]);
        assertFalse(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
    }

    /// stopSelf(startId) compares the id when the stop runs, in order with
    /// the starts, so a start delivered first keeps the service running.
    @Test
    public void stopSelfForAnOlderStartKeepsANewerOne() {
        context.startService(new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 1));
        MainThreadRule.drain();
        context.startService(new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 2));
        AndroidTestSupport.TestService.last.stopSelf(1);
        MainThreadRule.drain();
        assertTrue(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
        assertTrue(context.stopService(new Intent(context, AndroidTestSupport.TestService.class)));
        MainThreadRule.drain();
        assertFalse(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
    }

    /// A start that arrives after a stopSelf has stopped the service starts
    /// it again, and a later stopService still stops it.
    @Test
    public void aStartAfterStopSelfCanStillBeStopped() {
        context.startService(new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 1));
        MainThreadRule.drain();
        AndroidTestSupport.TestService.last.stopSelf(1);
        context.startService(new Intent(context, AndroidTestSupport.TestService.class).putExtra("n", 2));
        MainThreadRule.drain();
        assertTrue(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
        assertTrue(context.stopService(new Intent(context, AndroidTestSupport.TestService.class)));
        MainThreadRule.drain();
        assertFalse(ActiveServices.isRunning(AndroidTestSupport.TestService.class));
    }

    @Test
    public void anUndeclaredServiceDoesNotStart() {
        assertNull(context.startService(new Intent(context, Undeclared.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void implicitServiceIntentsAreRefused() {
        context.startService(new Intent("some.ACTION"));
    }

    @Test
    public void notificationsBuildAndPost() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assertSame(NotificationManager.getInstance(), nm);
        nm.createNotificationChannel(new NotificationChannel("c", "Channel", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(context, "c").setContentTitle("T").setContentText("B")
                .setProgress(10, 3, false).build();
        assertEquals("c", n.getChannelId());
        nm.notify(5, n);
        nm.cancel(5);
        assertNotNull(nm.getNotificationChannel("c"));
    }

    public static final class Undeclared extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new Binder();
        }
    }
}
