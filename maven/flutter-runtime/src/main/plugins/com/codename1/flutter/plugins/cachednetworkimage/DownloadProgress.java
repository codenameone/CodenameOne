/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.cachednetworkimage;

/**
 * cached_network_image's {@code DownloadProgress}, handed to
 * {@code progressIndicatorBuilder}: the URL, the bytes received so far and the total
 * when the server announced one.
 */
public class DownloadProgress {
    private final String originalUrl;
    private final Long totalSize;
    private final long downloaded;

    public DownloadProgress(String originalUrl, Long totalSize, long downloaded) {
        this.originalUrl = originalUrl;
        this.totalSize = totalSize;
        this.downloaded = downloaded;
    }

    public String originalUrl() {
        return originalUrl;
    }

    public Long totalSize() {
        return totalSize;
    }

    public long downloaded() {
        return downloaded;
    }

    /** Received over total, or null while the total is unknown. */
    public Double progress() {
        if (totalSize == null || totalSize.longValue() <= 0) {
            return null;
        }
        return Double.valueOf(Math.min(1.0, (double) downloaded / totalSize.longValue()));
    }
}
