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

/// Optional backend identity and backend-specific diagnostic strings attached
/// to a vision result. Portable application logic should use typed result
/// fields first; metadata keys are intentionally not guaranteed across
/// backends. The values map is immutable.
public final class VisionMetadata {
    private final String backendId;
    private final Map<String, String> values;

    /// Creates backend metadata with optional diagnostic values.
    /// @param backendId stable portable backend id
    /// @param values backend-specific string diagnostics, defensively copied
    public VisionMetadata(String backendId, Map<String, String> values) {
        this.backendId = backendId;
        this.values = values == null || values.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(values));
    }

    /// Creates metadata containing only the selected backend id.
    /// @param backendId stable portable backend id
    public VisionMetadata(String backendId) {
        this(backendId, null);
    }

    /// @return stable backend identifier such as {@code apple-vision} or {@code ml-kit}
    public String getBackendId() {
        return backendId;
    }

    public Map<String, String> getValues() {
        return values;
    }

    /// @param key backend-defined diagnostic key
    /// @return associated value, or {@code null}
    public String get(String key) {
        return values.get(key);
    }
}
