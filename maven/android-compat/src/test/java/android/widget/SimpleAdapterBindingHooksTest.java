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

import android.content.Context;
import android.view.View;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/// The default text and integer-image bindings go through the overridable
/// `setViewText` and `setViewImage(ImageView, int)`, as Android's do. They
/// used to set the views directly, so a subclass's formatting was skipped.
public class SimpleAdapterBindingHooksTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final int TEXT_ID = 1;
    private static final int IMAGE_ID = 2;

    @Test
    public void defaultBindingsUseTheAdapterHooks() {
        Context c = AndroidTestSupport.context();
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        HashMap<String, Object> row = new HashMap<String, Object>();
        row.put("name", "alice");
        row.put("icon", Integer.valueOf(android.R.drawable.toast_frame));
        data.add(row);
        final List<Integer> images = new ArrayList<Integer>();
        // The row is supplied as the convert view, so the layout id is a placeholder.
        SimpleAdapter adapter = new SimpleAdapter(c, data, 0,
                new String[] {"name", "icon"}, new int[] {TEXT_ID, IMAGE_ID}) {
            @Override
            public void setViewText(TextView v, String text) {
                v.setText("[" + text + "]");
            }

            @Override
            public void setViewImage(ImageView v, int value) {
                images.add(Integer.valueOf(value));
            }
        };
        LinearLayout rowView = new LinearLayout(c);
        TextView text = new TextView(c);
        text.setId(TEXT_ID);
        rowView.addView(text);
        ImageView image = new ImageView(c);
        image.setId(IMAGE_ID);
        rowView.addView(image);

        View bound = adapter.getView(0, rowView, new FrameLayout(c));
        assertSame(rowView, bound);
        assertEquals("[alice]", String.valueOf(text.getText()));
        assertEquals(1, images.size());
        assertEquals(Integer.valueOf(android.R.drawable.toast_frame), images.get(0));
    }
}
