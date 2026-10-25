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

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// STRETCH_SPACING_UNIFORM shares the leftover width among the leading,
/// trailing and inner gaps for any column count. A single column used to
/// take all of it as its leading gap and sat against the far edge.
public class GridViewUniformSpacingTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static View layOut(int columns) {
        GridView grid = new GridView(AndroidTestSupport.context());
        grid.setNumColumns(columns);
        grid.setColumnWidth(100);
        grid.setHorizontalSpacing(0);
        grid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        grid.setAdapter(new ArrayAdapter<String>(AndroidTestSupport.context(),
                android.R.layout.simple_list_item_1, new String[] {"a"}));
        grid.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        grid.layout(0, 0, 300, 300);
        assertEquals(1, grid.getChildCount());
        return grid.getChildAt(0);
    }

    @Test
    public void singleColumnIsCentred() {
        View cell = layOut(1);
        assertEquals("single uniform column not centred", 100, cell.getLeft());
        assertEquals(200, cell.getRight());
    }

    @Test
    public void twoColumnsShareTheSameRule() {
        // 300 - 2 * 100 = 100 left over, three gaps of 33.
        assertEquals(33, layOut(2).getLeft());
    }
}
