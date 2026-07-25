/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Corrected document pages returned as encoded images. */
public final class DocumentScanResult {
    private final byte[][] pages;
    private final VisionMetadata metadata;

    public DocumentScanResult(byte[][] pages) {
        this(pages, null);
    }

    public DocumentScanResult(byte[][] pages, VisionMetadata metadata) {
        if (pages == null) {
            this.pages = new byte[0][];
        } else {
            this.pages = new byte[pages.length][];
            for (int i = 0; i < pages.length; i++) {
                byte[] page = pages[i];
                if (page == null) {
                    throw new NullPointerException("pages[" + i + "]");
                }
                this.pages[i] = new byte[page.length];
                System.arraycopy(page, 0, this.pages[i], 0, page.length);
            }
        }
        this.metadata = metadata;
    }

    public int getPageCount() {
        return pages.length;
    }

    public byte[] getPage(int index) {
        byte[] page = pages[index];
        byte[] out = new byte[page.length];
        System.arraycopy(page, 0, out, 0, page.length);
        return out;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }
}
