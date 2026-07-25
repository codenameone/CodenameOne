/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Portable image classification label. */
public final class ImageLabel {
    private final String text;
    private final float confidence;
    private final int index;
    private final VisionMetadata metadata;

    public ImageLabel(String text, float confidence, int index) {
        this(text, confidence, index, null);
    }

    public ImageLabel(String text, float confidence, int index,
                      VisionMetadata metadata) {
        this.text = text == null ? "" : text;
        this.confidence = confidence;
        this.index = index;
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getIndex() {
        return index;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }
}
