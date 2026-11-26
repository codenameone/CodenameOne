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

import com.codenameone.examples.bazaar.domain.ProductPhoto;

import java.util.List;

/// The kinds of a product gallery entry and where each is fetched from.
///
/// A picture is a stored photo (`/media/{photoId}`). An uploaded video or 3D
/// model is a catalog file (`/media/file/{fileId}`); a linked video is its
/// own `https` address.
public final class Media {
    public static final String IMAGE = "IMAGE";
    public static final String VIDEO = "VIDEO";
    public static final String MODEL = "MODEL";

    private Media() {
    }

    /// The path an uploaded catalog file is served at.
    public static String filePath(String fileId) {
        return "/media/file/" + fileId;
    }

    /// Where a gallery entry is fetched from.
    public static String url(ProductPhoto p) {
        if (IMAGE.equals(p.getKind()) || p.getKind() == null || p.getKind().length() == 0) {
            return "/media/" + p.getPhotoId();
        }
        if (p.getUrl() != null && p.getUrl().length() > 0) {
            return p.getUrl();
        }
        return filePath(p.getPhotoId());
    }

    /// True for a picture.
    public static boolean isImage(ProductPhoto p) {
        return p.getKind() == null || p.getKind().length() == 0 || IMAGE.equals(p.getKind());
    }

    /// The stored photo id of the first picture of a gallery, or empty: the
    /// product's main photo is never a video or a model.
    public static String firstImage(List<ProductPhoto> gallery) {
        for (int iter = 0; iter < gallery.size(); iter++) {
            if (isImage(gallery.get(iter))) {
                return gallery.get(iter).getPhotoId();
            }
        }
        return "";
    }

    /// A linked video's address when it is a plain `https` URL of at most
    /// 500 characters with a host; null otherwise.
    public static String httpsUrl(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim();
        if (u.length() > 500 || !u.regionMatches(true, 0, "https://", 0, 8) || u.length() < 12) {
            return null;
        }
        for (int iter = 0; iter < u.length(); iter++) {
            char c = u.charAt(iter);
            if (c <= ' ' || c > '~' || c == '"' || c == '<' || c == '>' || c == '\\') {
                return null;
            }
        }
        int slash = u.indexOf('/', 8);
        String host = slash < 0 ? u.substring(8) : u.substring(8, slash);
        if (host.length() == 0) {
            return null;
        }
        // The host goes into a Content-Security-Policy header, so nothing
        // but a host name's own characters (no ';', ',' or user info).
        for (int iter = 0; iter < host.length(); iter++) {
            char c = host.charAt(iter);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '-' || c == ':')) {
                return null;
            }
        }
        return "https://" + u.substring(8);
    }

    /// `https://host[:port]` of a URL [#httpsUrl] accepted: the one origin a
    /// page showing that video adds to its `media-src`.
    public static String origin(String httpsUrl) {
        int slash = httpsUrl.indexOf('/', 8);
        return slash < 0 ? httpsUrl : httpsUrl.substring(0, slash);
    }
}
