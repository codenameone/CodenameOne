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
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.ReviewDto;
import com.codenameone.examples.bazaar.api.ReviewPageDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.SignInGate;
import com.codenameone.examples.bazaar.ui.Ui;

/// The reviews of a product. On the product page: the average, how the
/// stars split, the newest few and a way to the rest. On a screen of their
/// own ([#showAll]): every published review a page at a time. Each shows its
/// stars, the author's first name, the day and the words; "Write a review"
/// asks a guest to sign in first.
final class ProductReviews {
    /// How many the product page shows before "See all reviews".
    private static final int PREVIEW = 3;
    private static final int PAGE = 10;

    private final String productId;
    private final String title;
    private final boolean full;
    private final Container box = new Container(BoxLayout.y());
    private int page;

    private ProductReviews(String productId, String title, boolean full) {
        this.productId = productId;
        this.title = title;
        this.full = full;
        box.setName(full ? "reviews-list" : "product-reviews");
    }

    /// The summary for the product page.
    static Container create(String productId, String title) {
        ProductReviews r = new ProductReviews(productId, title, false);
        r.box.add(new com.codenameone.examples.bazaar.ui.Shimmer(2, "SvSkeleton"));
        r.load();
        return r.box;
    }

    /// Every review, on a screen of its own with a way back.
    static void showAll(String productId, String title) {
        Form previous = CN.getCurrentForm();
        Form form = Ui.form(Lang.tr("Reviews"), "Reviews");
        form.setLayout(new BorderLayout());
        form.setScrollable(false);
        Ui.back(form, previous);
        Container page = Ui.page();
        page.setName("reviews");
        ProductReviews r = new ProductReviews(productId, title, true);
        r.box.add(new com.codenameone.examples.bazaar.ui.Shimmer(4, "SvSkeleton"));
        page.add(r.box);
        form.add(BorderLayout.CENTER, page);
        form.show();
        r.load();
    }

    private void load() {
        Api.reviews().reviews(productId, page, full ? PAGE : PREVIEW, Net.to(this::paint,
                (status, message) -> {
                    box.removeAll();
                    box.add(Ui.text(Lang.tr("Reviews could not be loaded."), "SvMuted"));
                    Ui.relayout(box);
                }));
    }

    /// The share of `count` in `total` as a 0..1 fraction, 0 when empty.
    static double share(int count, int total) {
        return total <= 0 ? 0 : count / (double) total;
    }

    /// "4.3" from 43 tenths.
    static String average(int tenths) {
        return (tenths / 10) + "." + (tenths % 10);
    }

    /// The first word of a name: a review is signed with a first name.
    static String firstName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private void paint(ReviewPageDto p) {
        box.removeAll();
        if (!full || page == 0) {
            box.add(summary(p));
        }
        box.add(Ui.secondary(Lang.tr("Write a review"), "review-write",
                e -> SignInGate.require(box.getComponentForm(), "Write a review",
                        "Sign in to review what you bought. Reviews are signed with your "
                        + "first name.", this::write)));
        for (int iter = 0; p.reviews() != null && iter < p.reviews().size(); iter++) {
            box.add(review(p.reviews().get(iter)));
        }
        if (p.reviews() == null || p.reviews().isEmpty()) {
            box.add(Ui.text(Lang.tr("No reviews yet. Be the first."), "SvMuted"));
        }
        if (!full) {
            if (p.count() > PREVIEW) {
                box.add(Ui.link(Lang.tr("See all {0} reviews", String.valueOf(p.count())),
                        "reviews-all", e -> showAll(productId, title)));
            }
        } else if (page > 0 || (page + 1) * PAGE < p.count()) {
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

    private static Container summary(ReviewPageDto p) {
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
        return summary;
    }

    private static Container bar(double share) {
        Container track = new Container(new BorderLayout());
        track.setUIID("ShopReviewTrack");
        Label fill = new Label("", "ShopReviewFill");
        fill.setPreferredW(Math.max(1, (int) (CN.convertToPixels(30f) * share)));
        track.add(BorderLayout.WEST, fill);
        return track;
    }

    private static Container review(ReviewDto r) {
        Container c = new Container(BoxLayout.y());
        c.setUIID("ShopReview");
        c.setName("review");
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.WEST, Stars.show(r.rating() * 10, 2.4f));
        if (r.createdAt() > 0) {
            top.add(BorderLayout.EAST, Ui.plain(Ui.day(r.createdAt()), "SvRowDetail"));
        }
        c.add(top);
        if (r.title() != null && r.title().length() > 0) {
            c.add(Ui.plain(r.title(), "SvRowTitle"));
        }
        if (r.body() != null && r.body().length() > 0) {
            c.add(Ui.text(r.body(), "SvText"));
        }
        Label who = Ui.plain(firstName(r.authorName()), "ShopReviewAuthor");
        if (r.verified()) {
            who.setText(who.getText() + Ui.DOT + Lang.tr("Verified buyer"));
            FontImage.setMaterialIcon(who, FontImage.MATERIAL_VERIFIED, 2.4f);
        }
        c.add(who);
        return c;
    }

    private void write() {
        final int[] rating = {5};
        Container body = new Container(BoxLayout.y());
        body.add(Stars.pick(rating, null));
        TextField heading = Ui.field(body, "Title", "review-title", "", TextArea.ANY);
        body.add(Ui.label("Review", "SvLabel"));
        TextArea text = new TextArea("", 4, 30);
        text.setUIID("SvField");
        text.setName("review-body");
        body.add(text);
        body.add(Ui.text(Lang.tr("Reviews appear once the store has read them."), "SvMuted"));
        Command ok = new com.codename1.ui.Command(Lang.tr("Send"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Write a review"), body, cancel, ok) != ok) {
            return;
        }
        ReviewDto r = new ReviewDto(null, productId, null, rating[0], heading.getText().trim(),
                text.getText().trim(), null, false, 0);
        Api.reviews().submit(productId, r, Net.to(saved -> Ui.say("Thanks, your review is "
                + "waiting for the store"), Ui::fail));
    }
}
