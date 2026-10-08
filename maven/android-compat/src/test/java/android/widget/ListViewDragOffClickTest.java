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

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// A press that is dragged sideways off the list, without moving far enough
/// vertically to scroll it, clicks nothing on release, as on Android. It
/// used to click the row it started on.
public class ListViewDragOffClickTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Rows extends BaseAdapter {
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
            TextView t = new TextView(parent.getContext());
            t.setText("row " + position);
            t.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));
            return t;
        }
    }

    private final List<Integer> clicks = new ArrayList<Integer>();

    private ListView list() {
        ListView lv = new ListView(AndroidTestSupport.context());
        lv.setAdapter(new Rows());
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clicks.add(Integer.valueOf(position));
            }
        });
        lv.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        lv.layout(0, 0, 200, 300);
        assertTrue("rows laid out", lv.getChildCount() > 1);
        return lv;
    }

    private static void touch(View v, int action, float x, float y) {
        v.onTouchEvent(MotionEvent.obtain(0, 0, action, x, y, 0));
    }

    @Test
    public void tapClicksTheRow() {
        ListView lv = list();
        touch(lv, MotionEvent.ACTION_DOWN, 50, 60);
        touch(lv, MotionEvent.ACTION_UP, 52, 61);
        assertEquals("[1]", clicks.toString());
    }

    @Test
    public void dragOffTheListSidewaysCancelsTheClick() {
        ListView lv = list();
        touch(lv, MotionEvent.ACTION_DOWN, 50, 60);
        touch(lv, MotionEvent.ACTION_MOVE, 400, 60);
        touch(lv, MotionEvent.ACTION_UP, 400, 60);
        assertEquals("[]", clicks.toString());
    }

    @Test
    public void dragOffAndBackDoesNotClickEither() {
        ListView lv = list();
        touch(lv, MotionEvent.ACTION_DOWN, 50, 60);
        touch(lv, MotionEvent.ACTION_MOVE, 400, 60);
        touch(lv, MotionEvent.ACTION_MOVE, 50, 60);
        touch(lv, MotionEvent.ACTION_UP, 50, 60);
        assertEquals("[]", clicks.toString());
    }

    @Test
    public void releaseBesideTheListClicksNothing() {
        ListView lv = list();
        touch(lv, MotionEvent.ACTION_DOWN, 190, 60);
        // Within the slop, so the press survives the move, but outside the
        // list's width on release.
        touch(lv, MotionEvent.ACTION_UP, 201, 60);
        assertEquals("[]", clicks.toString());
    }
}
