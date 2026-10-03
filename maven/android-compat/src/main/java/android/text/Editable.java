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
package android.text;

/// Text that can be edited in place: what `EditText.getText()` returns.
public interface Editable extends CharSequence, GetChars, Spannable, Appendable {

    Editable replace(int st, int en, CharSequence source, int start, int end);

    Editable replace(int st, int en, CharSequence text);

    Editable insert(int where, CharSequence text, int start, int end);

    Editable insert(int where, CharSequence text);

    Editable delete(int st, int en);

    @Override
    Editable append(CharSequence text);

    @Override
    Editable append(CharSequence text, int start, int end);

    @Override
    Editable append(char text);

    void clear();

    void clearSpans();

    void setFilters(InputFilter[] filters);

    InputFilter[] getFilters();

    class Factory {
        private static final Factory INSTANCE = new Factory();

        public static Factory getInstance() {
            return INSTANCE;
        }

        public Editable newEditable(CharSequence source) {
            return new SpannableStringBuilder(source);
        }
    }
}
