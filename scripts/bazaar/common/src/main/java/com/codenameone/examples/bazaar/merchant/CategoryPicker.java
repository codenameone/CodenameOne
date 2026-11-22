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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.NameCountDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Choosing a product's category from the standard product taxonomy: a search
/// field over the server's copy of it, and the matching categories to tap.
/// The taxonomy is not shipped in the app; the server answers each search.
final class CategoryPicker {
    /// Told the chosen category: its id (0 for none) and its path.
    interface Chosen {
        void chosen(int id, String path);
    }

    /// Told the path of a category id.
    interface Named {
        void named(String path);
    }

    private CategoryPicker() {
    }

    /// Looks up the path of `id`, for showing a saved category.
    static void label(int id, final Named named) {
        Api.catalog().taxonomy(String.valueOf(id), Net.to(hits -> {
            if (hits != null && !hits.isEmpty()) {
                named.named(hits.get(0).name());
            }
        }, (status, message) -> { }));
    }

    /// A dialog with a search field; tapping a result, or "No category",
    /// closes it and tells `chosen`.
    static void show(int current, final Chosen chosen) {
        final Container body = new Container(BoxLayout.y());
        final TextField search = Ui.field(body, "Search categories", "category-search", "");
        final Container results = new Container(BoxLayout.y());
        results.setScrollableY(true);
        body.add(results);
        final Dialog[] dialog = new Dialog[1];
        final int[] asked = new int[1];
        final Runnable ask = () -> {
            final int ticket = ++asked[0];
            Api.catalog().taxonomy(search.getText(), Net.to(hits -> {
                if (ticket == asked[0]) {
                    draw(results, hits, dialog, chosen);
                }
            }, (status, message) -> { }));
        };
        search.addDataChangedListener((type, index) -> ask.run());
        Button none = new Button(Lang.tr("No category"));
        none.setName("category-none");
        none.addActionListener(e -> {
            dialog[0].dispose();
            chosen.chosen(0, "");
        });
        body.add(none);
        dialog[0] = new Dialog(Lang.tr("Category"));
        dialog[0].setLayout(new com.codename1.ui.layouts.BorderLayout());
        dialog[0].add(com.codename1.ui.layouts.BorderLayout.CENTER, body);
        dialog[0].addCommand(Command.create(Lang.tr("Cancel"), null, e -> dialog[0].dispose()));
        ask.run();
        dialog[0].show();
    }

    private static void draw(Container results, List<NameCountDto> hits, final Dialog[] dialog,
            final Chosen chosen) {
        results.removeAll();
        if (hits != null) {
            for (final NameCountDto hit : hits) {
                Button b = new Button(hit.name());
                b.setUIID("SvRowTitle");
                b.setName("category-" + hit.count());
                b.addActionListener(e -> {
                    dialog[0].dispose();
                    chosen.chosen(hit.count(), hit.name());
                });
                results.add(b);
            }
        }
        results.revalidate();
    }
}
