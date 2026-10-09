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

import android.view.Gravity;
import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Dividers in a weighted layout stay inside it whatever the gravity, and a
/// horizontal layout draws the ending divider it reserved space for.
public class LinearLayoutDividersTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// 300x300, two weighted children, a 10px divider at the beginning, the
    /// middle and the end.
    private static LinearLayout layout(int orientation, int gravity, RecordingDrawable divider) {
        LinearLayout ll = new LinearLayout(AndroidTestSupport.context());
        ll.setOrientation(orientation);
        ll.setGravity(gravity);
        ll.setDividerDrawable(divider);
        ll.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING | LinearLayout.SHOW_DIVIDER_MIDDLE
                | LinearLayout.SHOW_DIVIDER_END);
        boolean vertical = orientation == LinearLayout.VERTICAL;
        for (int i = 0; i < 2; i++) {
            View child = new View(AndroidTestSupport.context());
            ll.addView(child, new LinearLayout.LayoutParams(vertical ? 50 : 0, vertical ? 0 : 50, 1f));
        }
        ll.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        ll.layout(0, 0, 300, 300);
        return ll;
    }

    @Test
    public void weightedVerticalLayoutAtTheBottomKeepsItsDividersInside() {
        LinearLayout ll = layout(LinearLayout.VERTICAL, Gravity.BOTTOM, new RecordingDrawable(10, 10));
        // 300 minus three 10px dividers, shared by the two weights.
        assertEquals(135, ll.getChildAt(0).getHeight());
        assertEquals("first child sits below the beginning divider", 10, ll.getChildAt(0).getTop());
        assertEquals("the ending divider still fits", 290, ll.getChildAt(1).getBottom());
    }

    @Test
    public void weightedHorizontalLayoutCenteredKeepsItsDividersInside() {
        LinearLayout ll = layout(LinearLayout.HORIZONTAL, Gravity.CENTER_HORIZONTAL, new RecordingDrawable(10, 10));
        assertEquals(135, ll.getChildAt(0).getWidth());
        assertEquals(10, ll.getChildAt(0).getLeft());
        assertEquals(290, ll.getChildAt(1).getRight());
    }

    @Test
    public void horizontalLayoutDrawsItsEndingDivider() {
        RecordingDrawable divider = new RecordingDrawable(10, 10);
        LinearLayout ll = layout(LinearLayout.HORIZONTAL, Gravity.LEFT, divider);
        ll.onDraw(null);
        assertEquals("beginning, middle and end", 3, divider.drawn.size());
        assertEquals(0, divider.drawn.get(0).left);
        assertEquals(145, divider.drawn.get(1).left);
        assertEquals(290, divider.drawn.get(2).left);
        assertEquals(300, divider.drawn.get(2).right);
    }

    @Test
    public void verticalLayoutDrawsItsEndingDivider() {
        RecordingDrawable divider = new RecordingDrawable(10, 10);
        LinearLayout ll = layout(LinearLayout.VERTICAL, Gravity.TOP, divider);
        ll.onDraw(null);
        assertEquals(3, divider.drawn.size());
        assertEquals(290, divider.drawn.get(2).top);
    }
}
