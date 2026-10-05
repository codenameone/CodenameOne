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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/// A recreated activity is destroyed with `isFinishing()` false and
/// `isChangingConfigurations()` true, as on Android, so cleanup guarded by
/// `isFinishing()` does not run for a configuration replacement. Relaunch
/// used to go through the finishing destroy path.
public class RecreateNotFinishingTest {

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void recreateIsNotFinishing() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Activity[] seen = new Activity[2];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    app.startActivity(new Intent(app, AndroidTestSupport.TestActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    seen[0] = ActivityThread.getTopActivity();
                    seen[0].recreate();
                    seen[1] = ActivityThread.getTopActivity();
                    seen[1].finish();
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    ActivityThread.finishAllActivities();
                }
            }
        });
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
        AndroidTestSupport.TestActivity old = (AndroidTestSupport.TestActivity) seen[0];
        assertNotSame(seen[0], seen[1]);
        assertTrue(old.isDestroyed());
        assertTrue(old.isChangingConfigurations());
        assertEquals(Boolean.FALSE, old.finishingWhenDestroyed);
        assertEquals("a real finish still reports finishing", Boolean.TRUE,
                ((AndroidTestSupport.TestActivity) seen[1]).finishingWhenDestroyed);
    }
}
