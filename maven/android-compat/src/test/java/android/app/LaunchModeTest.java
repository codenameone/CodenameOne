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
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("n", 1);
                app.startActivity(i);
                Activity first = ActivityThread.getTopActivity();
                int count = ActivityThread.getActivityCount();
                app.startActivity(new Intent(app, AndroidTestSupport.SingleTopActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("n", 2));
                assertSame(first, ActivityThread.getTopActivity());
                assertEquals(count, ActivityThread.getActivityCount());
                assertEquals(1, ((AndroidTestSupport.SingleTopActivity) first).newIntents);
                assertEquals("onNewIntent does not replace getIntent()", 1, first.getIntent().getIntExtra("n", 0));
            }
        });
    }

    @Test
    public void singleTopOnTopIsPausedAroundTheIntent() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.SingleTopActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                final AndroidTestSupport.SingleTopActivity first =
                        (AndroidTestSupport.SingleTopActivity) ActivityThread.getTopActivity();
                final StringBuilder events = new StringBuilder();
                Application.ActivityLifecycleCallbacks cb = new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, android.os.Bundle saved) {
                        events.append("created;");
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                        events.append("started;");
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        events.append("resumed").append(first.newIntents).append(';');
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        events.append("paused").append(first.newIntents).append(';');
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        events.append("stopped;");
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, android.os.Bundle out) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        events.append("destroyed;");
                    }
                };
                first.getApplication().registerActivityLifecycleCallbacks(cb);
                try {
                    app.startActivity(new Intent(app, AndroidTestSupport.SingleTopActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } finally {
                    first.getApplication().unregisterActivityLifecycleCallbacks(cb);
                }
                assertSame(first, ActivityThread.getTopActivity());
                // Paused before onNewIntent and resumed after it, as Android does.
                assertEquals("paused0;resumed1;", events.toString());
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
                assertEquals(again.getFlags(), now.getIntent().getFlags());
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
                assertEquals(original.getFlags(), target.getIntent().getFlags());
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

    @Test
    public void reorderToFrontMovesTheExistingInstanceUp() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Activity target = ActivityThread.getTopActivity();
                target.startActivity(new Intent(target, AndroidTestSupport.UiModeHandlingActivity.class));
                Activity above = ActivityThread.getTopActivity();
                above.startActivity(new Intent(above, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                assertSame("a duplicate instance was stacked", target, ActivityThread.getTopActivity());
                assertEquals(2, ActivityThread.getActivityCount());
                assertTrue("the activity it moved past was destroyed", !above.isDestroyed());
                // The one it moved past is now beneath it.
                target.finish();
                assertSame(above, ActivityThread.getTopActivity());
            }
        });
    }

    @Test
    public void reorderToFrontStopsWhenNewIntentFinishesTarget() {
        onEdt(new Runnable() {
            @Override public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Activity target = ActivityThread.getTopActivity();
                target.startActivity(new Intent(target, AndroidTestSupport.UiModeHandlingActivity.class));
                Activity above = ActivityThread.getTopActivity();
                AndroidTestSupport.TestActivity.newIntentHandler =
                        new AndroidTestSupport.TestActivity.NewIntentHandler() {
                            @Override public void onNewIntent(AndroidTestSupport.TestActivity activity) {
                                activity.finish();
                            }
                        };
                try {
                    above.startActivity(new Intent(above, AndroidTestSupport.TestActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                } finally {
                    AndroidTestSupport.TestActivity.newIntentHandler = null;
                }
                assertTrue(target.isDestroyed());
                assertSame(above, ActivityThread.getTopActivity());
                assertTrue(above.mRecord.resumed);
                assertTrue(above.mRecord.started);
                assertSame(above.mRecord.form, Display.getInstance().getCurrent());
            }
        });
    }
    @Test
    public void clearTaskOverridesReuseModesAndFlags() {
        onEdt(new Runnable() {
            @Override public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                Class<?>[] targets = {AndroidTestSupport.SingleTaskActivity.class,
                        AndroidTestSupport.SingleTopActivity.class, AndroidTestSupport.TestActivity.class};
                for (Class<?> targetClass : targets) {
                    app.startActivity(new Intent(app, targetClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    Activity old = ActivityThread.getTopActivity();
                    if (targetClass == AndroidTestSupport.TestActivity.class) {
                        old.startActivity(new Intent(old, AndroidTestSupport.UiModeHandlingActivity.class));
                    }
                    app.startActivity(new Intent(app, targetClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    Activity fresh = ActivityThread.getTopActivity();
                    assertTrue("CLEAR_TASK must create a new instance", fresh != old);
                    assertTrue(old.isDestroyed());
                    assertEquals(1, ActivityThread.getActivityCount());
                    fresh.finish();
                }
            }
        });
    }

    @Test
    public void finishingDuringLaunchStopsFurtherCallbacksAndRestoresCaller() {
        onEdt(new Runnable() {
            @Override public void run() {
                Context app = AndroidTestSupport.context().getApplicationContext();
                app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Activity caller = ActivityThread.getTopActivity();
                for (String stage : new String[]{"create", "start", "resume"}) {
                    caller.startActivity(new Intent(caller, AndroidTestSupport.FinishingActivity.class)
                            .putExtra("finishAt", stage));
                    AndroidTestSupport.FinishingActivity finished = AndroidTestSupport.FinishingActivity.last;
                    assertTrue(finished.isDestroyed());
                    assertEquals("no callback after destruction: " + finished.events,
                            "destroy", finished.events.get(finished.events.size() - 1));
                    assertSame(caller, ActivityThread.getTopActivity());
                    assertTrue(caller.mRecord.resumed);
                    assertTrue(caller.mRecord.started);
                    assertSame(caller.mRecord.form, Display.getInstance().getCurrent());
                    assertTrue(!((Activity) finished).mRecord.resumed);
                    assertTrue(!((Activity) finished).mRecord.started);
                }
            }
        });
    }

}
