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
package android.widget;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A foreground with a gravity other than fill is drawn at its own size where
/// the gravity puts it; it used to be stretched over the whole view.
public class ForegroundGravityTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static Rect drawForeground(int gravity, boolean setGravity) {
        FrameLayout frame = new FrameLayout(AndroidTestSupport.context());
        RecordingDrawable badge = new RecordingDrawable(20, 10);
        frame.setForeground(badge);
        if (setGravity) {
            frame.setForegroundGravity(gravity);
        }
        frame.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        frame.layout(0, 0, 200, 100);
        frame.onDrawForeground(null);
        assertEquals(1, badge.drawn.size());
        return badge.drawn.get(0);
    }

    @Test
    public void defaultFillsTheView() {
        assertEquals(new Rect(0, 0, 200, 100), drawForeground(0, false));
    }

    @Test
    public void centerKeepsTheIntrinsicSize() {
        assertEquals(new Rect(90, 45, 110, 55), drawForeground(Gravity.CENTER, true));
    }

    @Test
    public void bottomRightSitsInTheCorner() {
        assertEquals(new Rect(180, 90, 200, 100), drawForeground(Gravity.BOTTOM | Gravity.RIGHT, true));
    }

    @Test
    public void fillHorizontalStretchesOneAxisOnly() {
        assertEquals(new Rect(0, 0, 200, 10), drawForeground(Gravity.TOP | Gravity.FILL_HORIZONTAL, true));
    }
}
