/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.convert;

/// Dart's {@code HtmlEscape} (and the {@code htmlEscape} constant): the default mode
/// escapes {@code & < > " ' /}, as Dart's {@code HtmlEscapeMode.unknown} does.
public final class HtmlEscape {

    private final HtmlEscapeMode mode;

    public HtmlEscape() {
        this(HtmlEscapeMode.unknown);
    }

    /// {@code HtmlEscape(mode)}: {@code &} is always escaped, the rest as the mode says.
    public HtmlEscape(HtmlEscapeMode mode) {
        this.mode = mode == null ? HtmlEscapeMode.unknown : mode;
    }

    public String convert(String text) {
        StringBuilder sb = null;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            String r;
            switch (ch) {
                case '&': r = "&amp;"; break;
                case '<': r = mode.escapeLtGt ? "&lt;" : null; break;
                case '>': r = mode.escapeLtGt ? "&gt;" : null; break;
                case '"': r = mode.escapeQuot ? "&quot;" : null; break;
                case '\'': r = mode.escapeApos ? "&#39;" : null; break;
                case '/': r = mode.escapeSlash ? "&#47;" : null; break;
                default: r = null;
            }
            if (r != null && sb == null) {
                sb = new StringBuilder(text.length() + 16);
                sb.append(text, 0, i);
            }
            if (sb != null) {
                if (r != null) {
                    sb.append(r);
                } else {
                    sb.append(ch);
                }
            }
        }
        return sb == null ? text : sb.toString();
    }
}
