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
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.FacetsDto;
import com.codenameone.examples.bazaar.api.NameCountDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The filters of a search: collection, kind, brand, tag and a price range,
/// each with how many products it would leave. The same panel sits beside
/// the results on a wide screen and rises in a sheet on a phone.
///
/// A chip changes the query at once and calls `changed`; the price range is
/// applied by its button (or Enter in either field).
final class FilterPanel {
    private final Container panel = new Container(BoxLayout.y());
    private final SearchQuery query;
    private final Runnable changed;
    private int asked;

    FilterPanel(SearchQuery query, Runnable changed) {
        this.query = query;
        this.changed = changed;
        panel.setUIID("ShopFilters");
        panel.setName("filters");
        panel.setScrollableY(true);
        panel.setScrollVisible(false);
    }

    Container component() {
        return panel;
    }

    /// Asks the server for the facets of the current query and redraws.
    void load() {
        StoreDto store = ShopState.store();
        if (store == null) {
            return;
        }
        final int ticket = ++asked;
        Api.catalog().facets(store.handle(), query.words(), query.collection, query.tag,
                query.type, query.vendor, query.minParam(), query.maxParam(),
                Net.to(facets -> {
                    if (ticket == asked) {
                        draw(facets);
                    }
                }, (status, message) -> { }));
    }

    private void draw(FacetsDto facets) {
        panel.removeAll();
        Container head = new Container(new com.codename1.ui.layouts.BorderLayout());
        head.add(com.codename1.ui.layouts.BorderLayout.CENTER, Ui.label("Filters", "SvSheetTitle"));
        if (query.active() > 0) {
            head.add(com.codename1.ui.layouts.BorderLayout.EAST, Ui.link("Clear all",
                    "filters-clear", e -> {
                        query.clearFilters();
                        changed.run();
                        load();
                    }));
        }
        panel.add(head);
        group("Collection", "collection", facets.collections(), query.collection, v -> {
            query.collection = SearchQuery.toggle(query.collection, v);
        });
        group("Type", "type", facets.types(), query.type, v -> {
            query.type = SearchQuery.toggle(query.type, v);
        });
        group("Brand", "vendor", facets.vendors(), query.vendor, v -> {
            query.vendor = SearchQuery.toggle(query.vendor, v);
        });
        group("Tag", "tag", facets.tags(), query.tag, v -> {
            query.tag = SearchQuery.toggle(query.tag, v);
        });
        price(facets);
        Ui.relayout(panel);
    }

    private interface Pick {
        void pick(String value);
    }

    private void group(String title, String name, List<NameCountDto> values, String current,
            final Pick pick) {
        if (values == null || values.isEmpty()) {
            return;
        }
        panel.add(Ui.label(title, "ShopFilterTitle"));
        Container chips = new Container(new FlowLayout());
        chips.setUIID("ShopChips");
        for (final NameCountDto v : values) {
            boolean on = v.name().equals(current);
            Button chip = new Button(v.name() + "  " + v.count(), on ? "ShopChipOn" : "ShopChip");
            chip.setName("filter-" + name + "-" + v.name());
            chip.addActionListener(e -> {
                ShopUi.tap();
                pick.pick(v.name());
                changed.run();
                load();
            });
            chips.add(chip);
        }
        panel.add(chips);
    }

    private void price(FacetsDto facets) {
        panel.add(Ui.label("Price", "ShopFilterTitle"));
        Container row = new Container(new com.codename1.ui.layouts.GridLayout(1, 2));
        final TextField min = new TextField(query.min >= 0 ? String.valueOf(query.min / 100) : "",
                ShopState.money(facets.minPriceCents()), 8, TextField.DECIMAL);
        final TextField max = new TextField(query.max >= 0 ? String.valueOf(query.max / 100) : "",
                ShopState.money(facets.maxPriceCents()), 8, TextField.DECIMAL);
        min.setUIID("SvField");
        max.setUIID("SvField");
        min.getHintLabel().setUIID("SvFieldHint");
        max.getHintLabel().setUIID("SvFieldHint");
        min.setName("price-min");
        max.setName("price-max");
        Container minBox = new Container(new BorderLayout());
        minBox.setUIID("SvPairLeft");
        minBox.add(BorderLayout.CENTER, min);
        Container maxBox = new Container(new BorderLayout());
        maxBox.setUIID("SvPairRight");
        maxBox.add(BorderLayout.CENTER, max);
        row.add(minBox).add(maxBox);
        panel.add(row);
        Runnable apply = () -> {
            long lo = Money.parse(min.getText());
            long hi = Money.parse(max.getText());
            if (lo >= 0 && hi >= 0 && hi < lo) {
                long t = lo;
                lo = hi;
                hi = t;
            }
            query.min = lo;
            query.max = hi;
            changed.run();
            load();
        };
        min.setDoneListener(e -> apply.run());
        max.setDoneListener(e -> apply.run());
        Component button = Ui.secondary("Apply price", "price-apply", e -> apply.run());
        panel.add(button);
        if (query.min >= 0 || query.max >= 0) {
            panel.add(FlowLayout.encloseCenter(Ui.link(Lang.tr("Any price"), "price-clear", e -> {
                query.min = -1;
                query.max = -1;
                changed.run();
                load();
            })));
        }
    }
}
