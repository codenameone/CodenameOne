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
package android.view;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// The background tint mode is kept on the view and applied to a
/// replacement background, like the tint list. It used to reach only the
/// drawable present at the time, so a new background fell back to SRC_IN.
public class BackgroundTintModeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    static final class RecordingDrawable extends ColorDrawable {
        PorterDuff.Mode mode;

        RecordingDrawable() {
            super(0xff00ff00);
        }

        @Override
        public void setTintMode(PorterDuff.Mode m) {
            super.setTintMode(m);
            mode = m;
        }
    }

    @Test
    public void replacementBackgroundKeepsTheMode() {
        View v = new View(AndroidTestSupport.context());
        v.setBackground(new RecordingDrawable());
        v.setBackgroundTintList(ColorStateList.valueOf(0xffff0000));
        v.setBackgroundTintMode(PorterDuff.Mode.MULTIPLY);

        RecordingDrawable next = new RecordingDrawable();
        v.setBackground(next);
        assertEquals(PorterDuff.Mode.MULTIPLY, next.mode);
    }
}
