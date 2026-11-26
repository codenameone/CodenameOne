/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.catalog;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.CatalogFile;
import com.codenameone.examples.bazaar.repository.CatalogFileRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// Stores and reads the catalog's non-picture files (`catalog_file`).
///
/// Every file is held to `bazaar.catalog.max-file-bytes` (25 MB unless set).
/// A video must be an MP4 or a WebM and a model a binary glTF (`.glb`), each
/// checked by its leading bytes rather than by what the upload claimed, and
/// stored under the type its bytes are. A download may be any file; it is
/// only ever served as an attachment, through a grant.
@Component
public class CatalogFiles {
    public static final String VIDEO = "VIDEO";
    public static final String MODEL = "MODEL";
    public static final String DOWNLOAD = "DOWNLOAD";

    private final CatalogFileRepository files;
    private final int maxBytes;

    public CatalogFiles(CatalogFileRepository files,
            @Value("${bazaar.catalog.max-file-bytes:26214400}") int maxBytes) {
        this.files = files;
        this.maxBytes = maxBytes;
    }

    /// The most bytes a file may have.
    public int maxBytes() {
        return maxBytes;
    }

    /// Stores base64 `data` as a file of `kind` and answers its id.
    public String storeBase64(String storeId, String kind, String name, String contentType,
            String data) throws IOException {
        if (data == null || data.length() == 0) {
            throw new ResponseStatusException(400, "No file");
        }
        if ((long) data.length() / 4 * 3 > maxBytes + 3L) {
            throw new ResponseStatusException(413, "That file is too large");
        }
        for (int iter = 0; iter < data.length(); iter++) {
            char c = data.charAt(iter);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '+' || c == '/' || c == '=')) {
                throw new ResponseStatusException(400, "The file is not base64");
            }
        }
        byte[] bytes = Base64.decode(data);
        if (bytes == null) {
            throw new ResponseStatusException(400, "The file is not base64");
        }
        return store(storeId, kind, name, contentType, bytes);
    }

    /// Stores `bytes` as a file of `kind` and answers its id.
    @Transactional(rollbackFor = IOException.class)
    public String store(String storeId, String kind, String name, String contentType,
            byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(400, "No file");
        }
        if (bytes.length > maxBytes) {
            throw new ResponseStatusException(413, "That file is too large");
        }
        String type = typeOf(kind, contentType, bytes);
        CatalogFile row = new CatalogFile();
        row.setId(Ids.next());
        row.setStoreId(storeId);
        row.setKind(kind);
        row.setName(fileName(name, kind));
        row.setContentType(type);
        row.setSizeBytes(bytes.length);
        row.setCreatedAt(Instant.now());
        row.setData(bytes);
        files.insert(row);
        return row.getId();
    }

    /// The type a file of `kind` is stored under; 415 when its bytes are not
    /// what the kind takes.
    static String typeOf(String kind, String declared, byte[] b) {
        if (VIDEO.equals(kind)) {
            if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
                return "video/mp4";
            }
            if (b.length >= 4 && (b[0] & 0xff) == 0x1a && (b[1] & 0xff) == 0x45
                    && (b[2] & 0xff) == 0xdf && (b[3] & 0xff) == 0xa3) {
                return "video/webm";
            }
            throw new ResponseStatusException(415, "A video is an MP4 or a WebM file");
        }
        if (MODEL.equals(kind)) {
            if (b.length >= 12 && b[0] == 'g' && b[1] == 'l' && b[2] == 'T' && b[3] == 'F') {
                return "model/gltf-binary";
            }
            throw new ResponseStatusException(415, "A 3D model is a binary glTF (.glb) file");
        }
        if (DOWNLOAD.equals(kind)) {
            return safeType(declared);
        }
        throw new ResponseStatusException(400, "Unknown file kind");
    }

    /// `declared` when it is a plain `type/subtype` token, else
    /// `application/octet-stream`.
    static String safeType(String declared) {
        if (declared == null || declared.length() == 0 || declared.length() > 64) {
            return "application/octet-stream";
        }
        int slash = -1;
        for (int iter = 0; iter < declared.length(); iter++) {
            char c = declared.charAt(iter);
            if (c == '/') {
                if (slash >= 0 || iter == 0) {
                    return "application/octet-stream";
                }
                slash = iter;
            } else if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '-' || c == '+')) {
                return "application/octet-stream";
            }
        }
        return slash > 0 && slash < declared.length() - 1 ? declared
                : "application/octet-stream";
    }

    /// A file name safe in a header: letters, digits, `.`, `-`, `_` and
    /// spaces, at most 200 characters, never empty.
    public static String fileName(String name, String kind) {
        StringBuilder out = new StringBuilder();
        String n = name == null ? "" : name.trim();
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) {
            n = n.substring(slash + 1);
        }
        for (int iter = 0; iter < n.length() && out.length() < 200; iter++) {
            char c = n.charAt(iter);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '-' || c == '_' || c == ' ';
            out.append(ok ? c : '_');
        }
        String s = out.toString().trim();
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        if (s.length() == 0) {
            return VIDEO.equals(kind) ? "video" : MODEL.equals(kind) ? "model.glb" : "download";
        }
        return s;
    }

    /// `{storeId, kind, name, contentType, sizeBytes}` of a file without its
    /// bytes, or null.
    @Transactional(readOnly = true)
    public Object[] meta(String fileId) throws IOException {
        if (fileId == null || fileId.length() == 0 || fileId.length() > 32) {
            return null;
        }
        List<Object[]> rows = files.meta(fileId);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /// The whole file, or null.
    @Transactional(readOnly = true)
    public CatalogFile load(String fileId) throws IOException {
        if (fileId == null || fileId.length() == 0 || fileId.length() > 32) {
            return null;
        }
        return files.findById(fileId);
    }

    /// Removes a file nothing names any more.
    @Transactional(rollbackFor = IOException.class)
    public void remove(String fileId) throws IOException {
        CatalogFile f = load(fileId);
        if (f != null) {
            files.delete(f);
        }
    }
}
