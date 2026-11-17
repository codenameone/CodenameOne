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
package com.codenameone.examples.hellocodenameone.tests.androidcompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.example.droid.MainActivity;

/// An AlertDialog whose message is taller than the screen, over the launcher
/// screen: the message scrolls and the title and both buttons stay on screen
/// (discussion #6259, where the buttons fell off the bottom and the title went
/// under the status bar).
public class AndroidCompatTallDialogScreenshotTest extends AndroidCompatScreenshotTest {
    public AndroidCompatTallDialogScreenshotTest() {
        super(MainActivity.class, "AndroidCompatTallDialog");
    }

    private AlertDialog dialog;

    @Override
    protected void prepare(Activity activity) {
        StringBuilder message = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            message.append("Paragraph ").append(i)
                    .append(": the guided tour moves the map to a training area so that nothing you try"
                            + " changes your real data.\n\n");
        }
        dialog = new AlertDialog.Builder(activity)
                .setTitle("Use a training area?")
                .setMessage(message.toString())
                .setPositiveButton("Continue", (DialogInterface.OnClickListener) null)
                .setNegativeButton("Cancel", (DialogInterface.OnClickListener) null)
                .show();
    }

    @Override
    protected com.codename1.ui.Form captureForm(com.codename1.ui.Form activityForm) {
        com.codename1.ui.Form host = dialog == null ? null
                : dialog.getButton(DialogInterface.BUTTON_POSITIVE).getPeer().getComponentForm();
        return host != null ? host : activityForm;
    }

    @Override
    protected void afterCapture() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
