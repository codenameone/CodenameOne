/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Creates reusable document-boundary and perspective-correction analyzers. */
public final class DocumentScanner extends AbstractVisionAnalyzer<DocumentScanResult> {
    public DocumentScanner() {
        this(null);
    }

    public DocumentScanner(VisionOptions options) {
        super(VisionFeature.DOCUMENT_SCANNING, options);
    }
}
