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
import android.widget.Button;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Component;
import com.codename1.ui.Display;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/// A message taller than the screen scrolls, and the buttons stay on screen.
/// The dialog had no height limit and its message did not scroll, so a few
/// paragraphs pushed the button bar below the screen and the title under the
/// status bar, and the user could not leave the dialog (discussion #6259).
public class AlertDialogTallMessageTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void buttonsStayOnScreenUnderATallMessage() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final int[] bounds = new int[6];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    Activity activity = ActivityThread.getTopActivity();
                    StringBuilder message = new StringBuilder();
                    for (int i = 0; i < 80; i++) {
                        message.append("Paragraph ").append(i)
                                .append(" of an explanation far longer than the screen.\n\n");
                    }
                    AlertDialog dialog = new AlertDialog.Builder(activity)
                            .setTitle("Use a training area?")
                            .setMessage(message.toString())
                            .setPositiveButton("Continue", (DialogInterface.OnClickListener) null)
                            .setNegativeButton("Cancel", (DialogInterface.OnClickListener) null)
                            .create();
                    dialog.show();
                    com.codename1.ui.Dialog host = dialog.host();
                    host.revalidate();
                    Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    Component p = positive.getPeer();
                    Component n = negative.getPeer();
                    Component frame = host.getContentPane();
                    bounds[0] = p.getAbsoluteY();
                    bounds[1] = p.getAbsoluteY() + p.getHeight();
                    bounds[2] = n.getAbsoluteY() + n.getHeight();
                    bounds[3] = frame.getAbsoluteY();
                    bounds[4] = Display.getInstance().getDisplayHeight();
                    bounds[5] = p.getHeight();
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
        String where = "button y=" + bounds[0] + ".." + bounds[1] + ", dialog top=" + bounds[3]
                + ", display height=" + bounds[4];
        assertTrue("the positive button has no height: " + where, bounds[5] > 0);
        assertTrue("the positive button is below the screen: " + where, bounds[1] <= bounds[4]);
        assertTrue("the negative button is below the screen: " + where, bounds[2] <= bounds[4]);
        assertTrue("the dialog starts above the screen: " + where, bounds[3] >= 0);
    }
}
