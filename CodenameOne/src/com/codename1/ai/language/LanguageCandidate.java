/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

/** Language identification candidate. */
public final class LanguageCandidate {
    private final String languageTag;
    private final float confidence;

    public LanguageCandidate(String languageTag, float confidence) {
        this.languageTag = languageTag;
        this.confidence = confidence;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public float getConfidence() {
        return confidence;
    }
}
