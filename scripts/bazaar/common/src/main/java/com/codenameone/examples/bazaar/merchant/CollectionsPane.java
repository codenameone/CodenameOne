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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Collections: manual ones the merchant fills and orders by hand, and smart
/// ones whose products are chosen by rules -- tag, type, vendor, price --
/// built one row at a time (`SmartRules` turns the rows into the server's
/// rule text and back).
final class CollectionsPane extends MerchantPane {
    private final Map<String, ProductCardDto> products = new HashMap<String, ProductCardDto>();
    private final List<ProductCardDto> catalog = new ArrayList<ProductCardDto>();

    CollectionsPane() {
        super("Collections", "merchant-collections");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add collection", "collection-add", () -> edit(null));
        final String store = MerchantState.storeId();
        Api.catalog().merchantProducts(store, null, null, null, null, null, null, "title", "0", "500",
                Net.to(page -> {
                    catalog.clear();
                    catalog.addAll(page.items());
                    products.clear();
                    for (ProductCardDto p : page.items()) {
                        products.put(p.id(), p);
                    }
                }, (s, m) -> { }));
        Api.catalog().merchantCollections(store, Net.to(this::fill, Ui::fail));
    }

    private void fill(List<CollectionDto> list) {
        Container page = MUi.page("collections-list");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        for (final CollectionDto c : list) {
            List<SmartRules.Rule> rules = SmartRules.parse(c.ruleText());
            String how = rules == null ? c.ruleText() : Lang.tr(SmartRules.describe(rules));
            Container row = Ui.row(c.ruleText() != null && c.ruleText().length() > 0
                    ? FontImage.MATERIAL_AUTO_AWESOME : FontImage.MATERIAL_COLLECTIONS_BOOKMARK,
                    c.title(), how + Ui.DOT + Lang.tr("{0} products", String.valueOf(c.productCount())),
                    MUi.badge(c.published() ? "ACTIVE" : "DRAFT"));
            card.add(Ui.tap(row, "collection-" + c.handle(), e -> edit(c)));
        }
        if (list.isEmpty()) {
            card.add(Ui.empty(FontImage.MATERIAL_COLLECTIONS_BOOKMARK, "No collections",
                    "Group products for the storefront: by hand, or by rules."));
        }
        page.add(card);
        root(page);
    }

    private void edit(final CollectionDto c) {
        Container page = MUi.page("collection-editor");
        Container words = new Container(BoxLayout.y());
        words.setUIID("MeCard");
        final TextField title = MUi.text(words, "Title", "collection-title", c == null ? "" : c.title());
        words.add(Ui.label("Description", "SvLabel"));
        final TextArea description = new TextArea(c == null || c.description() == null ? ""
                : c.description(), 3, 30);
        description.setUIID("SvField");
        words.add(description);
        final CheckBox published = new CheckBox(Lang.tr("Shown in the storefront"));
        published.setSelected(c == null || c.published());
        words.add(published);
        page.add(words);

        final boolean[] smart = {c != null && c.ruleText() != null && c.ruleText().length() > 0};
        final List<SmartRules.Rule> rules = new ArrayList<SmartRules.Rule>();
        if (smart[0]) {
            List<SmartRules.Rule> parsed = SmartRules.parse(c.ruleText());
            if (parsed != null) {
                rules.addAll(parsed);
            }
        }
        final List<String> ids = new ArrayList<String>();
        if (c != null && c.productIds() != null && !smart[0]) {
            ids.addAll(c.productIds());
        }
        final Container body = new Container(BoxLayout.y());
        body.setUIID("MeCard");
        final Runnable[] paint = new Runnable[1];
        paint[0] = () -> {
            body.removeAll();
            if (smart[0]) {
                rulesEditor(body, rules, paint[0]);
            } else {
                manualEditor(body, ids, paint[0]);
            }
            Ui.relayout(body);
        };
        Ui.section(page, "Products");
        page.add(Ui.choice("collection-kind", new String[] {Lang.tr("Manual"), Lang.tr("Smart")},
                new String[] {"manual", "smart"}, smart[0] ? "smart" : "manual", v -> {
                    smart[0] = "smart".equals(v);
                    paint[0].run();
                }));
        page.add(body);
        paint[0].run();

        Container buttons = MUi.buttons();
        if (c != null) {
            buttons.add(Ui.danger(Lang.tr("Delete"), "collection-delete", e -> {
                if (MUi.confirm("Delete collection", "The products stay; only the collection goes.",
                        "Delete")) {
                    Api.catalog().deleteCollection(MerchantState.storeId(), c.id(), Net.to(d -> {
                        popAll();
                        load();
                    }, Ui::fail));
                }
            }));
        }
        buttons.add(Ui.primary(Lang.tr("Save"), "collection-save", e -> {
            String t = title.getText().trim();
            if (t.length() == 0) {
                Ui.say("A collection needs a title");
                return;
            }
            String ruleText = smart[0] ? SmartRules.format(rules) : "";
            if (smart[0] && ruleText.length() == 0) {
                Ui.say("Add at least one rule");
                return;
            }
            CollectionDto wanted = new CollectionDto(c == null ? null : c.id(),
                    c == null ? Handles.of(t) : c.handle(), t, description.getText(),
                    c == null ? null : c.photoId(), ruleText, c == null ? 0 : c.position(),
                    published.isSelected(), smart[0] ? null : ids, 0);
            Net.Ok<CollectionDto> done = saved -> {
                Ui.say("Saved");
                popAll();
                load();
            };
            if (c == null) {
                Api.catalog().createCollection(MerchantState.storeId(), wanted, Net.to(done, Ui::fail));
            } else {
                Api.catalog().updateCollection(MerchantState.storeId(), c.id(), wanted,
                        Net.to(done, Ui::fail));
            }
        }));
        Container all = new Container(new BorderLayout());
        all.add(BorderLayout.CENTER, page);
        all.add(BorderLayout.SOUTH, buttons);
        push(c == null ? Lang.tr("New collection") : c.title(), all);
    }

