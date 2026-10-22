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
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A scaled or rotated child is touched where it is drawn, and receives the
/// event in its own, untransformed coordinates.
public class TransformedChildTouchTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Recorder extends View {
        float x = Float.NaN;
        float y = Float.NaN;

        Recorder(Context c) {
            super(c);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            x = ev.getX();
            y = ev.getY();
            return true;
        }
    }

    private static Recorder childAt100(FrameLayout parent) {
        Recorder child = new Recorder(parent.getContext());
        parent.addView(child, new FrameLayout.LayoutParams(100, 100));
        parent.layout(0, 0, 400, 400);
        child.layout(100, 100, 200, 200);
        return child;
    }

    private static boolean down(ViewGroup parent, float x, float y) {
        MotionEvent ev = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0);
        boolean r = parent.dispatchTouchEvent(ev);
        assertEquals("the event's location is put back", x, ev.getX(), 0f);
        return r;
    }

    @Test
    public void aScaledUpChildIsTouchableOutsideItsLayoutBounds() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        Recorder child = childAt100(parent);
        child.setScaleX(2f);
        child.setScaleY(2f);
        // Scaled about its centre (150, 150) it covers 50..250.
        assertTrue(down(parent, 60, 60));
        assertEquals(5f, child.x, 1e-3f);
        assertEquals(5f, child.y, 1e-3f);
    }

    @Test
    public void aScaledDownChildIsNotTouchedWhereItNoLongerDraws() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        Recorder child = childAt100(parent);
        child.setScaleX(0.5f);
        child.setScaleY(0.5f);
        // Now it covers 125..175 only.
        assertFalse(down(parent, 110, 110));
        assertTrue(Float.isNaN(child.x));
        assertTrue(down(parent, 130, 170));
        assertEquals(10f, child.x, 1e-3f);
        assertEquals(90f, child.y, 1e-3f);
    }

    @Test
    public void aRotatedChildReceivesUnrotatedCoordinates() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        Recorder child = childAt100(parent);
        child.setRotation(90f);
        // Rotated 90 degrees clockwise about (150, 150), its local (10, 10)
        // -- near the top-left corner -- is drawn at (190, 110).
        assertTrue(down(parent, 190, 110));
        assertEquals(10f, child.x, 1e-3f);
        assertEquals(10f, child.y, 1e-3f);
    }
}
