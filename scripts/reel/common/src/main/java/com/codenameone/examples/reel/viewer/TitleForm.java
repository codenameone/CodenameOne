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
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Slider;
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
import com.codenameone.examples.reel.api.Advisories;
import com.codenameone.examples.reel.api.CourseProgressDto;
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
        over.add(facts(t));
        if (t.advisories() != null && !t.advisories().isEmpty()) {
            StringBuilder why = new StringBuilder();
            for (int iter = 0; iter < t.advisories().size(); iter++) {
                if (iter > 0) {
                    why.append(Ui.DOT);
                }
                why.append(Lang.tr(Advisories.label(t.advisories().get(iter))));
            }
            Label advisories = Ui.label(why.toString(), "RlAdvisories");
            advisories.setName("advisories");
            over.add(advisories);
        }
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
        if (t.shareUrl() != null) {
            actions.add(Ui.action(FontImage.MATERIAL_SHARE, "Share", "share", e -> share(form, t)));
        }
        if (Account.enabled(Features.REPORTS)) {
            actions.add(Ui.action(FontImage.MATERIAL_FLAG, "Report", "report", e -> report(form, t)));
        }
        info.add(actions);
        info.add(Ui.text(t.synopsis(), "SvText"));
        if (t.creatorName() != null && t.creatorName().length() > 0) {
            info.add(Ui.label(Lang.tr("By {0}", t.creatorName()), "SvMuted"));
        }
        if ("COURSE".equals(t.kind())) {
            info.add(course(t));
        } else if (t.certificate()) {
            info.add(Ui.callout(FontImage.MATERIAL_WORKSPACE_PREMIUM, "Certificate course",
                    "Watch every lesson and pass the quizzes to earn a certificate anyone can verify."));
        }
        languages(info, t);

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

    /// The line under the name: year, the age rating in its box, how long,
    /// the best quality and CC when there are subtitles, and the stars.
    private static Container facts(TitleDto t) {
        Container row = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        row.setUIID("RlHeroFacts");
        if (t.year() > 0) {
            row.add(Ui.label(String.valueOf(t.year()), "RlHeroMeta"));
        }
        if (t.rating() != null && t.rating().length() > 0) {
            Label rating = Ui.badge(t.rating(), "RlRatingBox");
            rating.setName("ratingBox");
            row.add(rating);
        }
        if (t.durationSeconds() > 0) {
            row.add(Ui.label(Ui.minutes(t.durationSeconds()), "RlHeroMeta"));
        }
        if (t.maxHeight() >= 720) {
            row.add(Ui.badge(t.maxHeight() >= 2160 ? "4K" : "HD", "RlQualityBox"));
        }
        if (t.subtitleLanguages() != null && !t.subtitleLanguages().isEmpty()) {
            row.add(Ui.badge("CC", "RlQualityBox"));
        }
        if (t.ratingCount() > 0) {
            Label stars = Ui.label(Ui.tenths(t.averageRating()) + " / 5", "RlHeroMeta");
            Ui.icon(stars, FontImage.MATERIAL_STAR, 2.6f);
            row.add(stars);
        }
        return row;
    }

    /// Audio and subtitle languages, as a viewer reads them on a box.
    private static void languages(Container info, TitleDto t) {
        String audio = join(t.audioLanguages());
        String subs = join(t.subtitleLanguages());
        if (audio.length() == 0 && subs.length() == 0) {
            return;
        }
        Container facts = new Container(BoxLayout.y());
        facts.setName("languages");
        if (audio.length() > 0) {
            facts.add(Ui.fact(Lang.tr("Audio"), audio));
        }
        facts.add(Ui.fact(Lang.tr("Subtitles"), subs.length() == 0 ? Lang.tr("None") : subs));
        info.add(facts);
    }

    private static String join(java.util.List<String> all) {
        StringBuilder out = new StringBuilder();
        if (all != null) {
            for (int iter = 0; iter < all.size(); iter++) {
                if (iter > 0) {
                    out.append(", ");
                }
                out.append(all.get(iter));
            }
        }
        return out.toString();
    }

    /// What a course is, in three steps, and how far this profile is.
    private static Container course(final TitleDto t) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("RlCourse");
        box.setName("courseExplainer");
        box.add(Ui.label("How this course works", "RlCourseTitle"));
        int lessons = t.episodes() == null ? 0 : t.episodes().size();
        box.add(Ui.row(FontImage.MATERIAL_PLAY_LESSON, Lang.tr("{0} short lessons", String.valueOf(lessons)),
                Lang.tr("Watch them in order; each picks up where the last one ended"), null));
        box.add(Ui.row(FontImage.MATERIAL_QUIZ, Lang.tr("Quizzes inside the video"),
                Lang.tr("The lesson pauses on a question and carries on when you answer"), null));
        if (t.certificate()) {
            box.add(Ui.row(FontImage.MATERIAL_WORKSPACE_PREMIUM, Lang.tr("A certificate at the end"),
                    Lang.tr("Finish every lesson and pass the quizzes; anyone can verify it by its code"), null));
        }
        final Label progress = Ui.label("", "RlCourseProgress");
        progress.setName("courseProgress");
        final Slider bar = new Slider();
        bar.setUIID("RlCourseBar");
        bar.setMinValue(0);
        bar.setMaxValue(Math.max(1, lessons));
        bar.setProgress(0);
        box.add(progress);
        box.add(bar);
        Api.learning().progress(Account.profileId(), Net.to(all -> {
            for (int iter = 0; iter < all.size(); iter++) {
                CourseProgressDto c = all.get(iter);
                if (t.id().equals(c.titleId())) {
                    bar.setMaxValue(Math.max(1, c.lessonsTotal()));
                    bar.setProgress(c.lessonsCompleted());
                    String text = Lang.tr("{0} of {1} lessons done", String.valueOf(c.lessonsCompleted()),
                            String.valueOf(c.lessonsTotal()));
                    if (c.quizScorePercent() > 0) {
                        text += Ui.DOT + Lang.tr("quizzes {0}%", String.valueOf(c.quizScorePercent()));
                    }
                    if (c.certificateEarned()) {
                        text += Ui.DOT + Lang.tr("certificate earned");
                    }
                    progress.setText(text);
                    box.revalidateLater();
                    return;
                }
            }
            progress.setText(Lang.tr("Not started yet"));
            box.revalidateLater();
        }, (code, message) -> { }));
        return box;
    }

    /// The share sheet: how the link will look where it is pasted, the link
    /// itself, and the ways to send it.
    private static void share(final Form previous, final TitleDto t) {
        final Form form = Ui.form("Share", "Share");
        Ui.back(form, previous);
        Container page = Ui.page();
        final String link = com.codenameone.examples.reel.ui.Art.url(t.shareUrl());
        Container preview = new Container(new BorderLayout());
        preview.setUIID("RlSharePreview");
        Label poster = new Label("", "RlSharePoster");
        com.codenameone.examples.reel.ui.Art.load(poster, t.posterUrl(), 16f, 24f);
        preview.add(BorderLayout.WEST, poster);
        Container words = new Container(BoxLayout.y());
        words.add(Ui.label(t.name(), "RlShareTitle"));
        words.add(Ui.label(t.year() + Ui.DOT + t.rating(), "SvMuted"));
        words.add(Ui.text(t.synopsis(), "RlShareText"));
        preview.add(BorderLayout.CENTER, words);
        page.add(Ui.label("What people will see", "SvLabel"));
        page.add(preview);
        page.add(Ui.label("Link", "SvLabel"));
        Label url = Ui.label(link, "RlShareLink");
        url.setName("shareLink");
        page.add(url);
        page.add(Ui.primary("Share", "shareSend", e -> Display.getInstance().share(
                Lang.tr("Watch {0} with me", t.name()) + " " + link, null, "text/plain")));
        page.add(Ui.secondary("Copy link", "shareCopy", e -> {
            Display.getInstance().copyToClipboard(link);
            Ui.say("Link copied");
        }));
        page.add(Ui.text("Anyone with the link sees the title's page; watching it still needs an account.",
                "SvMuted"));
        form.add(BorderLayout.CENTER, page);
        form.show();
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
