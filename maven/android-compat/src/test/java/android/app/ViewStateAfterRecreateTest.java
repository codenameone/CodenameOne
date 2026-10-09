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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/// A recreated activity gets back the state of every view with an id --
/// the text typed into an `EditText`, a checked `CheckBox` -- as on Android.
/// The view hierarchy used to be neither saved nor restored, so a rotation
/// silently discarded what the user had entered.
public class ViewStateAfterRecreateTest {

    private static final int EDIT_ID = 0x7f0b0001;
    private static final int CHECK_ID = 0x7f0b0002;
    private static final int LABEL_ID = 0x7f0b0003;

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void typedTextAndCheckedStateSurviveRecreation() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final Activity[] seen = new Activity[2];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                AndroidTestSupport.TestActivity.content = new AndroidTestSupport.Content() {
                    @Override
                    public View create(Activity a) {
                        LinearLayout root = new LinearLayout(a);
                        EditText edit = new EditText(a);
                        edit.setId(EDIT_ID);
                        root.addView(edit);
                        CheckBox check = new CheckBox(a);
                        check.setId(CHECK_ID);
                        root.addView(check);
                        TextView label = new TextView(a);
                        label.setId(LABEL_ID);
                        label.setText("initial");
                        root.addView(label);
                        return root;
                    }
                };
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    seen[0] = ActivityThread.getTopActivity();
                    ((EditText) seen[0].findViewById(EDIT_ID)).setText("typed by the user");
                    ((CheckBox) seen[0].findViewById(CHECK_ID)).setChecked(true);
                    ((TextView) seen[0].findViewById(LABEL_ID)).setText("changed");
                    seen[0].recreate();
                    seen[1] = ActivityThread.getTopActivity();
                    assertNotSame(seen[0], seen[1]);
                    assertEquals("typed by the user",
                            ((EditText) seen[1].findViewById(EDIT_ID)).getText().toString());
                    assertTrue(((CheckBox) seen[1].findViewById(CHECK_ID)).isChecked());
                    // A plain TextView does not freeze its text, as on Android.
                    assertEquals("initial", ((TextView) seen[1].findViewById(LABEL_ID)).getText().toString());
                    // Nothing leaks into an activity that never saved any.
                    seen[1].finish();
                    app.startActivity(intent);
                    Activity third = ActivityThread.getTopActivity();
                    assertEquals("", ((EditText) third.findViewById(EDIT_ID)).getText().toString());
                    assertFalse(((CheckBox) third.findViewById(CHECK_ID)).isChecked());
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    AndroidTestSupport.TestActivity.content = null;
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
