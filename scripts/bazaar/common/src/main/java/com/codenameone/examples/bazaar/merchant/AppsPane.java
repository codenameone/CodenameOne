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
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.AppChargeDecisionDto;
import com.codenameone.examples.bazaar.api.AppChargeDto;
import com.codenameone.examples.bazaar.api.AppInstallationDto;
import com.codenameone.examples.bazaar.api.AppListingDto;
import com.codenameone.examples.bazaar.api.AppReviewDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.AppStoreText;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

import java.util.List;

/// The store's apps: charges waiting for the owner's answer, what is
/// installed (and removing it), and the app directory to find more.
/// Installing opens the app's own install link, which brings the owner to
/// the consent screen; the platform cannot start the consent itself, because
/// only the app holds its PKCE verifier.
final class AppsPane extends MerchantPane {
    private List<AppInstallationDto> installed;

    AppsPane() {
        super("Apps", "merchant-apps");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_SEARCH, "App directory", "apps-directory", () -> directory(""));
        final Container page = new Container(BoxLayout.y());
        page.setName("apps-page");
        root(page);
        Api.appStore().charges(MerchantState.storeId(), Net.to(charges -> {
            Container waiting = new Container(BoxLayout.y());
            for (int iter = 0; iter < charges.size(); iter++) {
                AppChargeDto c = charges.get(iter);
                if ("PENDING".equals(c.status())) {
                    waiting.add(charge(c));
                }
            }
            if (waiting.getComponentCount() > 0) {
                Container held = new Container(BoxLayout.y());
                Ui.section(held, "Charges to approve");
                held.add(waiting);
                page.addComponent(0, held);
                Ui.relayout(page);
            }
        }, Ui::fail));
        Api.apps().installed(MerchantState.storeId(), Net.to(found -> {
            installed = found;
            Ui.section(page, "Installed apps");
            int shown = 0;
            for (int iter = 0; iter < found.size(); iter++) {
                final AppInstallationDto i = found.get(iter);
                if (!"INSTALLED".equals(i.status())) {
                    continue;
                }
                shown++;
                page.add(Ui.row(FontImage.MATERIAL_EXTENSION, i.appName(),
                        AppsText.scopes(i.scopes()), Ui.danger(Lang.tr("Remove"),
                                "apps-remove-" + iter, e -> Api.apps().uninstall(
                                        MerchantState.storeId(), i.id(),
                                        Net.to(left -> load(), Ui::fail)))));
            }
            if (shown == 0) {
                page.add(Ui.empty(FontImage.MATERIAL_EXTENSION, "No apps installed",
                        "Find apps in the app directory."));
            }
            page.add(Ui.primary(Lang.tr("Browse apps"), "apps-browse", e -> directory("")));
            Ui.relayout(page);
        }, Ui::fail));
    }

    private Container charge(final AppChargeDto c) {
        Container row = new Container(BoxLayout.y());
        row.add(Ui.row(FontImage.MATERIAL_PAYMENTS, c.appName() + ": " + c.name(),
                AppsText.terms(c, Lang.tr("once"), Lang.tr("every 30 days"),
                        Lang.tr("days free")), null));
        row.add(Ui.primary(Lang.tr("Approve"), "apps-charge-approve-" + c.id(),
                e -> decide(c, true)));
        row.add(Ui.secondary(Lang.tr("Decline"), "apps-charge-decline-" + c.id(),
                e -> decide(c, false)));
        return row;
    }

    private void decide(AppChargeDto c, boolean approve) {
        Api.appStore().decideCharge(MerchantState.storeId(), c.id(),
                new AppChargeDecisionDto(approve), Net.to(done -> load(), Ui::fail));
    }

    private void directory(String query) {
        final Container page = new Container(BoxLayout.y());
        page.setName("apps-directory-page");
        final TextField q = Ui.field(page, "Search apps", "apps-search", "Search apps");
        q.setText(query);
        final Container results = new Container(BoxLayout.y());
        results.setName("apps-results");
        page.add(Ui.primary(Lang.tr("Search"), "apps-search-go",
                e -> search(results, q.getText())));
        page.add(results);
        push(Lang.tr("App directory"), page);
        search(results, query);
    }

    private void search(final Container results, String q) {
        Api.appStore().directory(q, "", 0, Net.to(found -> {
            results.removeAll();
            if (found.isEmpty()) {
                results.add(Ui.empty(FontImage.MATERIAL_SEARCH, "No apps match",
                        "Try other words."));
            }
            for (int iter = 0; iter < found.size(); iter++) {
                final AppListingDto l = found.get(iter);
                Container value = new Container(BoxLayout.y());
                value.add(Ui.plain(AppsText.rating(l), "SvRowValue"));
                results.add(Ui.tap(Ui.row(FontImage.MATERIAL_EXTENSION, l.name(), l.tagline(),
                        value), "apps-listing-" + iter, e -> open(l)));
            }
            Ui.relayout(results);
        }, Ui::fail));
    }

    private void open(final AppListingDto l) {
        final Container page = new Container(BoxLayout.y());
        page.setName("apps-listing-page");
        page.add(Ui.text(l.tagline(), "SvMuted"));
        page.add(Ui.text(l.description(), "SvMuted"));
        page.add(Ui.fact(Lang.tr("Category"), l.category()));
        if (l.pricing().length() > 0) {
            page.add(Ui.fact(Lang.tr("Pricing"), l.pricing()));
        }
        if (l.reviews() > 0) {
            page.add(Ui.fact(Lang.tr("Rating"), AppsText.rating(l)));
        }
        final boolean had = AppsText.hadInstalled(installed, l.appId());
        if (AppsText.isInstalled(installed, l.appId())) {
            page.add(Ui.plain(Lang.tr("Installed"), "SvMuted"));
        } else {
            final String link = AppStoreText.installUrl(l.installUrl(), MerchantState.storeId(),
                    l.clientId());
            page.add(Ui.primary(Lang.tr("Install"), "apps-install", e -> CN.execute(link)));
        }
        if (had) {
            Ui.section(page, "Your review");
            final TextField stars = Ui.field(page, "Rating (1 to 5)", "apps-review-rating", "5",
                    TextArea.NUMERIC);
            final TextArea body = new TextArea(3, 30);
            body.setName("apps-review-body");
            page.add(body);
            page.add(Ui.secondary(Lang.tr("Post review"), "apps-review-post", e -> {
                int rating = AppsText.typedRating(stars.getText());
                if (rating == 0) {
                    Ui.fail(400, "The rating is 1 to 5");
                    return;
                }
                Api.appStore().review(MerchantState.storeId(), new AppReviewDto("", l.appId(),
                        "", rating, body.getText(), false, 0), Net.to(
                                saved -> Ui.say(Lang.tr("Review posted")), Ui::fail));
            }));
        }
        final Container reviews = new Container(BoxLayout.y());
        page.add(reviews);
        Api.appStore().reviews(l.handle(), Net.to(found -> {
            for (int iter = 0; iter < found.size(); iter++) {
                AppReviewDto r = found.get(iter);
                reviews.add(Ui.row((char) 0, r.storeName(), r.body(),
                        Ui.plain(AppStoreText.stars(r.rating() * 10), "SvRowValue")));
            }
            Ui.relayout(reviews);
        }, Ui::fail));
        push(l.name(), page);
    }
}
