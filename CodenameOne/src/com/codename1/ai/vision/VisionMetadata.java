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
package com.codename1.ai.vision;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Optional backend identity and backend-specific string values attached to a
/// vision result. Portable code should use the typed result fields first.
public final class VisionMetadata {
    private final String backendId;
    private final Map<String, String> values;

    public VisionMetadata(String backendId, Map<String, String> values) {
        this.backendId = backendId;
        this.values = values == null || values.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(values));
    }

    public VisionMetadata(String backendId) {
        this(backendId, null);
    }

    public String getBackendId() {
        return backendId;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public String get(String key) {
        return values.get(key);
    }
}
