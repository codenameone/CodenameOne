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
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.HomeDto;
import com.codenameone.examples.reel.api.HomeRowDto;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Nav;
import com.codenameone.examples.reel.ui.SettingsForm;
import com.codenameone.examples.reel.ui.Shell;
import com.codenameone.examples.reel.ui.Ui;

/// The viewer's home: a featured title, then the rows the server composed
/// for the profile watching (continue watching, the profile's list, what is
/// popular, each genre, recommendations). Search, the library, learning and
/// plans are in the menu.
public final class HomeForm implements Nav.Home {
    private final Shell shell = new Shell("Home", Nav.VIEWER);
    private final Container page = Ui.list("home");

    @Override
    public void start() {
        final Form form = shell.form();
        shell.action(Ui.flat(FontImage.MATERIAL_SEARCH, "search", e -> SearchForm.show(form)));
        shell.menu().item(FontImage.MATERIAL_HOME, "Home", "home", this::load)
                .item(FontImage.MATERIAL_SEARCH, "Search", "search", () -> SearchForm.show(form))
                .item(FontImage.MATERIAL_VIDEO_LIBRARY, "My library", "library", () -> LibraryForm.show(form));
        if (Account.enabled(Features.INTERACTIVE) || Account.enabled(Features.CERTIFICATES)) {
            shell.menu().item(FontImage.MATERIAL_SCHOOL, "Learning", "learning", () -> LibraryForm.learning(form));
        }
        shell.menu().item(FontImage.MATERIAL_WORKSPACE_PREMIUM, "Plans", "plans", () -> PlansForm.show(form))
                .item(FontImage.MATERIAL_SETTINGS, "Settings", "settings", () -> SettingsForm.show(form));
        shell.show(Account.config() != null && Account.config().branding() != null
                ? Account.config().branding().serviceName() : "Reel", page);
        Ui.pull(page, this::load);
        shell.done();
        load();
    }

    @Override
    public Form form() {
        return shell.form();
    }

    private void load() {
        final Form form = shell.form();
        Api.catalog().home(Account.profileId(), Net.to(home -> {
            if (!Nav.isHome(form)) {
                return;
            }
            fill(form, home);
        }, Ui.failed(page, this::load)));
    }

    private void fill(Form form, HomeDto home) {
        page.removeAll();
        if (home.hero() != null) {
            page.add(hero(form, home.hero()));
        }
        boolean any = false;
        for (int iter = 0; home.rows() != null && iter < home.rows().size(); iter++) {
            HomeRowDto row = home.rows().get(iter);
            if (row.titles() != null && !row.titles().isEmpty()) {
                page.add(Cards.row(form, row.id(), row.title(), row.titles()));
                any = true;
            }
        }
        if (!any && home.hero() == null) {
            page.add(Ui.empty(FontImage.MATERIAL_MOVIE, "Nothing to watch yet",
                    "Titles appear here once a creator publishes them."));
        }
        page.revalidate();
    }

    private static Container hero(final Form form, final TitleCardDto t) {
        Container hero = new Container(new BorderLayout());
        hero.setUIID("RlHero");
        Label art = new Label("", "RlHeroArt");
        Art.load(art, t.backdropUrl() != null && t.backdropUrl().length() > 0 ? t.backdropUrl() : t.posterUrl(),
                90f, 50f);
        hero.add(BorderLayout.CENTER, art);
        Container info = new Container(BoxLayout.y());
        info.setUIID("RlHeroInfo");
        info.add(Ui.label(t.name(), "RlHeroTitle"));
        info.add(Ui.label(t.year() + Ui.DOT + t.rating(), "RlHeroMeta"));
        Container buttons = new Container(BoxLayout.x());
        buttons.add(Ui.primary("Play", "heroPlay", e -> TitleForm.play(form, t.id(), null)));
        com.codename1.ui.Button more = Ui.secondary("More info", "heroInfo", e -> TitleForm.show(form, t.id()));
        more.setUIID("RlHeroMore");
        buttons.add(more);
        info.add(buttons);
        hero.add(BorderLayout.SOUTH, info);
        return hero;
    }
}
