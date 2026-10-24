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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codenameone.examples.reel.api.JobDto;
import com.codenameone.examples.reel.api.StudioTitleDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Nav;
import com.codenameone.examples.reel.ui.SettingsForm;
import com.codenameone.examples.reel.ui.Shell;
import com.codenameone.examples.reel.ui.Ui;

/// The creator's home: their titles with where each stands (draft, in
/// review, published, rejected with the moderator's note), a new title, and
/// the processing queue of their uploads.
public final class StudioForm implements Nav.Home {
    private final Shell shell = new Shell("Studio", Nav.STUDIO);
    private final Container page = Ui.list("studioTitles");

    @Override
    public void start() {
        final Form form = shell.form();
        shell.action(Ui.flat(FontImage.MATERIAL_ADD, "newTitle", e -> TitleEditor.create(form, this::load)));
        shell.menu().item(FontImage.MATERIAL_VIDEO_LIBRARY, "My titles", "titles", this::load)
                .item(FontImage.MATERIAL_PENDING_ACTIONS, "Processing", "jobs", this::jobs)
                .item(FontImage.MATERIAL_SETTINGS, "Settings", "settings", () -> SettingsForm.show(form));
        shell.show("Studio", page);
        Ui.pull(page, this::load);
        shell.done();
        load();
    }

    @Override
    public Form form() {
        return shell.form();
    }

    void load() {
        final Form form = shell.form();
        shell.show("Studio", page);
        Api.studio().titles(Net.to(list -> {
            if (!Nav.isHome(form)) {
                return;
            }
            page.removeAll();
            if (list == null || list.isEmpty()) {
                page.add(Ui.empty(FontImage.MATERIAL_VIDEO_CALL, "No titles yet",
                        "Create a title, upload its video and submit it for review."));
                page.add(Ui.primary("New title", "createFirst", e -> TitleEditor.create(form, this::load)));
            }
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                final StudioTitleDto t = list.get(iter);
                page.add(Ui.tap(Ui.row(FontImage.MATERIAL_MOVIE, t.name(), Lang.tr(t.status())
                        + Ui.DOT + Lang.tr("{0} videos", String.valueOf(t.videos() == null ? 0 : t.videos().size())),
                        Ui.badge(Lang.tr(t.status()), badge(t.status()))), "studioTitle-" + iter,
                        e -> TitleEditor.show(form, t.id(), this::load)));
            }
            page.revalidate();
        }, Ui.failed(page, this::load)));
    }

    static String badge(String status) {
        return "PUBLISHED".equals(status) || "READY".equals(status) ? "SvBadgeGood"
                : "REJECTED".equals(status) || "REMOVED".equals(status) || "FAILED".equals(status)
                || "BLOCKED".equals(status) ? "SvBadgeBad" : "SvBadge";
    }

    private void jobs() {
        final Container list = Ui.list("studioJobs");
        shell.show("Processing", list);
        Api.studio().jobs(Net.to(jobs -> {
            list.removeAll();
            if (jobs == null || jobs.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_DONE_ALL, "Nothing processing", null));
            }
            for (int iter = 0; jobs != null && iter < jobs.size(); iter++) {
                JobDto j = jobs.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_MEMORY, j.videoId(), Lang.tr(j.status()) + Ui.DOT
                        + j.progressPercent() + "%" + (j.error() == null || j.error().length() == 0 ? ""
                        : Ui.DOT + j.error()), Ui.badge(Lang.tr(j.status()), badge(j.status()))));
            }
            list.revalidate();
        }, Ui.failed(list, this::jobs)));
    }
}
