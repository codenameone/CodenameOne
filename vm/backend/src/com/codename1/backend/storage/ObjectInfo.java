/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.storage;

/// What [ObjectStorage#headObject] reports about one object.
public class ObjectInfo {
    private final String key;
    private final long size;
    private final String contentType;
    private final String etag;
    private final String lastModified;

    /// An object's metadata. `size` is -1 when the provider did not say;
    /// `etag` is given without its surrounding quotes.
    public ObjectInfo(String key, long size, String contentType, String etag,
                      String lastModified) {
        this.key = key;
        this.size = size;
        this.contentType = contentType;
        this.etag = etag;
        this.lastModified = lastModified;
    }

    public String getKey() {
        return key;
    }

    /// The length in bytes, or -1 when the provider did not report it.
    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    /// The provider's entity tag, unquoted. Its format is the provider's own:
    /// an MD5 for a simple S3 upload, an opaque value for Azure and GCS.
    public String getEtag() {
        return etag;
    }

    /// The modification time exactly as the provider sent it: an HTTP date for
    /// S3 and Azure, an RFC 3339 timestamp for GCS. Not parsed, so nothing is
    /// lost in a conversion the caller may not want.
    public String getLastModified() {
        return lastModified;
    }
}