    /// The rule builder: one row per rule, a field picker and a value.
    private void rulesEditor(Container body, final List<SmartRules.Rule> rules, final Runnable repaint) {
        body.add(Ui.text(Lang.tr("Products that match every rule are in the collection."),
                "SvMuted"));
        for (int iter = 0; iter < rules.size(); iter++) {
            final int at = iter;
            SmartRules.Rule r = rules.get(iter);
            Container row = new Container(new BorderLayout());
            row.setUIID("MeRuleRow");
            final Picker field = new Picker();
            field.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
            field.setUIID("SvField");
            field.setName("rule-field-" + iter);
            String[] labels = new String[SmartRules.FIELDS.length];
            int chosen = 0;
            for (int f = 0; f < labels.length; f++) {
                labels[f] = Lang.tr(SmartRules.label(SmartRules.FIELDS[f]));
                if (SmartRules.FIELDS[f].equals(r.field)) {
                    chosen = f;
                }
            }
            field.setStrings(labels);
            field.setSelectedStringIndex(chosen);
            final TextField value = new TextField(r.isPrice() ? ProductCsv.major(cents(r.value)) : r.value,
                    Lang.tr("Value"), 12, TextArea.ANY);
            value.setUIID("SvField");
            value.setName("rule-value-" + iter);
            Runnable store = () -> {
                String f = SmartRules.FIELDS[Math.max(0, field.getSelectedStringIndex())];
                boolean price = SmartRules.PRICE_BELOW.equals(f) || SmartRules.PRICE_ABOVE.equals(f);
                String v = price ? String.valueOf(ProductCsv.cents(value.getText()))
                        : value.getText();
                rules.set(at, new SmartRules.Rule(f, v));
            };
            field.addActionListener(e -> store.run());
            value.addDataChangedListener((t, i) -> store.run());
            row.add(BorderLayout.WEST, field);
            row.add(BorderLayout.CENTER, value);
            row.add(BorderLayout.EAST, Ui.flat(FontImage.MATERIAL_DELETE, "rule-remove-" + iter, e -> {
                rules.remove(at);
                repaint.run();
            }));
            body.add(row);
        }
        body.add(Ui.secondary(Lang.tr("Add rule"), "rule-add", e -> {
            rules.add(new SmartRules.Rule(SmartRules.TAG, ""));
            repaint.run();
        }));
    }

    private static long cents(String digits) {
        long out = 0;
        for (int iter = 0; iter < digits.length(); iter++) {
            out = out * 10 + (digits.charAt(iter) - '0');
        }
        return out;
    }

    /// Manual: the products in order, moved up and down, removed, added.
    private void manualEditor(Container body, final List<String> ids, final Runnable repaint) {
        for (int iter = 0; iter < ids.size(); iter++) {
            final int at = iter;
            ProductCardDto p = products.get(ids.get(iter));
            Container row = new Container(new BorderLayout());
            row.setUIID("MeListRow");
            row.add(BorderLayout.CENTER, Ui.plain((iter + 1) + ".  " + (p == null ? ids.get(iter)
                    : p.title()), "SvRowTitle"));
            Container moves = new Container(BoxLayout.x());
            moves.add(Ui.flat(FontImage.MATERIAL_ARROW_UPWARD, "move-up-" + iter, e -> {
                if (at > 0) {
                    ids.add(at - 1, ids.remove(at));
                    repaint.run();
                }
            }));
            moves.add(Ui.flat(FontImage.MATERIAL_ARROW_DOWNWARD, "move-down-" + iter, e -> {
                if (at < ids.size() - 1) {
                    ids.add(at + 1, ids.remove(at));
                    repaint.run();
                }
            }));
            moves.add(Ui.flat(FontImage.MATERIAL_DELETE, "move-remove-" + iter, e -> {
                ids.remove(at);
                repaint.run();
            }));
            row.add(BorderLayout.EAST, moves);
            body.add(row);
        }
        body.add(Ui.secondary(Lang.tr("Add products"), "collection-add-products", e -> {
            Container pick = new Container(BoxLayout.y());
            pick.setScrollableY(true);
            List<CheckBox> boxes = new ArrayList<CheckBox>();
            for (ProductCardDto p : catalog) {
                if (ids.contains(p.id())) {
                    continue;
                }
                CheckBox box = new CheckBox(p.title());
                box.putClientProperty("id", p.id());
                boxes.add(box);
                pick.add(box);
            }
            Command ok = Command.create(Lang.tr("Add"), null, null);
            Command cancel = Command.create(Lang.tr("Cancel"), null, null);
            if (Dialog.show(Lang.tr("Add products"), pick, cancel, ok) == ok) {
                for (CheckBox box : boxes) {
                    if (box.isSelected()) {
                        ids.add((String) box.getClientProperty("id"));
                    }
                }
                repaint.run();
            }
        }));
    }
}
