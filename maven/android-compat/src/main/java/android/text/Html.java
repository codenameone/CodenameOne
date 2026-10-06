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

/// HTML to text. Tags are dropped and entities decoded; `<br>` and block
/// tags become line breaks. Styling spans are not produced yet.
public class Html {

    public static final int FROM_HTML_MODE_LEGACY = 0;
    public static final int FROM_HTML_MODE_COMPACT = 63;
    public static final int TO_HTML_PARAGRAPH_LINES_CONSECUTIVE = 0;

    public interface ImageGetter {
        android.graphics.drawable.Drawable getDrawable(String source);
    }

    public interface TagHandler {
        void handleTag(boolean opening, String tag, Editable output, Object xmlReader);
    }

    private Html() {
    }

    public static Spanned fromHtml(String source) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY);
    }

    public static Spanned fromHtml(String source, int flags) {
        return fromHtml(source, flags, null, null);
    }

    public static Spanned fromHtml(String source, ImageGetter imageGetter, TagHandler tagHandler) {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, imageGetter, tagHandler);
    }

    public static Spanned fromHtml(String source, int flags, ImageGetter imageGetter, TagHandler tagHandler) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean space = false;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '<') {
                int close = source.indexOf('>', i);
                if (close < 0) {
                    break;
                }
                String tag = source.substring(i + 1, close).trim();
                String name = tag.startsWith("/") ? tag.substring(1) : tag;
                int sp = name.indexOf(' ');
                if (sp > 0) {
                    name = name.substring(0, sp);
                }
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                if (name.equalsIgnoreCase("br")) {
                    out.append('\n');
                } else if ((name.equalsIgnoreCase("p") || name.equalsIgnoreCase("div")
                        || name.equalsIgnoreCase("li")) && out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                    out.append('\n');
                }
                i = close + 1;
                continue;
            }
            if (c == '&') {
                int semi = source.indexOf(';', i);
                if (semi > i && semi - i < 10) {
                    String ent = source.substring(i + 1, semi);
                    String rep = entity(ent);
                    if (rep != null) {
                        out.append(rep);
                        i = semi + 1;
                        space = false;
                        continue;
                    }
                }
            }
            if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                if (!space && out.length() > 0) {
                    out.append(' ');
                }
                space = true;
            } else {
                out.append(c);
                space = false;
            }
            i++;
        }
        return new SpannableString(out.toString().trim());
    }

    private static String entity(String e) {
        if (e.equals("amp")) {
            return "&";
        }
        if (e.equals("lt")) {
            return "<";
        }
        if (e.equals("gt")) {
            return ">";
        }
        if (e.equals("quot")) {
            return "\"";
        }
        if (e.equals("apos")) {
            return "'";
        }
        if (e.equals("nbsp")) {
            return "\u00a0";
        }
        if (e.startsWith("#x") || e.startsWith("#X")) {
            try {
                return codePoint(Integer.parseInt(e.substring(2), 16));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (e.startsWith("#")) {
            try {
                return codePoint(Integer.parseInt(e.substring(1)));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    /// A numeric entity's character; one past `U+FFFF` becomes its surrogate
    /// pair rather than a truncated BMP character. Out of range, it is left
    /// undecoded.
    private static String codePoint(int cp) {
        if (cp < 0 || cp > 0x10FFFF) {
            return null;
        }
        return new String(Character.toChars(cp));
    }

    public static String toHtml(Spanned text) {
        return TextUtils.htmlEncode(text.toString());
    }

    public static String toHtml(Spanned text, int option) {
        return toHtml(text);
    }

    public static String escapeHtml(CharSequence text) {
        return TextUtils.htmlEncode(text.toString());
    }
}
