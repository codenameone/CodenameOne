/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Creates reusable still-image or live-frame barcode analyzers. */
public final class BarcodeScanner extends AbstractVisionAnalyzer<Barcode[]> {
    public BarcodeScanner() {
        this(null);
    }

    public BarcodeScanner(VisionOptions options) {
        super(VisionFeature.BARCODE_SCANNING, options);
    }
}
