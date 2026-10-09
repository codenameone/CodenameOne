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
import com.codename1.ui.Display;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/// A finishing activity is not asked to save its state, as on Android: it is
/// never restored. An activity stopped under another one still saves. The
/// save used to run on every stop, finishing or not.
public class FinishSkipsSaveStateTest {

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void finishDoesNotSave() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final List<Activity> saved = new ArrayList<Activity>();
        final Activity[] seen = new Activity[2];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                Application.ActivityLifecycleCallbacks cb = new Application.ActivityLifecycleCallbacks() {
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    }

                    public void onActivityStarted(Activity activity) {
                    }

                    public void onActivityResumed(Activity activity) {
                    }

                    public void onActivityPaused(Activity activity) {
                    }

                    public void onActivityStopped(Activity activity) {
                    }

                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                        saved.add(activity);
                    }

                    public void onActivityDestroyed(Activity activity) {
                    }
                };
                Application application = (Application) app;
                application.registerActivityLifecycleCallbacks(cb);
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    seen[0] = ActivityThread.getTopActivity();
                    app.startActivity(intent);
                    seen[1] = ActivityThread.getTopActivity();
                    seen[1].finish();
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    application.unregisterActivityLifecycleCallbacks(cb);
                    ActivityThread.finishAllActivities();
                }
            }
        });
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
        assertNotSame(seen[0], seen[1]);
        // Only the first activity, stopped when the second covered it.
        assertEquals(1, saved.size());
        assertEquals(seen[0], saved.get(0));
    }
}
