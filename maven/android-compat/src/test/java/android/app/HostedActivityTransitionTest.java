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
import com.codename1.ui.Form;
import com.codename1.ui.animations.CommonTransitions;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// Activities started by a Codename One application while a form transition
/// runs. `Display.getCurrent()` keeps answering the form being left until the
/// transition ends, which the Android theme's slide makes the normal case on a
/// device: a screenshot test bound its timer to the departing form and never
/// fired, and the runtime attached frame callbacks, overlays and popups there.
public class HostedActivityTransitionTest {

    /// Runs `r` on the EDT and rethrows what it threw here: a failure left on
    /// the EDT never reaches JUnit, and the test would hang instead.
    private static void onEdt(final Runnable r) {
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    failure[0] = t;
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

    private static void awaitTransitionEnd() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        final boolean[] busy = {true};
        while (busy[0] && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            onEdt(new Runnable() {
                @Override
                public void run() {
                    busy[0] = Display.getInstance().isInTransition();
                }
            });
        }
        assertTrue("the transition never ended", !busy[0]);
    }

    private static Form activityForm(Activity a) {
        return a.getWindow().getDecorView().getPeer().getComponentForm();
    }

    private static Activity start(Context app) {
        Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(intent);
        return ActivityThread.getTopActivity();
    }

    @Test
    public void theArrivingActivityFormIsVisibleAndTheHostIsReturnedToTwice() throws Exception {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Form host = new Form("Host");
        onEdt(new Runnable() {
            @Override
            public void run() {
                host.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 300));
                host.show();
            }
        });
        awaitTransitionEnd();

        final Activity[] first = new Activity[1];
        final Object[] seen = new Object[3];
        onEdt(new Runnable() {
            @Override
            public void run() {
                first[0] = start(app);
                seen[0] = Display.getInstance().isInTransition() ? Boolean.TRUE : Boolean.FALSE;
                seen[1] = Display.getInstance().getCurrent();
                seen[2] = ActivityThread.visibleForm();
            }
        });
        awaitTransitionEnd();
        assertNotNull(first[0]);
        assertSame("the host's slide-out must still be running", Boolean.TRUE, seen[0]);
        assertSame("Display still answers the departing form", host, seen[1]);
        assertSame(activityForm(first[0]), seen[2]);

        // The next activity starts while the way back to the host is still
        // sliding: the departing activity form is current then, and finishing
        // the new activity must still return to the host.
        onEdt(new Runnable() {
            @Override
            public void run() {
                activityForm(first[0]).setTransitionOutAnimator(
                        CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 300));
                ActivityThread.finishAllActivities();
                start(app);
            }
        });
        awaitTransitionEnd();
        onEdt(new Runnable() {
            @Override
            public void run() {
                ActivityThread.finishAllActivities();
            }
        });
        awaitTransitionEnd();
        final Form[] end = new Form[1];
        onEdt(new Runnable() {
            @Override
            public void run() {
                end[0] = Display.getInstance().getCurrent();
            }
        });
        assertSame(host, end[0]);
    }
}
