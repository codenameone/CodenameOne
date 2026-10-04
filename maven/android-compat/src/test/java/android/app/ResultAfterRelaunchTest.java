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

import com.codename1.androidcompat.runtime.ResourceManager;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/// The configuration changed while a child started for a result was on top:
/// the child handles the change, its caller does not and waits below to be
/// recreated. Finishing the child recreates the caller, and the result must
/// reach the new instance -- it used to be compared with the destroyed one
/// and dropped.
public class ResultAfterRelaunchTest {

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void theResultReachesTheRecreatedCaller() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Activity[] seen = new Activity[2];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    Activity caller = ActivityThread.getTopActivity();
                    seen[0] = caller;
                    caller.startActivityForResult(
                            new Intent(caller, AndroidTestSupport.UiModeHandlingActivity.class), 7);
                    Activity child = ActivityThread.getTopActivity();
                    assertTrue(child instanceof AndroidTestSupport.UiModeHandlingActivity);

                    HeadlessImplementation.darkMode = true;
                    ResourceManager rm = ResourceManager.get();
                    assertTrue("dark mode did not change the configuration", rm.refresh());
                    ActivityThread.onConfigurationChanged(rm.configuration());
                    assertTrue("the child handles uiMode and must stay", child == ActivityThread.getTopActivity());

                    child.setResult(Activity.RESULT_OK);
                    child.finish();
                    seen[1] = ActivityThread.getTopActivity();
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    HeadlessImplementation.darkMode = false;
                    ResourceManager.get().refresh();
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
        assertTrue(seen[1] instanceof AndroidTestSupport.TestActivity);
        assertNotSame("the caller was not recreated for the change it does not handle", seen[0], seen[1]);
        AndroidTestSupport.TestActivity fresh = (AndroidTestSupport.TestActivity) seen[1];
        assertEquals("onActivityResult never reached the recreated caller", 7, fresh.resultRequestCode);
        assertEquals(Activity.RESULT_OK, fresh.resultCode);
    }
}
