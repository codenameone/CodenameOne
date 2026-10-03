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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/// Repainting an EditText's field alone -- what Codename One does when it
/// loses focus or its text changes -- paints what lies behind it first. The
/// field paints no background, and it used to skip that backdrop too, so on a
/// device its old pixels stayed on screen: a caret after the focus had gone.
public class EditTextRepaintBackdropTest {

    static final class RecordingBackground extends Drawable {
        boolean drawn;

        @Override
        public void draw(Canvas canvas) {
            drawn = true;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Test
    public void aFieldRepaintedAlonePaintsItsBackdrop() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final RecordingBackground parentBackground = new RecordingBackground();
        final RecordingBackground editBackground = new RecordingBackground();
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(intent);
                    Activity a = ActivityThread.getTopActivity();
                    FrameLayout frame = new FrameLayout(a);
                    frame.setBackground(parentBackground);
                    EditText edit = new EditText(a);
                    edit.setBackground(editBackground);
                    frame.addView(edit, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    a.setContentView(frame);
                    a.getWindow().getDecorView().getPeer().getComponentForm().revalidate();

                    Component field = ((Container) edit.getPeer()).getComponentAt(0);
                    parentBackground.drawn = false;
                    editBackground.drawn = false;
                    Image img = Image.createImage(Math.max(1, field.getWidth()), Math.max(1, field.getHeight()));
                    field.paintComponent(img.getGraphics(), true);
                    ActivityThread.finishAllActivities();
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        if (failure[0] != null) {
            throw new RuntimeException(failure[0]);
        }
        assertTrue("the EditText's own background was not repainted behind its field", editBackground.drawn);
        assertTrue("the parent's background was not repainted behind the field", parentBackground.drawn);
    }
}
