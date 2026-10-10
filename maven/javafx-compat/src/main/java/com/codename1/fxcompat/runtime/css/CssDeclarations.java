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
package com.codename1.fxcompat.runtime.css;

import java.util.List;

/// Reads a list of declarations -- the text between the braces of a rule,
/// or an inline `style` string -- into [CssDeclaration]s.
///
/// CSS is forgiving, and so is this: a declaration that cannot be read is
/// reported to the [Reporter] and left out, and the ones around it are
/// kept. Only the caller knows what a report is worth: the build prints it
/// with the file and the line, the device logs it once.
public final class CssDeclarations {

    /// Where the declarations that were left out are reported.
    public interface Reporter {
        /// Reports the declaration starting at `offset` in the text that was
        /// parsed.
        void warning(int offset, String message);
    }

    private CssDeclarations() {
    }

    /// `text` with every comment replaced by spaces, so that what is left
    /// keeps its offsets and its line numbers. A comment left open runs to
    /// the end of the text. Two slashes outside quotes and parentheses
    /// start a comment that ends with its line, as in JavaFX.
    public static String stripComments(String text) {
        if (text.indexOf("/*") < 0 && text.indexOf("//") < 0) {
            return text;
        }
        char[] out = text.toCharArray();
        int n = out.length;
        char quote = 0;
        int depth = 0;
        for (int i = 0; i < n; i++) {
            char c = out[i];
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '/' && i + 1 < n && out[i + 1] == '*') {
                int end = text.indexOf("*/", i + 2);
                end = end < 0 ? n : end + 2;
                for (int j = i; j < end; j++) {
                    if (out[j] != '\n' && out[j] != '\r') {
                        out[j] = ' ';
                    }
                }
                i = end - 1;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth = depth > 0 ? depth - 1 : 0;
            } else if (c == '/' && depth == 0 && i + 1 < n && out[i + 1] == '/') {
                // A comment to the end of the line, which JavaFX reads
                // though CSS has none. Inside parentheses it is the two
                // slashes of an address.
                while (i < n && out[i] != '\n' && out[i] != '\r') {
                    out[i++] = ' ';
                }
                i--;
            }
        }
        return new String(out);
    }

    /// Parses the declarations in `text` between `from` and `to`, which
    /// holds no comments, and adds them to `out` in the order written.
    /// `parser` is the caller's, so that one is enough for a whole style
    /// sheet.
    public static void parse(String text, int from, int to, CssValueParser parser, Reporter reporter,
            List<CssDeclaration> out) {
        int i = from;
        while (i < to) {
            while (i < to && (CssValueParser.isSpace(text.charAt(i)) || text.charAt(i) == ';')) {
                i++;
            }
            if (i >= to) {
                break;
            }
            int start = i;
            int colon = -1;
            int depth = 0;
            char quote = 0;
            for (; i < to; i++) {
                char c = text.charAt(i);
                if (quote != 0) {
                    if (c == '\\') {
                        i++;
                    } else if (c == quote) {
                        quote = 0;
                    }
                } else if (c == '"' || c == '\'') {
                    quote = c;
                } else if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (c == ':' && colon < 0 && depth == 0) {
                    colon = i;
                } else if (c == ';' && depth <= 0) {
                    break;
                }
            }
            int end = Math.min(i, to);
            if (colon < 0) {
                reporter.warning(start, "'" + text.substring(start, end).trim() + "' is not a declaration");
                continue;
            }
            String name = CssProperties.lower(text.substring(start, colon).trim());
            String value = text.substring(colon + 1, end).trim();
            boolean important = false;
            int bang = value.lastIndexOf('!');
            if (bang >= 0 && "important".equals(CssProperties.lower(value.substring(bang + 1).trim()))) {
                important = true;
                value = value.substring(0, bang).trim();
            }
            if (name.length() == 0) {
                reporter.warning(start, "a declaration needs a property name");
                continue;
            }
            if (CssProperties.kind(name) == CssProperties.UNKNOWN && !CssProperties.isDefinitionName(name)) {
                reporter.warning(start, name + " is not a property this layer styles; the declaration is ignored");
                continue;
            }
            CssValue parsed = parser.parse(name, value);
            if (parsed == null) {
                reporter.warning(start, name + ": '" + value + "': " + parser.error()
                        + "; the declaration is ignored");
                continue;
            }
            if (parser.warning() != null) {
                reporter.warning(start, name + ": " + parser.warning());
            }
            out.add(new CssDeclaration(name, CssValueParser.kindOf(name), parsed, important));
        }
    }
}
