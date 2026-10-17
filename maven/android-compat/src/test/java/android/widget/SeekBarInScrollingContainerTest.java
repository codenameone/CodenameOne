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

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// Inside a scrolling parent a seek bar waits for horizontal movement before
/// it claims the gesture, so a vertical swipe that starts on it scrolls the
/// page instead of changing its value. Outside one it seeks on touch down.
public class SeekBarInScrollingContainerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static SeekBar seekBar() {
        SeekBar bar = new SeekBar(AndroidTestSupport.context());
        bar.setMax(10);
        bar.setProgress(0);
        return bar;
    }

    /// A 200x100 scroll view over a column holding the seek bar and a tall
    /// filler, so there is something to scroll.
    private static ScrollView page(SeekBar bar) {
        ScrollView sv = new ScrollView(AndroidTestSupport.context());
        LinearLayout column = new LinearLayout(AndroidTestSupport.context());
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));
        column.addView(new View(AndroidTestSupport.context()), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 500));
        sv.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        sv.layout(0, 0, 200, 100);
        return sv;
    }

    private static boolean dispatch(View target, int action, float x, float y) {
        return target.dispatchTouchEvent(MotionEvent.obtain(0, 0, action, x, y, 0));
    }

    @Test
    public void verticalSwipeStartingOnTheBarScrollsThePage() {
        SeekBar bar = seekBar();
        ScrollView sv = page(bar);
        assertTrue("the bar is inside a scrolling container", bar.isInScrollingContainer());
        dispatch(sv, MotionEvent.ACTION_DOWN, 180, 20);
        assertEquals("touch down alone does not seek", 0, bar.getProgress());
        dispatch(sv, MotionEvent.ACTION_MOVE, 180, -10);
        dispatch(sv, MotionEvent.ACTION_MOVE, 180, -40);
        dispatch(sv, MotionEvent.ACTION_UP, 180, -40);
        assertEquals("the swipe did not change the value", 0, bar.getProgress());
        assertTrue("the page scrolled", sv.getScrollY() > 0);
    }

    @Test
    public void horizontalDragPastTheSlopSeeks() {
        SeekBar bar = seekBar();
        page(bar);
        bar.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 20, 20, 0));
        assertEquals(0, bar.getProgress());
        bar.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 190, 20, 0));
        assertTrue("dragging", bar.isDragging());
        assertEquals(10, bar.getProgress());
        bar.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 190, 20, 0));
        assertEquals(10, bar.getProgress());
    }

    @Test
    public void tapInsideAScrollingContainerStillSeeks() {
        SeekBar bar = seekBar();
        page(bar);
        bar.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 195, 20, 0));
        bar.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 195, 20, 0));
        assertEquals(10, bar.getProgress());
    }

    @Test
    public void outsideAScrollingContainerTouchDownSeeks() {
        SeekBar bar = seekBar();
        LinearLayout column = new LinearLayout(AndroidTestSupport.context());
        FrameLayout root = new FrameLayout(AndroidTestSupport.context());
        root.addView(column);
        column.addView(bar, new LinearLayout.LayoutParams(200, 40));
        root.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 200, 100);
        assertTrue(!bar.isInScrollingContainer());
        bar.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 195, 20, 0));
        assertTrue("dragging at once", bar.isDragging());
        assertEquals(10, bar.getProgress());
    }
}
