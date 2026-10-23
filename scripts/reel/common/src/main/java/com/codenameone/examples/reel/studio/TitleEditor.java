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
package com.codenameone.examples.reel.studio;

import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.AppealDto;
import com.codenameone.examples.reel.api.StudioTitleDto;
import com.codenameone.examples.reel.api.StudioTitleEditDto;
import com.codenameone.examples.reel.api.StudioVideoDto;
import com.codenameone.examples.reel.api.StudioVideoEditDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Chart;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// Creates and edits one title: its details, its videos and their uploads,
/// review (submit, unpublish, appeal a rejection) and how it is doing.
final class TitleEditor {
    static final String[] KINDS = {"MOVIE", "SERIES", "COURSE"};
    static final String[] ACCESS = {"SUBSCRIPTION", "RENTAL", "FREE"};
    static final String[] ROLES = {"FEATURE", "EPISODE", "LESSON", "TRAILER"};
    static final String[] RATINGS = {"G", "PG", "PG-13", "R"};

    private TitleEditor() {
    }

    static void create(final Form previous, final Runnable changed) {
        final Form form = Ui.form("New title", "NewTitle");
        Ui.back(form, previous);
        Container page = Ui.page();
        final Details d = new Details(page, null);
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        page.add(error);
        page.add(Ui.primary("Create", "create", e -> {
            StudioTitleEditDto edit = d.edit(error);
            if (edit == null) {
                return;
            }
            Api.studio().createTitle(edit, Net.to(t -> {
                changed.run();
                show(previous, t.id(), changed);
            }, (code, message) -> error.setText(message)));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    static void show(final Form previous, final String id, final Runnable changed) {
        final Form form = Ui.form("Title", "StudioTitle");
        Ui.back(form, () -> {
            changed.run();
            previous.showBack();
        });
        final Container page = Ui.list("studioTitle");
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, page, id);
    }

    static void load(final Form form, final Container page, final String id) {
        Api.studio().title(id, Net.to(t -> fill(form, page, t), Ui.failed(page, () -> load(form, page, id))));
    }

    private static void fill(final Form form, final Container page, final StudioTitleDto t) {
        page.removeAll();
        final Runnable reload = () -> load(form, page, t.id());
        form.setTitle(t.name());

        Container status = Ui.card();
        status.add(Ui.row(FontImage.MATERIAL_FACT_CHECK, Lang.tr(t.status()), review(t),
                Ui.badge(Lang.tr(t.status()), StudioForm.badge(t.status()))));
        if (t.moderationNote() != null && t.moderationNote().length() > 0) {
            status.add(Ui.callout(FontImage.MATERIAL_INFO, "Moderator's note", t.moderationNote()));
        }
        if ("DRAFT".equals(t.status()) || "REJECTED".equals(t.status()) && t.moderationNote() == null) {
            status.add(Ui.primary("Submit for review", "submit", e -> Api.studio().submit(t.id(), Net.to(x -> {
                Telemetry.submitted();
                Ui.say("Submitted for review");
                reload.run();
            }, Ui::fail))));
        }
        if ("REJECTED".equals(t.status())) {
            status.add(Ui.secondary("Appeal", "appeal", e -> appeal(form, t, reload)));
        }
        if ("PUBLISHED".equals(t.status())) {
            status.add(Ui.secondary("Unpublish", "unpublish", e -> Api.studio().unpublish(t.id(),
                    Net.to(x -> reload.run(), Ui::fail))));
        }
        page.add(status);

        Ui.section(page, "Videos");
        List<StudioVideoDto> videos = t.videos() == null ? new ArrayList<StudioVideoDto>() : t.videos();
        if (videos.isEmpty()) {
            page.add(Ui.empty(FontImage.MATERIAL_UPLOAD_FILE, "No videos",
                    "Add a video, then upload its file. It is packaged for streaming when the upload ends."));
        }
        for (int iter = 0; iter < videos.size(); iter++) {
            final StudioVideoDto v = videos.get(iter);
            String detail = Lang.tr(v.role()) + (v.season() > 0 ? Ui.DOT + "S" + v.season() + "E" + v.number()
                    : v.number() > 0 ? Ui.DOT + "#" + v.number() : "") + Ui.DOT + Lang.tr(v.status())
                    + ("PROCESSING".equals(v.status()) ? " " + v.progressPercent() + "%" : "")
                    + (v.durationSeconds() > 0 ? Ui.DOT + Ui.minutes(v.durationSeconds()) : "")
                    + (v.error() == null || v.error().length() == 0 ? "" : Ui.DOT + v.error());
            page.add(Ui.tap(Ui.row(FontImage.MATERIAL_MOVIE_FILTER, v.name(), detail,
                    Ui.badge(Lang.tr(v.status()), StudioForm.badge(v.status()))), "video-" + iter,
                    e -> VideoEditor.show(form, t, v, reload)));
        }
        page.add(Ui.secondary("Add video", "addVideo", e -> addVideo(form, t, reload)));

        Ui.section(page, "Details");
        Container details = Ui.page();
        final Details d = new Details(details, t);
        final SpanLabel error = Ui.text("", "SvError");
        details.add(error);
        details.add(Ui.primary("Save details", "saveTitle", e -> {
            StudioTitleEditDto edit = d.edit(error);
            if (edit != null) {
                Api.studio().updateTitle(t.id(), edit, Net.to(x -> {
                    Ui.say("Saved");
                    reload.run();
                }, (code, message) -> error.setText(message)));
            }
        }));
        page.add(details);

        if ("PUBLISHED".equals(t.status()) || t.publishedAt() > 0) {
            Ui.section(page, "Last 30 days");
            final Container stats = Ui.page();
            page.add(stats);
            Api.studio().analytics(t.id(), 30, Net.to(a -> {
                stats.add(Ui.fact("Views", String.valueOf(a.views())))
                        .add(Ui.fact("Viewers", String.valueOf(a.uniqueViewers())))
                        .add(Ui.fact("Minutes watched", String.valueOf(a.minutesWatched())))
                        .add(Ui.fact("Completion", a.completionPercent() + "%"))
                        .add(Ui.fact("Rating", Ui.tenths(a.averageRating())));
                if (a.revenueCents() > 0) {
                    stats.add(Ui.fact("Rental revenue", Ui.money(a.revenueCents(), null)));
                }
                if (a.quizAttempts() > 0) {
                    stats.add(Ui.fact("Quiz attempts", String.valueOf(a.quizAttempts())))
                            .add(Ui.fact("Passed", a.quizPassPercent() + "%"))
                            .add(Ui.fact("Certificates", String.valueOf(a.certificates())));
                }
                if (a.retention() != null && !a.retention().isEmpty()) {
                    double[] r = new double[a.retention().size()];
                    for (int iter = 0; iter < r.length; iter++) {
                        r[iter] = a.retention().get(iter);
                    }
                    stats.add(Ui.label("Audience retention", "SvLabel"));
                    stats.add(Chart.of("retention", r, Lang.tr("Start"), Lang.tr("End")));
                }
                for (int iter = 0; a.countries() != null && iter < a.countries().size(); iter++) {
                    stats.add(Ui.fact(a.countries().get(iter).name(), String.valueOf(a.countries().get(iter).count())));
                }
                stats.revalidate();
            }, Ui.failed(stats, reload)));
        }
        page.revalidate();
    }

    private static String review(StudioTitleDto t) {
        if ("IN_REVIEW".equals(t.status())) {
            return Lang.tr("A moderator will look at it shortly.");
        }
        if ("PUBLISHED".equals(t.status())) {
            return Lang.tr("Live since {0}. Changes go back to review.", Ui.day(t.publishedAt()));
        }
        if ("REJECTED".equals(t.status())) {
            return Lang.tr("Fix what the note asks and submit again, or appeal.");
        }
        if ("REMOVED".equals(t.status())) {
            return Lang.tr("Removed by moderation.");
        }
        return Lang.tr("Only you can see it.");
    }

    private static void appeal(Form previous, final StudioTitleDto t, final Runnable reload) {
        Ui.reason(previous, "Appeal", "Tell the moderators why the decision should change.", "Send appeal",
                why -> Api.studio().appeal(t.id(), new AppealDto(t.id(), why), Net.to(x -> {
                    Ui.say("Appeal sent");
                    previous.showBack();
                    reload.run();
                }, Ui::fail)));
    }

    private static void addVideo(final Form previous, final StudioTitleDto t, final Runnable reload) {
        final Form form = Ui.form("Add video", "AddVideo");
        Ui.back(form, previous);
        Container page = Ui.page();
        final String[] role = {"SERIES".equals(t.kind()) ? "EPISODE" : "COURSE".equals(t.kind()) ? "LESSON" : "FEATURE"};
        page.add(Ui.label("Role", "SvLabel"));
        page.add(Ui.choice("role", labels(ROLES), ROLES, role[0], v -> role[0] = v, 2));
        final TextField name = Ui.field(page, "Name", "videoName", "Episode 1");
        final TextField season = Ui.field(page, "Season", "season", "0 when it has none", TextArea.NUMERIC);
        final TextField number = Ui.field(page, "Number", "number", "Order in the title", TextArea.NUMERIC);
        final TextField synopsis = Ui.field(page, "Synopsis", "videoSynopsis", "Optional");
        final SpanLabel error = Ui.text("", "SvError");
        page.add(error);
        page.add(Ui.primary("Add", "add", e -> {
            if (name.getText().trim().length() == 0) {
                error.setText(Lang.tr("Give it a name"));
                return;
            }
            Api.studio().addVideo(t.id(), new StudioVideoEditDto(role[0], number(season), number(number),
                    name.getText().trim(), synopsis.getText().trim()), Net.to(v -> {
                        previous.showBack();
                        reload.run();
                    }, (code, message) -> error.setText(message)));
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    static String[] labels(String[] values) {
        String[] out = new String[values.length];
        for (int iter = 0; iter < values.length; iter++) {
            out[iter] = Lang.tr(values[iter]);
        }
        return out;
    }

    static int number(TextField f) {
        return number(f.getText());
    }

    static int number(String text) {
        String s = text.trim();
        if (s.length() == 0 || s.length() > 9) {
            return 0;
        }
        for (int iter = 0; iter < s.length(); iter++) {
            if (s.charAt(iter) < '0' || s.charAt(iter) > '9') {
                return 0;
            }
        }
        return Integer.parseInt(s);
    }

    static List<String> words(String s) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int iter = 0; iter <= s.length(); iter++) {
            if (iter == s.length() || s.charAt(iter) == ',') {
                String w = s.substring(start, iter).trim();
                if (w.length() > 0) {
                    out.add(w);
                }
                start = iter + 1;
            }
        }
        return out;
    }

    static String join(List<String> words) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; words != null && iter < words.size(); iter++) {
            if (iter > 0) {
                b.append(", ");
            }
            b.append(words.get(iter));
        }
        return b.toString();
    }

    /// The fields of a title, shared by create and edit.
    private static final class Details {
        final String[] kind = {"MOVIE"};
        final String[] access = {"SUBSCRIPTION"};
        final String[] rating = {"PG"};
        final TextField name;
        final TextField synopsis;
        final TextField year;
        final TextField genres;
        final TextField tags;
        final TextField price;
        final Slider pass;
        final boolean[] certificate = {false};

        Details(Container page, StudioTitleDto t) {
            if (t != null) {
                kind[0] = t.kind();
                access[0] = t.access();
                rating[0] = t.rating();
                certificate[0] = t.certificate();
            }
            page.add(Ui.label("Kind", "SvLabel"));
            page.add(Ui.choice("kind", labels(KINDS), KINDS, kind[0], v -> kind[0] = v));
            name = Ui.field(page, "Name", "name", "Big Buck Bunny");
            synopsis = Ui.field(page, "Synopsis", "synopsis", "A sentence or two");
            synopsis.setSingleLineTextArea(false);
            synopsis.setRows(3);
            year = Ui.field(page, "Year", "year", "2008", TextArea.NUMERIC);
            page.add(Ui.label("Rating", "SvLabel"));
            page.add(Ui.choice("rating", RATINGS, RATINGS, rating[0], v -> rating[0] = v));
            genres = Ui.field(page, "Genres", "genres", "Animation, Comedy");
            tags = Ui.field(page, "Tags", "tags", "Optional, comma separated");
            page.add(Ui.label("Access", "SvLabel"));
            page.add(Ui.choice("access", labels(ACCESS), ACCESS, access[0], v -> access[0] = v));
            price = Ui.field(page, "Rental price (cents)", "price", "399", TextArea.NUMERIC);
            page.add(Ui.row((char) 0, Lang.tr("Certificate on completion"), Lang.tr("For courses with quizzes"),
                    Ui.toggle("certificate", certificate[0], on -> certificate[0] = on)));
            pass = new Slider();
            pass.setName("pass");
            pass.setEditable(true);
            pass.setMinValue(0);
            pass.setMaxValue(100);
            pass.setRenderPercentageOnTop(true);
            pass.setProgress(t == null || t.passPercent() == 0 ? 70 : t.passPercent());
            page.add(Ui.label("Pass mark", "SvLabel")).add(pass);
            if (t != null) {
                name.setText(t.name());
                synopsis.setText(t.synopsis());
                year.setText(t.year() > 0 ? String.valueOf(t.year()) : "");
                genres.setText(join(t.genres()));
                tags.setText(join(t.tags()));
                price.setText(t.rentalPriceCents() > 0 ? String.valueOf(t.rentalPriceCents()) : "");
            }
        }

        StudioTitleEditDto edit(SpanLabel error) {
            if (name.getText().trim().length() == 0) {
                error.setText(Lang.tr("Give it a name"));
                return null;
            }
            if ("RENTAL".equals(access[0]) && number(price) <= 0) {
                error.setText(Lang.tr("A rental needs a price"));
                return null;
            }
            error.setText("");
            return new StudioTitleEditDto(kind[0], name.getText().trim(), synopsis.getText().trim(), number(year),
                    rating[0], words(genres.getText()), words(tags.getText()), access[0], number(price),
                    certificate[0], pass.getProgress());
        }
    }

    static void pickAndUpload(final Form form, final String videoId, final Label status, final Runnable done) {
        Display.getInstance().openGallery(e -> {
            if (e == null || !(e.getSource() instanceof String)) {
                return;
            }
            final String path = (String) e.getSource();
            status.setText(Lang.tr("Starting upload"));
            Uploader.upload(videoId, path, new Uploader.Progress() {
                @Override
                public void progress(long sent, long total) {
                    status.setText(Lang.tr("Uploaded {0}%", String.valueOf(total == 0 ? 100 : sent * 100 / total)));
                    form.revalidate();
                }

                @Override
                public void done() {
                    status.setText(Lang.tr("Uploaded. Packaging for streaming."));
                    done.run();
                }

                @Override
                public void failed(String message) {
                    status.setText(Lang.tr("Upload failed: {0}", message));
                }
            });
        }, CN.GALLERY_VIDEO);
    }

    /// A label for upload progress, named for tests.
    static Label progress() {
        Label l = Ui.label("", "SvMuted");
        l.setName("uploadStatus");
        return l;
    }

}
