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

/// A product or collection picture as a page draws it: the stored photo, or,
/// when there is none, an inline SVG placeholder -- a gradient chosen from a
/// hash of the id and the initials of the title -- so a new store with no
/// photos yet still has a page that looks finished.
public final class Art {
    /// Whether there is a stored photo; when false the placeholder is drawn.
    public final boolean photo;
    /// The photo address, or empty.
    public final String url;
    /// The placeholder gradient element id, unique on the page.
    public final String gid;
    /// `url(#gid)`: the fill that names the gradient.
    public final String fill;
    public final String from;
    public final String to;
    public final String initials;
    /// An uploaded video, played by a `<video>` element.
    public final boolean video;
    /// A video linked on another site: a link out, never embedded.
    public final boolean videoLink;
    /// A 3D model (`.glb`): shown in the vendored model viewer, with a link
    /// to the file under it.
    public final boolean model;
    /// The poster frame of a video: the product's first picture, or empty.
    public final String poster;

    Art(String key, String photoId, String title) {
        this(key, photoId, title, null, null, null);
    }

    /// A gallery entry of `kind` (`IMAGE`, `VIDEO`, `MODEL`); `url` is where a
    /// video or model is fetched from and `poster` a picture for a video.
    Art(String key, String photoId, String title, String kind, String url, String poster) {
        boolean media = "VIDEO".equals(kind) || "MODEL".equals(kind);
        this.photo = !media && photoId != null && photoId.length() > 0;
        this.video = "VIDEO".equals(kind) && url != null && url.startsWith("/");
        this.videoLink = "VIDEO".equals(kind) && !video;
        this.model = "MODEL".equals(kind);
        this.poster = poster == null ? "" : poster;
        this.url = photo ? "/media/" + photoId : media && url != null ? url : "";
        int h = 0;
        String seed = key == null ? "" : key;
        for (int iter = 0; iter < seed.length(); iter++) {
            h = h * 31 + seed.charAt(iter);
        }
        int hue = (h & 0x7fffffff) % 360;
        this.gid = "g" + Integer.toHexString(h & 0x7fffffff) + Integer.toString(seed.length());
        this.fill = "url(#" + gid + ")";
        this.from = "hsl(" + hue + ",70%,62%)";
        this.to = "hsl(" + ((hue + 40) % 360) + ",65%,42%)";
        this.initials = initials(title);
    }

    /// Up to two letters or digits: the first of the first two words.
    static String initials(String title) {
        StringBuilder out = new StringBuilder();
        boolean start = true;
        String t = title == null ? "" : title;
        for (int iter = 0; iter < t.length() && out.length() < 2; iter++) {
            char c = t.charAt(iter);
            boolean word = c >= 0x30 && c <= 0x39 || c >= 0x41 && c <= 0x5a
                    || c >= 0x61 && c <= 0x7a || c > 0x7f;
            if (word && start) {
                // ASCII upper case by hand: a locale fold would turn i into a
                // dotless I on a Turkish server.
                out.append(c >= 0x61 && c <= 0x7a ? (char) (c - 0x20) : c);
            }
            start = !word;
        }
        return out.length() == 0 ? "?" : out.toString();
    }
}
