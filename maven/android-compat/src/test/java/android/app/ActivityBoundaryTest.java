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
import android.content.Intent;
import android.os.Bundle;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.*;
import static org.junit.Assert.*;

public class ActivityBoundaryTest {
    @BeforeClass public static void initDisplay() { AndroidTestSupport.cleanDisplay(); }
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @After public void cleanup() { ActivityThread.finishAllActivities(); }

    @Test public void finishFromStop() { finishWhileStopping("stop"); }
    @Test public void finishFromSave() { finishWhileStopping("save"); }
    @Test public void finishFromSaveCallback() { finishWhileStopping("saveCallback"); }
    @Test public void finishFromStopCallback() { finishWhileStopping("stopCallback"); }

    @Test public void recreateStopsWhenPauseOrStopFinishesTheActivity() {
        for (String boundary : new String[] {"pause", "stop"}) {
            Context c = AndroidTestSupport.context().getApplicationContext();
            c.startActivity(new Intent(c, AndroidTestSupport.FinishingActivity.class)
                    .putExtra("finishAt", boundary).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AndroidTestSupport.FinishingActivity old = AndroidTestSupport.FinishingActivity.last;
            old.recreate();
            assertEquals(old.events.toString(), 1,
                    java.util.Collections.frequency(old.events, "destroy"));
            assertEquals("a finished activity must not be replaced", null,
                    ActivityThread.currentInstance(old));
            ActivityThread.finishAllActivities();
        }
    }

    @Test public void laterLifecycleCallbacksDoNotRunAfterFinish() {
        for (final String boundary : new String[] {"created", "started", "resumed"}) {
            final Context c = AndroidTestSupport.context().getApplicationContext();
            final Application app = (Application) c;
            final int[] late = {0};
            Application.ActivityLifecycleCallbacks finishing = new LifecycleCallbackAdapter() {
                @Override public void onActivityCreated(Activity a, Bundle b) {
                    if ("created".equals(boundary)) a.finish();
                }
                @Override public void onActivityStarted(Activity a) {
                    if ("started".equals(boundary)) a.finish();
                }
                @Override public void onActivityResumed(Activity a) {
                    if ("resumed".equals(boundary)) a.finish();
                }
            };
            Application.ActivityLifecycleCallbacks later = new LifecycleCallbackAdapter() {
                @Override public void onActivityCreated(Activity a, Bundle b) {
                    if ("created".equals(boundary)) late[0]++;
                }
                @Override public void onActivityStarted(Activity a) {
                    if ("started".equals(boundary)) late[0]++;
                }
                @Override public void onActivityResumed(Activity a) {
                    if ("resumed".equals(boundary)) late[0]++;
                }
            };
            app.registerActivityLifecycleCallbacks(finishing);
            app.registerActivityLifecycleCallbacks(later);
            try {
                c.startActivity(new Intent(c, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                assertEquals(boundary, 0, late[0]);
            } finally {
                app.unregisterActivityLifecycleCallbacks(finishing);
                app.unregisterActivityLifecycleCallbacks(later);
                ActivityThread.finishAllActivities();
            }
        }
    }

    private abstract static class LifecycleCallbackAdapter implements Application.ActivityLifecycleCallbacks {
        public void onActivityCreated(Activity a, Bundle b) { }
        public void onActivityStarted(Activity a) { }
        public void onActivityResumed(Activity a) { }
        public void onActivityPaused(Activity a) { }
        public void onActivityStopped(Activity a) { }
        public void onActivitySaveInstanceState(Activity a, Bundle b) { }
        public void onActivityDestroyed(Activity a) { }
    }

    private void finishWhileStopping(final String at) {
        Context c = AndroidTestSupport.context().getApplicationContext();
        c.startActivity(new Intent(c, AndroidTestSupport.FinishingActivity.class)
                .putExtra("finishAt", at).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        final AndroidTestSupport.FinishingActivity a = AndroidTestSupport.FinishingActivity.last;
        Application app = (Application)c;
        Application.ActivityLifecycleCallbacks cb = new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity x, Bundle b) { }
            public void onActivityStarted(Activity x) { }
            public void onActivityResumed(Activity x) { }
            public void onActivityPaused(Activity x) { }
            public void onActivityDestroyed(Activity x) { }
            public void onActivitySaveInstanceState(Activity x, Bundle b) { event(x, "saveCallback"); }
            public void onActivityStopped(Activity x) { event(x, "stopCallback"); }
            private void event(Activity x, String name) {
                if (x != a) return;
                a.events.add(name);
                if (at.equals(name)) a.finish();
            }
        };
        app.registerActivityLifecycleCallbacks(cb);
        try {
            a.startActivity(new Intent(a, AndroidTestSupport.TestActivity.class));
            assertEquals(a.events.toString(), 1, java.util.Collections.frequency(a.events, "destroy"));
            assertEquals(a.events.toString(), at.startsWith("stop") ? 1 : 0,
                    java.util.Collections.frequency(a.events, "stop"));
            assertEquals(a.events.toString(), "destroy", a.events.get(a.events.size() - 1));
        } finally { app.unregisterActivityLifecycleCallbacks(cb); }
    }

    private AndroidTestSupport.TestActivity startRoot() {
        Context c = AndroidTestSupport.context().getApplicationContext();
        c.startActivity(new Intent(c, AndroidTestSupport.TestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return (AndroidTestSupport.TestActivity)ActivityThread.getTopActivity();
    }

    @Test public void queuedResultSnapshotsMutableExtras() {
        AndroidTestSupport.TestActivity a = startRoot();
        a.startActivityForResult(new Intent(a, AndroidTestSupport.TestActivity.class), 5);
        Activity b = ActivityThread.getTopActivity();
        int[] values = {7};
        Intent result = new Intent("original").putExtra("values", values);
        b.setResult(Activity.RESULT_OK, result);
        b.startActivity(new Intent(b, AndroidTestSupport.TestActivity.class));
        Activity c = ActivityThread.getTopActivity();
        a.finishActivity(5);
        result.setAction("mutated");
        values[0] = 9;
        c.finish();
        assertEquals("original", a.resultData.getAction());
        assertArrayEquals(new int[]{7}, a.resultData.getIntArrayExtra("values"));
    }

    @Test public void permissionResultFollowsRecreation() {
        AndroidTestSupport.TestActivity old = startRoot();
        old.requestPermissions(new String[]{"camera"}, 8);
        old.recreate();
        AndroidTestSupport.TestActivity fresh = (AndroidTestSupport.TestActivity)ActivityThread.getTopActivity();
        MainThreadRule.drain();
        assertEquals(0, old.permissionResults);
        assertEquals(1, fresh.permissionResults);
    }

    @Test public void permissionResultIsDroppedAfterFinish() {
        AndroidTestSupport.TestActivity old = startRoot();
        old.requestPermissions(new String[]{"camera"}, 8);
        old.finish();
        MainThreadRule.drain();
        assertEquals(0, old.permissionResults);
    }
}
