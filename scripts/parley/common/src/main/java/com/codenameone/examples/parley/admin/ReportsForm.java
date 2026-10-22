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
package com.codenameone.examples.parley.admin;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.api.ReasonDto;
import com.codenameone.examples.parley.api.ReportViewDto;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.ui.Lang;
import com.codenameone.examples.parley.ui.Ui;
import java.util.List;

/// What people reported, and what an admin decided.
///
/// A report carries only what its sender chose to attach: the server cannot
/// read a conversation, so the evidence is the text the reporter's own device
/// decrypted and sent along, and nothing else.
final class ReportsForm {
    private static final String[] FILTERS = {"OPEN", "RESOLVED"};
    private static final String[] FILTER_NAMES = {"Open", "Resolved"};

    private final Panes panes;
    private final Container list = Ui.list("reports");
    private String filter = "OPEN";

    private ReportsForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("PlPage");
        top.add(Ui.choice("filter", FILTER_NAMES, FILTERS, filter, value -> {
            filter = value;
            load();
        }));
        panes.list("Reports", "Reports", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        new ReportsForm(Panes.phone(previous));
    }

    private void load() {
        final String asked = filter;
        Api.admin().reports(asked, Net.to(found -> {
            if (asked.equals(filter)) {
                fill(found);
            }
        }, Ui.failed(list, this::load)));
    }

    private void fill(List<ReportViewDto> reports) {
        list.removeAll();
        for (int iter = 0; reports != null && iter < reports.size(); iter++) {
            final ReportViewDto report = reports.get(iter);
            Container row = Ui.row(FontImage.MATERIAL_REPORT, report.subject(),
                    report.reason() + Ui.DOT + Ui.when(report.createdAt()), null);
            list.add(Ui.tap(row, "report-" + report.id(), e -> open(report)));
        }
        if (list.getComponentCount() == 0) {
            list.add(Ui.empty(FontImage.MATERIAL_TASK_ALT, "OPEN".equals(filter)
                    ? "Nothing to review" : "Nothing resolved yet", null));
        }
        panes.refresh();
    }

    private void open(final ReportViewDto report) {
        Container page = Ui.page();
        page.add(Ui.fact("About", report.subject()));
        page.add(Ui.fact("From", report.reporter()));
        page.add(Ui.fact("When", Ui.when(report.createdAt())));
        page.add(Ui.fact("Status", "OPEN".equals(report.status()) ? "Open" : "Resolved"));
        Ui.section(page, "Reason");
        page.add(Ui.text(report.reason(), "PlRowDetail"));
        if (report.evidence() != null && report.evidence().length() > 0) {
            Ui.section(page, "What the reporter attached");
            SpanLabel evidence = Ui.text(report.evidence(), "PlNotice");
            evidence.setName("evidence");
            page.add(evidence);
        }
        page.add(Ui.secondary("Open the account", "openSubject",
                e -> UsersForm.show(panes, report.subject())));
        if ("OPEN".equals(report.status())) {
            page.add(Ui.primary("Resolve", "resolve", e -> panes.reason("Resolve report",
                    Lang.tr("What was done about it?"), "Resolve", why ->
                            Api.admin().resolve(report.id(), new ReasonDto(why), Net.to(done -> {
                                Telemetry.reportResolved("resolved");
                                // Past the reason and the report alike: the list again.
                                panes.home();
                                load();
                            }, Ui::fail)))));
        }
        panes.detail(report.subject(), "Report", page);
    }
}
