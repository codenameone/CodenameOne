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
import android.view.ViewGroup;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Checked rows were kept by position alone across `notifyDataSetChanged()`,
/// so a reorder or removal moved a check onto another row, and a removed
/// last row left a check past the end that sent `getCheckedItemIds()` out of
/// the adapter. With stable ids the checks now follow their rows; without
/// them a check past the end is dropped.
public class ListCheckedRowsAfterDataChangeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class IdAdapter extends BaseAdapter {
        final List<Long> ids = new ArrayList<Long>();
        final boolean stable;

        IdAdapter(boolean stable, long... initial) {
            this.stable = stable;
            for (long id : initial) {
                ids.add(Long.valueOf(id));
            }
        }

        @Override
        public boolean hasStableIds() {
            return stable;
        }

        @Override
        public int getCount() {
            return ids.size();
        }

        @Override
        public Object getItem(int position) {
            return ids.get(position);
        }

        @Override
        public long getItemId(int position) {
            return ids.get(position).longValue();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return new TextView(parent.getContext());
        }
    }

    @Test
    public void stableIdChecksFollowTheirRows() {
        ListView lv = new ListView(AndroidTestSupport.context());
        IdAdapter adapter = new IdAdapter(true, 10, 11, 12, 13);
        lv.setAdapter(adapter);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        lv.setItemChecked(1, true);
        lv.setItemChecked(3, true);

        adapter.ids.remove(3);
        adapter.ids.remove(0);
        adapter.ids.add(0, Long.valueOf(20));
        adapter.ids.add(0, Long.valueOf(21));
        adapter.notifyDataSetChanged();

        // Now 21, 20, 11, 12: id 11 moved to position 2 and id 13 is gone.
        assertTrue(lv.isItemChecked(2));
        assertFalse(lv.isItemChecked(1));
        assertFalse(lv.isItemChecked(3));
        assertEquals(1, lv.getCheckedItemCount());
        assertArrayEquals(new long[] {11}, lv.getCheckedItemIds());
    }

    @Test
    public void checkPastTheEndIsDroppedWithoutStableIds() {
        ListView lv = new ListView(AndroidTestSupport.context());
        IdAdapter adapter = new IdAdapter(false, 10, 11, 12);
        lv.setAdapter(adapter);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        lv.setItemChecked(0, true);
        lv.setItemChecked(2, true);

        adapter.ids.remove(2);
        adapter.notifyDataSetChanged();

        assertTrue(lv.isItemChecked(0));
        assertFalse(lv.isItemChecked(2));
        assertEquals(1, lv.getCheckedItemCount());
        assertArrayEquals(new long[] {10}, lv.getCheckedItemIds());
    }
}
