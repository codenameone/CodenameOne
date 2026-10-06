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

    /// The default down only tracks the key and back happens on the up, as
    /// on Android, so an activity that overrides only `onKeyUp` sees a
    /// tracked up. The default down used to finish the activity at once and
    /// the up was never sent.
    @Test
    public void anOnKeyUpOverrideSeesTheTrackedUpOfTheDefaultDown() {
        final StringBuilder events = new StringBuilder();
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                AndroidTestSupport.TestActivity.keys = new AndroidTestSupport.KeyHandler() {
                    @Override
                    public boolean onKey(Activity activity, KeyEvent event) {
                        AndroidTestSupport.TestActivity t = (AndroidTestSupport.TestActivity) activity;
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            return t.defaultKeyDown(event);
                        }
                        events.append("up tracking=" + event.isTracking() + " finishing=" + t.isFinishing());
                        return true;
                    }
                };
                Activity root = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                Activity a = start(root, 0);
                ActivityThread.onBack(a.mRecord);
                assertFalse("the up handler consumed back", a.isFinishing());
            }
        });
        assertEquals("up tracking=true finishing=false", events.toString());
    }

    /// Claiming the down without tracking it still blocks back: the default
    /// up acts only on a tracked key.
    @Test
    public void aDownClaimedWithoutTrackingBlocksTheDefaultUp() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                AndroidTestSupport.TestActivity.keys = new AndroidTestSupport.KeyHandler() {
                    @Override
                    public boolean onKey(Activity activity, KeyEvent event) {
                        AndroidTestSupport.TestActivity t = (AndroidTestSupport.TestActivity) activity;
                        return event.getAction() == KeyEvent.ACTION_DOWN || t.defaultKeyUp(event);
                    }
                };
                Activity root = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                Activity a = start(root, 0);
                ActivityThread.onBack(a.mRecord);
                assertFalse("a claimed down did not block back", a.isFinishing());
            }
        });
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

    /// `android:noHistory="true"` in the manifest means the same as the
    /// launch flag. Only the flag used to count, so the activity came back.
    @Test
    public void aManifestNoHistoryActivityIsFinishedWhenCovered() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                Activity root = start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                root.startActivity(new Intent(root, AndroidTestSupport.NoHistoryActivity.class));
                Activity noHistory = ActivityThread.getTopActivity();
                assertTrue(noHistory instanceof AndroidTestSupport.NoHistoryActivity);
                Activity top = start(noHistory, 0);
                assertTrue("the covered manifest no-history activity was kept", noHistory.isFinishing());
                assertEquals(2, ActivityThread.getActivityCount());
                top.finish();
                assertSame(root, ActivityThread.getTopActivity());
            }
        });
    }

    /// A no-history activity started for a result still answers when it is
    /// finished by being covered: RESULT_CANCELED, as Android's no-history
    /// stop path finishes it. The caller used to never hear back.
    @Test
    public void aCoveredNoHistoryActivityStillReturnsItsResult() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                AndroidTestSupport.TestActivity root =
                        (AndroidTestSupport.TestActivity) start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent i = new Intent(root, AndroidTestSupport.TestActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                root.startActivityForResult(i, 7);
                Activity noHistory = ActivityThread.getTopActivity();
                Activity top = start(noHistory, 0);
                assertTrue(noHistory.isFinishing());
                assertEquals("delivered before the caller resumed", -1, root.resultRequestCode);
                top.finish();
                assertSame(root, ActivityThread.getTopActivity());
                assertEquals(7, root.resultRequestCode);
                assertEquals(Activity.RESULT_CANCELED, root.resultCode);
            }
        });
    }

    /// The same when it is covered by an activity brought forward with
    /// REORDER_TO_FRONT: the caller it owes a result is the one brought
    /// forward, and hears RESULT_CANCELED at once.
    @Test
    public void aNoHistoryActivityCoveredByAReorderStillReturnsItsResult() {
        onEdt(new StackWork() {
            @Override
            public void run(Context app) {
                AndroidTestSupport.TestActivity root =
                        (AndroidTestSupport.TestActivity) start(app, Intent.FLAG_ACTIVITY_NEW_TASK);
                root.startActivityForResult(new Intent(root, AndroidTestSupport.NoHistoryActivity.class), 9);
                Activity noHistory = ActivityThread.getTopActivity();
                start(noHistory, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                assertSame(root, ActivityThread.getTopActivity());
                assertTrue(noHistory.isFinishing());
                assertEquals(9, root.resultRequestCode);
                assertEquals(Activity.RESULT_CANCELED, root.resultCode);
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
