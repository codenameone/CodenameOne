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
import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Two things an activity below the top must still get: the result of a
/// child it started that finished off the top of the stack, and the loss of
/// window focus in its views.
public class OffStackResultAndFocusTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    /// Runs `body` on the EDT, where activities run, and rethrows its failure.
    private static void onEdt(final Runnable body) {
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    body.run();
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
    }

    private static AndroidTestSupport.TestActivity startRoot() {
        Context app = AndroidTestSupport.context().getApplicationContext();
        Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(intent);
        return (AndroidTestSupport.TestActivity) ActivityThread.getTopActivity();
    }

    /// A starts B for a result, B starts C, and A finishes B with
    /// finishActivity. B's result was offered to C, refused as not its
    /// caller and lost; it must reach A when A comes back.
    @Test
    public void aChildFinishedBelowTheTopDeliversToItsCaller() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                AndroidTestSupport.TestActivity a = startRoot();
                a.startActivityForResult(new Intent(a, AndroidTestSupport.TestActivity.class), 5);
                Activity b = ActivityThread.getTopActivity();
                b.setResult(Activity.RESULT_OK);
                b.startActivity(new Intent(b, AndroidTestSupport.TestActivity.class));
                Activity c = ActivityThread.getTopActivity();
                a.finishActivity(5);
                assertTrue(b.isFinishing());
                assertEquals("delivered to a stopped caller", -1, a.resultRequestCode);
                c.finish();
                assertTrue(ActivityThread.getTopActivity() == a);
                assertEquals("B's result never reached A", 5, a.resultRequestCode);
                assertEquals(Activity.RESULT_OK, a.resultCode);
            }
        });
    }

    private static final class FocusView extends View {
        final List<Boolean> calls = new ArrayList<Boolean>();

        FocusView(Context c) {
            super(c);
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            calls.add(Boolean.valueOf(hasWindowFocus));
        }
    }

    /// A covered activity's views are told they lost window focus and say
    /// so; they used to keep answering true from their attachment alone.
    @Test
    public void coveredActivityViewsLoseWindowFocus() {
        onEdt(new Runnable() {
            @Override
            public void run() {
                AndroidTestSupport.TestActivity a = startRoot();
                android.widget.FrameLayout root = new android.widget.FrameLayout(a);
                FocusView v = new FocusView(a);
                root.addView(v);
                a.setContentView(root);
                assertTrue(v.hasWindowFocus());
                a.startActivity(new Intent(a, AndroidTestSupport.TestActivity.class));
                assertFalse("a covered activity's view still has window focus", v.hasWindowFocus());
                assertTrue(v.calls.toString(), v.calls.contains(Boolean.FALSE));
                ActivityThread.getTopActivity().finish();
                assertTrue(v.hasWindowFocus());
                assertEquals(Boolean.TRUE, v.calls.get(v.calls.size() - 1));
            }
        });
    }
}
