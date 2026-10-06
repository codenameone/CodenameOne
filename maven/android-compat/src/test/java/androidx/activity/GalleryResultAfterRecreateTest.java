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
package androidx.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import org.junit.Before;
import org.junit.Test;

/// A gallery result reaches the activity that replaced the one that opened
/// the gallery, when it was recreated (rotated) while the gallery was open.
/// It used to reach the destroyed instance's callback, and the replacement,
/// whose views are the ones on screen, never heard it.
public class GalleryResultAfterRecreateTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
        HeadlessImplementation.gallery = null;
    }

    @Test
    public void theRecreatedActivityReceivesTheGalleryResult() {
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Context app = AndroidTestSupport.context().getApplicationContext();
                    Intent intent = new Intent(app, AndroidTestSupport.GalleryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    AndroidTestSupport.GalleryActivity first =
                            (AndroidTestSupport.GalleryActivity) ActivityThread.getTopActivity();
                    first.launcher.launch("image/*");
                    ActionListener picker = HeadlessImplementation.gallery;
                    assertNotNull("the launch did not open the gallery", picker);
                    first.recreate();
                    Activity top = ActivityThread.getTopActivity();
                    assertTrue("recreate() kept the old instance", top != first);
                    AndroidTestSupport.GalleryActivity fresh = (AndroidTestSupport.GalleryActivity) top;
                    picker.actionPerformed(new ActionEvent("file:///a.png"));
                    assertEquals("the destroyed instance heard the result", 0, first.results.size());
                    assertEquals("the recreated instance missed the result", 1, fresh.results.size());
                    assertEquals("file:///a.png", String.valueOf(fresh.results.get(0)));
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    HeadlessImplementation.gallery = null;
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
}
