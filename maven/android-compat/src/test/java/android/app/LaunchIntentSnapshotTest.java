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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// A launched activity owns a copy of its intent, as on Android where the
/// launch crosses a process boundary: the caller reusing or changing its
/// Intent afterwards does not reach getIntent(). The activity used to keep
/// the caller's object.
public class LaunchIntentSnapshotTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void mutatingTheIntentAfterTheLaunchDoesNotReachTheActivity() {
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Context app = AndroidTestSupport.context().getApplicationContext();
                    Intent i = new Intent(app, AndroidTestSupport.TestActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("n", 1);
                    android.os.Bundle nested = new android.os.Bundle();
                    int[] values = {1, 2};
                    java.util.ArrayList<String> names = new java.util.ArrayList<String>();
                    names.add("original");
                    nested.putIntArray("values", values);
                    i.putExtra("nested", nested);
                    i.putStringArrayListExtra("names", names);
                    app.startActivity(i);
                    values[0] = 99;
                    names.set(0, "changed");
                    Activity a = ActivityThread.getTopActivity();
                    i.putExtra("n", 2);
                    i.setAction("changed");
                    assertEquals(1, a.getIntent().getIntExtra("n", 0));
                    assertNull(a.getIntent().getAction());
                    assertEquals(1, a.getIntent().getBundleExtra("nested").getIntArray("values")[0]);
                    assertEquals("original", a.getIntent().getStringArrayListExtra("names").get(0));
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
}
