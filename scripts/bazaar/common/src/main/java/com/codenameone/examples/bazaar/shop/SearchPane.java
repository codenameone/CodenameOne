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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Form;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;

/// Search and browse: words (searched a moment after typing stops, or at
/// once on Enter; Esc clears them), filters beside the results on a wide
/// screen or in a sheet on a phone, and the order of the results. A
/// collection opened from the storefront is this screen with that
/// collection's filter on.
final class SearchPane extends ShopPane {
    private static final int DEBOUNCE_MS = 320;
    private static final float FILTERS_MM = 62f;
    private static final int ESC = 27;

    private final SearchQuery query = ShopState.query();
    private final boolean wide = Layouts.wide();
    private final TextField words = new TextField("", "", 20, TextField.ANY);
    private final Button filters = new Button("", "ShopToolButton");
    private final Button sort = new Button("", "ShopToolButton");
    private ProductGrid grid;
    private FilterPanel panel;
    private UITimer typing;
    /// Predictive search (`/s/{handle}/suggest`), under the search bar.
    private final Container suggestions = new Container(BoxLayout.y());
    private final SuggestGate gate = new SuggestGate();
    private boolean scopedSearch;
    private final ActionListener<com.codename1.ui.events.ActionEvent> escape = e -> {
        if (words.getText().length() > 0) {
            words.setText("");
            query.q = "";
            changed();
        }
    };

    SearchPane() {
        super(new BorderLayout());
        setName("shop-search");
        build();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        Form f = getComponentForm();
        if (f != null) {
            f.addKeyListener(ESC, escape);
        }
        if (CN.isDesktop()) {
            words.requestFocus();
        }
    }

    @Override
    protected void deinitialize() {
        Form f = getComponentForm();
        if (f != null) {
            f.removeKeyListener(ESC, escape);
        }
        if (typing != null) {
            typing.cancel();
        }
        super.deinitialize();
    }

    @Override
    void find() {
        words.requestFocus();
        words.startEditingAsync();
    }

    @Override
    void reload() {
        if (grid == null || scoped != ShopState.inStore()) {
            build();
            return;
        }
        grid.reload();
        if (scoped) {
            panel.load();
        }
        marks();
    }

    /// Whether the screen was built for one store's search.
    private boolean scoped;

    private void build() {
        removeAll();
        scoped = ShopState.inStore();
        // The pill is a box around the field and its magnifier, not the field
        // itself: a hint icon sat under the caret, which drew through it.
        words.setUIID("SvSearchInput");
        words.getHintLabel().setUIID("SvSearchHint");
        words.setName("search");
        // Inside a store the search is that store's, with its filters; from
        // Discover it runs over every store.
        words.setHint(scoped ? Lang.tr("Search {0}", ShopState.store().name())
                : Lang.tr("Search every store"));
        words.setText(query.q == null ? "" : query.q);
        words.addDataChangedListener((type, index) -> {
            if (typing != null) {
                typing.cancel();
            }
            Form f = getComponentForm();
            if (f != null) {
                typing = UITimer.timer(DEBOUNCE_MS, false, f, () -> {
                    suggest(words.getText());
                    if (!words.getText().equals(query.q)) {
                        query.q = words.getText();
                        changed();
                    }
                });
            }
        });
        // Enter searches at once.
        words.setDoneListener(e -> {
            if (typing != null) {
                typing.cancel();
            }
            query.q = words.getText();
            clearSuggestions();
            changed();
        });

        filters.setName("filters-open");
        FontImage.setMaterialIcon(filters, FontImage.MATERIAL_TUNE, 3.4f);
        filters.addActionListener(e -> openFilters());
        sort.setName("sort-open");
        FontImage.setMaterialIcon(sort, FontImage.MATERIAL_SORT, 3.4f);
        sort.addActionListener(e -> openSort());

        Container bar = new Container(new BorderLayout());
        bar.setUIID("ShopSearchBar");
        Container box = new Container(new BorderLayout());
        box.setUIID("SvSearchField");
        Label glass = new Label("", "SvSearchIcon");
        FontImage.setMaterialIcon(glass, FontImage.MATERIAL_SEARCH, 3.2f);
        box.add(BorderLayout.WEST, glass);
        box.add(BorderLayout.CENTER, words);
        bar.add(BorderLayout.CENTER, box);
        Container tools = new Container(BoxLayout.x());
        if (!wide && scoped) {
            tools.add(filters);
        }
        tools.add(sort);
        bar.add(BorderLayout.EAST, tools);

        Container north = new Container(BoxLayout.y());
        north.add(bar);
        scopedSearch = scoped;
        suggestions.setName("search-suggestions");
        if (suggestions.getParent() != null) {
            suggestions.remove();
        }
        north.add(suggestions);
        if (query.collection != null) {
            Container on = new Container(new BorderLayout());
            on.setUIID("ShopFilterOn");
            on.add(BorderLayout.CENTER, Ui.plain(Lang.tr("Collection: {0}",
                    query.collectionTitle != null ? query.collectionTitle : query.collection),
                    "SvMutedSmall"));
            on.add(BorderLayout.EAST, Ui.link("Clear", "collection-clear", e -> {
                query.collection = null;
                query.collectionTitle = null;
                build();
                Ui.relayout(this);
            }));
            north.add(on);
        }
        add(BorderLayout.NORTH, north);

        panel = new FilterPanel(query, this::changed);
        int side = wide && scoped ? CN.convertToPixels(FILTERS_MM) : 0;
        grid = new ProductGrid(null, query, side);
        if (wide && scoped) {
            Container left = new Container(new BorderLayout()) {
                @Override
                protected Dimension calcPreferredSize() {
                    Dimension d = super.calcPreferredSize();
                    d.setWidth(CN.convertToPixels(FILTERS_MM));
                    return d;
                }
            };
            left.setUIID("ShopFilterSide");
            left.add(BorderLayout.CENTER, panel.component());
            add(BorderLayout.WEST, left);
        }
        add(BorderLayout.CENTER, grid.component());
        grid.reload();
        if (scoped) {
            panel.load();
        }
        marks();
    }

