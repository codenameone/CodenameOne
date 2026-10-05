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
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/// A radio group whose checked button is unchecked from code has no checked
/// id, and an item added to a filtered `ArrayAdapter` shows only when the
/// active filter keeps it.
public class RadioGroupAndFilteredAdapterTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void uncheckingTheCheckedButtonClearsTheGroup() {
        Context c = AndroidTestSupport.context();
        RadioGroup group = new RadioGroup(c);
        RadioButton first = new RadioButton(c);
        RadioButton second = new RadioButton(c);
        group.addView(first);
        group.addView(second);
        final ArrayList<Integer> notified = new ArrayList<Integer>();
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup g, int checkedId) {
                notified.add(Integer.valueOf(checkedId));
            }
        });
        first.setChecked(true);
        assertEquals(first.getId(), group.getCheckedRadioButtonId());

        first.setChecked(false);
        assertEquals("the group still names the unchecked button", View.NO_ID, group.getCheckedRadioButtonId());
        assertEquals(Arrays.asList(Integer.valueOf(first.getId()), Integer.valueOf(View.NO_ID)), notified);

        // Selecting again works as before.
        second.setChecked(true);
        assertEquals(second.getId(), group.getCheckedRadioButtonId());
    }

    @Test
    public void anAdditionShowsOnlyWhenTheActiveFilterKeepsIt() {
        Context c = AndroidTestSupport.context();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(c, android.R.layout.simple_list_item_1,
                new ArrayList<String>(Arrays.asList("apple", "banana")));
        adapter.getFilter().filter("ap");
        assertEquals(1, adapter.getCount());

        adapter.add("cherry");
        assertEquals("an item the filter rejects was shown", 1, adapter.getCount());
        adapter.addAll(Arrays.asList("date", "apricot"));
        adapter.insert("grape", 0);
        assertEquals(2, adapter.getCount());
        assertEquals("apple", adapter.getItem(0));
        assertEquals("apricot", adapter.getItem(1));

        // Clearing the filter shows everything that was added.
        adapter.getFilter().filter("");
        assertEquals(6, adapter.getCount());
        adapter.add("fig");
        assertEquals("an addition under an empty filter stayed hidden", 7, adapter.getCount());
    }
}
