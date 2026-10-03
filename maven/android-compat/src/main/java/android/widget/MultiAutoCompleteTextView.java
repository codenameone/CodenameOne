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
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;

/// An auto-complete field whose text is a list of tokens: suggestions are
/// filtered and inserted for the token at the cursor only.
public class MultiAutoCompleteTextView extends AutoCompleteTextView {

    public interface Tokenizer {
        int findTokenStart(CharSequence text, int cursor);

        int findTokenEnd(CharSequence text, int cursor);

        CharSequence terminateToken(CharSequence text);
    }

    /// Tokens separated by commas and the spaces after them.
    public static class CommaTokenizer implements Tokenizer {
        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != ',') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                if (text.charAt(i) == ',') {
                    return i;
                }
                i++;
            }
            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();
            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }
            if (i > 0 && text.charAt(i - 1) == ',') {
                return text;
            }
            return text + ", ";
        }
    }

    private Tokenizer mTokenizer;

    public MultiAutoCompleteTextView(Context context) {
        this(context, null);
    }

    public MultiAutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public MultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MultiAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setTokenizer(Tokenizer t) {
        mTokenizer = t;
    }

    @Override
    public boolean enoughToFilter() {
        Editable text = getText();
        int end = getSelectionEnd();
        if (end < 0 || mTokenizer == null) {
            return false;
        }
        int start = mTokenizer.findTokenStart(text, end);
        return end - start >= getThreshold();
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (enoughToFilter()) {
            int end = getSelectionEnd();
            int start = mTokenizer.findTokenStart(text, end);
            performFiltering(text, start, end, keyCode);
        } else {
            dismissDropDown();
            Filter f = getFilter();
            if (f != null) {
                f.filter(null);
            }
        }
    }

    protected void performFiltering(CharSequence text, int start, int end, int keyCode) {
        getFilter().filter(text.subSequence(start, end), this);
    }

    @Override
    protected void replaceText(CharSequence text) {
        Editable editable = getText();
        int end = getSelectionEnd();
        int start = mTokenizer == null ? 0 : mTokenizer.findTokenStart(editable, end);
        String original = TextUtils.substring(editable, start, end);
        CharSequence replacement = mTokenizer == null ? text : mTokenizer.terminateToken(text);
        if (!original.equals(replacement.toString())) {
            String before = editable.toString();
            String next = before.substring(0, start) + replacement + before.substring(end);
            setText(next);
            setSelection(start + replacement.length());
        }
    }

    @Override
    public void performValidation() {
        Validator v = getValidator();
        if (v == null || mTokenizer == null) {
            return;
        }
        Editable e = getText();
        String text = e.toString();
        StringBuilder out = new StringBuilder();
        int i = 0;
        int len = text.length();
        while (i < len) {
            int end = mTokenizer.findTokenEnd(text, i);
            String token = text.substring(mTokenizer.findTokenStart(text, end), end);
            if (token.trim().length() > 0) {
                CharSequence fixed = v.isValid(token) ? token : v.fixText(token);
                if (fixed != null && fixed.length() > 0) {
                    out.append(mTokenizer.terminateToken(fixed));
                }
            }
            i = end + 1;
        }
        if (!out.toString().equals(text)) {
            setText(out.toString());
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return MultiAutoCompleteTextView.class.getName();
    }
}
