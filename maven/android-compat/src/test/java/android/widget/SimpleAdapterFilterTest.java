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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/// `SimpleAdapter`'s filter keeps the rows whose bound values, or a word of
/// them, start with the constraint; it used to keep every row.
public class SimpleAdapterFilterTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// Starts the display before the rule runs, so the test runs on the EDT
    /// and the filter publishes before `filter()` returns.
    @BeforeClass
    public static void startDisplay() {
        AndroidTestSupport.context();
    }

    private static Map<String, Object> row(String name, String city) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("name", name);
        m.put("city", city);
        m.put("unbound", "zebra");
        return m;
    }

    private static Object name(SimpleAdapter adapter, int position) {
        return ((Map<?, ?>) adapter.getItem(position)).get("name");
    }

    @Test
    public void filterKeepsMatchingRows() {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(row("Alice Smith", "Paris"));
        data.add(row("Bob Jones", "Berlin"));
        data.add(row("Carol White", "Boston"));
        // The rows are never inflated here, so the layout and ids are placeholders.
        SimpleAdapter adapter = new SimpleAdapter(AndroidTestSupport.context(), data, 0,
                new String[] {"name", "city"}, new int[] {1, 2});

        adapter.getFilter().filter("b");
        assertEquals("the constraint did not filter the rows", 2, adapter.getCount());
        assertEquals("Bob Jones", name(adapter, 0));
        assertEquals("Carol White", name(adapter, 1));

        // A later word of a value matches, ignoring case.
        adapter.getFilter().filter("SMI");
        assertEquals(1, adapter.getCount());
        assertEquals("Alice Smith", name(adapter, 0));

        // Values not bound to a view are not searched.
        adapter.getFilter().filter("zeb");
        assertEquals(0, adapter.getCount());

        adapter.getFilter().filter("");
        assertEquals(3, adapter.getCount());
    }
}
