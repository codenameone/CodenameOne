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

/// Filters text as it is inserted into an editable.
public interface InputFilter {

    CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend);

    class LengthFilter implements InputFilter {
        private final int max;

        public LengthFilter(int max) {
            this.max = max;
        }

        public int getMax() {
            return max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            int keep = max - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                return "";
            }
            if (keep >= end - start) {
                return null;
            }
            int cut = start + keep;
            if (Character.isHighSurrogate(source.charAt(cut - 1))) {
                // Never keep half of a surrogate pair, as on Android.
                cut--;
                if (cut == start) {
                    return "";
                }
            }
            return source.subSequence(start, cut);
        }
    }

    class AllCaps implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String s = source.subSequence(start, end).toString();
            // Android's own uppercasing (String.toUpperCase in the default
            // locale), deliberately not an ASCII fold: this is user text.
            String u = s.toUpperCase();
            if (u.equals(s)) {
                return null;
            }
            if (!(source instanceof Spanned)) {
                return u;
            }
            // Keep the inserted text's styling, as Android does. A case
            // mapping that changes the length (German sharp s) only clamps the
            // offsets rather than mapping them character by character.
            Spanned sp = (Spanned) source;
            SpannableString out = new SpannableString(u);
            int len = u.length();
            Object[] spans = sp.getSpans(start, end, Object.class);
            for (Object span : spans) {
                int spanStart = sp.getSpanStart(span);
                int spanEnd = sp.getSpanEnd(span);
                int st = Math.min(Math.max(spanStart, start) - start, len);
                int en = Math.min(Math.min(spanEnd, end) - start, len);
                if (en < st || (en == st && spanEnd > spanStart)) {
                    // Only touches the range from outside.
                    continue;
                }
                out.setSpan(span, st, en, sp.getSpanFlags(span));
            }
            return out;
        }
    }
}
