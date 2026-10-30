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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * firebase_ai's {@code Tool}. {@code googleSearch} and {@code codeExecution} are sent as the
 * REST API's tool entries; {@code functionDeclarations} is accepted and not sent (there is no
 * function-calling loop here), so the model never answers with a function call.
 */
public class Tool {
    private final String kind;

    private Tool(String kind) {
        this.kind = kind;
    }

    public static Tool functionDeclarations(Iterable<?> declarations) {
        return new Tool(null);
    }

    public static Tool googleSearch(Object googleSearch) {
        return new Tool("googleSearch");
    }

    public static Tool codeExecution() {
        return new Tool("codeExecution");
    }

    /** The tool entry, or null when this kind is not sent. */
    public Map<String, Object> toJson() {
        if (kind == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(kind, new LinkedHashMap<String, Object>());
        return m;
    }
}
