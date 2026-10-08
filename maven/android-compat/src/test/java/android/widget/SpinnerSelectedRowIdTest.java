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
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/// After a data change that keeps the selected position, the selected row id
/// is the id of the row now at that position. It used to keep the id of the
/// row that was there before, disagreeing with `getSelectedItem()`.
public class SpinnerSelectedRowIdTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class IdAdapter extends BaseAdapter {
        final List<Long> ids = new ArrayList<Long>();

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
    public void rowIdFollowsTheRowAtTheSelectedPosition() {
        IdAdapter adapter = new IdAdapter();
        adapter.ids.addAll(Arrays.asList(Long.valueOf(10), Long.valueOf(20), Long.valueOf(30)));
        Spinner spinner = new Spinner(AndroidTestSupport.context());
        spinner.setAdapter(adapter);
        spinner.setSelection(1);
        assertEquals(20L, spinner.getSelectedItemId());
        adapter.ids.set(1, Long.valueOf(99));
        adapter.notifyDataSetChanged();
        assertEquals(1, spinner.getSelectedItemPosition());
        assertEquals(Long.valueOf(99), spinner.getSelectedItem());
        assertEquals(99L, spinner.getSelectedItemId());
    }
}
