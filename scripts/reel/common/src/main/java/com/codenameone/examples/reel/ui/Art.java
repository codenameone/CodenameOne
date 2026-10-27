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
package com.codenameone.examples.reel.ui;

import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codenameone.examples.reel.net.Session;

/// Poster, backdrop and thumbnail art. The server names them by path
/// (`/art/{video}/poster.jpg`); they are public and cached on the device, so a
/// catalog scrolls without asking twice.
public final class Art {
    private Art() {
    }

    /// The absolute URL of an art path, or null for none.
    public static String url(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return Session.serverUrl() + (path.startsWith("/") ? path : "/" + path);
    }

    /// Fills `into` with the image at `path`, scaled to fill `widthMm` by
    /// `heightMm`, once it arrives. The label keeps its placeholder style
    /// until then, and when there is no art at all.
    public static void load(final Label into, String path, final float widthMm, final float heightMm) {
        String url = url(path);
        final int w = CN.convertToPixels(widthMm);
        final int h = CN.convertToPixels(heightMm);
        into.setPreferredW(w);
        into.setPreferredH(h);
        // Until the picture arrives -- and for good when there is none, as on
        // a server that has not packaged the demo video -- a film mark in the
        // middle says what the space is for.
        if (into.getIcon() == null) {
            into.getAllStyles().setAlignment(com.codename1.ui.Component.CENTER);
            into.setIcon(com.codename1.ui.FontImage.createMaterial(com.codename1.ui.FontImage.MATERIAL_MOVIE,
                    into.getUnselectedStyle(), Math.max(4f, Math.min(widthMm, heightMm) / 4f)));
        }
        if (url == null) {
            return;
        }
        Util.downloadImageToCache(url, img -> {
            Image fitted = img.fill(w, h);
            into.setIcon(fitted);
            into.setText("");
            if (into.getParent() != null) {
                into.getParent().revalidateLater();
            }
        }, (sender, err, code, message) -> { });
    }
}
