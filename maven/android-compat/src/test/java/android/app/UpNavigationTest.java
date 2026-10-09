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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// Up navigation goes to the manifest's parent activity, and
/// `startActivityIfNeeded` leaves an intent that would only reach the caller
/// again to the caller.
public class UpNavigationTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    private interface StackWork {
        void run(Context app) throws Exception;
    }

    private static void onEdt(final StackWork work) {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    work.run(app);
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

    private static Activity start(Context from, Class<?> type) {
        Intent intent = new Intent(from, type);
        if (!(from instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        from.startActivity(intent);
        return ActivityThread.getTopActivity();
    }

    /// Up returns to the parent beneath, finishing what is above it. Up
    /// used to finish only the current activity, so an activity started
    /// between the parent and it came back instead.
    @Test
    public void upReturnsToTheParentBeneath() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity parent = start(app, AndroidTestSupport.TestActivity.class);
                Activity middle = start(parent, AndroidTestSupport.UiModeHandlingActivity.class);
                Activity child = start(middle, AndroidTestSupport.ChildActivity.class);
                assertEquals(AndroidTestSupport.TestActivity.class.getName(),
                        child.getParentActivityIntent().getComponent().getClassName());
                assertTrue(child.onNavigateUp());
                assertTrue(child.isFinishing());
                Activity top = ActivityThread.getTopActivity();
                assertEquals(AndroidTestSupport.TestActivity.class, top.getClass());
                assertEquals(1, ActivityThread.getActivityCount());
            }
        });
    }

    /// Entered directly, as from a deep link, Up creates the parent in its
    /// place. It used to finish the activity and leave the application.
    @Test
    public void upFromADeepLinkCreatesTheParent() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity child = start(app, AndroidTestSupport.ChildActivity.class);
                assertTrue(child.shouldUpRecreateTask(child.getParentActivityIntent()));
                assertTrue(child.onNavigateUp());
                assertTrue(child.isFinishing());
                Activity top = ActivityThread.getTopActivity();
                assertNotSame(child, top);
                assertEquals(AndroidTestSupport.TestActivity.class, top.getClass());
                assertEquals(1, ActivityThread.getActivityCount());
            }
        });
    }

    /// With no parent declared there is nowhere up to go: false, and the
    /// activity stays. It used to finish and report success.
    @Test
    public void upWithoutAParentDoesNothing() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity root = start(app, AndroidTestSupport.TestActivity.class);
                Activity top = start(root, AndroidTestSupport.TestActivity.class);
                assertNull(top.getParentActivityIntent());
                assertFalse(top.onNavigateUp());
                assertFalse(top.isFinishing());
                assertSame(top, ActivityThread.getTopActivity());
            }
        });
    }

    /// navigateUpTo a class that is not beneath finishes this activity alone
    /// and answers false, as Android's activity manager does.
    @Test
    public void navigateUpToAnAbsentParentFinishesAndAnswersFalse() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity root = start(app, AndroidTestSupport.TestActivity.class);
                Activity top = start(root, AndroidTestSupport.UiModeHandlingActivity.class);
                assertFalse(top.navigateUpTo(new Intent(top, AndroidTestSupport.ChildActivity.class)));
                assertTrue(top.isFinishing());
                assertSame(root, ActivityThread.getTopActivity());
            }
        });
    }

    /// AppCompat's Up follows the same parent.
    @Test
    public void supportUpFollowsTheParent() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity parent = start(app, AndroidTestSupport.TestActivity.class);
                Activity middle = start(parent, AndroidTestSupport.UiModeHandlingActivity.class);
                androidx.appcompat.app.AppCompatActivity child = (androidx.appcompat.app.AppCompatActivity)
                        start(middle, AndroidTestSupport.CompatChildActivity.class);
                assertTrue(child.onSupportNavigateUp());
                assertEquals(1, ActivityThread.getActivityCount());
                assertEquals(AndroidTestSupport.TestActivity.class, ActivityThread.getTopActivity().getClass());
            }
        });
    }

    /// A single-top activity on top asking for itself gets false and no
    /// `onNewIntent`. It used to receive the intent and report a launch.
    @Test
    public void startActivityIfNeededLeavesASingleTopSelfLaunchToTheCaller() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                AndroidTestSupport.SingleTopActivity a = (AndroidTestSupport.SingleTopActivity)
                        start(app, AndroidTestSupport.SingleTopActivity.class);
                assertFalse(a.startActivityIfNeeded(new Intent(a, AndroidTestSupport.SingleTopActivity.class), -1));
                assertEquals(0, a.newIntents);
                assertEquals(1, ActivityThread.getActivityCount());
                // Another activity is needed: started, and true.
                assertTrue(a.startActivityIfNeeded(new Intent(a, AndroidTestSupport.TestActivity.class), -1));
                assertEquals(2, ActivityThread.getActivityCount());
            }
        });
    }
}
