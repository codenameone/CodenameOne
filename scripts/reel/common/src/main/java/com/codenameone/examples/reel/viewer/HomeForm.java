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
import com.codenameone.examples.reel.ui.Backdrop;
import com.codenameone.examples.reel.ui.Layouts;
import com.codenameone.examples.reel.ui.Nav;
import com.codenameone.examples.reel.ui.SettingsForm;
import com.codenameone.examples.reel.ui.Shell;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;

/// The viewer's home: a featured title, then the rows the server composed
/// for the profile watching (continue watching, the profile's list, what is
/// popular, each genre, recommendations). Search, the library, learning and
/// plans are in the menu.
public final class HomeForm implements Nav.Home {
    private final Shell shell = new Shell("Home", Nav.VIEWER);
    private final Container page = Ui.bleed("home");

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

    /// What a row is, for the rows whose name alone does not say.
    static String caption(String row) {
        if ("courses".equals(row)) {
            return Lang.tr("Short video lessons, taken in order, with quizzes inside the video.");
        }
        if ("for-you".equals(row)) {
            return Lang.tr("Picked from what this profile watches and rates.");
        }
        if ("trending".equals(row)) {
            return Lang.tr("What most people watched this week.");
        }
        return null;
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
                page.add(Cards.row(form, row.id(), row.title(), caption(row.id()), row.titles()));
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
        // Taller than the picture on a phone, where the title and buttons sit
        // on its lower part; on a desktop it stops at two thirds of the window.
        Backdrop hero = new Backdrop(t.backdropUrl() != null && t.backdropUrl().length() > 0
                ? t.backdropUrl() : t.posterUrl(), Layouts.wide() ? 1f : 1.45f, Layouts.wide() ? 0.58f : 0.68f);
        Container info = new Container(BoxLayout.y());
        info.setUIID("RlHeroInfo");
        int side = com.codename1.ui.CN.convertToPixels(Layouts.gutter());
        info.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        info.getAllStyles().setPaddingLeft(side);
        info.getAllStyles().setPaddingRight(side);
        Label kicker = Ui.label("FEATURED", "RlHeroKicker");
        info.add(kicker);
        com.codename1.components.SpanLabel name = new com.codename1.components.SpanLabel(t.name());
        name.setTextUIID("RlHeroTitle");
        name.setUIID("Container");
        info.add(name);
        info.add(Ui.label(t.year() + Ui.DOT + t.rating(), "RlHeroMeta"));
        Container buttons = new Container(BoxLayout.x());
        com.codename1.ui.Button play = Ui.primary("Play", "heroPlay", e -> TitleForm.play(form, t.id(), null));
        play.setUIID("RlHeroPlay");
        com.codename1.ui.FontImage.setMaterialIcon(play, com.codename1.ui.FontImage.MATERIAL_PLAY_ARROW);
        buttons.add(play);
        com.codename1.ui.Button more = Ui.secondary("More info", "heroInfo", e -> TitleForm.show(form, t.id()));
        more.setUIID("RlHeroMore");
        com.codename1.ui.FontImage.setMaterialIcon(more, com.codename1.ui.FontImage.MATERIAL_INFO_OUTLINE);
        buttons.add(more);
        info.add(buttons);
        if (Layouts.wide()) {
            // Beside the picture rather than across it: a line of buttons as
            // wide as a desktop window reads as a toolbar, not a title.
            Container left = new Container(new BorderLayout());
            left.add(BorderLayout.SOUTH, info);
            info.setPreferredW(com.codename1.ui.CN.getDisplayWidth() * 45 / 100);
            hero.add(BorderLayout.WEST, left);
        } else {
            hero.add(BorderLayout.SOUTH, info);
        }
        return hero;
    }
}
