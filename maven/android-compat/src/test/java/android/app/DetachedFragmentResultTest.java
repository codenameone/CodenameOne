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

/// A fragment that started an activity for a result and was detached while
/// it ran still gets the result -- a detached fragment is still active, as
/// Android finds it by its active-fragment id -- while one removed for good
/// does not. The detached fragment's result used to be dropped.
public class DetachedFragmentResultTest {

    /// Records the result it receives.
    public static final class Asker extends Fragment {
        int requestCode = -1;
        int resultCode;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            this.requestCode = requestCode;
            this.resultCode = resultCode;
        }
    }

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void aDetachedFragmentGetsItsResultAndARemovedOneDoesNot() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Asker detached = new Asker();
        final Asker removed = new Asker();
        final AndroidTestSupport.TestActivity[] caller = new AndroidTestSupport.TestActivity[1];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    caller[0] = (AndroidTestSupport.TestActivity) ActivityThread.getTopActivity();
                    FragmentManager fm = caller[0].getFragmentManager();
                    fm.beginTransaction().add(detached, "detached").add(removed, "removed").commitNow();

                    detached.startActivityForResult(
                            new Intent(caller[0], AndroidTestSupport.UiModeHandlingActivity.class), 5);
                    Activity child = ActivityThread.getTopActivity();
                    fm.beginTransaction().detach(detached).commitNowAllowingStateLoss();
                    child.setResult(Activity.RESULT_OK);
                    child.finish();

                    removed.startActivityForResult(
                            new Intent(caller[0], AndroidTestSupport.UiModeHandlingActivity.class), 6);
                    child = ActivityThread.getTopActivity();
                    fm.beginTransaction().remove(removed).commitNowAllowingStateLoss();
                    child.setResult(Activity.RESULT_OK);
                    child.finish();
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
        assertEquals("the detached fragment's result was dropped", 5, detached.requestCode);
        assertEquals(Activity.RESULT_OK, detached.resultCode);
        assertEquals("a removed fragment got a result", -1, removed.requestCode);
        assertEquals("a fragment's result reached the activity", -1, caller[0].resultRequestCode);
    }
}
