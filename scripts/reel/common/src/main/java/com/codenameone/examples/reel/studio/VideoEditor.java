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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.api.ChapterDto;
import com.codenameone.examples.reel.api.ChaptersDto;
import com.codenameone.examples.reel.api.InteractionEditDto;
import com.codenameone.examples.reel.api.InteractionsDto;
import com.codenameone.examples.reel.api.StudioTitleDto;
import com.codenameone.examples.reel.api.StudioVideoDto;
import com.codenameone.examples.reel.api.SubtitleDto;
import com.codenameone.examples.reel.api.SubtitleUploadDto;
import com.codenameone.examples.reel.api.StudioVideoEditDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// One video of a title: its details, its file, its chapters and the quizzes,
/// polls and branches that pause it.
final class VideoEditor {
    static final String[] KINDS = {"QUIZ", "POLL", "BRANCH"};

    private VideoEditor() {
    }

    static void show(final Form previous, final StudioTitleDto t, final StudioVideoDto v, final Runnable changed) {
        final Form form = Ui.form(v.name(), "StudioVideo");
        Ui.back(form, () -> {
            changed.run();
            previous.showBack();
        });
        Container page = Ui.page();

        Ui.section(page, "File");
        final Label status = TitleEditor.progress();
        status.setText(Lang.tr(v.status()) + (v.error() == null || v.error().length() == 0 ? "" : Ui.DOT + v.error()));
        page.add(status);
        if ("UPLOADING".equals(v.status()) || "FAILED".equals(v.status()) || "BLOCKED".equals(v.status())
                || "READY".equals(v.status())) {
            page.add(Ui.secondary("READY".equals(v.status()) ? "Replace file" : "Upload file", "upload",
                    e -> TitleEditor.pickAndUpload(form, v.id(), status, changed)));
        }

        Ui.section(page, "Details");
        final String[] role = {v.role()};
        page.add(Ui.choice("role", TitleEditor.labels(TitleEditor.ROLES), TitleEditor.ROLES, role[0],
                x -> role[0] = x, 2));
        final TextField name = Ui.field(page, "Name", "videoName", "");
        name.setText(v.name());
        final TextField season = Ui.field(page, "Season", "season", "", TextArea.NUMERIC);
        season.setText(String.valueOf(v.season()));
        final TextField number = Ui.field(page, "Number", "number", "", TextArea.NUMERIC);
        number.setText(String.valueOf(v.number()));
        final TextField synopsis = Ui.field(page, "Synopsis", "videoSynopsis", "");
        synopsis.setText(v.synopsis());
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        page.add(error);
        page.add(Ui.primary("Save", "saveVideo", e -> Api.studio().updateVideo(v.id(), new StudioVideoEditDto(
                role[0], TitleEditor.number(season), TitleEditor.number(number), name.getText().trim(),
                synopsis.getText().trim()), Net.to(x -> Ui.say("Saved"), (code, message) -> error.setText(message)))));

        Ui.section(page, "Chapters");
        page.add(Ui.text("One a line: the time it starts, then its title. 0:00 Intro", "SvMuted"));
        final TextArea chapters = new TextArea(chapters(v.chapters()), 5, 24);
        chapters.setUIID("SvField");
        chapters.setName("chapters");
        page.add(chapters);
        page.add(Ui.secondary("Save chapters", "saveChapters", e -> {
            List<ChapterDto> list = parse(chapters.getText());
            if (list == null) {
                error.setText(Lang.tr("Each chapter line starts with a time such as 1:30"));
                return;
            }
            Api.studio().setChapters(v.id(), new ChaptersDto(list), Net.to(x -> Ui.say("Chapters saved"),
                    (code, message) -> error.setText(message)));
        }));

        subtitles(page, form, v, error);

        Ui.section(page, "Interactions");
        final List<InteractionEditDto> items = v.interactions() == null ? new ArrayList<InteractionEditDto>()
                : new ArrayList<InteractionEditDto>(v.interactions());
        final Container list = new Container(com.codename1.ui.layouts.BoxLayout.y());
        page.add(list);
        final Runnable save = () -> Api.studio().setInteractions(v.id(), new InteractionsDto(items),
                Net.to(x -> Ui.say("Interactions saved"), (code, message) -> error.setText(message)));
        final Runnable[] draw = new Runnable[1];
        draw[0] = () -> {
            list.removeAll();
            for (int iter = 0; iter < items.size(); iter++) {
                final InteractionEditDto it = items.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_QUIZ, Lang.tr(it.kind()) + " " + clock(it.atSeconds()), it.prompt(),
                        Ui.flat(FontImage.MATERIAL_DELETE, "removeInteraction-" + iter, e -> {
                            items.remove(it);
                            draw[0].run();
                            save.run();
                        })));
            }
            list.revalidate();
        };
        draw[0].run();
        page.add(Ui.secondary("Add interaction", "addInteraction", e -> interaction(form, t, items, () -> {
            draw[0].run();
            save.run();
        })));

        page.add(Ui.danger("Delete video", "deleteVideo", e -> Api.studio().deleteVideo(v.id(), Net.to(x -> {
            changed.run();
            previous.showBack();
        }, Ui::fail))));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    /// The video's subtitle tracks: one per language, WebVTT pasted in.
    /// The player draws them itself, so they work with every DRM scheme.
    private static void subtitles(Container page, final Form form, final StudioVideoDto v, final SpanLabel error) {
        Ui.section(page, "Subtitles");
        final Container list = new Container(com.codename1.ui.layouts.BoxLayout.y());
        list.setName("subtitleTracks");
        page.add(list);
        final List<SubtitleDto> tracks = v.subtitles() == null ? new ArrayList<SubtitleDto>()
                : new ArrayList<SubtitleDto>(v.subtitles());
        final Runnable[] draw = new Runnable[1];
        draw[0] = () -> {
            list.removeAll();
            if (tracks.isEmpty()) {
                list.add(Ui.text("No subtitles yet. Viewers who need captions cannot follow this video.",
                        "SvMuted"));
            }
            for (int iter = 0; iter < tracks.size(); iter++) {
                final SubtitleDto track = tracks.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_SUBTITLES, track.label(), track.language(),
                        Ui.flat(FontImage.MATERIAL_DELETE, "removeSubtitle-" + track.language(),
                                e -> Api.studio().deleteSubtitle(v.id(), track.language(), Net.to(updated -> {
                                    tracks.clear();
                                    tracks.addAll(updated.subtitles());
                                    draw[0].run();
                                }, (code, message) -> error.setText(message))))));
            }
            list.revalidate();
        };
        draw[0].run();
        final TextField language = Ui.field(page, "Language", "subtitleLanguage", "en, es, pt-BR");
        final TextField label = Ui.field(page, "Name viewers see", "subtitleLabel", "English");
        page.add(Ui.label("WebVTT", "SvLabel"));
        final TextArea vtt = new TextArea("", 6, 24);
        vtt.setUIID("SvField");
        vtt.setName("subtitleVtt");
        vtt.setHint("Paste a .vtt file: WEBVTT, then its cues");
        page.add(vtt);
        page.add(Ui.secondary("Add subtitles", "addSubtitle", e -> Api.studio().setSubtitle(v.id(),
                language.getText().trim(), new SubtitleUploadDto(label.getText().trim(), vtt.getText()),
                Net.to(updated -> {
                    tracks.clear();
                    tracks.addAll(updated.subtitles());
                    language.setText("");
                    label.setText("");
                    vtt.setText("");
                    error.setText("");
                    draw[0].run();
                    form.revalidate();
                }, (code, message) -> error.setText(message)))));
    }

    private static void interaction(final Form previous, StudioTitleDto t, final List<InteractionEditDto> items,
            final Runnable added) {
        final Form form = Ui.form("Interaction", "Interaction");
        Ui.back(form, previous);
        Container page = Ui.page();
        final String[] kind = {"QUIZ"};
        page.add(Ui.choice("kind", TitleEditor.labels(KINDS), KINDS, kind[0], x -> kind[0] = x));
        final TextField at = Ui.field(page, "At", "at", "1:30");
        final TextField prompt = Ui.field(page, "Question", "prompt", "What did the narrator say?");
        page.add(Ui.label("Options, one a line", "SvLabel"));
        final TextArea options = new TextArea(4, 24);
        options.setUIID("SvField");
        options.setName("options");
        page.add(options);
        final TextField answers = Ui.field(page, "Correct options (quiz)", "answers", "1, 3");
        final TextField explanation = Ui.field(page, "Explanation (quiz)", "explanation", "Shown after answering");
        final TextField targets = Ui.field(page, "Branch targets (video ids, one per option)", "targets", "");
        final boolean[] required = {false};
        page.add(Ui.row((char) 0, Lang.tr("Required"), Lang.tr("The viewer cannot skip it"),
                Ui.toggle("required", false, on -> required[0] = on)));
        final SpanLabel error = Ui.text("", "SvError");
        error.setName("error");
        page.add(error);
        page.add(Ui.primary("Add", "add", e -> {
            int seconds = seconds(at.getText().trim());
            List<String> opts = lines(options.getText());
            if (seconds < 0 || prompt.getText().trim().length() == 0 || opts.size() < 2) {
                error.setText(Lang.tr("A time, a question and at least two options"));
                return;
            }
            List<Integer> right = new ArrayList<Integer>();
            List<String> picked = TitleEditor.words(answers.getText());
            for (int iter = 0; iter < picked.size(); iter++) {
                int n = TitleEditor.number(picked.get(iter));
                if (n < 1 || n > opts.size()) {
                    error.setText(Lang.tr("Correct options are numbers from 1 to {0}", String.valueOf(opts.size())));
                    return;
                }
                right.add(Integer.valueOf(n - 1));
            }
            if ("QUIZ".equals(kind[0]) && right.isEmpty()) {
                error.setText(Lang.tr("A quiz needs a correct option"));
                return;
            }
            List<String> branch = TitleEditor.words(targets.getText());
            if ("BRANCH".equals(kind[0]) && branch.size() != opts.size()) {
                error.setText(Lang.tr("A branch needs one target per option"));
                return;
            }
            items.add(new InteractionEditDto(null, kind[0], seconds, prompt.getText().trim(), opts,
                    "QUIZ".equals(kind[0]) ? right : new ArrayList<Integer>(), explanation.getText().trim(),
                    required[0], "BRANCH".equals(kind[0]) ? branch : new ArrayList<String>()));
            previous.showBack();
            added.run();
        }));
        form.add(BorderLayout.CENTER, page);
        form.show();
    }

    static String clock(int seconds) {
        int s = seconds % 60;
        return (seconds / 60) + (s < 10 ? ":0" : ":") + s;
    }

    /// "1:30" or "90" as seconds, or -1.
    static int seconds(String s) {
        int colon = s.indexOf(':');
        String m = colon < 0 ? "0" : s.substring(0, colon);
        String sec = colon < 0 ? s : s.substring(colon + 1);
        if (!digits(m) || !digits(sec) || sec.length() == 0 || m.length() > 5 || sec.length() > 5) {
            return -1;
        }
        return Integer.parseInt(m.length() == 0 ? "0" : m) * 60 + Integer.parseInt(sec);
    }

    private static boolean digits(String s) {
        for (int iter = 0; iter < s.length(); iter++) {
            if (s.charAt(iter) < '0' || s.charAt(iter) > '9') {
                return false;
            }
        }
        return true;
    }

    static List<String> lines(String s) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        for (int iter = 0; iter <= s.length(); iter++) {
            if (iter == s.length() || s.charAt(iter) == '\n') {
                String line = s.substring(start, iter).trim();
                if (line.length() > 0) {
                    out.add(line);
                }
                start = iter + 1;
            }
        }
        return out;
    }

    static String chapters(List<ChapterDto> list) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; list != null && iter < list.size(); iter++) {
            b.append(clock(list.get(iter).startSeconds())).append(' ').append(list.get(iter).title()).append('\n');
        }
        return b.toString();
    }

    /// The chapters text as chapters, or null when a line has no time.
    static List<ChapterDto> parse(String text) {
        List<ChapterDto> out = new ArrayList<ChapterDto>();
        List<String> all = lines(text);
        for (int iter = 0; iter < all.size(); iter++) {
            String line = all.get(iter);
            int space = line.indexOf(' ');
            int at = seconds(space < 0 ? line : line.substring(0, space));
            if (at < 0 || space < 0) {
                return null;
            }
            out.add(new ChapterDto(null, at, line.substring(space + 1).trim()));
        }
        return out;
    }
}
