/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Optional backend identity and backend-specific string values attached to a
 * vision result. Portable code should use the typed result fields first.
 */
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
