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

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;

/// A product in a grid: its picture, title, price (struck-through compare-at
/// price on a sale), stars, and a sold-out ribbon. A tap opens it; a right
/// click or a long press offers Open in new window, Add to cart and Copy
/// link.
final class ProductCard {
    private ProductCard() {
    }

    static Container create(final ProductCardDto p, int width) {
        int artW = Math.max(CN.convertToPixels(20), width);
        int artH = artW * 4 / 5;
        Container card = new Container(new BorderLayout());
        card.setUIID("SvProductCard");
        Label art = ProductArt.label(p.id(), p.title(), p.productType(), p.photoId(), artW, artH,
                "SvProductPhoto");
        Container picture = new Container(new LayeredLayout());
        picture.add(art);
        int off = Money.percentOff(p.minPriceCents(), p.compareAtCents());
        if (p.available() == 0) {
            picture.add(BorderLayout.north(com.codename1.ui.layouts.FlowLayout.encloseRight(
                    Ui.badge(Lang.tr("Sold out"), "ShopRibbon"))));
        } else if (off > 0) {
            picture.add(BorderLayout.north(com.codename1.ui.layouts.FlowLayout.encloseRight(
                    Ui.badge("-" + off + "%", "ShopSale"))));
        }
        Container text = new Container(BoxLayout.y());
        Label title = Ui.plain(p.title(), "SvProductName");
        text.add(title);
        if (p.vendor() != null && p.vendor().length() > 0) {
            text.add(Ui.plain(p.vendor(), "ShopVendor"));
        }
        Container price = new Container(new com.codename1.ui.layouts.FlowLayout());
        price.setUIID("ShopPriceRow");
        price.add(Ui.plain(p.maxPriceCents() > p.minPriceCents()
                ? Lang.tr("From {0}", ShopState.money(p.minPriceCents()))
                : ShopState.money(p.minPriceCents()), "SvProductPrice"));
        if (off > 0) {
            price.add(Ui.plain(ShopState.money(p.compareAtCents()), "ShopCompareAt"));
        }
        text.add(price);
        if (p.ratingCount() > 0) {
            text.add(ShopUi.stars(p.rating(), p.ratingCount()));
        }
        card.add(BorderLayout.NORTH, picture);
        card.add(BorderLayout.CENTER, text);
        Container tappable = Ui.tap(card, "product-" + p.handle(),
                e -> ProductForm.show(p.id(), p.title()));
        RowMenus.attach(tappable, () -> new Command[] {
            Command.create(Lang.tr("Open in new window"), null,
                    e -> ProductForm.openInWindow(p.id(), p.title())),
            Command.create(Lang.tr("Add to cart"), null, e -> ProductForm.quickAdd(p.id())),
            Command.create(Lang.tr("Copy link"), null, e -> {
                Display.getInstance().copyToClipboard(ShopSections.link(p.handle()));
                Ui.say("Link copied");
            })
        });
        return tappable;
    }

    /// The outline of a card while the grid loads.
    static Component placeholder(int width) {
        Container card = new Container(BoxLayout.y());
        card.setUIID("SvProductCard");
        Label box = new Label("", "ShopPlaceholderArt");
        box.setPreferredH(Math.max(CN.convertToPixels(16), width * 4 / 5));
        card.add(box);
        Label line = new Label(" ", "ShopPlaceholderLine");
        Label shorter = new Label(" ", "ShopPlaceholderLineShort");
        card.add(line).add(shorter);
        return card;
    }

    static char wishIcon(boolean on) {
        return on ? FontImage.MATERIAL_FAVORITE : FontImage.MATERIAL_FAVORITE_BORDER;
    }
}
