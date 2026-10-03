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

import java.util.ArrayList;
import java.util.Iterator;

/// String helpers.
public class TextUtils {

    public enum TruncateAt { START, MIDDLE, END, MARQUEE, END_SMALL }

    public static final int CAP_MODE_CHARACTERS = 0x1000;
    public static final int CAP_MODE_WORDS = 0x2000;
    public static final int CAP_MODE_SENTENCES = 0x4000;

    private TextUtils() {
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        return a.toString().equals(b.toString());
    }

    public static int getTrimmedLength(CharSequence s) {
        int len = s.length();
        int start = 0;
        while (start < len && s.charAt(start) <= ' ') {
            start++;
        }
        int end = len;
        while (end > start && s.charAt(end - 1) <= ' ') {
            end--;
        }
        return end - start;
    }

    public static String join(CharSequence delimiter, Object[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    public static String join(CharSequence delimiter, Iterable tokens) {
        StringBuilder sb = new StringBuilder();
        Iterator it = tokens.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                sb.append(delimiter);
            }
            first = false;
            sb.append(it.next());
        }
        return sb.toString();
    }

    /// Splits on a literal separator (Android treats it as a regular
    /// expression; plain separators are by far the common use).
    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return new String[0];
        }
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        while (true) {
            int i = text.indexOf(expression, start);
            if (i < 0 || expression.length() == 0) {
                out.add(text.substring(start));
                break;
            }
            out.add(text.substring(start, i));
            start = i + expression.length();
        }
        return out.toArray(new String[out.size()]);
    }

    public static CharSequence concat(CharSequence... text) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (CharSequence t : text) {
            sb.append(t);
        }
        return sb;
    }

    public static boolean isDigitsOnly(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isGraphic(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String htmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static int indexOf(CharSequence s, char ch) {
        return s.toString().indexOf(ch);
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint p, float avail, TruncateAt where) {
        String s = text.toString();
        if (p.measureText(s) <= avail) {
            return text;
        }
        String ell = "\u2026";
        int n = s.length();
        while (n > 0 && p.measureText(s.substring(0, n) + ell) > avail) {
            n--;
        }
        return s.substring(0, n) + ell;
    }

    public static String substring(CharSequence source, int start, int end) {
        return source.subSequence(start, end).toString();
    }

    public static CharSequence stringOrSpannedString(CharSequence source) {
        return source;
    }

    public static int getLayoutDirectionFromLocale(java.util.Locale locale) {
        if (locale == null) {
            return 0;
        }
        String l = locale.getLanguage();
        return "ar".equals(l) || "he".equals(l) || "iw".equals(l) || "fa".equals(l) || "ur".equals(l) ? 1 : 0;
    }

    public static CharSequence commaEllipsize(CharSequence text, TextPaint p, float avail, String oneMore,
                                              String more) {
        return ellipsize(text, p, avail, TruncateAt.END);
    }
}
