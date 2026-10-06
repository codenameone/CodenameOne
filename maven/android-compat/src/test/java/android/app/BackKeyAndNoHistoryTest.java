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
import android.view.KeyEvent;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The back key and the activity stack: the back key reaches a handler as
/// both its down and its up, and an activity launched without history is
/// gone once another covers it.
public class BackKeyAndNoHistoryTest {

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
                    AndroidTestSupport.TestActivity.keys = null;
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

    private static Activity start(Context from, int flags) {
        Intent intent = new Intent(from, AndroidTestSupport.TestActivity.class);
        intent.addFlags(flags);
        from.startActivity(intent);
        return ActivityThread.getTopActivity();
    }

    /// The usual pattern: claim the down, act on the up. The up used to be
    /// dropped, so the handler never ran.
    @Test
    public void theBackKeyUpReachesAHandlerThatClaimedTheDown() {
        final StringBuilder events = new StringBuilder();
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                AndroidTestSupport.TestActivity.keys = new AndroidTestSupport.KeyHandler() {
                    @Override
                    public boolean onKey(Activity activity, KeyEvent event) {
                        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                            return false;
                        }
                        events.append(event.getAction() == KeyEvent.ACTION_DOWN ? "down;" : "up;");
                        return true;
                    }
                };
                Activity a = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                ActivityThread.onBack(a.mRecord);
                assertFalse("a consumed back key finished the activity", a.isFinishing());
            }
        });
        assertEquals("down;up;", events.toString());
    }

    @Test
    public void anUnhandledBackKeyStillFinishes() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity root = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                Activity top = start(root, 0);
                ActivityThread.onBack(top.mRecord);
                assertTrue(top.isFinishing());
                assertSame(root, ActivityThread.getTopActivity());
            }
        });
    }

    /// Finishing the activity started from a no-history one returns to the
    /// activity beneath it. The no-history one used to stay on the stack and
    /// come back.
    @Test
    public void aNoHistoryActivityIsFinishedWhenCovered() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity root = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                Activity noHistory = start(root, Intent.FLAG_ACTIVITY_NO_HISTORY);
                assertFalse(noHistory.isFinishing());
                Activity top = start(noHistory, 0);
                assertTrue("the covered no-history activity was kept", noHistory.isFinishing());
                assertEquals(2, ActivityThread.getActivityCount());
                top.finish();
                assertSame(root, ActivityThread.getTopActivity());
            }
        });
    }

    /// A no-history activity recreated for a configuration change is still
    /// no-history: the replacement used to drop the flag and come back once
    /// the activity it started finished.
    @Test
    public void aRecreatedNoHistoryActivityKeepsItsFlag() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity root = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                Activity noHistory = start(root, Intent.FLAG_ACTIVITY_NO_HISTORY);
                noHistory.recreate();
                Activity fresh = ActivityThread.getTopActivity();
                assertTrue("recreate() kept the old instance", fresh != noHistory);
                Activity top = start(fresh, 0);
                assertTrue("the recreated no-history activity was kept", fresh.isFinishing());
                assertEquals(2, ActivityThread.getActivityCount());
                top.finish();
                assertSame(root, ActivityThread.getTopActivity());
            }
        });
    }
}
