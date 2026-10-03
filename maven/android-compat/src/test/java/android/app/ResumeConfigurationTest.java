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
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

/// Dark mode (or the locale) changed while the application was in the
/// background: nothing resizes on the way back, so the change used to go
/// unnoticed until an unrelated resize. An activity that does not handle the
/// change is recreated when the application returns, as on Android.
public class ResumeConfigurationTest {

    @Test
    public void aChangeMadeInTheBackgroundRecreatesTheActivityOnReturn() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Activity[] before = new Activity[1];
        final Activity[] after = new Activity[1];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    before[0] = ActivityThread.getTopActivity();
                    ActivityThread.onAppStop();
                    HeadlessImplementation.darkMode = true;
                    ActivityThread.onAppStart();
                    after[0] = ActivityThread.getTopActivity();
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    HeadlessImplementation.darkMode = false;
                    ActivityThread.finishAllActivities();
                }
            }
        });
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
        assertNotNull(before[0]);
        assertNotNull(after[0]);
        assertNotSame("the activity kept running with the old configuration", before[0], after[0]);
    }
}
