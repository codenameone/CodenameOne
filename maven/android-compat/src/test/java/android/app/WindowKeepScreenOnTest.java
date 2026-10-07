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
import android.view.WindowManager;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
public class WindowKeepScreenOnTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void clearingBackgroundingAndFinishingReleaseTheWindowRequest() {
        Context c = AndroidTestSupport.context().getApplicationContext();
        c.startActivity(new Intent(c,AndroidTestSupport.TestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Activity a = ActivityThread.getTopActivity();
        int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        try {
            a.getWindow().addFlags(flag); assertTrue(HeadlessImplementation.screenLocked);
            android.view.View child = new android.view.View(a);
            child.dispatchAttachedToWindow(true);
            child.setKeepScreenOn(true);
            try {
                a.getWindow().clearFlags(flag);
                assertTrue("the view still holds its own request", HeadlessImplementation.screenLocked);
            } finally {
                child.dispatchAttachedToWindow(false);
            }
            assertFalse(HeadlessImplementation.screenLocked);
            a.getWindow().addFlags(flag); ActivityThread.onAppStop();
            assertFalse(HeadlessImplementation.screenLocked);
            a.getWindow().addFlags(flag); assertFalse(HeadlessImplementation.screenLocked);
            ActivityThread.onAppStart(); assertTrue(HeadlessImplementation.screenLocked);
            a.finish(); assertFalse(HeadlessImplementation.screenLocked);
        } finally { a.finish(); ActivityThread.onAppStart(); }
    }
}
