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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The manifest's `android:launchMode`: a notification or deep link that
/// starts a singleTop or singleTask activity again reaches the live instance
/// through `onNewIntent` instead of stacking a duplicate screen.
public class LaunchModeTest {

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    private static void onEdt(final Runnable r) {
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    ActivityThread.finishAllActivities();
                }
            }
        });
        if (failure[0] instanceof Error) {
            throw (Error) failure[0];
        }
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
    }

    @Test
    public void singleTopOnTopReceivesTheIntent() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                Intent i = new Intent(app, AndroidTestSupport.SingleTopActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(i);
                Activity first = ActivityThread.getTopActivity();
                int count = ActivityThread.getActivityCount();
                app.startActivity(new Intent(app, AndroidTestSupport.SingleTopActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                assertSame(first, ActivityThread.getTopActivity());
                assertEquals(count, ActivityThread.getActivityCount());
                assertEquals(1, ((AndroidTestSupport.SingleTopActivity) first).newIntents);
                assertSame("onNewIntent does not replace getIntent()", i, first.getIntent());
            }
        });
    }

    @Test
    public void clearTopRecreatesAStandardTarget() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Activity target = ActivityThread.getTopActivity();
                int count = ActivityThread.getActivityCount();
                target.startActivity(new Intent(target, AndroidTestSupport.UiModeHandlingActivity.class));
                Activity above = ActivityThread.getTopActivity();
                Intent again = new Intent(above, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                above.startActivity(again);
                Activity now = ActivityThread.getTopActivity();
                assertTrue(now instanceof AndroidTestSupport.TestActivity);
                assertTrue("a standard target is recreated", now != target);
                assertTrue(target.isDestroyed());
                assertTrue(above.isDestroyed());
                assertSame(again, now.getIntent());
                assertEquals(count, ActivityThread.getActivityCount());
                assertSame(now, ActivityThread.getTopActivity());
            }
        });
    }

    @Test
    public void clearTopWithSingleTopReusesAStandardTarget() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                Intent original = new Intent(app, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(original);
                Activity target = ActivityThread.getTopActivity();
                int count = ActivityThread.getActivityCount();
                target.startActivity(new Intent(target, AndroidTestSupport.UiModeHandlingActivity.class));
                Activity above = ActivityThread.getTopActivity();
                Intent again = new Intent(above, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                above.startActivity(again);
                assertSame(target, ActivityThread.getTopActivity());
                assertTrue(!target.isDestroyed());
                assertTrue(above.isDestroyed());
                // As on Android, a reused instance keeps its launch intent;
                // the new one reaches onNewIntent only.
                assertSame(original, target.getIntent());
                assertEquals(count, ActivityThread.getActivityCount());
            }
        });
    }

    @Test
    public void clearTopDeliversTheResultOfARemovedActivity() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                AndroidTestSupport.TestActivity target = (AndroidTestSupport.TestActivity) ActivityThread.getTopActivity();
                target.startActivityForResult(new Intent(target, AndroidTestSupport.UiModeHandlingActivity.class), 7);
                Activity child = ActivityThread.getTopActivity();
                child.startActivity(new Intent(child, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                assertSame(target, ActivityThread.getTopActivity());
                assertTrue(child.isDestroyed());
                // As on Android, the cleared child is finished: its caller
                // still hears RESULT_CANCELED for the request it made.
                assertEquals(7, target.resultRequestCode);
                assertEquals(Activity.RESULT_CANCELED, target.resultCode);
            }
        });
    }

    @Test
    public void singleTaskClearsWhatIsAboveIt() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.SingleTaskActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Activity task = ActivityThread.getTopActivity();
                int count = ActivityThread.getActivityCount();
                task.startActivity(new Intent(task, AndroidTestSupport.TestActivity.class));
                assertTrue(ActivityThread.getTopActivity() instanceof AndroidTestSupport.TestActivity);
                Activity above = ActivityThread.getTopActivity();
                above.startActivity(new Intent(above, AndroidTestSupport.SingleTaskActivity.class));
                assertSame(task, ActivityThread.getTopActivity());
                assertEquals(count, ActivityThread.getActivityCount());
                assertEquals(1, ((AndroidTestSupport.SingleTaskActivity) task).newIntents);
            }
        });
    }
}
