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
package com.codenameone.examples.bazaar.admin;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.AppListingDecisionDto;
import com.codenameone.examples.bazaar.api.AppListingDto;
import com.codenameone.examples.bazaar.api.AppReviewDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.AppStoreText;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/// The app listings review queue: what developers submitted, oldest first.
/// A listing opens with what it would show and the three decisions --
/// approve, reject with a reason, and (once approved) delist with one -- and
/// its reviews, each of which can be hidden.
public final class AppListingsForm {
    private AppListingsForm() {
    }

    static void show(final Panes panes) {
        final Container list = Ui.list("app-listings");
        final Runnable[] load = new Runnable[1];
        // Waiting ones first, then the approved ones, which can be delisted.
        load[0] = () -> Api.appStore().listings("PENDING", Net.to(pending ->
                Api.appStore().listings("APPROVED", Net.to(approved -> {
                    List<AppListingDto> all = new ArrayList<AppListingDto>(pending);
                    all.addAll(approved);
                    fill(panes, list, all, load[0]);
                }, Ui.failed(list, () -> load[0].run()))), Ui.failed(list, () -> load[0].run())));
        panes.list("App listings", "App listings", Panes.listed(null, list), load[0]);
    }

    private static void fill(final Panes panes, Container list, List<AppListingDto> found,
            final Runnable reload) {
        list.removeAll();
        if (found == null || found.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_EXTENSION, "No listings to review",
                    "A listing shows up here when a developer submits it."));
        }
        for (int iter = 0; found != null && iter < found.size(); iter++) {
            final AppListingDto l = found.get(iter);
            final Container picked = Ui.row(FontImage.MATERIAL_EXTENSION, l.name(),
                    l.developer(), Ui.badge(Lang.tr(l.status()), "SvBadge"));
            list.add(Ui.tap(picked, "app-listing-" + iter, e -> {
                panes.picked(picked);
                open(panes, l, reload);
            }));
        }
        panes.refresh();
    }

    private static void open(final Panes panes, final AppListingDto l, final Runnable reload) {
        final Container page = new Container(BoxLayout.y());
        page.setName("app-listing-page");
        page.add(Ui.fact(Lang.tr("Tagline"), l.tagline()));
        page.add(Ui.text(l.description(), "SvMuted"));
        page.add(Ui.fact(Lang.tr("Category"), l.category()));
        page.add(Ui.fact(Lang.tr("Pricing"), l.pricing()));
        page.add(Ui.fact(Lang.tr("Screenshots"), String.valueOf(l.screenshots().size())));
        page.add(Ui.fact(Lang.tr("Support"), l.supportUrl()));
        page.add(Ui.fact(Lang.tr("Privacy policy"), l.privacyUrl()));
        final TextField reason = Ui.field(page, "Reason, for the developer", "app-listing-reason",
                "Required to reject or delist");
        page.add(Ui.primary(Lang.tr("Approve"), "app-listing-approve",
                e -> decide(l, "APPROVE", "", reload)));
        page.add(Ui.secondary(Lang.tr("Reject"), "app-listing-reject",
                e -> decide(l, "REJECT", reason.getText(), reload)));
        if ("APPROVED".equals(l.status())) {
            page.add(Ui.danger(Lang.tr("Delist"), "app-listing-delist",
                    e -> decide(l, "DELIST", reason.getText(), reload)));
        }
        final Container reviews = new Container(BoxLayout.y());
        Ui.section(page, "Reviews");
        page.add(reviews);
        Api.appStore().allReviews(l.id(), Net.to(found -> {
            for (int iter = 0; iter < found.size(); iter++) {
                final AppReviewDto r = found.get(iter);
                reviews.add(Ui.row((char) 0, AppStoreText.stars(r.rating() * 10) + " "
                        + r.storeName(), r.body(), r.hidden() ? Ui.badge(Lang.tr("Hidden"),
                                "SvBadge") : Ui.secondary(Lang.tr("Hide"),
                                        "app-review-hide-" + iter, e -> Api.appStore()
                                                .hideReview(r.id(), Net.to(done -> open(panes,
                                                        l, reload), Ui::fail)))));
            }
            Ui.relayout(reviews);
        }, Ui::fail));
        panes.detail(l.name(), "App listing", page);
    }

    private static void decide(AppListingDto l, String action, String reason,
            final Runnable reload) {
        Api.appStore().decide(l.id(), new AppListingDecisionDto(action, reason), Net.to(done -> {
            Ui.say(Lang.tr("Saved"));
            reload.run();
        }, Ui::fail));
    }
}
