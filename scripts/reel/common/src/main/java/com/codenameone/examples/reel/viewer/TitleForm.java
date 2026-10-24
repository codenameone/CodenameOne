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

import com.codename1.media.DrmScheme;
import com.codename1.media.MediaManager;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.reel.Prefs;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.EpisodeDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.RatingDto;
import com.codenameone.examples.reel.api.RentDto;
import com.codenameone.examples.reel.api.ReportDto;
import com.codenameone.examples.reel.api.TitleDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Device;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.player.PlayerForm;
import com.codenameone.examples.reel.ui.Backdrop;
import com.codenameone.examples.reel.ui.Layouts;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;

/// One title: what it is, whether this account may watch it and how (free,
/// the plan, a rental), and everything a viewer does with it -- play or
/// resume, episodes, the list, a rating, a download, a report.
///
/// What is shown as allowed is what the server said (`entitled`,
/// `downloadable`); pressing Play asks again, and the server's answer is the
/// one that counts.
public final class TitleForm {
    private TitleForm() {
    }

    public static void show(final Form previous, final String id) {
        final Form form = Ui.form("", "Title");
        Ui.back(form, previous);
        final Container page = Ui.bleed("titlePage");
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, page, id);
    }

    /// Starts the title (or an episode) without the details screen, as the
    /// home screen's Play does.
    public static void play(Form from, String titleId, String episodeId) {
        PlayerForm.start(from, titleId, episodeId);
    }

    private static void load(final Form form, final Container page, final String id) {
        Api.catalog().title(id, Account.profileId(), Net.to(t -> fill(form, page, t),
                Ui.failed(page, () -> load(form, page, id))));
    }

    private static void fill(final Form form, final Container page, final TitleDto t) {
        page.removeAll();
        form.setTitle(t.name());
        Backdrop art = new Backdrop(t.backdropUrl() != null && t.backdropUrl().length() > 0
                ? t.backdropUrl() : t.posterUrl(), 1f, 0.6f);
        Container over = new Container(BoxLayout.y());
        over.setUIID("RlHeroInfo");
        int side = Math.max(com.codename1.ui.CN.convertToPixels(Layouts.gutter()),
                (com.codename1.ui.CN.getDisplayWidth() - Layouts.column()) / 2);
        over.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        over.getAllStyles().setPaddingLeft(side);
        over.getAllStyles().setPaddingRight(side);
        com.codename1.components.SpanLabel name = new com.codename1.components.SpanLabel(t.name());
        name.setUIID("Container");
        name.setTextUIID("RlHeroTitle");
        over.add(name);
        String meta = t.year() + Ui.DOT + t.rating() + Ui.DOT + Ui.minutes(t.durationSeconds());
        if (t.ratingCount() > 0) {
            meta += Ui.DOT + Ui.tenths(t.averageRating()) + " / 5";
        }
        over.add(Ui.label(meta, "RlHeroMeta"));
        art.add(BorderLayout.SOUTH, over);
        page.add(art);
        Container info = Ui.page();
        // The page scrolls; the column in it does not.
        info.setScrollableY(false);
        if (t.genres() != null && !t.genres().isEmpty()) {
            Container chips = new Container(new FlowLayout());
            for (String g : t.genres()) {
                chips.add(Ui.chip(g, FontImage.MATERIAL_LOCAL_OFFER));
            }
            info.add(chips);
        }

        if (t.entitled()) {
            String label = t.resumeSeconds() > 0 ? Lang.tr("Resume from {0}", clock(t.resumeSeconds())) : "Play";
            info.add(Ui.primary(label, "play", e -> PlayerForm.start(form, t.id(), null)));
        } else if ("RENTAL".equals(t.access()) && Account.enabled(Features.RENTALS)) {
            info.add(Ui.primary(Lang.tr("Rent for {0}", Ui.money(t.rentalPriceCents(), t.currency())), "rent",
                    e -> rent(form, page, t)));
        } else {
            info.add(Ui.primary("Choose a plan to watch", "choosePlan", e -> PlansForm.show(form)));
        }

        Container actions = new Container(new FlowLayout());
        actions.add(Ui.action(t.inMyList() ? FontImage.MATERIAL_CHECK : FontImage.MATERIAL_ADD, "My list",
                "myList", e -> toggleList(form, page, t)));
        // Every download is ClearKey CENC; a device without ClearKey (iOS)
        // could fetch the file but never play it.
        if (t.downloadable() && Account.enabled(Features.DOWNLOADS)
                && MediaManager.isDrmSupported(DrmScheme.CLEARKEY)) {
            actions.add(Ui.action(FontImage.MATERIAL_DOWNLOAD, "Download", "download", e -> download(t)));
        }
        if (Account.enabled(Features.REPORTS)) {
            actions.add(Ui.action(FontImage.MATERIAL_FLAG, "Report", "report", e -> report(form, t)));
        }
        info.add(actions);
        info.add(Ui.text(t.synopsis(), "SvText"));
        if (t.creatorName() != null && t.creatorName().length() > 0) {
            info.add(Ui.label(Lang.tr("By {0}", t.creatorName()), "SvMuted"));
        }
        if (t.certificate()) {
            info.add(Ui.callout(FontImage.MATERIAL_WORKSPACE_PREMIUM, "Certificate course",
                    "Watch every lesson and pass the quizzes to earn a certificate anyone can verify."));
        }

        info.add(Ui.label("Your rating", "SvLabel"));
        info.add(stars(form, page, t));

        if (t.episodes() != null && t.episodes().size() > 1) {
            Ui.section(info, "SERIES".equals(t.kind()) ? "Episodes" : "Lessons");
            for (int iter = 0; iter < t.episodes().size(); iter++) {
                info.add(episode(form, t, t.episodes().get(iter)));
            }
        }
        if (t.chapters() != null && !t.chapters().isEmpty()) {
            Ui.section(info, "Chapters");
            for (int iter = 0; iter < t.chapters().size(); iter++) {
                info.add(Ui.row(FontImage.MATERIAL_BOOKMARK_BORDER, t.chapters().get(iter).title(),
                        clock(t.chapters().get(iter).startSeconds()), null));
            }
        }
        page.add(info);
        if (t.similar() != null && !t.similar().isEmpty()) {
            page.add(Cards.row(form, "similar", "More like this", t.similar()));
        }
        page.revalidate();
    }

    private static Container episode(final Form form, final TitleDto t, final EpisodeDto ep) {
        String detail = Ui.minutes(ep.durationSeconds()) + (ep.completed() ? Ui.DOT + Lang.tr("Watched")
                : ep.progressPercent() > 0 ? Ui.DOT + ep.progressPercent() + "%" : "");
        Container row = Ui.row(ep.locked() ? FontImage.MATERIAL_LOCK : FontImage.MATERIAL_PLAY_CIRCLE_OUTLINE,
                ep.number() + ". " + ep.name(), detail, null);
        if (ep.locked() || !t.entitled()) {
            return row;
        }
        return Ui.tap(row, "episode-" + ep.number(), e -> PlayerForm.start(form, t.id(), ep.id()));
    }

    private static Container stars(final Form form, final Container page, final TitleDto t) {
        Container row = new Container(BoxLayout.x());
        for (int iter = 1; iter <= 5; iter++) {
            final int n = iter;
            Button star = Ui.flat(n <= t.myRating() ? FontImage.MATERIAL_STAR : FontImage.MATERIAL_STAR_BORDER,
                    "star-" + n, e -> Api.library().rate(t.id(), Account.profileId(), new RatingDto(n),
                            Net.to(ok -> {
                                Telemetry.rated(n);
                                load(form, page, t.id());
                            }, Ui::fail)));
            row.add(star);
        }
        return row;
    }

    private static void toggleList(final Form form, final Container page, final TitleDto t) {
        if (t.inMyList()) {
            Api.library().removeFromList(t.id(), Account.profileId(), Net.to(ok -> load(form, page, t.id()), Ui::fail));
        } else {
            Api.library().addToList(t.id(), Account.profileId(), Net.to(ok -> load(form, page, t.id()), Ui::fail));
        }
    }

    private static void rent(final Form form, final Container page, final TitleDto t) {
        String card = Prefs.paymentMethod();
        Api.billing().rent(new RentDto(t.id(), card.length() == 0 ? null : card), Net.to(rental -> {
            Telemetry.rented();
            Ui.say(Lang.tr("Rented until {0}", Ui.when(rental.expiresAt())));
            load(form, page, t.id());
        }, (status, message) -> {
            Ui.fail(status, message);
            if (status == 400) {
                com.codenameone.examples.reel.pay.WalletForm.show(form);
            }
        }));
    }

    private static void download(final TitleDto t) {
        Api.library().download(t.id(), Device.id(), null, Account.profileId(), Net.to(d -> {
            Ui.say(Lang.tr("Licensed for offline play until {0}", Ui.when(d.licenseExpiresAt())));
            Downloads.fetch(d);
        }, Ui::fail));
    }

    private static void report(final Form previous, final TitleDto t) {
        final Form form = Ui.form("Report a problem", "Report");
        Ui.back(form, previous);
        Container page = Ui.page();
        final String[] reason = {"inappropriate"};
        page.add(Ui.label("What is wrong?", "SvLabel"));
        page.add(Ui.choice("reason", new String[] {"Inappropriate", "Copyright", "Misleading", "Broken"},
                new String[] {"inappropriate", "copyright", "misleading", "broken"}, reason[0],
                value -> reason[0] = value));
        final TextField detail = Ui.field(page, "Details", "detail", "Optional", TextArea.ANY);
        page.add(Ui.primary("Send report", "submit", e -> Api.catalog().report(t.id(),
                new ReportDto(t.id(), reason[0], detail.getText()), Net.to(ok -> {
                    Ui.say("Thanks, a moderator will look at it");
                    previous.showBack();
                }, Ui::fail))));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// `h:mm:ss` or `m:ss`.
    public static String clock(int seconds) {
        int h = seconds / 3600;
        int m = (seconds / 60) % 60;
        int s = seconds % 60;
        String ms = (h > 0 && m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
        return h > 0 ? h + ":" + ms : ms;
    }
}
