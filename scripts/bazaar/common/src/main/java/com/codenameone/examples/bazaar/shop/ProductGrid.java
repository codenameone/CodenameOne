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
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.components.InfiniteProgress;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;

/// A scrolling grid of products that fetches the next page as the end comes
/// into view, shows the outline of cards while the first page loads, and an
/// empty state when nothing matches. Two columns on a phone, as many as fit
/// on a wide screen.
///
/// Answers are numbered, and one that arrives after a newer question was
/// asked -- the words changed while it was on its way -- is dropped.
final class ProductGrid {
    private static final int PAGE = 24;
    private static final float CARD_MM = 46f;

    private final Container scroller = new Container(BoxLayout.y());
    private final Container grid;
    private final Container footer = new Container(new FlowLayout(Component.CENTER));
    private final int columns;
    private final int cardWidth;
    private final SearchQuery query;
    private int page;
    private boolean more;
    private boolean loading;
    private int asked;
    private int count;

    /// @param query what to show; read afresh on every [#reload]
    ProductGrid(Component header, SearchQuery query, int sideWidth) {
        this.query = query;
        int width = Math.max(1, CN.getDisplayWidth() - sideWidth);
        columns = Math.max(2, Layouts.columns(width, CARD_MM, 6));
        cardWidth = width / columns - CN.convertToPixels(3);
        grid = new Container(new GridLayout(1, columns));
        grid.setUIID("ShopGrid");
        grid.setName("product-grid");
        scroller.setUIID("ShopScroller");
        scroller.setScrollableY(true);
        scroller.setScrollVisible(false);
        if (header != null) {
            scroller.add(header);
        }
        scroller.add(grid);
        scroller.add(footer);
        scroller.addScrollListener((x, y, oldX, oldY) -> {
            if (more && !loading && y + scroller.getHeight() * 2 > scroller.getScrollDimension()
                    .getHeight()) {
                next();
            }
        });
        Ui.pull(scroller, this::reload);
    }

    Container component() {
        return scroller;
    }

    int count() {
        return count;
    }

    /// Starts again from the first page.
    void reload() {
        page = 0;
        more = false;
        count = 0;
        grid.removeAll();
        grid.setLayout(new GridLayout(2, columns));
        for (int iter = 0; iter < columns * 2; iter++) {
            grid.add(ProductCard.placeholder(cardWidth));
        }
        footer.removeAll();
        fetch(true);
    }

    private void next() {
        page++;
        footer.removeAll();
        footer.add(new InfiniteProgress());
        footer.revalidate();
        fetch(false);
    }

    private void fetch(final boolean first) {
        StoreDto store = ShopState.store();
        if (store == null) {
            return;
        }
        loading = true;
        final int ticket = ++asked;
        Api.catalog().products(store.handle(), query.words(), query.collection, query.tag,
                query.type, query.vendor, query.minParam(), query.maxParam(), query.sortParam(),
                String.valueOf(page), String.valueOf(PAGE), Net.to(answer -> {
                    if (ticket != asked) {
                        return;
                    }
                    loading = false;
                    show(answer, first);
                }, (status, message) -> {
                    if (ticket != asked) {
                        return;
                    }
                    loading = false;
                    if (first) {
                        grid.removeAll();
                        footer.removeAll();
                        footer.add(Ui.empty(FontImage.MATERIAL_CLOUD_OFF, message, null));
                        footer.add(Ui.link("Try again", "retry", e -> reload()));
                        Ui.relayout(scroller);
                    }
                    Ui.fail(status, message);
                }));
    }

    private void show(ProductPageDto answer, boolean first) {
        if (first) {
            grid.removeAll();
        }
        footer.removeAll();
        int added = answer.items() == null ? 0 : answer.items().size();
        for (int iter = 0; iter < added; iter++) {
            ProductCardDto p = answer.items().get(iter);
            grid.add(ProductCard.create(p, cardWidth));
        }
        count += added;
        // Whole rows only: a grid layout with too few rows squeezes the cards.
        int rows = Math.max(1, (count + columns - 1) / columns);
        grid.setLayout(new GridLayout(rows, columns));
        more = count < answer.total() && added > 0;
        if (count == 0) {
            footer.add(Ui.empty(FontImage.MATERIAL_SEARCH_OFF, "Nothing matches",
                    "Try other words, or fewer filters."));
        }
        Ui.relayout(scroller);
    }
}
