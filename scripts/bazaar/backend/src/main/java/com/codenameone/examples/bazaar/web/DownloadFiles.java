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
package com.codenameone.examples.bazaar.web;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.catalog.CatalogFiles;
import com.codenameone.examples.bazaar.catalog.Downloads;
import com.codenameone.examples.bazaar.domain.CatalogFile;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.Stores;

import java.io.IOException;

/// The catalog files a browser fetches: a product's video or 3D model at
/// `/media/file/{fileId}`, public like its pictures, and a buyer's download
/// at `/download/{token}`, only through a signed grant.
///
/// A video is answered in byte ranges when asked (a `Range: bytes=a-b`
/// header), which is how a browser seeks and how Safari plays one at all. A
/// download is never ranged: every fetch counts against the grant, so a
/// download is one whole response, `no-store`, as an attachment.
@RestController
public class DownloadFiles {
    private final CatalogFiles files;
    private final Downloads downloads;
    private final StoreRepository stores;

    public DownloadFiles(CatalogFiles files, Downloads downloads, StoreRepository stores) {
        this.files = files;
        this.downloads = downloads;
        this.stores = stores;
    }

    /// A video or 3D model of an active store. Files are never changed in
    /// place -- a new upload is a new id -- so a browser keeps one for a year.
    /// A download is not found here, whatever its id.
    @GetMapping("/media/file/{fileId}")
    public HttpServer.Response media(@PathVariable("fileId") String fileId,
            HttpServer.Request request) throws IOException {
        Object[] meta = files.meta(fileId);
        String kind = meta == null ? "" : Rows.text(meta[1]);
        if (!CatalogFiles.VIDEO.equals(kind) && !CatalogFiles.MODEL.equals(kind)) {
            throw new ResponseStatusException(404, "No such file");
        }
        Store owner = stores.findById(Rows.text(meta[0]));
        if (owner == null || !Stores.ACTIVE.equals(owner.getStatus())) {
            throw new ResponseStatusException(404, "No such file");
        }
        String tag = "\"" + fileId + "\"";
        String type = Rows.text(meta[3]);
        if (tag.equals(request.getHeader("If-None-Match"))) {
            return new HttpServer.Response(304, type, new byte[0]).header("ETag", tag)
                    .header("Cache-Control", "public, max-age=31536000, immutable");
        }
        CatalogFile f = files.load(fileId);
        byte[] data = f.getData();
        long[] range = range(request.getHeader("Range"), data.length);
        HttpServer.Response r;
        if (range == null) {
            r = new HttpServer.Response(200, type, data);
        } else if (range.length == 0) {
            return new HttpServer.Response(416, "text/plain", new byte[0])
                    .header("Content-Range", "bytes */" + data.length)
                    .header("Accept-Ranges", "bytes");
        } else {
            int from = (int) range[0];
            int to = (int) range[1];
            byte[] part = new byte[to - from + 1];
            System.arraycopy(data, from, part, 0, part.length);
            r = new HttpServer.Response(206, type, part)
                    .header("Content-Range", "bytes " + from + "-" + to + "/" + data.length);
        }
        if (CatalogFiles.MODEL.equals(kind)) {
            r = r.header("Content-Disposition", disposition("inline", f.getName()));
        }
        return r.header("ETag", tag)
                .header("Accept-Ranges", "bytes")
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox");
    }

    /// A buyer's download: counted, then sent whole as an attachment that no
    /// cache keeps. Anything wrong with the link is a 404 page.
    @GetMapping("/download/{token}")
    public HttpServer.Response download(@PathVariable("token") String token) throws IOException {
        Downloads.Served served = downloads.consume(token);
        CatalogFile f = files.load(served.fileId);
        if (f == null) {
            throw new ResponseStatusException(404, "No such file");
        }
        return new HttpServer.Response(200, "application/octet-stream".equals(
                f.getContentType()) ? "application/octet-stream" : f.getContentType(), f.getData())
                .header("Content-Disposition", disposition("attachment", served.name))
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Robots-Tag", "noindex")
                .header("Content-Security-Policy", "default-src 'none'; sandbox");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "" : e.getReason());
    }

    /// `kind; filename="name"`; [CatalogFiles#fileName] already left only
    /// characters that need no quoting.
    static String disposition(String kind, String name) {
        return kind + "; filename=\"" + CatalogFiles.fileName(name, CatalogFiles.DOWNLOAD) + "\"";
    }

    /// `{from, to}` (inclusive) of a single `bytes=` range within `size`;
    /// null for no header or one this does not serve (several ranges, other
    /// units), which gets the whole file; an empty array for a range outside
    /// the file (416).
    static long[] range(String header, long size) {
        if (header == null || !header.startsWith("bytes=") || header.indexOf(',') >= 0) {
            return null;
        }
        String spec = header.substring(6).trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return null;
        }
        String a = spec.substring(0, dash).trim();
        String b = spec.substring(dash + 1).trim();
        long from;
        long to;
        if (a.length() == 0) {
            long suffix = number(b);
            if (suffix <= 0) {
                return suffix == 0 ? new long[0] : null;
            }
            from = Math.max(0, size - suffix);
            to = size - 1;
        } else {
            from = number(a);
            to = b.length() == 0 ? size - 1 : number(b);
            if (from < 0 || to < 0) {
                return null;
            }
        }
        if (from >= size || to < from) {
            return new long[0];
        }
        return new long[] {from, Math.min(to, size - 1)};
    }

    private static long number(String s) {
        if (s.length() == 0 || s.length() > 15) {
            return -1;
        }
        long v = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }
}
