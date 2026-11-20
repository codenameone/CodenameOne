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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.ReviewDto;
import com.codenameone.examples.bazaar.api.ReviewPageDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The reviews on a product page: the average, how the stars split, the
/// published reviews a page at a time, and a form to write one.
final class ProductReviews {
    private static final int PAGE = 5;

    private final String productId;
    private final Container box = new Container(BoxLayout.y());
    private int page;

    private ProductReviews(String productId) {
        this.productId = productId;
        box.setName("product-reviews");
    }

    static Container create(String productId) {
        ProductReviews r = new ProductReviews(productId);
        r.load();
        return r.box;
    }

    private void load() {
        Api.reviews().reviews(productId, page, PAGE, Net.to(this::paint,
                (status, message) -> { }));
    }

    /// The share of `count` in `total` as a 0..1 fraction, 0 when empty.
    static double share(int count, int total) {
        return total <= 0 ? 0 : count / (double) total;
    }

    /// "4.3" from 43 tenths.
    static String average(int tenths) {
        return (tenths / 10) + "." + (tenths % 10);
    }

    private void paint(ReviewPageDto p) {
        box.removeAll();
        Container summary = new Container(new BorderLayout());
        summary.setUIID("ShopReviewSummary");
        Container left = new Container(BoxLayout.y());
        left.add(Ui.plain(p.count() == 0 ? "-" : average(p.averageTenths()), "ShopReviewAverage"));
        left.add(Stars.show(p.averageTenths(), 3f));
        left.add(Ui.plain(Lang.tr("{0} reviews", String.valueOf(p.count())), "SvMuted"));
        summary.add(BorderLayout.WEST, left);
        Container bars = new Container(BoxLayout.y());
        for (int star = 5; star >= 1; star--) {
            int n = p.histogram() == null || p.histogram().size() < star ? 0
                    : p.histogram().get(star - 1).intValue();
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.WEST, Ui.plain(String.valueOf(star), "SvRowDetail"));
            row.add(BorderLayout.CENTER, bar(share(n, p.count())));
            row.add(BorderLayout.EAST, Ui.plain(String.valueOf(n), "SvRowDetail"));
            bars.add(row);
        }
        summary.add(BorderLayout.CENTER, bars);
        box.add(summary);
        if (Session.user() != null) {
            box.add(Ui.secondary(Lang.tr("Write a review"), "review-write", e -> write()));
        }
        for (int iter = 0; p.reviews() != null && iter < p.reviews().size(); iter++) {
            box.add(review(p.reviews().get(iter)));
        }
        if (p.reviews() == null || p.reviews().isEmpty()) {
            box.add(Ui.text(Lang.tr("No reviews yet. Be the first."), "SvMuted"));
        }
        if (page > 0 || (page + 1) * PAGE < p.count()) {
            Container nav = new Container(new BorderLayout());
            if (page > 0) {
                nav.add(BorderLayout.WEST, Ui.link(Lang.tr("Newer"), "reviews-newer", e -> {
                    page--;
                    load();
                }));
            }
            if ((page + 1) * PAGE < p.count()) {
                nav.add(BorderLayout.EAST, Ui.link(Lang.tr("Older"), "reviews-older", e -> {
                    page++;
                    load();
                }));
            }
            box.add(nav);
        }
        Ui.relayout(box);
    }

    private static Container bar(double share) {
        Container track = new Container(new BorderLayout());
        track.setUIID("ShopReviewTrack");
        Label fill = new Label("", "ShopReviewFill");
        fill.setPreferredW(Math.max(1, (int) (com.codename1.ui.CN.convertToPixels(30f) * share)));
        track.add(BorderLayout.WEST, fill);
        return track;
    }

    private static Container review(ReviewDto r) {
        Container c = new Container(BoxLayout.y());
        c.setUIID("ShopReview");
        c.add(Stars.show(r.rating() * 10, 2.4f));
        if (r.title() != null && r.title().length() > 0) {
            c.add(Ui.plain(r.title(), "SvRowTitle"));
        }
        if (r.body() != null && r.body().length() > 0) {
            c.add(Ui.text(r.body(), "SvText"));
        }
        c.add(Ui.plain((r.authorName() == null ? "" : r.authorName())
                + (r.verified() ? Ui.DOT + Lang.tr("Verified buyer") : ""), "SvRowDetail"));
        return c;
    }

    private void write() {
        final int[] rating = {5};
        Container body = new Container(BoxLayout.y());
        body.add(Stars.pick(rating, null));
        TextField title = Ui.field(body, "Title", "review-title", "", TextArea.ANY);
        body.add(Ui.label("Review", "SvLabel"));
        TextArea text = new TextArea("", 4, 30);
        text.setUIID("SvField");
        text.setName("review-body");
        body.add(text);
        body.add(Ui.text(Lang.tr("Reviews appear once the store has read them."), "SvMuted"));
        Command ok = Command.create(Lang.tr("Send"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Write a review"), body, cancel, ok) != ok) {
            return;
        }
        ReviewDto r = new ReviewDto(null, productId, null, rating[0], title.getText().trim(),
                text.getText().trim(), null, false, 0);
        Api.reviews().submit(productId, r, Net.to(saved -> Ui.say("Thanks, your review is "
                + "waiting for the store"), Ui::fail));
    }
}
