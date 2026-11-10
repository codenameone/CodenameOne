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

/// Dart's {@code HtmlEscapeMode}: which characters besides {@code &} an
/// {@link HtmlEscape} replaces. Named arguments arrive as Object because an omitted
/// one may be the absent marker; only an explicit {@code true} turns a flag on.
public final class HtmlEscapeMode {
    public static final HtmlEscapeMode unknown = new HtmlEscapeMode("unknown", true, true, true, true);
    public static final HtmlEscapeMode attribute = new HtmlEscapeMode("attribute", true, true, false, false);
    public static final HtmlEscapeMode sqAttribute = new HtmlEscapeMode("attribute", true, false, true, false);
    public static final HtmlEscapeMode element = new HtmlEscapeMode("element", true, false, false, false);

    String name;
    boolean escapeLtGt;
    boolean escapeQuot;
    boolean escapeApos;
    boolean escapeSlash;

    /// {@code HtmlEscapeMode()}: a custom mode that escapes only {@code &}.
    public HtmlEscapeMode() {
        this("custom", false, false, false, false);
    }

    private HtmlEscapeMode(String name, boolean ltGt, boolean quot, boolean apos, boolean slash) {
        this.name = name;
        this.escapeLtGt = ltGt;
        this.escapeQuot = quot;
        this.escapeApos = apos;
        this.escapeSlash = slash;
    }

    public HtmlEscapeMode(Object name, Object escapeLtGt, Object escapeQuot, Object escapeApos,
            Object escapeSlash) {
        this(name instanceof String ? (String) name : "custom", Boolean.TRUE.equals(escapeLtGt),
                Boolean.TRUE.equals(escapeQuot), Boolean.TRUE.equals(escapeApos),
                Boolean.TRUE.equals(escapeSlash));
    }

    // A stub constructor's named arguments may be applied to a fresh instance one
    // setter per argument (`new HtmlEscapeMode()` then `escapeLtGt(true)`).
    public void name(String v) {
        name = v;
    }

    public void escapeLtGt(boolean v) {
        escapeLtGt = v;
    }

    public void escapeQuot(boolean v) {
        escapeQuot = v;
    }

    public void escapeApos(boolean v) {
        escapeApos = v;
    }

    public void escapeSlash(boolean v) {
        escapeSlash = v;
    }

    @Override
    public String toString() {
        return name;
    }
}
