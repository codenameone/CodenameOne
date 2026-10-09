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
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/// Changes made on a ViewGroup reach the descendants that depend on them:
/// visibility through onVisibilityChanged, and a layout direction the
/// children inherit through their start/end padding.
public class AncestorChangeDispatchTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void hidingAGroupTellsItsDescendants() {
        final List<String> log = new ArrayList<String>();
        final View[] changed = new View[1];
        FrameLayout outer = new FrameLayout(AndroidTestSupport.context());
        FrameLayout inner = new FrameLayout(AndroidTestSupport.context());
        View leaf = new View(AndroidTestSupport.context()) {
            @Override
            protected void onVisibilityChanged(View changedView, int visibility) {
                changed[0] = changedView;
                log.add(String.valueOf(visibility));
            }
        };
        inner.addView(leaf);
        outer.addView(inner);
        outer.setVisibility(View.GONE);
        outer.setVisibility(View.VISIBLE);
        assertEquals("[" + View.GONE + ", " + View.VISIBLE + "]", log.toString());
        assertSame(outer, changed[0]);
    }

    @Test
    public void parentDirectionChangeMirrorsInheritedRelativePadding() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        parent.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        View child = new View(AndroidTestSupport.context());
        parent.addView(child);
        child.setPaddingRelative(7, 0, 3, 0);
        assertEquals(7, child.getPaddingLeft());
        parent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        assertEquals("start padding kept its old side", 7, child.getPaddingRight());
        assertEquals(3, child.getPaddingLeft());
        assertEquals(7, child.getPaddingStart());
    }

    @Test
    public void relativePaddingResolvesAgainstTheParentItJoins() {
        FrameLayout parent = new FrameLayout(AndroidTestSupport.context());
        parent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        View child = new View(AndroidTestSupport.context());
        child.setPaddingRelative(7, 0, 3, 0);
        parent.addView(child);
        assertEquals(7, child.getPaddingRight());
        assertEquals(3, child.getPaddingLeft());
    }
}
