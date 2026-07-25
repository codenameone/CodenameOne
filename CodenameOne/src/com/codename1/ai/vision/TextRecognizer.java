/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Creates reusable on-device OCR analyzers. */
public final class TextRecognizer extends AbstractVisionAnalyzer<TextRecognitionResult> {
    public TextRecognizer() {
        this(null);
    }

    public TextRecognizer(VisionOptions options) {
        super(VisionFeature.TEXT_RECOGNITION, options);
    }
}
