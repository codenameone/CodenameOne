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
    /** Parameters other than charset (a multipart boundary), serialized after it. */
    private final java.util.Map<String, String> parameters = new java.util.LinkedHashMap<String, String>();

    public ContentType(String primaryType, String subType) {
        this(primaryType, subType, null);
    }

    public ContentType(String primaryType, String subType, String charset) {
        this.primaryType = primaryType;
        this.subType = subType;
        this.charset = charset;
    }

    /** {@code ContentType(primary, sub, charset:, parameters:)}. */
    public ContentType(String primaryType, String subType, String charset, Object parameters) {
        this(primaryType, subType, charset);
        if (parameters instanceof java.util.Map) {
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) parameters).entrySet()) {
                String k = String.valueOf(e.getKey());
                if (!"charset".equalsIgnoreCase(k)) {
                    this.parameters.put(k, e.getValue() == null ? null : String.valueOf(e.getValue()));
                }
            }
        }
    }

    public java.util.Map<String, String> parameters() {
        return parameters;
    }

    /** Dart's {@code ContentType.parse}: {@code type/subtype; charset=x}. */
    public static ContentType parse(String value) {
        String[] parts = value.split(";");
        String mime = parts[0].trim();
        int slash = mime.indexOf('/');
        String cs = null;
        java.util.Map<String, String> params = new java.util.LinkedHashMap<String, String>();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.regionMatches(true, 0, "charset=", 0, 8)) {
                cs = p.substring(8).trim();
                if (cs.length() > 1 && cs.startsWith("\"") && cs.endsWith("\"")) {
                    cs = cs.substring(1, cs.length() - 1);
                }
            } else if (p.length() > 0) {
                int eq = p.indexOf('=');
                params.put(eq < 0 ? p : p.substring(0, eq).trim(), eq < 0 ? null : p.substring(eq + 1).trim());
            }
        }
        return slash < 0 ? new ContentType(mime, "", cs, params)
                : new ContentType(mime.substring(0, slash), mime.substring(slash + 1), cs, params);
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
        StringBuilder sb = new StringBuilder(mimeType());
        if (charset != null) {
            sb.append("; charset=").append(charset);
        }
        for (java.util.Map.Entry<String, String> e : parameters.entrySet()) {
            sb.append("; ").append(e.getKey());
            if (e.getValue() != null) {
                sb.append('=').append(e.getValue());
            }
        }
        return sb.toString();
    }
}
