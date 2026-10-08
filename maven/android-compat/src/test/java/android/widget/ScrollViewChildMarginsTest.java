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

import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// The scroll range of a scroll view reaches the end of its child's margins:
/// the child is laid out past its leading margin, so a range built from its
/// size alone left the end of the content unreachable.
public class ScrollViewChildMarginsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// A view of a fixed size whatever the measure spec.
    private static final class Fixed extends View {
        private final int w;
        private final int h;

        Fixed(int w, int h) {
            super(AndroidTestSupport.context());
            this.w = w;
            this.h = h;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(w, h);
        }
    }

    private static ScrollView vertical() {
        ScrollView sv = new ScrollView(AndroidTestSupport.context());
        sv.setSmoothScrollingEnabled(false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(100, 300);
        lp.topMargin = 20;
        lp.bottomMargin = 30;
        sv.addView(new Fixed(100, 300), lp);
        sv.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        sv.layout(0, 0, 100, 100);
        return sv;
    }

    private static HorizontalScrollView horizontal() {
        HorizontalScrollView sv = new HorizontalScrollView(AndroidTestSupport.context());
        sv.setSmoothScrollingEnabled(false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(300, 100);
        lp.leftMargin = 20;
        lp.rightMargin = 30;
        sv.addView(new Fixed(300, 100), lp);
        sv.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        sv.layout(0, 0, 100, 100);
        return sv;
    }

    @Test
    public void verticalRangeIncludesBothMargins() {
        ScrollView sv = vertical();
        assertEquals(20, sv.getChildAt(0).getTop());
        // 20 + 300 + 30 of content in a 100px viewport.
        assertEquals(250, sv.getScrollRange());
        sv.scrollTo(0, 1000);
        assertEquals(250, sv.getScrollY());
    }

    @Test
    public void verticalFullScrollReachesTheBottomMargin() {
        ScrollView sv = vertical();
        sv.fullScroll(View.FOCUS_DOWN);
        assertEquals(250, sv.getScrollY());
    }

    @Test
    public void horizontalRangeIncludesBothMargins() {
        HorizontalScrollView sv = horizontal();
        assertEquals(20, sv.getChildAt(0).getLeft());
        assertEquals(250, sv.getScrollRange());
        sv.scrollTo(1000, 0);
        assertEquals(250, sv.getScrollX());
    }

    @Test
    public void horizontalFullScrollReachesTheRightMargin() {
        HorizontalScrollView sv = horizontal();
        sv.fullScroll(View.FOCUS_RIGHT);
        assertEquals(250, sv.getScrollX());
    }
}
