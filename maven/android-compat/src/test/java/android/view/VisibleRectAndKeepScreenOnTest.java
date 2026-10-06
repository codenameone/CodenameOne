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

import android.graphics.Rect;
import android.widget.FrameLayout;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The visible rectangle is cut to the ancestors that clip the view, and
/// the keep-screen-on request is held per view.
public class VisibleRectAndKeepScreenOnTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static View childOf(FrameLayout parent, int l, int t, int r, int b) {
        View child = new View(parent.getContext());
        parent.addView(child, new FrameLayout.LayoutParams(r - l, b - t));
        parent.layout(0, 0, 400, 400);
        child.layout(l, t, r, b);
        return child;
    }

    @Test
    public void aChildHangingOutOfItsParentIsCutToIt() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        View child = childOf(parent, 300, 100, 600, 200);
        parent.dispatchAttachedToWindow(true);
        try {
            Rect r = new Rect();
            assertTrue(child.getGlobalVisibleRect(r));
            assertEquals(new Rect(300, 100, 400, 200), r);
            assertTrue(child.getLocalVisibleRect(r));
            assertEquals(new Rect(0, 0, 100, 100), r);
        } finally {
            parent.dispatchAttachedToWindow(false);
        }
    }

    @Test
    public void aChildEntirelyOutsideItsParentIsNotVisible() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        View child = childOf(parent, 500, 100, 600, 200);
        parent.dispatchAttachedToWindow(true);
        try {
            assertFalse(child.getGlobalVisibleRect(new Rect()));
            assertFalse(child.getLocalVisibleRect(new Rect()));
        } finally {
            parent.dispatchAttachedToWindow(false);
        }
    }

    @Test
    public void oneViewClearingItsRequestDoesNotOverrideAnother() {
        View a = new View(AndroidTestSupport.context());
        View b = new View(AndroidTestSupport.context());
        a.dispatchAttachedToWindow(true);
        b.dispatchAttachedToWindow(true);
        try {
            a.setKeepScreenOn(true);
            b.setKeepScreenOn(true);
            assertTrue(a.getKeepScreenOn());
            assertTrue(HeadlessImplementation.screenLocked);
            a.setKeepScreenOn(false);
            assertTrue("b still asks for the screen to stay on", HeadlessImplementation.screenLocked);
            b.dispatchAttachedToWindow(false);
            assertFalse("a detached view's request is released", HeadlessImplementation.screenLocked);
            b.dispatchAttachedToWindow(true);
            assertTrue("reattaching restores the request", HeadlessImplementation.screenLocked);
        } finally {
            a.setKeepScreenOn(false);
            b.setKeepScreenOn(false);
            a.dispatchAttachedToWindow(false);
            b.dispatchAttachedToWindow(false);
        }
        assertFalse(HeadlessImplementation.screenLocked);
    }
}
