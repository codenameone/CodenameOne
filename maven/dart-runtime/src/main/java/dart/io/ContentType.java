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
package dart.io;

/** Dart's {@code ContentType}: a MIME type with an optional charset. */
public final class ContentType {
    public static final ContentType text = new ContentType("text", "plain", "utf-8");
    public static final ContentType html = new ContentType("text", "html", "utf-8");
    public static final ContentType json = new ContentType("application", "json", "utf-8");
    public static final ContentType binary = new ContentType("application", "octet-stream", null);

    private final String primaryType;
    private final String subType;
    private final String charset;

    public ContentType(String primaryType, String subType) {
        this(primaryType, subType, null);
    }

    public ContentType(String primaryType, String subType, String charset) {
        this.primaryType = primaryType;
        this.subType = subType;
        this.charset = charset;
    }

    /** {@code ContentType(primary, sub, charset:, parameters:)}; parameters are ignored. */
    public ContentType(String primaryType, String subType, String charset, Object parameters) {
        this(primaryType, subType, charset);
    }

    /** Dart's {@code ContentType.parse}: {@code type/subtype; charset=x}. */
    public static ContentType parse(String value) {
        String[] parts = value.split(";");
        String mime = parts[0].trim();
        int slash = mime.indexOf('/');
        String cs = null;
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.regionMatches(true, 0, "charset=", 0, 8)) {
                cs = p.substring(8).trim();
                if (cs.length() > 1 && cs.startsWith("\"") && cs.endsWith("\"")) {
                    cs = cs.substring(1, cs.length() - 1);
                }
            }
        }
        return slash < 0 ? new ContentType(mime, "", cs)
                : new ContentType(mime.substring(0, slash), mime.substring(slash + 1), cs);
    }

    public String primaryType() {
        return primaryType;
    }

    public String subType() {
        return subType;
    }

    public String mimeType() {
        return primaryType + "/" + subType;
    }

    public String charset() {
        return charset;
    }

    public String value() {
        return mimeType();
    }

    @Override
    public String toString() {
        return charset == null ? mimeType() : mimeType() + "; charset=" + charset;
    }
}
