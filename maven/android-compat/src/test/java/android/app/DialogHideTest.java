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
import android.widget.TextView;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A hidden dialog is not showing, as on Android, and showing it again
/// makes it so; `dismiss()` still closes a hidden one.
public class DialogHideTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void aHiddenDialogIsNotShowing() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final StringBuilder states = new StringBuilder();
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    Activity activity = ActivityThread.getTopActivity();
                    Dialog dialog = new Dialog(activity);
                    TextView text = new TextView(activity);
                    text.setText("Content");
                    dialog.setContentView(text);
                    states.append(dialog.isShowing()).append(';');
                    dialog.show();
                    states.append(dialog.isShowing()).append(';');
                    dialog.hide();
                    states.append(dialog.isShowing()).append(';');
                    dialog.show();
                    states.append(dialog.isShowing()).append(';');
                    dialog.hide();
                    dialog.dismiss();
                    states.append(dialog.host() == null);
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
        assertEquals("false;true;false;true;true", states.toString());
    }
}
