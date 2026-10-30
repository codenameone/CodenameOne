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
package com.codename1.flutter.plugins.firebaseai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.util.Base64;

import dart.core.DartList;
import dart.core.DartMap;
import dart.typed_data.Uint8List;

/** firebase_ai's {@code Content}: a role and its parts. */
public class Content {
    private final String role;
    private final DartList<Part> parts = new DartList<Part>();

    public Content(String role, Iterable<?> parts) {
        this.role = role;
        if (parts != null) {
            for (Object o : parts) {
                if (o instanceof Part) {
                    this.parts.add((Part) o);
                }
            }
        }
    }

    public static Content text(String text) {
        return new Content("user", DartList.of(new TextPart(text)));
    }

    public static Content multi(Iterable<?> parts) {
        return new Content("user", parts);
    }

    public static Content model(Iterable<?> parts) {
        return new Content("model", parts);
    }

    public static Content data(String mimeType, Uint8List bytes) {
        return new Content("user", DartList.of(new InlineDataPart(mimeType, bytes)));
    }

    public String role() {
        return role;
    }

    public DartList<Part> parts() {
        return parts;
    }

    public DartMap<String, Object> toJson() {
        DartMap<String, Object> m = new DartMap<String, Object>();
        if (role != null) {
            m.put("role", role);
        }
        DartList<Object> ps = new DartList<Object>();
        for (Part p : parts) {
            ps.add(p.toJson());
        }
        m.put("parts", ps);
        return m;
    }

    /** Rebuilds a Content from the REST API's JSON object; parts of a kind not modelled here are dropped. */
    static Content fromMap(Map<?, ?> json) {
        List<Part> out = new ArrayList<Part>();
        Object ps = json.get("parts");
        if (ps instanceof List) {
            for (Object o : (List<?>) ps) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<?, ?> p = (Map<?, ?>) o;
                Object t = p.get("text");
                Object inline = p.get("inlineData");
                if (t instanceof String) {
                    out.add(new TextPart((String) t));
                } else if (inline instanceof Map) {
                    Map<?, ?> d = (Map<?, ?>) inline;
                    Object mime = d.get("mimeType");
                    Object data = d.get("data");
                    if (data instanceof String) {
                        out.add(new InlineDataPart(mime instanceof String ? (String) mime : null,
                                Uint8List.fromBytes(Base64.decode(((String) data).getBytes()))));
                    }
                }
            }
        }
        Object r = json.get("role");
        return new Content(r instanceof String ? (String) r : null, out);
    }
}