    /// Asks the store for suggestions for `text`; only the newest answer is
    /// shown. Across every store there is no suggestion endpoint.
    private void suggest(String text) {
        if (!scopedSearch || !SuggestGate.worthAsking(text)) {
            clearSuggestions();
            return;
        }
        final int ticket = gate.issue();
        final String store = ShopState.store().handle();
        com.codenameone.examples.bazaar.net.Api.catalog().suggest(store, text.trim(),
                com.codename1.appkit.Net.to(list -> {
                    if (!gate.current(ticket)) {
                        return;
                    }
                    suggestions.removeAll();
                    if (list != null) {
                        for (int iter = 0; iter < list.size(); iter++) {
                            suggestions.add(suggestion(store, list.get(iter)));
                        }
                    }
                    Ui.relayout(this);
                }, (status, message) -> { }));
    }

    private Button suggestion(String store,
            com.codenameone.examples.bazaar.api.SearchSuggestionDto s) {
        Button b = new Button(s.title(), "ShopSuggestion");
        b.setName("suggest-" + s.handle());
        boolean product = "product".equals(s.kind());
        Ui.icon(b, product ? FontImage.MATERIAL_SELL
                : FontImage.MATERIAL_COLLECTIONS, 3f);
        b.addActionListener(e -> {
            clearSuggestions();
            if (product) {
                com.codenameone.examples.bazaar.net.Api.catalog().productByHandle(store,
                        s.handle(), com.codename1.appkit.Net.to(p -> {
                            if (p != null) {
                                ProductForm.show(p.id(), p.title());
                            }
                        }, (status, message) -> { }));
            } else {
                query.collection = s.handle();
                query.collectionTitle = s.title();
                query.q = "";
                build();
                Ui.relayout(this);
            }
        });
        return b;
    }

    private void clearSuggestions() {
        gate.invalidate();
        if (suggestions.getComponentCount() > 0) {
            suggestions.removeAll();
            Ui.relayout(this);
        }
    }

    /// The query changed: new results, new facets.
    private void changed() {
        if (grid != null) {
            grid.reload();
        }
        marks();
    }

    private void marks() {
        int active = query.active();
        filters.setBadgeText(active > 0 ? String.valueOf(active) : null);
        filters.setBadgeUIID("ShopBadge");
        filters.repaint();
    }

    private void openFilters() {
        final Sheet sheet = new Sheet(null, Lang.tr("Filters"));
        sheet.setName("filters-sheet");
        FilterPanel inSheet = new FilterPanel(query, () -> {
            changed();
            panel.load();
        });
        sheet.getContentPane().setLayout(new BorderLayout());
        sheet.getContentPane().add(BorderLayout.CENTER, inSheet.component());
        sheet.getContentPane().add(BorderLayout.SOUTH, Ui.primary("Show results",
                "filters-done", e -> sheet.back()));
        inSheet.load();
        sheet.show();
    }

    private void openSort() {
        final Sheet sheet = new Sheet(null, Lang.tr("Sort by"));
        sheet.setName("sort-sheet");
        sheet.getContentPane().setLayout(BoxLayout.y());
        for (int iter = 0; iter < SearchQuery.SORTS.length; iter++) {
            final String key = SearchQuery.SORTS[iter];
            boolean on = key.equals(query.sort);
            Container row = Ui.row(on ? FontImage.MATERIAL_CHECK : 0,
                    SearchQuery.SORT_LABELS[iter], null, null);
            sheet.getContentPane().add(Ui.tap(row, "sort-" + key, e -> {
                query.sort = key;
                sheet.back();
                changed();
            }));
        }
        sheet.show();
    }
}
