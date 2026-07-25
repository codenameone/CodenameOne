/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Creates reusable foreground/person segmentation analyzers. */
public final class SelfieSegmenter extends AbstractVisionAnalyzer<SegmentationMask> {
    public SelfieSegmenter() {
        this(null);
    }

    public SelfieSegmenter(VisionOptions options) {
        super(VisionFeature.SELFIE_SEGMENTATION, options);
    }
}
