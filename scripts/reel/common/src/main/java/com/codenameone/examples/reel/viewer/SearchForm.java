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
package com.codenameone.examples.reel.viewer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.reel.api.GenreDto;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Ui;
import java.util.List;

/// Search as the viewer types, and the genres to browse when nothing is
/// typed. Results are what the profile may see; the server filters them.
public final class SearchForm {
    private SearchForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("Search", "Search");
        Ui.back(form, previous);
        final TextField query = new TextField("", "Titles, genres, tags", 20, TextField.ANY);
        query.setName("query");
        query.setUIID("SvSearch");
        final Container results = Ui.list("results");
        form.add(BorderLayout.NORTH, query);
        form.add(BorderLayout.CENTER, results);
        final UITimer[] typing = {null};
        query.addDataChangedListener((type, index) -> {
            if (typing[0] != null) {
                typing[0].cancel();
            }
            typing[0] = UITimer.timer(300, false, form, () -> search(form, results, query.getText().trim()));
        });
        form.show();
        genres(form, results);
    }

    private static void search(final Form form, final Container results, final String q) {
        if (q.length() == 0) {
            genres(form, results);
            return;
        }
        Api.catalog().search(q, Account.profileId(), Net.to(found -> {
            results.removeAll();
            if (found.titles() == null || found.titles().isEmpty()) {
                results.add(Ui.empty(FontImage.MATERIAL_SEARCH_OFF, "Nothing found", "Try another word."));
            } else {
                results.add(grid(form, found.titles()));
            }
            results.revalidate();
        }, Ui::fail));
    }

    private static void genres(final Form form, final Container results) {
        Api.catalog().genres(Net.to(list -> {
            results.removeAll();
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                final GenreDto g = list.get(iter);
                results.add(Ui.tap(Ui.row(FontImage.MATERIAL_LOCAL_OFFER, g.name(), String.valueOf(g.titleCount()),
                        null), "genre-" + g.id(), e -> genre(form, g)));
            }
            results.revalidate();
        }, Ui.failed(results, () -> genres(form, results))));
    }

    private static void genre(final Form previous, final GenreDto g) {
        final Form form = Ui.form(g.name(), "Genre");
        Ui.back(form, previous);
        final Container results = Ui.list("genreTitles");
        form.add(BorderLayout.CENTER, results);
        form.show();
        Api.catalog().genre(g.id(), Account.profileId(), Net.to(list -> {
            results.removeAll();
            results.add(grid(form, list));
            results.revalidate();
        }, Ui.failed(results, null)));
    }

    static Container grid(Form form, List<TitleCardDto> titles) {
        Container grid = new Container(new GridLayout(Math.max(1, titles.size() / 3 + 1), 3));
        for (int iter = 0; iter < titles.size(); iter++) {
            grid.add(Cards.card(form, titles.get(iter)));
        }
        return grid;
    }
}
