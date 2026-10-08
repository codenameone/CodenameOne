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

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/// setText() runs the InputFilter chain over the new text, and a TextView
/// with watchers delivers afterTextChanged, as on Android. Filters used to be
/// installed only after the unfiltered text was stored, and a plain
/// TextView's watchers never saw the final callback.
public class TextViewFilterAndWatcherTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    @Test
    public void setTextHonoursTheLengthFilterOnAnEditText() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setFilters(new InputFilter[] {new InputFilter.LengthFilter(4)});
        e.setText("12345");
        assertEquals("1234", e.getText().toString());
    }

    @Test
    public void setTextHonoursFiltersOnAPlainTextView() {
        TextView t = new TextView(AndroidTestSupport.context());
        t.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
        t.setText("abc");
        assertEquals("ABC", t.getText().toString());
    }

    private static TextWatcher recorder(final List<String> log) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                log.add("before " + s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                log.add("on " + s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                log.add("after " + s);
            }
        };
    }

    @Test
    public void aPlainTextViewDeliversAfterTextChanged() {
        TextView t = new TextView(AndroidTestSupport.context());
        t.setText("a");
        List<String> log = new ArrayList<String>();
        t.addTextChangedListener(recorder(log));
        t.setText("b");
        assertEquals("[before a, on b, after b]", log.toString());
    }

    @Test
    public void appendToAWatchedTextViewNotifiesOnce() {
        TextView t = new TextView(AndroidTestSupport.context());
        List<String> log = new ArrayList<String>();
        t.addTextChangedListener(recorder(log));
        t.setText("a");
        log.clear();
        t.append("b");
        assertEquals("ab", t.getText().toString());
        assertEquals("[before a, on ab, after ab]", log.toString());
    }
}
