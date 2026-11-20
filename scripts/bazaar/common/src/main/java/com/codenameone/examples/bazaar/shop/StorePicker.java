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

import com.codename1.appkit.Net;
import com.codename1.appkit.Skeleton;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Which store the shopper is in. The platform hosts many; the one chosen
/// last is remembered on the device and opened straight away next time.
final class StorePicker {
    interface Picked {
        void picked(StoreDto store);
    }

    private StorePicker() {
    }

    /// Opens the remembered store, or the only one, or asks: `into` shows the
    /// list until a store is picked.
    static void restore(final Container into, final Picked picked) {
        into.removeAll();
        into.add(BorderLayout.CENTER, new Skeleton(4, "SvSkeleton"));
        Api.stores().stores(Net.to(stores -> {
            String remembered = ShopState.rememberedHandle();
            for (StoreDto s : stores) {
                if (s.handle().equals(remembered) && "ACTIVE".equals(s.status())) {
                    picked.picked(s);
                    return;
                }
            }
            if (active(stores) == 1) {
                for (StoreDto s : stores) {
                    if ("ACTIVE".equals(s.status())) {
                        picked.picked(s);
                        return;
                    }
                }
            }
            fill(into, stores, picked);
        }, ShopUi.failed(into, () -> restore(into, picked))));
    }

    /// The list of stores, always: from the "Switch store" button.
    static void choose(final Container into, final Picked picked) {
        into.removeAll();
        into.add(BorderLayout.CENTER, new Skeleton(4, "SvSkeleton"));
        Api.stores().stores(Net.to(stores -> fill(into, stores, picked),
                ShopUi.failed(into, () -> choose(into, picked))));
    }

    private static int active(List<StoreDto> stores) {
        int n = 0;
        for (StoreDto s : stores) {
            n += "ACTIVE".equals(s.status()) ? 1 : 0;
        }
        return n;
    }

    private static void fill(Container into, List<StoreDto> stores, final Picked picked) {
        into.removeAll();
        Container page = Ui.page();
        page.setName("store-picker");
        page.add(Ui.label("Choose a store", "SvTitle"));
        page.add(Ui.text("Every store on Bazaar has its own catalog, cart and checkout.",
                "SvMuted"));
        int side = CN.convertToPixels(12);
        int shown = 0;
        for (final StoreDto s : stores) {
            if (!"ACTIVE".equals(s.status())) {
                continue;
            }
            shown++;
            Container row = new Container(new BorderLayout());
            row.setUIID("ShopStoreRow");
            Label logo = ProductArt.label(s.id(), s.name(), "storefront", s.logoPhotoId(), side,
                    side, "ShopStoreLogo");
            row.add(BorderLayout.WEST, logo);
            Container lines = new Container(BoxLayout.y());
            lines.add(Ui.plain(s.name(), "SvRowTitle"));
            if (s.tagline() != null) {
                lines.add(Ui.plain(s.tagline(), "SvRowDetail"));
            }
            row.add(BorderLayout.CENTER, lines);
            Label go = new Label("", "SvRowIcon");
            FontImage.setMaterialIcon(go, FontImage.MATERIAL_CHEVRON_RIGHT, 3.6f);
            row.add(BorderLayout.EAST, go);
            page.add(Ui.tap(row, "store-" + s.handle(), e -> picked.picked(s)));
        }
        if (shown == 0) {
            page.add(Ui.empty(FontImage.MATERIAL_STOREFRONT, "No stores yet",
                    "Stores appear here once they open."));
        }
        into.add(BorderLayout.CENTER, page);
        Ui.relayout(into);
    }
}
