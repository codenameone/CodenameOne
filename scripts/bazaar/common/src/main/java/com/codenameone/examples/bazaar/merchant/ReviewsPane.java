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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bazaar.api.ReviewDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.shop.Stars;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Review moderation: what shoppers wrote waits as pending until a manager
/// publishes it on the product page or rejects it.
final class ReviewsPane extends MerchantPane {
    static final String[] STATUSES = {"PENDING", "PUBLISHED", "REJECTED"};

    private String status = STATUSES[0];
    private List<ReviewDto> reviews;

    ReviewsPane() {
        super("Reviews", "merchant-reviews");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "reviews-refresh", this::load);
        final String store = MerchantState.storeId();
        Api.reviews().storeReviews(store, status, Net.to(found -> {
            if (store.equals(MerchantState.storeId())) {
                reviews = found;
                paint();
            }
        }, Ui::fail));
    }

    private void paint() {
        Container page = MUi.page("reviews-page");
        Container chips = new Container(new FlowLayout());
        for (int iter = 0; iter < STATUSES.length; iter++) {
            final String s = STATUSES[iter];
            Button chip = new Button(Lang.tr(Words.of(s)), s.equals(status) ? "MeChipOn" : "MeChip");
            chip.setName("reviews-" + s);
            chip.addActionListener(e -> {
                status = s;
                load();
            });
            chips.add(chip);
        }
        page.add(chips);
        Container list = new Container(BoxLayout.y());
        list.setUIID("MeCard");
        if (reviews == null || reviews.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_RATE_REVIEW, "No reviews here",
                    "New reviews wait here until you publish them."));
        }
        for (int iter = 0; reviews != null && iter < reviews.size(); iter++) {
            list.add(row(reviews.get(iter), iter));
        }
        page.add(list);
        root(page);
    }

    private Container row(final ReviewDto r, int index) {
        Container row = new Container(BoxLayout.y());
        row.setUIID("MeListRow");
        row.setName("review-" + index);
        Container head = new Container(new BorderLayout());
        head.add(BorderLayout.WEST, Stars.show(r.rating() * 10, 2.6f));
        head.add(BorderLayout.EAST, MUi.badge(r.status()));
        row.add(head);
        if (r.title() != null && r.title().length() > 0) {
            row.add(Ui.plain(r.title(), "SvRowTitle"));
        }
        if (r.body() != null && r.body().length() > 0) {
            row.add(Ui.text(r.body(), "SvText"));
        }
        row.add(Ui.plain((r.authorName() == null ? "" : r.authorName())
                + (r.verified() ? Ui.DOT + Lang.tr("Verified buyer") : "") + Ui.DOT
                + MUi.when(r.createdAt()), "SvRowDetail"));
        Container actions = new Container(new FlowLayout(com.codename1.ui.Component.RIGHT));
        if (!"PUBLISHED".equals(r.status())) {
            actions.add(Ui.primary(Lang.tr("Publish"), "review-publish-" + index,
                    e -> Api.reviews().publish(MerchantState.storeId(), r.id(),
                            Net.to(done -> load(), Ui::fail))));
        }
        if (!"REJECTED".equals(r.status())) {
            actions.add(Ui.danger(Lang.tr("Reject"), "review-reject-" + index,
                    e -> Api.reviews().reject(MerchantState.storeId(), r.id(),
                            Net.to(done -> load(), Ui::fail))));
        }
        row.add(actions);
        return row;
    }
}
