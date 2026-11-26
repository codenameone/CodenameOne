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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.CombinedListingDto;
import com.codenameone.examples.bazaar.api.CsvErrorDto;
import com.codenameone.examples.bazaar.api.ImportDto;
import com.codenameone.examples.bazaar.api.ImportResultDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// Combined listings and the marketplace import. The import is previewed
/// first: every row is checked and its problems listed by line, and the
/// products are made only when nothing is wrong and "create" is ticked.
final class ListingsPane extends MerchantPane {
    ListingsPane() {
        super("Listings", "merchant-listings");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_UPLOAD_FILE, "Import", "listings-import", this::importer);
        Api.ops().combinedListings(MerchantState.storeId(), Net.to(found -> {
            Container list = new Container(BoxLayout.y());
            list.setName("listings-list");
            if (found.isEmpty()) {
                list.add(Ui.plain(Lang.tr("No combined listings yet"), "SvMuted"));
            }
            for (int iter = 0; iter < found.size(); iter++) {
                CombinedListingDto l = found.get(iter);
                StringBuilder colours = new StringBuilder();
                for (int k = 0; k < l.members().size(); k++) {
                    colours.append(k == 0 ? "" : ", ").append(l.members().get(k).color());
                }
                list.add(Ui.row((char) 0, l.title(), colours.toString(),
                        Ui.plain(String.valueOf(l.members().size()), "SvRowValue")));
            }
            root(list);
        }, Ui::fail));
    }

    private void importer() {
        final Container page = new Container(BoxLayout.y());
        page.setName("listings-import-page");
        final TextArea csv = new TextArea(8, 40);
        csv.setName("listings-csv");
        csv.setHint("listing_id,title,description,brand,category,color,size,sku,price,quantity");
        page.add(csv);
        final CheckBox commit = new CheckBox(Lang.tr("Create the products when nothing is wrong"));
        commit.setName("listings-commit");
        page.add(commit);
        final Container result = new Container(BoxLayout.y());
        page.add(Ui.primary(Lang.tr("Preview"), "listings-preview", e -> Api.ops().importMarketplace(
                MerchantState.storeId(), new ImportDto(csv.getText(), commit.isSelected()),
                Net.to(r -> show(result, r), Ui::fail))));
        page.add(result);
        push(Lang.tr("Marketplace import"), page);
    }

    private void show(Container result, ImportResultDto r) {
        result.removeAll();
        result.add(Ui.fact(Lang.tr(r.committed() ? "Imported" : "Preview"), r.rows() + " / "
                + r.products() + " / " + r.variants()));
        for (int iter = 0; iter < r.errors().size(); iter++) {
            CsvErrorDto e = r.errors().get(iter);
            result.add(Ui.row((char) 0, Lang.tr("Line") + " " + e.line(), e.message(),
                    Ui.plain("", "SvRowValue")));
        }
        for (int iter = 0; iter < r.preview().size(); iter++) {
            result.add(Ui.plain(r.preview().get(iter), "SvMuted"));
        }
        if (r.committed()) {
            load();
        }
        Ui.relayout(result);
    }
}
