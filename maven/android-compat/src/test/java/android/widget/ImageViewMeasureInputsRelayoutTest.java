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
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Changing a measurement input of an ImageView that is already laid out
/// requests a layout. Toggling adjust-view-bounds (or a maximum size) used
/// to leave the old measured size in place until an unrelated layout.
public class ImageViewMeasureInputsRelayoutTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private ImageView view;

    private void layOut() {
        int spec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        assertFalse(view.isLayoutRequested());
    }

    @Before
    public void start() {
        view = new ImageView(AndroidTestSupport.context());
        layOut();
    }

    @Test
    public void enablingAdjustViewBoundsRequestsLayout() {
        view.setAdjustViewBounds(true);
        assertTrue(view.isLayoutRequested());
    }

    @Test
    public void disablingAdjustViewBoundsRequestsLayout() {
        view.setAdjustViewBounds(true);
        layOut();
        view.setAdjustViewBounds(false);
        assertTrue(view.isLayoutRequested());
    }

    @Test
    public void maximumSizesRequestLayout() {
        view.setMaxWidth(10);
        assertTrue(view.isLayoutRequested());
        layOut();
        view.setMaxHeight(10);
        assertTrue(view.isLayoutRequested());
    }

    @Test
    public void anUnchangedValueDoesNotRequestLayout() {
        view.setAdjustViewBounds(false);
        view.setMaxWidth(Integer.MAX_VALUE);
        view.setMaxHeight(Integer.MAX_VALUE);
        assertFalse(view.isLayoutRequested());
    }
}
