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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.bazaar.api.MediaUploadDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The product editor's video and 3D model tools: buttons that add a video
/// (a file from the device or an `https` link) or a `.glb` model, and the
/// arrows that move a gallery entry. Each answers the updated product.
final class ProductMedia {
    /// Told the product after a change.
    interface Changed {
        void changed(ProductDto p);
    }

    private ProductMedia() {
    }

    /// True for a gallery entry that is not a picture.
    static boolean isMedia(ProductPhotoDto p) {
        return "VIDEO".equals(p.kind()) || "MODEL".equals(p.kind());
    }

    /// The tile of a video or model in the editor's gallery.
    static Label tile(ProductPhotoDto p, int size) {
        Label l = new Label("VIDEO".equals(p.kind()) ? Lang.tr("Video") : Lang.tr("3D model"),
                "MeThumb");
        FontImage.setMaterialIcon(l, "VIDEO".equals(p.kind()) ? FontImage.MATERIAL_MOVIE
                : FontImage.MATERIAL_VIEW_IN_AR, 4f);
        l.setTextPosition(Label.BOTTOM);
        l.setPreferredW(size);
        l.setPreferredH(size);
        return l;
    }

    /// The buttons under the gallery that add media to a saved product.
    static Container buttons(final String productId, final Changed changed) {
        Container row = new Container(new FlowLayout());
        Button video = Ui.secondary(Lang.tr("Add video"), "media-video",
                e -> CN.openGallery(picked -> upload(productId, "VIDEO", picked == null ? null
                    : picked.getSource(), changed), Display.GALLERY_VIDEO));
        Ui.icon(video, FontImage.MATERIAL_VIDEO_LIBRARY, 3.2f);
        Button link = Ui.secondary(Lang.tr("Add video link"), "media-link",
                e -> askLink(productId, changed));
        Button model = Ui.secondary(Lang.tr("Add 3D model"), "media-model",
                e -> CN.openGallery(picked -> upload(productId, "MODEL", picked == null ? null
                    : picked.getSource(), changed), Display.GALLERY_ALL));
        Ui.icon(model, FontImage.MATERIAL_VIEW_IN_AR, 3.2f);
        return row.add(video).add(link).add(model);
    }

    /// Uploads a dropped or picked file, by its extension.
    static void dropped(String productId, String path, Changed changed) {
        String kind = kindOf(path);
        if (kind != null) {
            upload(productId, kind, path, changed);
        }
    }

    /// `VIDEO` for `.mp4`/`.webm`, `MODEL` for `.glb`, otherwise null.
    static String kindOf(String path) {
        if (ends(path, ".mp4") || ends(path, ".webm")) {
            return "VIDEO";
        }
        return ends(path, ".glb") ? "MODEL" : null;
    }

    private static boolean ends(String s, String suffix) {
        return s != null && s.length() >= suffix.length()
                && s.regionMatches(true, s.length() - suffix.length(), suffix, 0, suffix.length());
    }

    private static void upload(String productId, String kind, Object source, Changed changed) {
        if (!(source instanceof String)) {
            return;
        }
        String path = (String) source;
        byte[] bytes = DigitalFiles.read(path);
        if (bytes == null) {
            return;
        }
        // The server checks the bytes themselves; the declared type is a hint.
        Api.digital().addMedia(MerchantState.storeId(), productId, new MediaUploadDto(kind,
                DigitalFiles.name(path), "", Base64.encodeNoNewline(bytes), null),
                Net.to(changed::changed, Ui::fail));
    }

    private static void askLink(String productId, Changed changed) {
        Container body = new Container(BoxLayout.y());
        TextField url = new TextField("", "https://", 40, TextField.URL);
        url.setUIID("SvField");
        url.setName("media-url");
        body.add(url);
        Command add = new com.codename1.ui.Command(Lang.tr("Add"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Video link (https)"), body, cancel, add) != add) {
            return;
        }
        Api.digital().addMedia(MerchantState.storeId(), productId, new MediaUploadDto("VIDEO",
                null, null, null, url.getText().trim()), Net.to(changed::changed, Ui::fail));
    }

    /// Moves a gallery entry one place earlier (`up`) or later.
    static void move(String productId, ProductPhotoDto p, boolean up, Changed changed) {
        Api.digital().moveMedia(MerchantState.storeId(), productId, p.id(), up ? "up" : "down",
                Net.to(changed::changed, Ui::fail));
    }
}
