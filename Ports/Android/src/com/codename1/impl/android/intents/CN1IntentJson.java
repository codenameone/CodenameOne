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
package com.codename1.impl.android.intents;

import java.util.ArrayList;
import java.util.List;

/// A deliberately tiny reader for the two framework-produced documents this port consumes.
///
/// Not a JSON parser and not trying to be one. The payloads come from Codename One's own
/// serializer, so their shape is known, and the alternative -- adding a JSON dependency to the
/// Android port -- would be paid for by every application whether or not it uses intents. Any
/// value it cannot make sense of is skipped rather than guessed at.
final class CN1IntentJson {

    private CN1IntentJson() {
    }

    /// Returns `{uid, title, subtitle}` for each entity in an index document.
    static List<String[]> entities(String json) {
        List<String[]> out = new ArrayList<String[]>();
        if (json == null) {
            return out;
        }
        int at = 0;
        while (true) {
            int uidAt = json.indexOf("\"uid\"", at);
            if (uidAt < 0) {
                break;
            }
            String uid = valueAfter(json, uidAt);
            if (uid == null) {
                break;
            }
            // Bounded to this entity's object so a title belonging to the next entry cannot be
            // picked up when this one omits it.
            int end = json.indexOf("\"uid\"", uidAt + 5);
            String scope = end < 0 ? json.substring(uidAt) : json.substring(uidAt, end);
            out.add(new String[]{uid, field(scope, "title"), field(scope, "subtitle")});
            at = uidAt + 5;
        }
        return out;
    }

    /// Returns the uids named in a removal document.
    static List<String> refs(String json) {
        List<String> out = new ArrayList<String>();
        if (json == null) {
            return out;
        }
        int at = 0;
        while (true) {
            int uidAt = json.indexOf("\"uid\"", at);
            if (uidAt < 0) {
                break;
            }
            String uid = valueAfter(json, uidAt);
            if (uid != null) {
                out.add(uid);
            }
            at = uidAt + 5;
        }
        return out;
    }

    private static String field(String scope, String name) {
        int at = scope.indexOf("\"" + name + "\"");
        return at < 0 ? null : valueAfter(scope, at);
    }

    /// Reads the string value that follows a key, honouring backslash escapes.
    private static String valueAfter(String json, int keyAt) {
        int colon = json.indexOf(':', keyAt);
        if (colon < 0) {
            return null;
        }
        int open = json.indexOf('"', colon);
        if (open < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = open + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    default: sb.append(next);
                }
                continue;
            }
            if (c == '"') {
                return sb.toString();
            }
            sb.append(c);
        }
        return null;
    }
}
