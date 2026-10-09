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

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A dialog-mode spinner lists the adapter's own drop-down rows, as the
/// drop-down mode does. It used to flatten every item to its `toString()`,
/// so custom row layouts, icons and formatting were lost.
public class SpinnerDialogCustomRowsTest {

    @Before
    public void startClean() {
        AndroidTestSupport.cleanDisplay();
    }

    /// Rows are a layout holding an icon, which a text-only list cannot show.
    private static final class IconRowAdapter extends BaseAdapter {
        final List<View> dropDownRows = new ArrayList<View>();

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Object getItem(int position) {
            return "item" + position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView t = new TextView(parent.getContext());
            t.setText("item" + position);
            return t;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.addView(new ImageView(parent.getContext()));
            TextView t = new TextView(parent.getContext());
            t.setText("row" + position);
            row.addView(t);
            row.setTag("custom" + position);
            dropDownRows.add(row);
            return row;
        }
    }

    @Test
    public void dialogShowsTheAdapterRows() {
        final Context app = AndroidTestSupport.context().getApplicationContext();
        final IconRowAdapter adapter = new IconRowAdapter();
        final Spinner[] spinner = new Spinner[1];
        final Throwable[] failure = new Throwable[1];
        try {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent(app, AndroidTestSupport.TestActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        app.startActivity(intent);
                        Activity activity = ActivityThread.getTopActivity();
                        spinner[0] = new Spinner(activity, Spinner.MODE_DIALOG);
                        spinner[0].setAdapter(adapter);
                        spinner[0].setSelection(1);
                        assertTrue(spinner[0].performClick());
                    } catch (Throwable t) {
                        failure[0] = t;
                    }
                }
            });
            rethrow(failure[0]);
            // A second pass lets the dialog lay its list out.
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        assertFalse("the dialog never asked the adapter for its drop-down rows",
                                adapter.dropDownRows.isEmpty());
                        View row = adapter.dropDownRows.get(adapter.dropDownRows.size() - 1);
                        assertTrue("the adapter's row is not in the dialog's list",
                                row.getParent() instanceof ListView);
                        ListView list = (ListView) row.getParent();
                        assertEquals(1, list.getCheckedItemPosition());
                        View third = null;
                        for (int i = 0; i < list.getChildCount(); i++) {
                            if ("custom2".equals(list.getChildAt(i).getTag())) {
                                third = list.getChildAt(i);
                            }
                        }
                        assertTrue("row 2 was not laid out", third != null);
                        list.performItemClick(third, 2, 2);
                        assertEquals(2, spinner[0].getSelectedItemPosition());
                    } catch (Throwable t) {
                        failure[0] = t;
                    }
                }
            });
            rethrow(failure[0]);
        } finally {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                @Override
                public void run() {
                    ActivityThread.finishAllActivities();
                }
            });
        }
    }

    private static void rethrow(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t != null) {
            throw new RuntimeException(t);
        }
    }
}
