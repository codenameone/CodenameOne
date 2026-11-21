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
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.ArtPalette;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Look;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The storefront: the store's own header in its colour, a hero, its
/// collections in a row that scrolls sideways, and the newest products in a
/// grid that keeps loading as it is scrolled.
final class HomePane extends ShopPane {
    private ProductGrid grid;
    private final SearchQuery featured = new SearchQuery();

    HomePane() {
        super(new BorderLayout());
        setName("shop-home");
        featured.sort = "newest";
        if (ShopState.store() == null) {
            pick(false);
        } else {
            build();
        }
    }

    /// Asks which store, or opens the remembered one.
    void pick(boolean always) {
        StorePicker.Picked picked = s -> {
            ShopState.useStore(s);
            build();
        };
        if (always) {
            StorePicker.choose(this, picked);
        } else {
            StorePicker.restore(this, picked);
        }
    }

    @Override
    void storeChanged() {
        // build() follows the pick that changed it.
    }

    @Override
    void reload() {
        if (grid != null) {
            grid.reload();
        }
    }

    private void build() {
        removeAll();
        StoreDto store = ShopState.store();
        if (store == null) {
            pick(true);
            return;
        }
        Button change = new Button("", "ShopHeaderButton");
        change.setName("switch-store");
        FontImage.setMaterialIcon(change, FontImage.MATERIAL_SWAP_HORIZ, 3.6f);
        change.addActionListener(e -> pick(true));

        Container top = new Container(BoxLayout.y());
        // One header: the hero carries the store's name, wrapped rather than
        // ellipsized, and the switch-store action sits on its top corner.
        top.add(hero(store, change));
        final Container collections = new Container(BoxLayout.x());
        collections.setUIID("ShopCarousel");
        collections.setName("collections");
        collections.setScrollableX(true);
        collections.setScrollVisible(false);
        top.add(collections);
        top.add(Ui.label("New arrivals", "ShopSectionTitle"));
        grid = new ProductGrid(top, featured, Layouts.wide() ? CN.convertToPixels(56) : 0);
        add(BorderLayout.CENTER, grid.component());
        grid.reload();
        Api.catalog().collections(store.handle(), Net.to(list -> fill(collections, list),
                (status, message) -> collections.setHidden(true)));
        Ui.relayout(this);
    }

    private Container hero(final StoreDto store, Button change) {
        int width = Math.min(CN.getDisplayWidth(), Layouts.column() * 2);
        int height = CN.convertToPixels(Layouts.wide() ? 46 : 52);
        Container hero = new Container(new LayeredLayout());
        hero.setUIID("ShopHero");
        // A long name or description makes the hero taller than its art; the
        // art's own deeper colour fills what it does not cover, not a grey band.
        int[] colors = ArtPalette.colors(store.id() + "-hero", Look.dark());
        hero.getAllStyles().setBgColor(colors[1]);
        hero.getAllStyles().setBgTransparency(255);
        // No glyph on the hero's drawn art: the store's name is laid over it.
        Label picture = ProductArt.label(store.id() + "-hero", store.name(),
                ProductArt.NO_GLYPH, store.heroPhotoId(), width, height, "ShopHeroArt");
        hero.add(picture);
        // One scrim over the whole hero, so its top is not a lighter band
        // above the words.
        Container shade = new Container();
        shade.setUIID("ShopHeroShade");
        hero.add(shade);
        Container words = new Container(BoxLayout.y());
        words.setUIID("ShopHeroWords");
        SpanLabel name = Ui.text(store.name(), "ShopHeroTitle");
        name.setShouldLocalize(false);
        words.add(name);
        String about = store.description() != null && store.description().length() > 0
                ? store.description() : store.tagline();
        if (about != null) {
            words.add(Ui.text(about, "ShopHeroText"));
        }
        words.add(Ui.primary("Shop all", "shop-all", e -> ShopSections.search(null)));
        hero.add(BorderLayout.south(words));
        hero.add(FlowLayout.encloseRight(change));
        return hero;
    }

    private void fill(Container row, List<CollectionDto> collections) {
        row.removeAll();
        int w = CN.convertToPixels(34);
        int h = CN.convertToPixels(22);
        for (final CollectionDto c : collections) {
            if (!c.published()) {
                continue;
            }
            Container tile = new Container(new LayeredLayout());
            tile.setUIID("ShopCollection");
            tile.add(ProductArt.label(c.id(), c.title(), c.title(), c.photoId(), w, h,
                    "ShopCollectionArt"));
            Container words = new Container(BoxLayout.y());
            words.setUIID("ShopCollectionWords");
            words.add(Ui.plain(c.title(), "ShopCollectionTitle"));
            words.add(Ui.plain(Lang.tr("{0} products", String.valueOf(c.productCount())), "ShopCollectionCount"));
            tile.add(BorderLayout.south(words));
            row.add(Ui.tap(BorderLayout.center(tile), "collection-" + c.handle(),
                    e -> ShopSections.collection(c.handle(), c.title())));
        }
        row.setHidden(row.getComponentCount() == 0);
        Ui.relayout(row);
    }
}
