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

import android.widget.FrameLayout;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/// Hiding a focused view, or a group holding it, clears its focus, as on
/// Android. The hidden view used to stay findFocus() and keep receiving keys.
public class HiddenViewLosesFocusTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static View focusableChild(FrameLayout parent) {
        View child = new View(AndroidTestSupport.context());
        child.setFocusable(true);
        parent.addView(child);
        return child;
    }

    @Test
    public void hidingAFocusedViewClearsItsFocus() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        View child = focusableChild(parent);
        child.requestFocus();
        assertSame(child, parent.findFocus());
        child.setVisibility(View.INVISIBLE);
        assertFalse(child.isFocused());
        assertNull(parent.findFocus());
    }

    @Test
    public void hidingAGroupClearsTheFocusOfItsDescendant() {
        FrameLayout outer = new FrameLayout(AndroidTestSupport.context());
        FrameLayout inner = new FrameLayout(AndroidTestSupport.context());
        outer.addView(inner);
        View child = focusableChild(inner);
        child.requestFocus();
        assertSame(child, outer.findFocus());
        inner.setVisibility(View.GONE);
        assertFalse(child.isFocused());
        assertNull(outer.findFocus());
    }
    @Test
    public void removingAGroupClearsTheFocusOfItsDescendant() {
        FrameLayout outer = new FrameLayout(AndroidTestSupport.context());
        FrameLayout inner = new FrameLayout(AndroidTestSupport.context());
        outer.addView(inner);
        View child = focusableChild(inner);
        child.requestFocus();
        outer.removeView(inner);
        assertFalse(child.isFocused());
        assertNull(inner.findFocus());
        assertNull(com.codename1.androidcompat.runtime.AndroidRuntime.getInstance().getFocusedView());
    }

}
