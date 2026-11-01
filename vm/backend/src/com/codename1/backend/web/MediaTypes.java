/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.web;

/// Media-type matching for a route's `consumes` and `produces`, called by the
/// generated router before it binds anything.
///
/// Type names are compared without case folding through `toLowerCase`, which is
/// locale sensitive; parameters (`; charset=utf-8`) are ignored, and either side
/// may use a `*` wildcard for the type or the subtype.
///
/// Internal to generated code.
public final class MediaTypes {
    private MediaTypes() {
    }

    /// Whether a request whose `Content-Type` is `contentType` may reach a route
    /// that consumes `accepted`, a comma separated list. A request with no
    /// `Content-Type` is not consumable: it is answered 415, as Spring answers it.
    public static boolean consumable(String contentType, String accepted) {
        if (contentType == null) {
            return false;
        }
        String type = bare(contentType, 0, contentType.length());
        if (type.length() == 0) {
            return false;
        }
        int pos = 0;
        while (pos <= accepted.length()) {
            int comma = accepted.indexOf(',', pos);
            int end = comma < 0 ? accepted.length() : comma;
            if (matches(bare(accepted, pos, end), type)) {
                return true;
            }
            if (comma < 0) {
                break;
            }
            pos = comma + 1;
        }
        return false;
    }

    /// Whether a request whose `Accept` header is `accept` takes any of
    /// `offered`, a comma separated list; when it does not, the route answers 406.
    /// No `Accept` header accepts anything, and a range with `q=0` accepts nothing.
    public static boolean acceptable(String accept, String offered) {
        if (accept == null || accept.trim().length() == 0) {
            return true;
        }
        int pos = 0;
        while (pos <= accept.length()) {
            int comma = accept.indexOf(',', pos);
            int end = comma < 0 ? accept.length() : comma;
            if (!refused(accept, pos, end)) {
                String range = bare(accept, pos, end);
                int o = 0;
                while (range.length() > 0 && o <= offered.length()) {
                    int oc = offered.indexOf(',', o);
                    int oe = oc < 0 ? offered.length() : oc;
                    if (matches(range, bare(offered, o, oe))) {
                        return true;
                    }
                    if (oc < 0) {
                        break;
                    }
                    o = oc + 1;
                }
            }
            if (comma < 0) {
                break;
            }
            pos = comma + 1;
        }
        return false;
    }

    /// The media type in `value[from, to)`, without parameters or whitespace.
    private static String bare(String value, int from, int to) {
        int semi = value.indexOf(';', from);
        if (semi >= 0 && semi < to) {
            to = semi;
        }
        return value.substring(from, to).trim();
    }

    /// Whether the range in `value[from, to)` carries `q=0`, which excludes it.
    private static boolean refused(String value, int from, int to) {
        int semi = value.indexOf(';', from);
        while (semi >= 0 && semi < to) {
            int next = value.indexOf(';', semi + 1);
            int end = next < 0 || next > to ? to : next;
            String param = value.substring(semi + 1, end).trim();
            if (param.length() > 2 && (param.charAt(0) == 'q' || param.charAt(0) == 'Q') && param.charAt(1) == '=') {
                String q = param.substring(2).trim();
                boolean zero = q.length() > 0;
                for (int i = 0; i < q.length(); i++) {
                    char c = q.charAt(i);
                    if (c != '0' && c != '.') {
                        zero = false;
                    }
                }
                return zero;
            }
            semi = next < 0 || next > to ? -1 : next;
        }
        return false;
    }

    /// Whether two media types match, a `*` on either side matching anything in
    /// its place.
    private static boolean matches(String a, String b) {
        int sa = a.indexOf('/');
        int sb = b.indexOf('/');
        if (sa < 0 || sb < 0) {
            return "*".equals(a) || "*".equals(b);
        }
        return part(a, 0, sa, b, 0, sb) && part(a, sa + 1, a.length(), b, sb + 1, b.length());
    }

    private static boolean part(String a, int af, int at, String b, int bf, int bt) {
        if (at - af == 1 && a.charAt(af) == '*' || bt - bf == 1 && b.charAt(bf) == '*') {
            return true;
        }
        return at - af == bt - bf && a.regionMatches(true, af, b, bf, at - af);
    }
}
