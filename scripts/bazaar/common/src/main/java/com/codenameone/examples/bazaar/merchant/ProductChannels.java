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
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.BundleDto;
import com.codenameone.examples.bazaar.api.BundlePartDto;
import com.codenameone.examples.bazaar.api.ChannelScheduleDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The product editor's bundle and publishing schedule cards: what a bundle
/// is made of, and when the product shows on each channel.
final class ProductChannels {
    /// Component rows offered beyond those the bundle has.
    private static final int SPARE = 3;
    private static final int MAX_PARTS = 20;

    private final String productId;
    private final Container bundleCard;
    private final Container scheduleCard;

    private ProductChannels(String productId, Container bundleCard, Container scheduleCard) {
        this.productId = productId;
        this.bundleCard = bundleCard;
        this.scheduleCard = scheduleCard;
    }

    /// Adds both cards, filled once the server answers.
    static void add(Container bundleCard, Container scheduleCard, String productId) {
        ProductChannels c = new ProductChannels(productId, bundleCard, scheduleCard);
        c.loadBundle();
        c.loadSchedule();
    }

    private void loadBundle() {
        Api.catalog().bundle(MerchantState.storeId(), productId, Net.to(this::paintBundle,
                Ui::fail));
    }

    private void loadSchedule() {
        Api.catalog().schedule(MerchantState.storeId(), productId, Net.to(this::paintSchedule,
                Ui::fail));
    }

    private void paintBundle(BundleDto b) {
        bundleCard.removeAll();
        bundleCard.add(Ui.text(Lang.tr("A bundle has its own price and no stock of its own: "
                + "selling one takes its components out of stock."), "SvMuted"));
        final CheckBox on = new CheckBox(Lang.tr("Sell as a bundle of the components below"));
        on.setName("bundle-on");
        on.setSelected(b.bundle());
        bundleCard.add(on);
        if (b.bundle()) {
            bundleCard.add(Ui.plain(Lang.tr("Bundles the components make: {0}",
                    String.valueOf(b.available())), "SvMuted"));
        }
        int rows = Math.min(MAX_PARTS, b.parts().size() + SPARE);
        final List<TextField[]> fields = new ArrayList<TextField[]>();
        for (int iter = 0; iter < rows; iter++) {
            BundlePartDto part = iter < b.parts().size() ? b.parts().get(iter) : null;
            Container row = new Container(new GridLayout(1, 2));
            TextField sku = new TextField(part == null || part.sku() == null ? "" : part.sku(),
                    Lang.tr("Component SKU"), 12, TextArea.ANY);
            sku.setUIID("SvField");
            sku.setName("bundle-sku-" + iter);
            TextField qty = new TextField(part == null ? "" : String.valueOf(part.quantity()),
                    Lang.tr("Units"), 4, TextArea.NUMERIC);
            qty.setUIID("SvField");
            qty.setName("bundle-qty-" + iter);
            row.add(sku).add(qty);
            bundleCard.add(row);
            if (part != null && part.title() != null) {
                bundleCard.add(Ui.plain(part.title(), "SvMuted"));
            }
            fields.add(new TextField[] {sku, qty});
        }
        bundleCard.add(MUi.buttons(Ui.secondary(Lang.tr("Save bundle"), "bundle-save", e -> {
            List<BundlePartDto> parts = new ArrayList<BundlePartDto>();
            for (int iter = 0; iter < fields.size(); iter++) {
                String sku = fields.get(iter)[0].getText().trim();
                if (sku.length() == 0) {
                    continue;
                }
                int n = MUi.integer(fields.get(iter)[1]);
                parts.add(new BundlePartDto(null, sku, n <= 0 ? 1 : n, "", 0));
            }
            Api.catalog().setBundle(MerchantState.storeId(), productId, new BundleDto(productId,
                    on.isSelected(), parts, 0), Net.to(saved -> {
                        Ui.say("Bundle saved");
                        paintBundle(saved);
                    }, Ui::fail));
        })));
        Ui.relayout(bundleCard);
    }

    private void paintSchedule(final List<ChannelScheduleDto> windows) {
        scheduleCard.removeAll();
        scheduleCard.add(Ui.text(Lang.tr("Times are UTC, as YYYY-MM-DD HH:MM. Leave both empty "
                + "to follow the status alone; an empty end keeps it published."), "SvMuted"));
        final List<TextField[]> fields = new ArrayList<TextField[]>();
        for (int iter = 0; iter < windows.size(); iter++) {
            ChannelScheduleDto w = windows.get(iter);
            Container block = new Container(BoxLayout.y());
            String now = w.publishAt() == 0 && w.unpublishAt() == 0 ? Lang.tr("Always")
                    : w.visible() ? Lang.tr("Showing now") : Lang.tr("Hidden now");
            block.add(Ui.label(w.name() + Ui.DOT + now, "SvLabel"));
            Container row = new Container(new GridLayout(1, 2));
            TextField pub = new TextField(UtcTimes.format(w.publishAt()), Lang.tr("Publish at"),
                    16, TextArea.ANY);
            pub.setUIID("SvField");
            pub.setName("schedule-publish-" + iter);
            TextField unpub = new TextField(UtcTimes.format(w.unpublishAt()),
                    Lang.tr("Unpublish at"), 16, TextArea.ANY);
            unpub.setUIID("SvField");
            unpub.setName("schedule-unpublish-" + iter);
            row.add(pub).add(unpub);
            block.add(row);
            scheduleCard.add(block);
            fields.add(new TextField[] {pub, unpub});
        }
        scheduleCard.add(MUi.buttons(Ui.secondary(Lang.tr("Save schedule"), "schedule-save",
                e -> saveSchedule(windows, fields, 0, false))));
        Ui.relayout(scheduleCard);
    }

    /// Sends the changed windows one after another, from `from` on.
    private void saveSchedule(List<ChannelScheduleDto> windows, List<TextField[]> fields,
            int from, boolean any) {
        for (int iter = from; iter < windows.size(); iter++) {
            ChannelScheduleDto w = windows.get(iter);
            long pub = UtcTimes.parse(fields.get(iter)[0].getText());
            long unpub = UtcTimes.parse(fields.get(iter)[1].getText());
            if (pub < 0 || unpub < 0) {
                Ui.say("Enter times as YYYY-MM-DD HH:MM");
                return;
            }
            if (pub != w.publishAt() || unpub != w.unpublishAt()) {
                final int next = iter + 1;
                Api.catalog().setSchedule(MerchantState.storeId(), productId,
                        new ChannelScheduleDto(w.channel(), "", pub, unpub, true),
                        Net.to(list -> saveSchedule(windows, fields, next, true), Ui::fail));
                return;
            }
        }
        if (any) {
            Ui.say("Schedule saved");
        }
        loadSchedule();
    }
}
