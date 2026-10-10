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
import android.text.TextWatcher;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/// Mutating an EditText through its live Editable -- `getText().clear()`,
/// `replace()` -- notifies its TextWatchers, as setText() and keyboard edits
/// do. It used to update only the native field, silently.
public class EditTextEditableWatcherTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    private static TextWatcher recorder(final List<String> log) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                log.add("before " + s + " " + start + "," + count + "," + after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                log.add("on " + s + " " + start + "," + before + "," + count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                log.add("after " + s);
            }
        };
    }

    @Test
    public void editableMutationsReachTheWatchers() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("hello");
        List<String> log = new ArrayList<String>();
        e.addTextChangedListener(recorder(log));

        e.getText().replace(1, 5, "i");
        assertEquals("[before hello 1,4,1, on hi 1,4,1, after hi]", log.toString());

        log.clear();
        e.getText().clear();
        assertEquals("[before hi 0,2,0, on  0,2,0, after ]", log.toString());
    }

    @Test
    public void aKeyboardEditIsReportedOnce() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("ab");
        List<String> log = new ArrayList<String>();
        e.addTextChangedListener(recorder(log));
        e.onPeerTextChanged("abc");
        assertEquals("[before ab 2,0,1, on abc 2,0,1, after abc]", log.toString());
    }
    @Test
    public void filteredKeyboardEditsReportOnlyInsertedCharacters() {
        EditText e = new EditText(AndroidTestSupport.context());
        e.setText("ab");
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});
        List<String> log = new ArrayList<String>();
        e.addTextChangedListener(recorder(log));
        e.onPeerTextChanged("abcde");
        assertEquals("[before ab 2,0,1, on abc 2,0,1, after abc]", log.toString());
        log.clear();
        e.onPeerTextChanged("abcd");
        assertEquals("abc", e.getText().toString());
        assertEquals("[]", log.toString());
    }

}
