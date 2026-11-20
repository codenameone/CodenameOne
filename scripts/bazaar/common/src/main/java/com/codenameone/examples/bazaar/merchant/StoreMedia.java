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
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDropEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.bazaar.api.PhotoDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Photos;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;
import java.io.InputStream;

/// The store's logo or hero picture in Settings: what is there now, and a
/// camera, a gallery and (on a desktop) a drop target to change it.
final class StoreMedia {
    interface Changed {
        void changed(StoreDto saved);
    }

    private StoreMedia() {
    }

    static Container editor(final boolean hero, String photoId, final Changed changed) {
        final Container box = new Container(new BorderLayout());
        box.setUIID("MeMediaRow");
        box.setName(hero ? "settings-hero" : "settings-logo");
        int w = CN.convertToPixels(hero ? 32f : 16f);
        int h = CN.convertToPixels(16f);
        Label picture = new Label("", "MeMediaThumb");
        if (photoId != null && photoId.length() > 0) {
            ProductArt.photo(picture, photoId, w, h);
        } else {
            FontImage.setMaterialIcon(picture, FontImage.MATERIAL_IMAGE, 8f);
        }
        box.add(BorderLayout.WEST, picture);
        Container words = new Container(BoxLayout.y());
        words.add(Ui.plain(Lang.tr(hero ? "Hero picture" : "Logo"), "SvRowTitle"));
        words.add(Ui.plain(Lang.tr(photoId == null ? "Not set" : "Set"), "SvRowDetail"));
        Container buttons = new Container(new FlowLayout());
        final Photos.Taken taken = (jpeg, image) -> upload(hero, jpeg, "image/jpeg", changed);
        buttons.add(Ui.flat(FontImage.MATERIAL_PHOTO_CAMERA, hero ? "hero-camera" : "logo-camera",
                e -> Photos.camera(taken)));
        buttons.add(Ui.flat(FontImage.MATERIAL_PHOTO_LIBRARY, hero ? "hero-gallery"
                : "logo-gallery", e -> Photos.gallery(taken)));
        if (photoId != null) {
            buttons.add(Ui.flat(FontImage.MATERIAL_DELETE, hero ? "hero-remove" : "logo-remove",
                    e -> remove(hero, changed)));
        }
        words.add(buttons);
        box.add(BorderLayout.CENTER, words);
        if (NativeDragAndDrop.isSupported()) {
            Label drop = new Label(Lang.tr("Drop a picture here"), "MeDrop");
            drop.setName(hero ? "hero-drop" : "logo-drop");
            drop.setNativeDropTarget(true);
            drop.addNativeDropListener(e -> {
                if (e instanceof NativeDropEvent && ((NativeDropEvent) e).getFiles() != null
                        && ((NativeDropEvent) e).getFiles().length > 0) {
                    dropped(hero, ((NativeDropEvent) e).getFiles()[0], changed);
                }
            });
            box.add(BorderLayout.SOUTH, drop);
        }
        return box;
    }

    private static void dropped(boolean hero, String file, Changed changed) {
        boolean png = file.regionMatches(true, file.length() - 4, ".png", 0, 4);
        boolean jpeg = file.regionMatches(true, file.length() - 4, ".jpg", 0, 4)
                || file.regionMatches(true, file.length() - 5, ".jpeg", 0, 5);
        if (!png && !jpeg) {
            Ui.say("Drop a .jpg or .png picture");
            return;
        }
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(file);
            upload(hero, Util.readInputStream(in), png ? "image/png" : "image/jpeg", changed);
        } catch (java.io.IOException failed) {
            Log.e(failed);
            Ui.say("That picture could not be read");
        } finally {
            Util.cleanup(in);
        }
    }

    private static void upload(boolean hero, byte[] bytes, String type, final Changed changed) {
        if (bytes == null) {
            return;
        }
        PhotoDto photo = new PhotoDto(null, type, Base64.encodeNoNewline(bytes));
        Net.Ok<StoreDto> done = saved -> {
            Ui.say("Picture saved");
            changed.changed(saved);
        };
        if (hero) {
            Api.storeMedia().uploadHero(MerchantState.storeId(), photo, Net.to(done, Ui::fail));
        } else {
            Api.storeMedia().uploadLogo(MerchantState.storeId(), photo, Net.to(done, Ui::fail));
        }
    }

    private static void remove(boolean hero, final Changed changed) {
        Net.Ok<StoreDto> done = changed::changed;
        if (hero) {
            Api.storeMedia().removeHero(MerchantState.storeId(), Net.to(done, Ui::fail));
        } else {
            Api.storeMedia().removeLogo(MerchantState.storeId(), Net.to(done, Ui::fail));
        }
    }
}
