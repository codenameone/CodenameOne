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

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbsListViewSelectionAndStackTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Rows extends BaseAdapter {
        private final int count;
        Rows(int count) { this.count = count; }
        @Override public int getCount() { return count; }
        @Override public Object getItem(int position) { return Integer.valueOf(position); }
        @Override public long getItemId(int position) { return 100 + position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View row = new View(parent.getContext());
            row.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));
            return row;
        }
    }

    private static final class ChangingRows extends BaseAdapter {
        int count;
        ChangingRows(int count) { this.count = count; }
        @Override public int getCount() { return count; }
        @Override public Object getItem(int position) {
            if (position >= count) { throw new IndexOutOfBoundsException(); }
            return Integer.valueOf(position);
        }
        @Override public long getItemId(int position) {
            if (position >= count) { throw new IndexOutOfBoundsException(); }
            return 100 + position;
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View row = new View(parent.getContext());
            row.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));
            return row;
        }
    }

    @Test public void selectionClampsWhenAdapterShrinksOrEmpties() {
        ListView list = new ListView(AndroidTestSupport.context());
        ChangingRows rows = new ChangingRows(5);
        list.setAdapter(rows);
        list.setSelection(4);
        layOut(list);
        rows.count = 2;
        rows.notifyDataSetChanged();
        assertEquals(1, list.getSelectedItemPosition());
        assertEquals(101, list.getSelectedItemId());
        assertEquals(Integer.valueOf(1), list.getSelectedItem());
        rows.count = 0;
        rows.notifyDataSetChanged();
        assertEquals(AdapterView.INVALID_POSITION, list.getSelectedItemPosition());
        assertEquals(AdapterView.INVALID_ROW_ID, list.getSelectedItemId());
        assertNull(list.getSelectedItem());
    }

    private static void layOut(AbsListView view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 200, 120);
    }

    @Test public void listSelectionTracksPositionIdViewAndListener() {
        ListView list = new ListView(AndroidTestSupport.context());
        list.setAdapter(new Rows(8));
        list.dispatchAttachedToWindow(true);
        final int[] notified = {-1};
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                notified[0] = position;
                assertNotNull(view);
                assertEquals(103, id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        list.setSelection(3);
        layOut(list);
        MainThreadRule.drain();
        assertEquals(3, list.getSelectedItemPosition());
        assertEquals(103, list.getSelectedItemId());
        assertNotNull(list.getSelectedView());
        assertEquals(3, notified[0]);
        list.dispatchAttachedToWindow(false);
    }

    @Test public void shortAndLongListsStartAtBottom() {
        ListView shortList = new ListView(AndroidTestSupport.context());
        shortList.setStackFromBottom(true);
        shortList.setAdapter(new Rows(2));
        layOut(shortList);
        assertTrue(shortList.getChildAt(0).getTop() > 0);
        assertEquals(120, shortList.getChildAt(1).getBottom());

        ListView longList = new ListView(AndroidTestSupport.context());
        longList.setStackFromBottom(true);
        longList.setAdapter(new Rows(8));
        layOut(longList);
        assertTrue(longList.getFirstVisiblePosition() > 0);
        assertEquals(7, longList.getLastVisiblePosition());
        assertEquals(120, longList.getChildAt(longList.getChildCount() - 1).getBottom());
    }

    @Test public void gridStartsWithLastRowAtBottom() {
        GridView grid = new GridView(AndroidTestSupport.context());
        grid.setNumColumns(2);
        grid.setColumnWidth(100);
        grid.setStackFromBottom(true);
        grid.setAdapter(new Rows(3));
        layOut(grid);
        assertEquals(40, grid.getChildAt(0).getTop());
        assertEquals(120, grid.getChildAt(grid.getChildCount() - 1).getBottom());

        grid.setAdapter(new Rows(9));
        layOut(grid);
        assertTrue(grid.getFirstVisiblePosition() > 0);
        assertEquals(8, grid.getLastVisiblePosition());
    }

    @Test public void selectorUsesTheLayerMatchingDrawSelectorOnTop() throws Exception {
        GridView grid = new GridView(AndroidTestSupport.context());
        grid.setNumColumns(1);
        grid.setAdapter(new Rows(1));
        layOut(grid);
        Field pressed = AbsListView.class.getDeclaredField("mPressedChild");
        pressed.setAccessible(true);
        pressed.set(grid, grid.getChildAt(0));
        final int[] draws = {0};
        grid.setSelector(new ColorDrawable(0xff000000) {
            @Override public void draw(Canvas canvas) { draws[0]++; }
        });
        Canvas canvas = new Canvas();

        grid.setDrawSelectorOnTop(false);
        grid.onDraw(canvas);
        assertEquals(1, draws[0]);
        grid.dispatchDraw(canvas);
        assertEquals(1, draws[0]);

        grid.setDrawSelectorOnTop(true);
        grid.onDraw(canvas);
        assertEquals(1, draws[0]);
        grid.dispatchDraw(canvas);
        assertEquals(2, draws[0]);
    }
}
