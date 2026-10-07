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

import com.codename1.util.regex.RE;

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

    /// Splits around matches of the regular expression `expression`, keeping
    /// trailing empty strings, as Android does (`text.split(expression, -1)`).
    /// An empty text gives an empty array. The expression is evaluated by
    /// Codename One's `RE`, which covers the usual separators (`"\\s+"`,
    /// `"[,;]"`, `"\\|"`); constructs it lacks, such as lookaround, are not
    /// supported.
    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return new String[0];
        }
        ArrayList<String> out = new ArrayList<String>();
        if (expression.length() == 0) {
            // The empty expression matches between every two characters and
            // at the end, so Java yields each character plus a trailing "".
            for (int i = 0; i < text.length(); i++) {
                out.add(text.substring(i, i + 1));
            }
            out.add("");
            return out.toArray(new String[out.size()]);
        }
        if (!hasRegexMeta(expression)) {
            int start = 0;
            while (true) {
                int i = text.indexOf(expression, start);
                if (i < 0) {
                    out.add(text.substring(start));
                    break;
                }
                out.add(text.substring(start, i));
                start = i + expression.length();
            }
            return out.toArray(new String[out.size()]);
        }
        RE re = new RE(expression);
        int len = text.length();
        int last = 0;
        int pos = 0;
        while (pos < len && re.match(text, pos)) {
            int ms = re.getParenStart(0);
            int me = re.getParenEnd(0);
            if (me == ms) {
                // A zero-width match splits between characters; one at the
                // very start yields no leading empty string, as in Java.
                if (ms > 0) {
                    out.add(text.substring(last, ms));
                    last = ms;
                }
                pos = ms + 1;
            } else {
                out.add(text.substring(last, ms));
                last = me;
                pos = me;
            }
        }
        out.add(text.substring(last));
        return out.toArray(new String[out.size()]);
    }

    private static boolean hasRegexMeta(String expression) {
        for (int i = 0; i < expression.length(); i++) {
            if (".$|()[]{}^?*+\\".indexOf(expression.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
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
        int mode = where == TruncateAt.START ? com.codename1.androidcompat.runtime.TextLayout.ELLIPSIZE_START
                : where == TruncateAt.MIDDLE ? com.codename1.androidcompat.runtime.TextLayout.ELLIPSIZE_MIDDLE
                : com.codename1.androidcompat.runtime.TextLayout.ELLIPSIZE_END;
        String shortened = com.codename1.androidcompat.runtime.TextLayout.ellipsize(s, p, avail, mode);
        if (!(text instanceof Spanned)) {
            return shortened;
        }
        // TextLayout retains a prefix and/or suffix around one ellipsis.
        // Derive their lengths from its result, not by searching for an
        // ellipsis that may also have been present in the original text.
        int kept = shortened.length() - 1;
        int head = where == TruncateAt.START ? 0 : where == TruncateAt.MIDDLE ? (kept + 1) / 2 : kept;
        int tailStart = s.length() - (kept - head);
        Spanned source = (Spanned) text;
        SpannableString result = new SpannableString(shortened);
        for (Object span : source.getSpans(0, s.length(), Object.class)) {
            int start = source.getSpanStart(span);
            int end = source.getSpanEnd(span);
            // Clip to retained characters and shift the suffix. A span
            // crossing both pieces remains one span across the ellipsis.
            int mappedStart = start < head ? start : Math.max(start, tailStart) - tailStart + head + 1;
            int mappedEnd = end > tailStart ? end - tailStart + head + 1 : Math.min(end, head);
            if (mappedStart < mappedEnd) {
                result.setSpan(span, mappedStart, mappedEnd, source.getSpanFlags(span));
            }
        }
        return result;
    }

    public static String substring(CharSequence source, int start, int end) {
        return source.subSequence(start, end).toString();
    }

    /// An immutable snapshot of `source`: a String, or a [SpannedString]
    /// when it carries spans, so a later edit of a mutable source cannot
    /// reach the copy.
    public static CharSequence stringOrSpannedString(CharSequence source) {
        if (source == null) {
            return null;
        }
        if (source instanceof SpannedString) {
            return source;
        }
        if (source instanceof Spanned) {
            return new SpannedString(source);
        }
        return source.toString();
    }

    public static int getLayoutDirectionFromLocale(java.util.Locale locale) {
        if (locale == null) {
            return 0;
        }
        String l = locale.getLanguage();
        return "ar".equals(l) || "he".equals(l) || "iw".equals(l) || "fa".equals(l) || "ur".equals(l) ? 1 : 0;
    }

    /// Fits a comma-separated list into `avail` by keeping whole items: the
    /// longest run of leading items (each with its comma) that fits together
    /// with the suffix `" " + oneMore` when one item is dropped, or
    /// `" " + String.format(more, count)` when `count` are. As on Android, the
    /// result is empty when not even the first item fits with its suffix.
    public static CharSequence commaEllipsize(CharSequence text, TextPaint p, float avail, String oneMore,
                                              String more) {
        String s = text.toString();
        if (p.measureText(s) <= avail) {
            return text;
        }
        int len = s.length();
        int remaining = 1;
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == ',') {
                remaining++;
            }
        }
        int ok = 0;
        String okFormat = "";
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) != ',') {
                continue;
            }
            remaining--;
            String format = remaining == 1 ? " " + oneMore : " " + String.format(more, remaining);
            if (p.measureText(s, 0, i + 1) + p.measureText(format) <= avail) {
                ok = i + 1;
                okFormat = format;
            }
        }
        SpannableStringBuilder out = new SpannableStringBuilder(okFormat);
        out.insert(0, text, 0, ok);
        return out;
    }
}
