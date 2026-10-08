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
import android.content.Intent;
import android.view.View;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.ui.Display;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ProcessLifecycleInitializationTest {
    @Test public void firstAccessInOnCreateTracksBackgroundAndForeground() throws Throwable {
        AndroidTestSupport.cleanDisplay();
        final Throwable[] error = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                Application app = AndroidRuntime.getInstance().getApplication();
                Application.ActivityLifecycleCallbacks[] before = app.callbacks();
                Field singleton = null;
                Object previous = null;
                try {
                    singleton = ProcessLifecycleOwner.class.getDeclaredField("sInstance");
                    singleton.setAccessible(true);
                    previous = singleton.get(null);
                    singleton.set(null, null);
                    final LifecycleOwner[] owner = new LifecycleOwner[1];
                    final Lifecycle.State[] initial = new Lifecycle.State[1];
                    AndroidTestSupport.TestActivity.content = new AndroidTestSupport.Content() {
                        public View create(Activity activity) {
                            owner[0] = ProcessLifecycleOwner.get();
                            initial[0] = owner[0].getLifecycle().getCurrentState();
                            return new View(activity);
                        }
                    };
                    app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    assertEquals(Lifecycle.State.CREATED, initial[0]);
                    assertEquals(Lifecycle.State.RESUMED, owner[0].getLifecycle().getCurrentState());
                    ActivityThread.onAppStop();
                    assertEquals(Lifecycle.State.CREATED, owner[0].getLifecycle().getCurrentState());
                    ActivityThread.onAppStart();
                    assertEquals(Lifecycle.State.RESUMED, owner[0].getLifecycle().getCurrentState());
                    ActivityThread.onAppStop();
                    assertEquals(Lifecycle.State.CREATED, owner[0].getLifecycle().getCurrentState());
                } catch (Throwable failure) {
                    error[0] = failure;
                } finally {
                    AndroidTestSupport.TestActivity.content = null;
                    ActivityThread.finishAllActivities();
                    ActivityThread.onAppStart();
                    for (Application.ActivityLifecycleCallbacks cb : app.callbacks()) {
                        if (!Arrays.asList(before).contains(cb)) app.unregisterActivityLifecycleCallbacks(cb);
                    }
                    if (singleton != null) {
                        try { singleton.set(null, previous); } catch (Exception e) { error[0] = e; }
                    }
                }
            }
        });
        if (error[0] != null) throw error[0];
    }
}
