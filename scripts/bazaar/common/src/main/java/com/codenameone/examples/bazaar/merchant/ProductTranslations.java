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
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.api.TranslationFieldDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The product editor's translations card: a language, then every field of
/// the product -- title, description, SEO text, options, pictures' alt text,
/// text metafields -- with its original, its translation and whether that is
/// missing or outdated. Saving sends only the fields that changed.
final class ProductTranslations {
    private final String productId;
    private final Container card;

    private ProductTranslations(String productId, Container card) {
        this.productId = productId;
        this.card = card;
    }

    /// Fills `card` with the language picker.
    static void add(Container card, String productId) {
        new ProductTranslations(productId, card).pick("");
    }

    private void pick(String locale) {
        card.removeAll();
        card.add(Ui.text(Lang.tr("Shoppers see the translation for their language; an empty "
                + "field shows the original."), "SvMuted"));
        final TextField language = new TextField(locale, Lang.tr("Language, e.g. de or fr-CA"),
                8, TextArea.ANY);
        language.setUIID("SvField");
        language.setName("translation-locale");
        card.add(language);
        card.add(MUi.cardButtons(Ui.secondary(Lang.tr("Open translation"), "translation-open",
                e -> load(language.getText().trim()))));
        Ui.relayout(card);
    }

    private void load(final String locale) {
        if (locale.length() == 0) {
            return;
        }
        Api.markets().translationSheet(MerchantState.storeId(), "PRODUCT", productId, locale,
                Net.to(rows -> paint(locale, rows), Ui::fail));
    }

    private void paint(final String locale, final List<TranslationFieldDto> rows) {
        pick(locale);
        final List<TextArea> fields = new ArrayList<TextArea>();
        for (int iter = 0; iter < rows.size(); iter++) {
            TranslationFieldDto f = rows.get(iter);
            String status = "MISSING".equals(f.status()) ? Lang.tr("Missing")
                    : "OUTDATED".equals(f.status()) ? Lang.tr("Outdated") : Lang.tr("Done");
            card.add(Ui.plain(f.label() + " - " + status, "SvLabel"));
            card.add(Ui.plain(f.original() == null ? "" : f.original(), "SvMuted"));
            TextArea value = new TextArea(f.value() == null ? "" : f.value(), 2, 30);
            value.setUIID("SvField");
            value.setName("translation-" + iter);
            card.add(value);
            fields.add(value);
        }
        card.add(MUi.cardButtons(Ui.primary(Lang.tr("Save translation"), "translation-save",
                e -> save(locale, rows, fields, 0))));
        Ui.relayout(card);
    }

    /// Saves the changed fields one after another, then reloads the sheet so
    /// each status is the server's.
    private void save(final String locale, final List<TranslationFieldDto> rows,
            final List<TextArea> fields, final int from) {
        for (int iter = from; iter < rows.size(); iter++) {
            final TranslationFieldDto f = rows.get(iter);
            String now = fields.get(iter).getText().trim();
            if (now.equals(f.value() == null ? "" : f.value())) {
                continue;
            }
            final int next = iter + 1;
            Api.markets().putTranslation(MerchantState.storeId(), new TranslationDto(f.kind(),
                    f.refId(), locale, f.field(), now), Net.to(done -> save(locale, rows, fields,
                    next), Ui::fail));
            return;
        }
        Ui.say("Translation saved");
        load(locale);
    }
}
