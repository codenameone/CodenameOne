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
import android.widget.TextView;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// A tap outside a cancelable dialog closes it through `cancel()`, as on
/// Android. Codename One's own outside-tap disposal used to close the host
/// directly: no cancel or dismiss listener, no `onStop()`, and `isShowing()`
/// stayed true.
public class DialogOutsideTouchTest {

    @org.junit.Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    @Test
    public void anOutsideTapCancelsAndDismisses() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final StringBuilder events = new StringBuilder();
        final Object[] state = new Object[3];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    Activity activity = ActivityThread.getTopActivity();
                    Dialog dialog = new Dialog(activity) {
                        @Override
                        protected void onStop() {
                            events.append("stop;");
                        }
                    };
                    TextView text = new TextView(activity);
                    text.setText("Content");
                    dialog.setContentView(text);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface d) {
                            events.append("cancel;");
                        }
                    });
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface d) {
                            events.append("dismiss;");
                        }
                    });
                    dialog.show();
                    com.codename1.ui.Dialog host = dialog.host();
                    assertNotNull(host);
                    assertTrue(host.isDisposeWhenPointerOutOfBounds());
                    host.pointerPressed(0, 0);
                    host.pointerReleased(0, 0);
                    state[1] = Boolean.valueOf(dialog.isShowing());
                    state[2] = dialog.host();
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
        assertEquals("cancel;stop;dismiss;", events.toString());
        assertFalse("the dialog still reports itself showing", ((Boolean) state[1]).booleanValue());
        assertNull("the dialog kept the disposed host", state[2]);
    }
}
