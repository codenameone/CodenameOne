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

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.api.CertificateDto;
import com.codenameone.examples.reel.api.CourseProgressDto;
import com.codenameone.examples.reel.api.DownloadDto;
import java.util.List;
import com.codenameone.examples.reel.api.HistoryEntryDto;
import com.codenameone.examples.reel.net.Account;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Device;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.player.PlayerForm;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Ui;

/// The profile's own things: its list, what it watched, this device's
/// downloads, and -- on the learning screen -- course progress and the
/// certificates earned.
public final class LibraryForm {
    private LibraryForm() {
    }

    public static void show(Form previous) {
        final Form form = Ui.form("My library", "Library");
        Ui.back(form, previous);
        final Container page = Ui.list("library");
        form.add(BorderLayout.CENTER, page);
        form.show();
        load(form, page);
    }

    private static void load(final Form form, final Container page) {
        Api.library().myList(Account.profileId(), Net.to(list -> {
            page.removeAll();
            if (list != null && !list.isEmpty()) {
                page.add(Cards.row(form, "myList", "My list", list));
            } else {
                page.add(Ui.empty(FontImage.MATERIAL_PLAYLIST_ADD, "Your list is empty",
                        "Add titles with the + on their page."));
            }
            final Container downloads = new Container(com.codename1.ui.layouts.BoxLayout.y());
            final Container history = new Container(com.codename1.ui.layouts.BoxLayout.y());
            page.add(downloads).add(history);
            loadDownloads(form, page, downloads);
            loadHistory(form, page, history);
            page.revalidate();
        }, Ui.failed(page, () -> load(form, page))));
    }

    private static void loadHistory(final Form form, final Container page, final Container into) {
        Api.library().history(Account.profileId(), Net.to(list -> {
            into.removeAll();
            if (list == null || list.isEmpty()) {
                return;
            }
            Ui.section(into, "Watched");
            for (int iter = 0; iter < list.size(); iter++) {
                final HistoryEntryDto h = list.get(iter);
                int percent = h.durationSeconds() <= 0 ? 0 : h.positionSeconds() * 100 / h.durationSeconds();
                into.add(Ui.tap(Ui.row(FontImage.MATERIAL_HISTORY, h.titleName(),
                        percent + "%" + Ui.DOT + Ui.when(h.watchedAt()),
                        Ui.flat(FontImage.MATERIAL_CLOSE, "forget-" + iter,
                                e -> Api.library().forget(h.titleId(), Account.profileId(),
                                        Net.to(ok -> load(form, page), Ui::fail)))),
                        "history-" + iter, e -> TitleForm.show(form, h.titleId())));
            }
            page.revalidate();
        }, Ui::fail));
    }

    /// Drawn from what this device keeps, so it works offline; the server's
    /// list then drops what it no longer licenses (revoked from another
    /// device, or expired).
    private static void loadDownloads(final Form form, final Container page, final Container into) {
        showDownloads(form, page, into);
        Api.library().downloads(Device.id(), Net.to(list -> {
            java.util.Set<String> live = new java.util.HashSet<String>();
            if (list != null) {
                for (int iter = 0; iter < list.size(); iter++) {
                    live.add(list.get(iter).id());
                }
            }
            List<DownloadDto> local = Downloads.list();
            for (int iter = 0; iter < local.size(); iter++) {
                if (!live.contains(local.get(iter).id())) {
                    Downloads.forget(local.get(iter).id());
                }
            }
            showDownloads(form, page, into);
        }, (code, message) -> { }));
    }

    private static void showDownloads(final Form form, final Container page, final Container into) {
        into.removeAll();
        List<DownloadDto> list = Downloads.list();
        if (!list.isEmpty()) {
            Ui.section(into, "Downloads");
        }
        for (int iter = 0; iter < list.size(); iter++) {
            final DownloadDto d = list.get(iter);
            boolean ready = Downloads.have(d.id());
            String state = ready ? Lang.tr("Until {0}", Ui.day(d.licenseExpiresAt())) : Lang.tr("Downloading");
            Container row = Ui.row(ready ? FontImage.MATERIAL_OFFLINE_PIN : FontImage.MATERIAL_DOWNLOADING,
                    d.titleName(), state, Ui.flat(FontImage.MATERIAL_DELETE_OUTLINE, "deleteDownload-" + iter,
                            e -> {
                                // The file goes now; the license is returned
                                // when the server can be reached.
                                Downloads.forget(d.id());
                                Api.library().deleteDownload(d.id(), Net.to(ok -> { }, (code, message) -> { }));
                                showDownloads(form, page, into);
                            }));
            into.add(ready ? Ui.tap(row, "playDownload-" + iter, e -> PlayerForm.offline(form, d)) : row);
        }
        page.revalidate();
    }

    /// Course progress and certificates.
    public static void learning(Form previous) {
        final Form form = Ui.form("Learning", "Learning");
        Ui.back(form, previous);
        final Container page = Ui.list("learning");
        form.add(BorderLayout.CENTER, page);
        form.show();
        Api.learning().progress(Account.profileId(), Net.to(courses -> {
            page.removeAll();
            if (courses == null || courses.isEmpty()) {
                page.add(Ui.empty(FontImage.MATERIAL_SCHOOL, "No courses started",
                        "Courses you watch show their progress here."));
            }
            for (int iter = 0; courses != null && iter < courses.size(); iter++) {
                final CourseProgressDto c = courses.get(iter);
                String detail = Lang.tr("{0} of {1} lessons", String.valueOf(c.lessonsCompleted()),
                        String.valueOf(c.lessonsTotal())) + Ui.DOT + Lang.tr("Quizzes {0}%",
                        String.valueOf(c.quizScorePercent()));
                page.add(Ui.tap(Ui.row(c.certificateEarned() ? FontImage.MATERIAL_WORKSPACE_PREMIUM
                        : FontImage.MATERIAL_SCHOOL, c.titleName(), detail, null),
                        "course-" + iter, e -> TitleForm.show(form, c.titleId())));
            }
            Api.learning().certificates(Net.to(certs -> {
                if (certs != null && !certs.isEmpty()) {
                    Ui.section(page, "Certificates");
                }
                for (int iter = 0; certs != null && iter < certs.size(); iter++) {
                    final CertificateDto cert = certs.get(iter);
                    page.add(Ui.tap(Ui.row(FontImage.MATERIAL_VERIFIED, cert.titleName(),
                            cert.scorePercent() + "%" + Ui.DOT + Ui.day(cert.issuedAt()) + Ui.DOT + cert.verifyCode(),
                            null), "certificate-" + iter, e -> CN.execute(Art.url("/verify/" + cert.verifyCode()))));
                }
                page.revalidate();
            }, Ui::fail));
        }, Ui.failed(page, null)));
    }
}
