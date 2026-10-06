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

import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// A list's rows are recycled views that share one id, so saving the
/// hierarchy leaves them out, as Android's AdapterView does: one row's
/// state restored into every row would check every box in the list.
public class RowStateNotSavedTest {

    private static final int ROW_ID = 0x7f0b0101;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void listRowsAreNotSavedWithTheHierarchy() {
        ListView lv = new ListView(AndroidTestSupport.context());
        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 5;
            }

            @Override
            public Object getItem(int position) {
                return Integer.valueOf(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckBox c = new CheckBox(parent.getContext());
                c.setId(ROW_ID);
                c.setChecked(position == 1);
                c.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));
                return c;
            }
        });
        lv.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        lv.layout(0, 0, 200, 300);
        assertTrue("rows laid out", lv.getChildCount() > 1);
        FrameLayout root = new FrameLayout(AndroidTestSupport.context());
        root.addView(lv);
        SparseArray<Parcelable> saved = new SparseArray<Parcelable>();
        root.saveHierarchyState(saved);
        assertEquals("a row's state was saved", null, saved.get(ROW_ID));
    }
}
