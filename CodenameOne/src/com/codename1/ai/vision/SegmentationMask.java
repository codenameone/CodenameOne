/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Per-pixel foreground confidence mask. */
public final class SegmentationMask {
    private final int width;
    private final int height;
    private final float[] confidence;
    private final VisionMetadata metadata;

    public SegmentationMask(int width, int height, float[] confidence) {
        this(width, height, confidence, null);
    }

    public SegmentationMask(int width, int height, float[] confidence,
                            VisionMetadata metadata) {
        if (width < 0 || height < 0
                || confidence == null || confidence.length != width * height) {
            throw new IllegalArgumentException("Mask dimensions do not match its data");
        }
        this.width = width;
        this.height = height;
        this.confidence = new float[confidence.length];
        System.arraycopy(confidence, 0, this.confidence, 0, confidence.length);
        this.metadata = metadata;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float[] getConfidence() {
        float[] out = new float[confidence.length];
        System.arraycopy(confidence, 0, out, 0, confidence.length);
        return out;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }
}
