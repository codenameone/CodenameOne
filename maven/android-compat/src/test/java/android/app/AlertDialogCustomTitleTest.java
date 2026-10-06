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
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/// `Builder.setCustomTitle` puts the supplied view at the top of the dialog
/// in place of the ordinary title row. The builder used to drop the view, so
/// the dialog (and AppCompat's, which delegates to it) showed no title.
public class AlertDialogCustomTitleTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void customTitleIsTheFirstRow() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final View[] firstRow = new View[1];
        final View[] header = new View[1];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    Activity activity = ActivityThread.getTopActivity();
                    TextView custom = new TextView(activity);
                    custom.setText("custom header");
                    header[0] = custom;
                    AlertDialog dialog = new AlertDialog.Builder(activity)
                            .setTitle("plain title")
                            .setCustomTitle(custom)
                            .setMessage("body")
                            .setPositiveButton("OK", (DialogInterface.OnClickListener) null)
                            .create();
                    dialog.show();
                    ViewGroup root = (ViewGroup) dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            .getParent().getParent();
                    firstRow[0] = root.getChildAt(0);
                    dialog.dismiss();
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
        assertSame(header[0], firstRow[0]);
    }
}
