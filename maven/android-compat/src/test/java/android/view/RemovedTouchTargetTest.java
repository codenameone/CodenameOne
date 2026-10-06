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

import android.content.Context;
import android.widget.FrameLayout;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A child removed in the middle of a gesture it is handling gets
/// ACTION_CANCEL first, so it drops its pressed and drag state. It used to
/// be unlinked silently and stayed pressed if it was attached again.
public class RemovedTouchTargetTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Recorder extends View {
        final List<Integer> actions = new ArrayList<Integer>();

        Recorder(Context c) {
            super(c);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            actions.add(Integer.valueOf(ev.getActionMasked()));
            return true;
        }
    }

    @Test
    public void removingTheTouchTargetCancelsItsGesture() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        Recorder child = new Recorder(parent.getContext());
        parent.addView(child, new FrameLayout.LayoutParams(100, 100));
        parent.layout(0, 0, 400, 400);
        child.layout(0, 0, 100, 100);
        parent.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50, 50, 0));
        parent.removeView(child);
        assertEquals("[" + MotionEvent.ACTION_DOWN + ", " + MotionEvent.ACTION_CANCEL + "]",
                child.actions.toString());
        assertEquals(0, parent.getChildCount());
        // The rest of the gesture no longer reaches it.
        parent.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 50, 50, 0));
        assertEquals(2, child.actions.size());
    }

    @Test
    public void removingAnotherChildSendsNoCancel() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        Recorder touched = new Recorder(parent.getContext());
        Recorder other = new Recorder(parent.getContext());
        parent.addView(touched, new FrameLayout.LayoutParams(100, 100));
        parent.addView(other, new FrameLayout.LayoutParams(100, 100));
        parent.layout(0, 0, 400, 400);
        touched.layout(0, 0, 100, 100);
        other.layout(200, 200, 300, 300);
        parent.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50, 50, 0));
        parent.removeView(other);
        assertEquals(0, other.actions.size());
        assertEquals(1, touched.actions.size());
    }
}
