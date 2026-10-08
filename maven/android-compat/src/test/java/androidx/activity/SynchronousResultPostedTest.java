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
import static org.junit.Assert.assertNotSame;

import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/// A contract that answers synchronously -- a permission already granted --
/// reaches its callback from the main queue after `launch()` returns, as in
/// AndroidX, not from inside the caller's own `launch()` call.
public class SynchronousResultPostedTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// With no display a main-looper post runs inline, which would hide the
    /// ordering under test; this starts the display and its queue.
    @BeforeClass
    public static void startDisplay() {
        AndroidTestSupport.context();
    }

    private static final class Immediate extends ActivityResultContract<String, String> {
        @Override
        public Intent createIntent(Context context, String input) {
            throw new AssertionError("a synchronous result needs no intent");
        }

        @Override
        public String parseResult(int resultCode, Intent intent) {
            throw new AssertionError("a synchronous result is not parsed");
        }

        @Override
        public SynchronousResult<String> getSynchronousResult(Context context, String input) {
            return new SynchronousResult<String>(input);
        }
    }

    @Test
    public void theCallbackRunsAfterLaunchReturns() {
        final StringBuilder events = new StringBuilder();
        ComponentActivity a = new ComponentActivity();
        ActivityResultLauncher<String> launcher = a.registerForActivityResult(new Immediate(),
                new ActivityResultCallback<String>() {
                    @Override
                    public void onActivityResult(String result) {
                        events.append("result:").append(result).append(';');
                    }
                });
        launcher.launch("x");
        events.append("returned;");
        MainThreadRule.drain();
        assertEquals("the callback ran inside launch()", "returned;result:x;", events.toString());

        // Unregistered before the posted result runs: it is dropped.
        launcher.launch("y");
        launcher.unregister();
        MainThreadRule.drain();
        assertEquals("returned;result:x;", events.toString());
    }

    @Test
    public void aPostedResultReachesTheRecreatedRegistrationAndNotTheOldInstance() {
        Context app = AndroidTestSupport.context().getApplicationContext();
        app.startActivity(new Intent(app, AndroidTestSupport.GalleryActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        AndroidTestSupport.GalleryActivity old =
                (AndroidTestSupport.GalleryActivity) ActivityThread.getTopActivity();
        try {
            old.immediateLauncher.launch("before-recreate");
            old.recreate();
            AndroidTestSupport.GalleryActivity fresh =
                    (AndroidTestSupport.GalleryActivity) ActivityThread.getTopActivity();
            assertNotSame(old, fresh);
            MainThreadRule.drain();
            assertEquals(0, old.immediateResults.size());
            assertEquals(1, fresh.immediateResults.size());
            assertEquals("before-recreate", fresh.immediateResults.get(0));

            fresh.immediateLauncher.launch("after-finish");
            fresh.finish();
            MainThreadRule.drain();
            assertEquals(1, fresh.immediateResults.size());
        } finally {
            ActivityThread.finishAllActivities();
        }
    }
}
