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
package com.codenameone.examples.bazaar.shop;

import com.codename1.components.MediaPlayer;
import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bazaar.AppConfig;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.ui.Lang;

/// A product gallery's video or 3D model slide. A video shows a play button
/// and becomes a player when tapped, so swiping past it loads nothing; a model
/// opens in the device's own viewer.
public final class GalleryMedia {
    private GalleryMedia() {
    }

    /// True for a gallery entry this class draws instead of a picture.
    public static boolean isMedia(ProductPhotoDto p) {
        return "VIDEO".equals(p.kind()) || "MODEL".equals(p.kind());
    }

    /// The absolute address of an entry's video or model.
    static String address(ProductPhotoDto p) {
        String url = p.url() == null ? "" : p.url();
        return url.startsWith("/") ? AppConfig.serverUrl() + url : url;
    }

    /// The slide for `p`, `width` by `height`.
    public static Component slide(final ProductPhotoDto p, int width, int height) {
        final Container holder = new Container(new BorderLayout());
        holder.setUIID("ShopGalleryImage");
        holder.setPreferredW(width);
        holder.setPreferredH(height);
        boolean video = "VIDEO".equals(p.kind());
        Button start = new Button(video ? Lang.tr("Play video") : Lang.tr("Open 3D model"),
                "ShopGalleryImage");
        start.setName(video ? "gallery-video" : "gallery-model");
        FontImage.setMaterialIcon(start, video ? FontImage.MATERIAL_PLAY_CIRCLE
                : FontImage.MATERIAL_VIEW_IN_AR, 8f);
        start.setTextPosition(Component.BOTTOM);
        holder.add(BorderLayout.CENTER, start);
        final String address = address(p);
        if (!video) {
            start.addActionListener(e -> CN.execute(address));
            return holder;
        }
        start.addActionListener(e -> play(holder, address));
        return holder;
    }

    private static void play(final Container holder, String address) {
        try {
            Media m = MediaManager.createMedia(address, true);
            MediaPlayer player = new MediaPlayer(m);
            player.setName("gallery-player");
            holder.removeAll();
            holder.add(BorderLayout.CENTER, player);
            holder.revalidate();
            m.play();
        } catch (java.io.IOException failed) {
            // No player for this format here: hand it to the platform.
            Log.e(failed);
            CN.execute(address);
        }
    }
}
