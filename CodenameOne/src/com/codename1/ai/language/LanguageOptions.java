/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

/** Common options for on-device language services. */
public final class LanguageOptions {
    private LanguageBackend backend = LanguageBackends.auto();
    private float minimumConfidence;

    public LanguageOptions backend(LanguageBackend value) {
        backend = value == null ? LanguageBackends.auto() : value;
        return this;
    }

    public LanguageOptions minimumConfidence(float value) {
        minimumConfidence = Math.max(0, Math.min(1, value));
        return this;
    }

    public LanguageBackend getBackend() {
        return backend;
    }

    public float getMinimumConfidence() {
        return minimumConfidence;
    }
}
