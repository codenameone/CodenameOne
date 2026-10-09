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
import static org.junit.Assert.assertSame;

/// A child started for a result finishes on top of its stopped caller. The
/// caller is restarted (`onRestart`, `onStart`) before `onActivityResult`,
/// as on Android; the result used to be delivered while it was still stopped.
public class ResultAfterCallerStartsTest {

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void theCallerIsStartedBeforeItsResult() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final AndroidTestSupport.TestActivity[] caller = new AndroidTestSupport.TestActivity[1];
        final Activity[] topAfter = new Activity[1];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    caller[0] = (AndroidTestSupport.TestActivity) ActivityThread.getTopActivity();
                    caller[0].startActivityForResult(new Intent(caller[0], AndroidTestSupport.TestActivity.class), 5);
                    Activity child = ActivityThread.getTopActivity();
                    child.setResult(Activity.RESULT_OK);
                    child.finish();
                    topAfter[0] = ActivityThread.getTopActivity();
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
        assertSame(caller[0], topAfter[0]);
        assertEquals(5, caller[0].resultRequestCode);
        assertEquals(Activity.RESULT_OK, caller[0].resultCode);
        assertEquals("the caller was stopped when it was covered and must be restarted before its result",
                2, caller[0].startsBeforeResult);
    }
}
