/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.ai.vision;

/// Corrected document pages returned as encoded image data. Pages are ordered
/// as detected and both construction and access make defensive copies, so the
/// result can safely cross asynchronous boundaries. The still-image scanner is
/// currently Apple-only; Android's ML Kit document API owns an interactive
/// camera flow and does not implement this analyzer contract.
public final class DocumentScanResult {
    private final byte[][] pages;
    private final VisionMetadata metadata;

    /// Creates a corrected document scan without backend metadata.
    /// @param pages encoded corrected page images, deeply defensively copied
    public DocumentScanResult(byte[][] pages) {
        this(pages, null);
    }

    /// Creates a corrected document scan with backend diagnostics.
    /// @param pages encoded corrected page images, deeply defensively copied
    /// @param metadata backend details, or {@code null}
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

    /// @return number of corrected pages
    public int getPageCount() {
        return pages.length;
    }

    /// @param index zero-based page index
    /// @return defensive copy of that page's encoded image
    public byte[] getPage(int index) {
        byte[] page = pages[index];
        byte[] out = new byte[page.length];
        System.arraycopy(page, 0, out, 0, page.length);
        return out;
    }

    /// @return backend metadata, or {@code null} for manually created results
    public VisionMetadata getMetadata() {
        return metadata;
    }
}
