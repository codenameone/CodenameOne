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

import java.io.IOException;
import java.util.List;

/// A bucket-and-key object store: Amazon S3 and everything that speaks its API
/// (Cloudflare R2, MinIO, Backblaze B2), Google Cloud Storage, Azure Blob
/// Storage, or the in-memory store a test runs against.
///
/// Inject it into a bean, or ask [ObjectStorages#current()], and the provider is
/// the one `cn1.storage.url` names, so the same code moves between clouds by
/// changing one line of configuration:
///
/// ```java
/// @Service
/// public class Avatars {
///     private final ObjectStorage storage;
///
///     public Avatars(ObjectStorage storage) {
///         this.storage = storage;
///     }
///
///     public String uploadUrl(String user) throws IOException {
///         return storage.presignPut("avatars", user + ".png", 600);
///     }
/// }
/// ```
///
/// A "bucket" is what the provider calls its top-level container: an S3 or GCS
/// bucket, an Azure container. A key is the object's name inside it; a slash in
/// a key is part of the name, which every provider's console shows as folders.
///
/// The presigned URL is the method to reach for from a mobile app: the device
/// uploads or downloads directly, and the object bytes never pass through the
/// server.
public interface ObjectStorage {
    /// The provider's short name: `s3` (Amazon S3 and every S3-compatible
    /// endpoint, R2 included), `gcs`, `azure` or `memory`.
    String getName();

    /// Creates a bucket, and says nothing when this account already owns it.
    ///
    /// Usually infrastructure's job, but a first run against an emulator or a
    /// test fixture needs it.
    void createBucket(String bucket) throws IOException;

    /// Uploads an object, replacing one of the same key. Returns its ETag, the
    /// provider's receipt for exactly these bytes.
    ///
    /// `contentType` may be null, which stores `application/octet-stream`.
    String putObject(String bucket, String key, byte[] content, String contentType)
            throws IOException;

    /// Downloads an object. Throws when it is missing, rather than returning null.
    byte[] getObject(String bucket, String key) throws IOException;

    /// The object's metadata, or null when it does not exist.
    ObjectInfo headObject(String bucket, String key) throws IOException;

    /// Deletes an object. Deleting one that is not there is not an error.
    void deleteObject(String bucket, String key) throws IOException;

    /// Up to `max` keys under `prefix` (null or empty for all), following the
    /// provider's pagination until the listing ends or `max` is reached. Zero
    /// answers an empty list; a negative `max` is refused.
    List<String> listObjects(String bucket, String prefix, int max) throws IOException;

    /// A URL that downloads the object with no credentials, for `seconds`.
    ///
    /// Computed, not requested, wherever the provider allows it: the URL can be
    /// handed straight to a client.
    String presignGet(String bucket, String key, int seconds) throws IOException;

    /// The upload counterpart: a URL a client can `PUT` the bytes to, for
    /// `seconds`.
    ///
    /// Azure needs one header on that `PUT` that the others do not:
    /// `x-ms-blob-type: BlockBlob`. A client that serves more than one provider
    /// can send it everywhere; S3 and GCS ignore it.
    String presignPut(String bucket, String key, int seconds) throws IOException;
}
